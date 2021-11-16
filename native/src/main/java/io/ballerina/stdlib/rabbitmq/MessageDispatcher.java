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

package io.ballerina.stdlib.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObserverContext;
import io.ballerina.stdlib.rabbitmq.util.ModuleUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.FUNC_ON_MESSAGE;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.FUNC_ON_REQUEST;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.ORG_NAME;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.RABBITMQ;

/**
 * Handles and dispatched messages with data binding.
 *
 * @since 0.995
 */
public class MessageDispatcher {
    private final String consumerTag;
    private static final PrintStream console;
    private final Channel channel;
    private final boolean autoAck;
    private final BObject service;
    private final String queueName;
    private final BObject listenerObj;
    private final Runtime runtime;

    public MessageDispatcher(BObject service, Channel channel, boolean autoAck, Runtime runtime,
                             BObject listener) {
        this.channel = channel;
        this.autoAck = autoAck;
        this.service = service;
        this.queueName = getQueueNameFromConfig(service);
        this.consumerTag = service.getType().getName();
        this.runtime = runtime;
        this.listenerObj = listener;
    }

    private String getQueueNameFromConfig(BObject service) {
        if (service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue()) != null) {
            return (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
        } else {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> serviceConfig = (BMap<BString, Object>) ((AnnotatableType) service.getType())
                    .getAnnotation(StringUtils.fromString(ModuleUtils.getModule().getOrg() + ORG_NAME_SEPARATOR
                                                                  + ModuleUtils.getModule().getName() +
                                                                  VERSION_SEPARATOR
                                                                  + ModuleUtils.getModule().getVersion() + ":"
                                                                  + RabbitMQConstants.SERVICE_CONFIG));
            return serviceConfig.getStringValue(RabbitMQConstants.ALIAS_QUEUE_NAME).getValue();
        }
    }

    /**
     * Start receiving messages and dispatch the messages to the attached service.
     *
     * @param listener Listener object value.
     */
    public void receiveMessages(BObject listener) {
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) {
                handleDispatch(body, envelope, properties);
            }
        };
        try {
            channel.basicConsume(queueName, autoAck, consumerTag, consumer);
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            throw RabbitMQUtils.returnErrorValue("Error occurred while consuming messages; " +
                                                         exception.getMessage());
        }
        @SuppressWarnings("unchecked")
        ArrayList<BObject> startedServices =
                (ArrayList<BObject>) listener.getNativeData(RabbitMQConstants.STARTED_SERVICES);
        startedServices.add(service);
        service.addNativeData(RabbitMQConstants.QUEUE_NAME.getValue(), queueName);
    }

    private void handleDispatch(byte[] message, Envelope envelope, AMQP.BasicProperties properties) {
        if (properties.getReplyTo() != null && getAttachedFunctionType(service, FUNC_ON_REQUEST) != null) {
            MethodType onRequestFunction = getAttachedFunctionType(service, FUNC_ON_REQUEST);
            Type[] paramTypes = onRequestFunction.getParameterTypes();
            Type returnType = onRequestFunction.getReturnType();
            int paramSize = paramTypes.length;
            if (paramSize == 2) {
                dispatchMessageToOnRequest(message, getCallerBObject(envelope.getDeliveryTag()), envelope, properties,
                        returnType);
            } else if (paramSize == 1) {
                dispatchMessageToOnRequest(message, envelope, properties, returnType);
            } else {
                throw RabbitMQUtils.returnErrorValue("Invalid remote function signature");
            }
        } else {
            MethodType onMessageFunction = getAttachedFunctionType(service, FUNC_ON_MESSAGE);
            Type[] paramTypes = onMessageFunction.getParameterTypes();
            Type returnType = onMessageFunction.getReturnType();
            int paramSize = paramTypes.length;
            if (paramSize == 2) {
                dispatchMessage(message, getCallerBObject(envelope.getDeliveryTag()), envelope, properties, returnType);
            } else if (paramSize == 1) {
                dispatchMessage(message, envelope, properties, returnType);
            } else {
                throw RabbitMQUtils.returnErrorValue("Invalid remote function signature");
            }
        }
    }

    // dispatch only Message
    private void dispatchMessageToOnRequest(byte[] message, Envelope envelope, AMQP.BasicProperties properties,
                                            Type returnType) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length, properties.getReplyTo(),
                                                             envelope.getExchange());
            Object[] values = new Object[2];
            values[0] = createAndPopulateMessageRecord(message, envelope, properties);
            values[1] = true;
            executeResourceOnRequest(callback, returnType, values);
            countDownLatch.await();
        } catch (InterruptedException | AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
        }
    }

    // dispatch Message and Caller
    private void dispatchMessageToOnRequest(byte[] message, BObject caller, Envelope envelope,
                                            AMQP.BasicProperties properties, Type returnType) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length, properties.getReplyTo(),
                                                             envelope.getExchange());
            Object[] values = new Object[4];
            values[0] = createAndPopulateMessageRecord(message, envelope, properties);
            values[1] = true;
            values[2] = caller;
            values[3] = true;
            executeResourceOnRequest(callback, returnType, values);
            countDownLatch.await();
        } catch (InterruptedException | AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
        }
    }

    // dispatch only Message
    private void dispatchMessage(byte[] message, Envelope envelope, AMQP.BasicProperties properties, Type returnType) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length);
            Object[] values = new Object[2];
            values[0] = createAndPopulateMessageRecord(message, envelope, properties);
            values[1] = true;
            executeResourceOnMessage(callback, returnType, values);
            countDownLatch.await();
        } catch (InterruptedException | AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
        }
    }

    // dispatch Message and Caller
    private void dispatchMessage(byte[] message, BObject caller, Envelope envelope, AMQP.BasicProperties properties,
                                 Type returnType) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length);
            Object[] values = new Object[4];
            values[0] = createAndPopulateMessageRecord(message, envelope, properties);
            values[1] = true;
            values[2] = caller;
            values[3] = true;
            executeResourceOnMessage(callback, returnType, values);
            countDownLatch.await();
        } catch (InterruptedException | AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
        }
    }

    private static BMap<BString, Object> createAndPopulateMessageRecord(byte[] message, Envelope envelope,
                                                                        AMQP.BasicProperties properties) {
        Object[] values = new Object[5];
        values[0] = ValueCreator.createArrayValue(message);
        values[1] = envelope.getRoutingKey();
        values[2] = envelope.getExchange();
        values[3] = envelope.getDeliveryTag();
        if (properties != null) {
            String replyTo = properties.getReplyTo();
            String contentType = properties.getContentType();
            String contentEncoding = properties.getContentEncoding();
            String correlationId = properties.getCorrelationId();
            BMap<BString, Object> basicProperties =
                    ValueCreator.createRecordValue(ModuleUtils.getModule(),
                                                   RabbitMQConstants.RECORD_BASIC_PROPERTIES);
            Object[] propValues = new Object[4];
            propValues[0] = replyTo;
            propValues[1] = contentType;
            propValues[2] = contentEncoding;
            propValues[3] = correlationId;
            values[4] = ValueCreator.createRecordValue(basicProperties, propValues);
        }
        BMap<BString, Object> messageRecord = ValueCreator.createRecordValue(ModuleUtils.getModule(),
                                                                             RabbitMQConstants.MESSAGE_RECORD);
        return ValueCreator.createRecordValue(messageRecord, values);
    }

    private BObject getCallerBObject(long deliveryTag) {
        BObject callerObj = ValueCreator.createObjectValue(ModuleUtils.getModule(),
                                                           RabbitMQConstants.CALLER_OBJECT);
        RabbitMQTransactionContext transactionContext =
                (RabbitMQTransactionContext) listenerObj.getNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT);
        callerObj.addNativeData(RabbitMQConstants.DELIVERY_TAG.getValue(), deliveryTag);
        callerObj.addNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT, channel);
        callerObj.addNativeData(RabbitMQConstants.ACK_MODE, autoAck);
        callerObj.addNativeData(RabbitMQConstants.ACK_STATUS, false);
        callerObj.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT, transactionContext);
        return callerObj;
    }

    private void executeResourceOnMessage(Callback callback, Type returnType, Object... args) {
        StrandMetadata metadata = new StrandMetadata(ORG_NAME, RABBITMQ,
                                                     ModuleUtils.getModule().getVersion(), FUNC_ON_MESSAGE);
        executeResource(RabbitMQConstants.FUNC_ON_MESSAGE, callback, metadata, returnType, args);
    }

    private void executeResourceOnRequest(Callback callback, Type returnType, Object... args) {
        StrandMetadata metadata = new StrandMetadata(ORG_NAME, RABBITMQ,
                                                     ModuleUtils.getModule().getVersion(), FUNC_ON_REQUEST);
        executeResource(FUNC_ON_REQUEST, callback, metadata, returnType, args);
    }

    private void executeResource(String function, Callback callback, StrandMetadata metaData, Type returnType,
                                 Object... args) {
        if (ObserveUtils.isTracingEnabled()) {
            runtime.invokeMethodAsync(service, function, null, metaData, callback, getNewObserverContextInProperties(),
                                      returnType, args);
            return;
        }
        runtime.invokeMethodAsync(service, function, null, metaData, callback, args);
    }

    private Map<String, Object> getNewObserverContextInProperties() {
        Map<String, Object> properties = new HashMap<>();
        RabbitMQObserverContext observerContext = new RabbitMQObserverContext(channel);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_QUEUE, queueName);
        properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
        return properties;
    }

    public static MethodType getAttachedFunctionType(BObject serviceObject, String functionName) {
        MethodType function = null;
        MethodType[] resourceFunctions = serviceObject.getType().getMethods();
        for (MethodType resourceFunction : resourceFunctions) {
            if (functionName.equals(resourceFunction.getName())) {
                function = resourceFunction;
                break;
            }
        }
        return function;
    }

    static {
        console = System.out;
    }
}
