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

package org.ballerinalang.messaging.rabbitmq.util;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.messaging.rabbitmq.MessageDispatcher;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQTransactionContext;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;

/**
 * Util class for RabbitMQ Listener actions handling.
 *
 * @since 1.1.0
 */
public class ListenerUtils {
    private static final PrintStream console;
    private static boolean started = false;
    private static ArrayList<BObject> services = new ArrayList<>();
    private static ArrayList<BObject> startedServices = new ArrayList<>();
    private static Runtime runtime;
    private static final BString IO_ERROR_MSG = StringUtils
            .fromString("An I/O error occurred while setting the global quality of service settings for the listener");

    public static void init(BObject listenerBObject, BMap<BString, Object> connectionConfig) {
        Connection connection = ConnectionUtils.createConnection(connectionConfig);
        Channel channel = null;
        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            RabbitMQMetricsUtil.reportError(connection, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CREATE);
            throw RabbitMQUtils.returnErrorValue("Error occurred while initializing the listener: "
                                                         + e.getMessage());
        }
        String connectorId = listenerBObject.getStringValue(RabbitMQConstants.CONNECTOR_ID).getValue();
        listenerBObject.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT,
                                 new RabbitMQTransactionContext(channel, connectorId));
        listenerBObject.addNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT, channel);
        listenerBObject.addNativeData(RabbitMQConstants.CONSUMER_SERVICES, services);
        listenerBObject.addNativeData(RabbitMQConstants.STARTED_SERVICES, startedServices);
        RabbitMQMetricsUtil.reportNewConsumer(channel);
    }

    public static Object registerListener(Environment environment, BObject listenerBObject, BObject service) {
        runtime = environment.getRuntime();
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        if (service == null) {
            return null;
        }
        try {
            declareQueueIfNotExists(service, channel);
        } catch (IOException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_REGISTER);
            return RabbitMQUtils.returnErrorValue("I/O Error occurred while declaring the queue: " +
                                                          e.getMessage());
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
        String serviceName = service.getType().getName();
        String queueName = (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
        try {
            channel.basicCancel(serviceName);
            console.println("[ballerina/rabbitmq] Consumer service unsubscribed from the queue " + queueName);
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
        BMap serviceConfig = (BMap) ((AnnotatableType) service.getType())
                .getAnnotation(StringUtils.fromString(ModuleUtils.getModule().getOrg() + ORG_NAME_SEPARATOR
                                                              + ModuleUtils.getModule().getName() + VERSION_SEPARATOR
                                                              + ModuleUtils.getModule().getVersion() + ":"
                                                              + RabbitMQConstants.SERVICE_CONFIG));
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        String queueName = serviceConfig.getStringValue(RabbitMQConstants.QUEUE_NAME).getValue();
        channel.queueDeclare(queueName, false, false, true, null);
        RabbitMQMetricsUtil.reportNewQueue(channel, queueName);
    }

    public static Object setQosSettings(int prefetchCount, Object prefetchSize, boolean global,
                                        BObject listenerBObject) {
        Channel channel = (Channel) listenerBObject.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        boolean isValidSize = prefetchSize != null && RabbitMQUtils.checkIfInt(prefetchSize);
        try {
            if (isValidSize) {
                channel.basicQos(Math.toIntExact(((Number) prefetchSize).longValue()),
                                 prefetchCount,
                                 global);
            } else {
                channel.basicQos(prefetchCount, global);
            }
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_SET_QOS);
            return ErrorCreator.createError(IO_ERROR_MSG);
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
        BMap serviceConfig = (BMap) ((AnnotatableType) service.getType())
                .getAnnotation(StringUtils.fromString(ModuleUtils.getModule().getOrg() + ORG_NAME_SEPARATOR
                                                              + ModuleUtils.getModule().getName() + VERSION_SEPARATOR
                                                              + ModuleUtils.getModule().getVersion() + ":"
                                                              + RabbitMQConstants.SERVICE_CONFIG));
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        boolean autoAck = serviceConfig.getBooleanValue(RabbitMQConstants.AUTO_ACK);
        return autoAck;
    }

    public static Object stop(BObject listenerBObject) {
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
            } catch (IOException | TimeoutException exception) {
                return RabbitMQUtils.returnErrorValue(RabbitMQConstants.CLOSE_CHANNEL_ERROR
                                                              + exception.getMessage());
            }
        }
        return null;
    }

    public static Object abortConnection(BObject listenerBObject) {
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

    static {
        console = System.out;
    }
}
