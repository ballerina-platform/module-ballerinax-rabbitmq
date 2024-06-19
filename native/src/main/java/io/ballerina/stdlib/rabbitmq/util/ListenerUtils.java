/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.rabbitmq.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownSignalException;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.rabbitmq.MessageDispatcher;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;
import io.ballerina.stdlib.rabbitmq.RabbitMQTransactionContext;
import io.ballerina.stdlib.rabbitmq.RabbitMQUtils;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.CONSTRAINT_VALIDATION;

/**
 * Util class for RabbitMQ Listener actions handling.
 *
 * @since 1.1.0
 */
public class ListenerUtils {
    private static boolean started = false;
    private static ArrayList<BObject> services = new ArrayList<>();
    private static ArrayList<BObject> startedServices = new ArrayList<>();
    private static Runtime runtime;
    private static final BString IO_ERROR_MSG = StringUtils
            .fromString("An I/O error occurred while setting the global quality of service settings for the listener");

    public static Object init(BString host, long port, BObject listenerBObject,
                              BMap<BString, Object> connectionConfig) {
        Object result = ConnectionUtils.createConnection(host, port, connectionConfig);
        if (result instanceof Connection) {
            Connection connection = (Connection) result;
            Channel channel;
            try {
                channel = connection.createChannel();
            } catch (IOException e) {
                RabbitMQMetricsUtil.reportError(connection, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CREATE);
                return RabbitMQUtils.returnErrorValue("Error occurred while initializing the listener: "
                        + e.getMessage());
            }
            String connectorId = listenerBObject.getStringValue(RabbitMQConstants.CONNECTOR_ID).getValue();
            listenerBObject.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT,
                    new RabbitMQTransactionContext(channel, connectorId));
            listenerBObject.addNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT, channel);
            listenerBObject.addNativeData(RabbitMQConstants.CONSUMER_SERVICES, services);
            listenerBObject.addNativeData(RabbitMQConstants.STARTED_SERVICES, startedServices);
            listenerBObject.addNativeData(CONSTRAINT_VALIDATION,
                    connectionConfig.getBooleanValue(StringUtils.fromString(CONSTRAINT_VALIDATION)));
            RabbitMQMetricsUtil.reportNewConsumer(channel);
            return null;
        }
        return result;
    }

    public static Object attach(Environment environment, BObject listenerBObject, BObject service,
                                Object queueName) {
        runtime = environment.getRuntime();
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        if (service == null) {
            return null;
        }
        if (queueName != null && TypeUtils.getType(queueName).getTag() == TypeTags.STRING_TAG) {
            service.addNativeData(RabbitMQConstants.QUEUE_NAME.getValue(), ((BString) queueName).getValue());
        }
        try {
            declareQueueIfNotExists(service, channel);
        } catch (IOException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_REGISTER);
            return RabbitMQUtils.returnErrorValue("I/O Error occurred while declaring the queue: " +
                    e.getCause().getMessage());
        }
        if (isStarted()) {
            services =
                    (ArrayList<BObject>) listenerBObject.getNativeData(RabbitMQConstants.CONSUMER_SERVICES);
            startReceivingMessages(service, channel, listenerBObject);
        }
        services.add(service);
        return null;
    }

    public static Object start(Environment environment, BObject listenerBObject) {
        runtime = environment.getRuntime();
        boolean autoAck;
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        ArrayList<BObject> services =
                (ArrayList<BObject>) listenerBObject.getNativeData(RabbitMQConstants.CONSUMER_SERVICES);
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        ArrayList<BObject> startedServices =
                (ArrayList<BObject>) listenerBObject.getNativeData(RabbitMQConstants.STARTED_SERVICES);
        if (services == null || services.isEmpty()) {
            return null;
        }
        for (BObject service : services) {
            if (startedServices == null || !startedServices.contains(service)) {
                autoAck = getAckMode(service);
                MessageDispatcher messageDispatcher =
                        new MessageDispatcher(service, channel, autoAck, runtime, listenerBObject);
                messageDispatcher.receiveMessages(listenerBObject);
                RabbitMQMetricsUtil.reportSubscription(channel, service);
            }
        }
        started = true;
        return null;
    }

    public static Object detach(Environment environment, BObject listenerBObject, BObject service) {
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        ArrayList<BObject> startedServices =
                (ArrayList<BObject>) listenerBObject.getNativeData(RabbitMQConstants.STARTED_SERVICES);
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        ArrayList<BObject> services =
                (ArrayList<BObject>) listenerBObject.getNativeData(RabbitMQConstants.CONSUMER_SERVICES);
        String serviceName = TypeUtils.getType(service).getName();
        String queueName = (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
        try {
            channel.basicCancel(serviceName);
        } catch (IOException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_DETACH);
            return RabbitMQUtils.returnErrorValue("Error occurred while detaching the service");
        }
        listenerBObject.addNativeData(RabbitMQConstants.CONSUMER_SERVICES,
                RabbitMQUtils.removeFromList(services, service));
        listenerBObject.addNativeData(RabbitMQConstants.STARTED_SERVICES,
                RabbitMQUtils.removeFromList(startedServices, service));
        RabbitMQMetricsUtil.reportUnsubscription(channel, service);
        RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName, environment);
        return null;
    }

    private static void declareQueueIfNotExists(BObject service, Channel channel) throws IOException {
        BMap serviceConfig = (BMap) ((AnnotatableType) TypeUtils.getType(service))
                .getAnnotation(StringUtils.fromString(ModuleUtils.getModule().getOrg() + ORG_NAME_SEPARATOR
                        + ModuleUtils.getModule().getName() + VERSION_SEPARATOR
                        + ModuleUtils.getModule().getVersion() + ":"
                        + RabbitMQConstants.SERVICE_CONFIG));
        String queueName = "";
        Map<String, Object> argumentsMap = new HashMap<>();
        boolean durable = false;
        boolean exclusive = false;
        boolean autoDelete = true;


        if (service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue()) != null) {
            // if the queue name is given as the service name
            queueName = (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
        }

        // if serviceConfig is not null, name and configs given in the service config will replace the service name
        if (serviceConfig != null) {
            queueName = serviceConfig.getStringValue(RabbitMQConstants.QUEUE_NAME).getValue();

            if ((BMap<BString, Object>) serviceConfig.getMapValue(RabbitMQConstants.QUEUE_CONFIG) != null) {

                @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                BMap<BString, Object> queueConfig =
                        (BMap<BString, Object>) serviceConfig.getMapValue(RabbitMQConstants.QUEUE_CONFIG);
                durable = queueConfig.getBooleanValue(RabbitMQConstants.QUEUE_DURABLE);
                exclusive = queueConfig.getBooleanValue(RabbitMQConstants.QUEUE_EXCLUSIVE);
                autoDelete = queueConfig.getBooleanValue(RabbitMQConstants.QUEUE_AUTO_DELETE);
                if (queueConfig.containsKey(RabbitMQConstants.QUEUE_ARGUMENTS)) {
                    @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                    HashMap<BString, Object> queueArgs =
                            (HashMap<BString, Object>) queueConfig.getMapValue(RabbitMQConstants.QUEUE_ARGUMENTS);
                    queueArgs.forEach((k, v) -> argumentsMap.put(k.getValue(), ChannelUtils.getConvertedValue(v)));
                }
            }
        }

        // declare queue with user given values or default set
        channel.queueDeclare(queueName, durable, exclusive, autoDelete, null);
        RabbitMQMetricsUtil.reportNewQueue(channel, queueName);
    }

    public static Object setQosSettings(long prefetchCount, Object prefetchSize, boolean global,
                                        BObject listenerBObject) {
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        boolean isValidSize = prefetchSize != null && RabbitMQUtils.checkIfInt(prefetchSize);
        try {
            if (isValidSize) {
                channel.basicQos(((Long) prefetchSize).intValue(), (int) prefetchCount, global);
            } else {
                channel.basicQos((int) prefetchCount, global);
            }
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_SET_QOS);
            return RabbitMQUtils.returnErrorValue(IO_ERROR_MSG.getValue());
        }
        return null;
    }

    private static void startReceivingMessages(BObject service, Channel channel, BObject listener) {
        MessageDispatcher messageDispatcher =
                new MessageDispatcher(service, channel, getAckMode(service), runtime, listener);
        messageDispatcher.receiveMessages(listener);

    }

    private static boolean isStarted() {
        return started;
    }

    private static boolean getAckMode(BObject service) {
        ObjectType serviceType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
        @SuppressWarnings("unchecked")
        BMap<BString, Object> serviceConfig = (BMap<BString, Object>) serviceType
                .getAnnotation(StringUtils.fromString(ModuleUtils.getModule().getOrg() + ORG_NAME_SEPARATOR
                        + ModuleUtils.getModule().getName() + VERSION_SEPARATOR
                        + ModuleUtils.getModule().getVersion() + ":"
                        + RabbitMQConstants.SERVICE_CONFIG));
        boolean autoAck = true;
        if (serviceConfig != null && serviceConfig.containsKey(RabbitMQConstants.AUTO_ACK)) {
            autoAck = serviceConfig.getBooleanValue(RabbitMQConstants.AUTO_ACK);
        }
        return autoAck;
    }

    public static Object gracefulStop(BObject listenerBObject) {
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        if (channel == null) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_STOP);
            return RabbitMQUtils.returnErrorValue("ChannelListener is not properly initialised.");
        } else {
            try {
                Connection connection = channel.getConnection();
                RabbitMQMetricsUtil.reportBulkUnsubscription(channel, listenerBObject);
                RabbitMQMetricsUtil.reportConsumerClose(channel);
                RabbitMQMetricsUtil.reportChannelClose(channel);
                RabbitMQMetricsUtil.reportConnectionClose(connection);
                channel.close();
                connection.close();
            } catch (IOException | TimeoutException | ShutdownSignalException exception) {
                return RabbitMQUtils.returnErrorValue(RabbitMQConstants.CLOSE_CHANNEL_ERROR
                        + exception.getMessage());
            }
        }
        return null;
    }

    public static Object immediateStop(BObject listenerBObject) {
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        if (channel == null) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_CONNECTION_ABORT);
            return RabbitMQUtils.returnErrorValue("ChannelListener is not properly initialised.");
        } else {
            Connection connection = channel.getConnection();
            RabbitMQMetricsUtil.reportBulkUnsubscription(channel, listenerBObject);
            RabbitMQMetricsUtil.reportConsumerClose(channel);
            RabbitMQMetricsUtil.reportChannelClose(channel);
            RabbitMQMetricsUtil.reportConnectionClose(connection);
            connection.abort();
        }
        return null;
    }
}
