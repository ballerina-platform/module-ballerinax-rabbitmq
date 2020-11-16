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

package org.ballerinalang.messaging.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.StructureType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObserverContext;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.messaging.rabbitmq.RabbitMQConstants.FUNC_ON_ERROR;
import static org.ballerinalang.messaging.rabbitmq.RabbitMQConstants.FUNC_ON_MESSAGE;
import static org.ballerinalang.messaging.rabbitmq.RabbitMQConstants.ORG_NAME;
import static org.ballerinalang.messaging.rabbitmq.RabbitMQConstants.RABBITMQ;
import static org.ballerinalang.messaging.rabbitmq.RabbitMQConstants.RABBITMQ_VERSION;

/**
 * Handles and dispatched messages with data binding.
 *
 * @since 0.995
 */
public class MessageDispatcher {
    private String consumerTag;
    private static final PrintStream console;
    private Channel channel;
    private boolean autoAck;
    private BObject service;
    private BObject channelObj;
    private String queueName;
    private Runtime runtime;
    private static final StrandMetadata ON_MESSAGE_METADATA = new StrandMetadata(ORG_NAME, RABBITMQ,
                                                                                 RABBITMQ_VERSION, FUNC_ON_MESSAGE);
    private static final StrandMetadata ON_ERROR_METADATA = new StrandMetadata(ORG_NAME, RABBITMQ, RABBITMQ_VERSION,
                                                                               FUNC_ON_ERROR);

    public MessageDispatcher(BObject service, Channel channel, boolean autoAck, Runtime runtime,
                             BObject channelObj) {
        this.channel = channel;
        this.autoAck = autoAck;
        this.service = service;
        this.queueName = getQueueNameFromConfig(service);
        this.consumerTag = service.getType().getName();
        this.runtime = runtime;
        this.channelObj = channelObj;
    }

    private String getQueueNameFromConfig(BObject service) {
        BMap serviceConfig = (BMap) ((AnnotatableType) service.getType())
                .getAnnotation(StringUtils.fromString(RabbitMQConstants.PACKAGE_RABBITMQ_FQN + ":"
                               + RabbitMQConstants.SERVICE_CONFIG));
        @SuppressWarnings(RabbitMQConstants.UNCHECKED)
        BMap<BString, Object> queueConfig =
                (BMap) serviceConfig.getMapValue(RabbitMQConstants.ALIAS_QUEUE_CONFIG);
        return queueConfig.getStringValue(RabbitMQConstants.QUEUE_NAME).getValue();
    }

    /**
     * Start receiving messages and dispatch the messages to the attached service.
     *
     * @param listener Listener object value.
     */
    public void receiveMessages(BObject listener) {
        console.println("[ballerina/rabbitmq] Consumer service started for queue " + queueName);
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) {
                handleDispatch(body, envelope.getDeliveryTag(), properties);
            }
        };
        try {
            channel.basicConsume(queueName, autoAck, consumerTag, consumer);
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            throw RabbitMQUtils.returnErrorValue("Error occurred while consuming messages; " +
                                                         exception.getMessage());
        }
        ArrayList<BObject> startedServices =
                (ArrayList<BObject>) listener.getNativeData(RabbitMQConstants.STARTED_SERVICES);
        startedServices.add(service);
        service.addNativeData(RabbitMQConstants.QUEUE_NAME.getValue(), queueName);
    }

    private void handleDispatch(byte[] message, long deliveryTag, AMQP.BasicProperties properties) {
        AttachedFunctionType[] attachedFunctions = service.getType().getAttachedFunctions();
        AttachedFunctionType onMessageFunction;
        if (FUNC_ON_MESSAGE.equals(attachedFunctions[0].getName())) {
            onMessageFunction = attachedFunctions[0];
        } else if (FUNC_ON_MESSAGE.equals(attachedFunctions[1].getName())) {
            onMessageFunction = attachedFunctions[1];
        } else {
            return;
        }
        Type[] paramTypes = onMessageFunction.getParameterTypes();
        int paramSize = paramTypes.length;
        if (paramSize > 1) {
            dispatchMessageWithDataBinding(message, deliveryTag, onMessageFunction, properties);
        } else {
            dispatchMessage(message, deliveryTag, properties);
        }
    }

    private void dispatchMessage(byte[] message, long deliveryTag, AMQP.BasicProperties properties) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length);
            BObject messageBObject = getMessageBObject(message, deliveryTag, properties);
            executeResourceOnMessage(callback, messageBObject, true);
            countDownLatch.await();
        } catch (InterruptedException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            Thread.currentThread().interrupt();
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.THREAD_INTERRUPTED);
        } catch (AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            handleError(message, deliveryTag, properties);
        }
    }

    private void dispatchMessageWithDataBinding(byte[] message, long deliveryTag, AttachedFunctionType onMessage,
                                                AMQP.BasicProperties properties) {
        Type[] paramTypes = onMessage.getParameterTypes();
        try {
            Object forContent = getMessageContentForType(message, paramTypes[1]);
            BObject messageBObject = getMessageBObject(message, deliveryTag, properties);
            CountDownLatch countDownLatch = new CountDownLatch(1);
            Callback callback = new RabbitMQResourceCallback(countDownLatch, channel, queueName,
                                                             message.length);
            executeResourceOnMessage(callback, messageBObject,
                                     true, forContent, true);
            countDownLatch.await();
        } catch (BError | UnsupportedEncodingException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            handleError(message, deliveryTag, properties);
        } catch (InterruptedException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            Thread.currentThread().interrupt();
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.THREAD_INTERRUPTED);
        }
    }

    private Object getMessageContentForType(byte[] message, Type dataType) throws UnsupportedEncodingException {
        int dataTypeTag = dataType.getTag();
        switch (dataTypeTag) {
            case TypeTags.STRING_TAG:
                return StringUtils.fromString(new String(message, StandardCharsets.UTF_8.name()));
            case TypeTags.JSON_TAG:
                return JsonUtils.parse(new String(message, StandardCharsets.UTF_8.name()));
            case TypeTags.XML_TAG:
                return XmlUtils.parse(new String(message, StandardCharsets.UTF_8.name()));
            case TypeTags.FLOAT_TAG:
                return Float.parseFloat(new String(message, StandardCharsets.UTF_8.name()));
            case TypeTags.INT_TAG:
                return Integer.parseInt(new String(message, StandardCharsets.UTF_8.name()));
            case TypeTags.RECORD_TYPE_TAG:
                return JsonUtils.convertJSONToRecord(JsonUtils.parse(new String(message,
                                                                                StandardCharsets.UTF_8.name())),
                                                     (StructureType) dataType);
            case TypeTags.ARRAY_TAG:
                if (((BArray) dataType).getElementType().getTag() == TypeTags.BYTE_TAG) {
                    return message;
                } else {
                    RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
                    throw RabbitMQUtils.returnErrorValue("Only type byte[] is supported in data binding.");
                }
            default:
                RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
                throw RabbitMQUtils.returnErrorValue(
                        "The content type of the message received does not match the resource signature type.");
        }
    }

    private BObject getMessageBObject(byte[] message, long deliveryTag, AMQP.BasicProperties properties) {
        BObject messageBObject = ValueCreator.createObjectValue(RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                                RabbitMQConstants.MESSAGE_OBJECT);
        RabbitMQTransactionContext transactionContext =
                (RabbitMQTransactionContext) channelObj.getNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT);
        messageBObject.set(RabbitMQConstants.DELIVERY_TAG, deliveryTag);
        messageBObject.set(RabbitMQConstants.JAVA_CLIENT_CHANNEL, ValueCreator.createHandleValue(channel));
        messageBObject.set(RabbitMQConstants.MESSAGE_CONTENT, ValueCreator.createArrayValue(message));
        messageBObject.set(RabbitMQConstants.AUTO_ACK_STATUS, autoAck);
        messageBObject.set(RabbitMQConstants.MESSAGE_ACK_STATUS, false);
        messageBObject.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT, transactionContext);
        if (properties != null) {
            String replyTo = properties.getReplyTo();
            String contentType = properties.getContentType();
            String contentEncoding = properties.getContentEncoding();
            String correlationId = properties.getCorrelationId();
            BMap<BString, Object> basicProperties =
                    ValueCreator.createRecordValue(RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                   RabbitMQConstants.RECORD_BASIC_PROPERTIES);
            Object[] values = new Object[4];
            values[0] = replyTo;
            values[1] = contentType;
            values[2] = contentEncoding;
            values[3] = correlationId;
            messageBObject.set(RabbitMQConstants.BASIC_PROPERTIES,
                               ValueCreator.createRecordValue(basicProperties, values));
        }
        return messageBObject;
    }

    private void handleError(byte[] message, long deliveryTag, AMQP.BasicProperties properties) {
        BError error = RabbitMQUtils.returnErrorValue(RabbitMQConstants.DISPATCH_ERROR);
        BObject messageBObject = getMessageBObject(message, deliveryTag, properties);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Callback callback = new RabbitMQErrorResourceCallback(countDownLatch);
            executeResourceOnError(callback, messageBObject, true, error, true);
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.THREAD_INTERRUPTED);
        } catch (AlreadyClosedException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CONSUME);
            throw RabbitMQUtils.returnErrorValue("Error occurred in RabbitMQ service. ");
        }
    }

    private void executeResourceOnMessage(Callback callback, Object... args) {
        executeResource(RabbitMQConstants.FUNC_ON_MESSAGE, callback, ON_MESSAGE_METADATA, args);
    }

    private void executeResourceOnError(Callback callback, Object... args) {
        executeResource(RabbitMQConstants.FUNC_ON_ERROR, callback, ON_ERROR_METADATA, args);
    }

    private void executeResource(String function, Callback callback, StrandMetadata metaData,
                                 Object... args) {
        if (ObserveUtils.isTracingEnabled()) {
            runtime.invokeMethodAsync(service, function, null, metaData, callback, getNewObserverContextInProperties(),
                                      args);
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

    static {
        console = System.out;
    }
}
