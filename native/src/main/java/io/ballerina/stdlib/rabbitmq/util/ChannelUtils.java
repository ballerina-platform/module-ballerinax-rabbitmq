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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.runtime.transactions.TransactionResourceManager;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;
import io.ballerina.stdlib.rabbitmq.RabbitMQTransactionContext;
import io.ballerina.stdlib.rabbitmq.RabbitMQUtils;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Util class for RabbitMQ Channel handling.
 *
 * @since 0.995.0
 */
public class ChannelUtils {
    public static Object createChannel(BString host, long port, BObject channelObj,
                                       BMap<BString, Object> connectionConfig) {
        Object result = ConnectionUtils.createConnection(host, port, connectionConfig);
        if (result instanceof Connection) {
            Connection connection = (Connection) result;
            try {
                Channel channel = connection.createChannel();
                RabbitMQMetricsUtil.reportNewChannel(channel);
                String connectorId = channelObj.getStringValue(RabbitMQConstants.CONNECTOR_ID).getValue();
                channelObj.addNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT, channel);
                channelObj.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT,
                        new RabbitMQTransactionContext(channel, connectorId));
                return null;
            } catch (IOException exception) {
                RabbitMQMetricsUtil.reportError(connection, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CREATE);
                return RabbitMQUtils.returnErrorValue("Error occurred while initializing the channel: "
                        + exception.getMessage());
            }
        }
        return result;
    }

    public static Object queueDeclare(Environment environment, BObject clientObj,
                                      BString queueName, Object queueConfig) {
        boolean durable = false;
        boolean exclusive = false;
        boolean autoDelete = true;
        Map<String, Object> argumentsMap = null;
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            if (queueConfig != null) {
                @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                BMap<BString, Object> config = (BMap<BString, Object>) queueConfig;
                durable = config.getBooleanValue(RabbitMQConstants.QUEUE_DURABLE);
                exclusive = config.getBooleanValue(RabbitMQConstants.QUEUE_EXCLUSIVE);
                autoDelete = config.getBooleanValue(RabbitMQConstants.QUEUE_AUTO_DELETE);
                if (config.getMapValue(RabbitMQConstants.QUEUE_ARGUMENTS) != null) {
                    argumentsMap = (HashMap<String, Object>) config.getMapValue(RabbitMQConstants.QUEUE_ARGUMENTS);
                }
            }
            channel.queueDeclare(queueName.getValue(), durable, exclusive, autoDelete, argumentsMap);
            RabbitMQMetricsUtil.reportNewQueue(channel, queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue(), environment);
        } catch (IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueAutoGenerate(BObject clientObj) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            AMQP.Queue.DeclareOk result = channel.queueDeclare();
            return StringUtils.fromString(result.getQueue());
        } catch (IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DECLARE);
            return RabbitMQUtils.returnErrorValue("error occurred while declaring the queue: "
                                                          + exception.getMessage());
        }
    }

    public static Object consumeMessage(BObject clientObj, BString queueName, boolean ackMode, BTypedesc bTypedesc) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            GetResponse response = channel.basicGet(queueName.getValue(), ackMode);
            if (Objects.isNull(response)) {
                return RabbitMQUtils.returnErrorValue("No messages are found in the queue.");
            }
            RecordType recordType = RabbitMQUtils.getRecordType(bTypedesc);
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(recordType);
            Map<String, Field> fieldMap = recordType.getFields();
            Type contentType = fieldMap.get(RabbitMQConstants.MESSAGE_CONTENT_FIELD).getFieldType();

            return createAndPopulateMessageRecord(response.getBody(), response.getEnvelope(),
                                                                    response.getProps(), msgRecord, contentType);
        } catch (IOException | ShutdownSignalException | BError e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_BASIC_GET);
            return RabbitMQUtils.returnErrorValue("error occurred while retrieving the message: " +
                                                          e.getMessage());
        }
    }

    private static BMap<BString, Object> createAndPopulateMessageRecord(byte[] message, Envelope envelope,
                                                                        AMQP.BasicProperties properties,
                                                                        BMap<BString, Object> msgRecord,
                                                                        Type content) {
        Object[] values = new Object[5];
        Object messageContent = RabbitMQUtils.getValueWithIntendedType(content, message);
        if (messageContent instanceof BError) {
            throw (BError) messageContent;
        }
        values[0] = messageContent;
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
        return ValueCreator.createRecordValue(msgRecord, values);
    }

    public static Object basicAck(Environment environment, BObject clientObj, BMap<BString, Object> message,
                                  boolean multiple) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        int deliveryTag =
                Integer.parseInt(message.getIntValue(RabbitMQConstants.DELIVERY_TAG).toString());
        try {
            channel.basicAck(deliveryTag, multiple);
            RabbitMQMetricsUtil.reportAcknowledgement(channel, RabbitMQObservabilityConstants.ACK);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
        } catch (IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_ACK);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.ACK_ERROR + exception.getMessage());
        }
        return null;
    }

    public static Object basicNack(Environment environment, BObject clientObj, BMap<BString, Object> message,
                                   boolean multiple, boolean requeue) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        int deliveryTag = (int) ((long) message.getIntValue(RabbitMQConstants.DELIVERY_TAG));
        try {
            channel.basicNack(deliveryTag, multiple, requeue);
            RabbitMQMetricsUtil.reportAcknowledgement(channel, RabbitMQObservabilityConstants.NACK);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
        } catch (IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_NACK);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.NACK_ERROR
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDeclare(Environment environment, BObject clientObj, BString exchangeName,
                                         BString exchangeType, Object exchangeConfig) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        boolean durable = false;
        boolean autoDelete = true;
        Map<String, Object> argumentsMap = null;
        RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
        try {
            if (exchangeConfig != null) {
                @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                BMap<BString, Object> config = (BMap<BString, Object>) exchangeConfig;
                durable = config.getBooleanValue(RabbitMQConstants.EXCHANGE_DURABLE);
                autoDelete = config.getBooleanValue(RabbitMQConstants.EXCHANGE_AUTO_DELETE);
                if (config.getMapValue(RabbitMQConstants.EXCHANGE_ARGUMENTS) != null) {
                    argumentsMap =
                            (HashMap<String, Object>) config.getMapValue(RabbitMQConstants.EXCHANGE_ARGUMENTS);
                }
            }
            channel.exchangeDeclare(exchangeName.getValue(), exchangeType.getValue(), durable, autoDelete,
                                    argumentsMap);
            RabbitMQMetricsUtil.reportNewExchange(channel, exchangeName.getValue());
            RabbitMQTracingUtil.traceExchangeResourceInvocation(channel, exchangeName.getValue(), environment);
        } catch (BError | IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueBind(Environment environment, BObject clientObj, BString queueName, BString exchangeName,
                                   BString bindingKey) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            channel.queueBind(queueName.getValue(), exchangeName.getValue(), bindingKey.getValue(), null);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
            RabbitMQTracingUtil.traceQueueBindResourceInvocation(channel, queueName.getValue(), exchangeName.getValue(),
                                                                 bindingKey.getValue(), environment);
        } catch (IOException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_BIND);
            return RabbitMQUtils.returnErrorValue("Error occurred while binding the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object publishNative(Environment environment, BObject channelObj, BMap<BString, Object> message) {
        Channel channel = (Channel) channelObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        BArray messageContent = message.getArrayValue(RabbitMQConstants.MESSAGE_CONTENT);
        BString exchangeName = message.getStringValue(RabbitMQConstants.MESSAGE_EXCHANGE);
        BString routingKey = message.getStringValue(RabbitMQConstants.MESSAGE_ROUTING_KEY);
        Object properties = message.get(RabbitMQConstants.BASIC_PROPERTIES);
        String defaultExchangeName = "";
        if (exchangeName != null) {
            defaultExchangeName = exchangeName.getValue();
        }
        try {
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            if (properties != null) {
                @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                BMap<BString, Object> basicPropsMap = (BMap) properties;
                String replyTo = null;
                String contentType = null;
                String contentEncoding = null;
                String correlationId = null;
                if (basicPropsMap.containsKey(RabbitMQConstants.ALIAS_REPLY_TO)) {
                    replyTo = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_REPLY_TO).getValue();
                }
                if (basicPropsMap.containsKey(RabbitMQConstants.ALIAS_CONTENT_TYPE)) {
                    contentType = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CONTENT_TYPE).getValue();
                }
                if (basicPropsMap.containsKey(RabbitMQConstants.ALIAS_CONTENT_ENCODING)) {
                    contentEncoding = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CONTENT_ENCODING)
                            .getValue();
                }
                if (basicPropsMap.containsKey(RabbitMQConstants.ALIAS_CORRELATION_ID)) {
                    correlationId = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CORRELATION_ID).getValue();
                }
                if (replyTo != null) {
                    builder.replyTo(replyTo);
                }
                if (contentType != null) {
                    builder.contentType(contentType);
                }
                if (contentEncoding != null) {
                    builder.contentEncoding(contentEncoding);
                }
                if (correlationId != null) {
                    builder.correlationId(correlationId);
                }
            }
            if (TransactionResourceManager.getInstance().isInTransaction()) {
                RabbitMQUtils.handleTransaction(channelObj);
            }
            AMQP.BasicProperties basicProps = builder.build();
            byte[] messageContentBytes = messageContent.getBytes();
            channel.basicPublish(defaultExchangeName, routingKey.getValue(), basicProps, messageContentBytes);
            RabbitMQMetricsUtil.reportPublish(channel, defaultExchangeName, routingKey.getValue(),
                                              messageContentBytes.length);
            RabbitMQTracingUtil.tracePublishResourceInvocation(channel, defaultExchangeName, routingKey.getValue(),
                                                               environment);
        } catch (IOException | BError | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_PUBLISH);
            return RabbitMQUtils.returnErrorValue("Error occurred while publishing the message: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueDelete(Environment environment, BObject clientObj, BString queueName, boolean ifUnused,
                                     boolean ifEmpty) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            channel.queueDelete(queueName.getValue(), ifUnused, ifEmpty);
            RabbitMQMetricsUtil.reportQueueDeletion(channel, queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue(), environment);
        } catch (IOException | BError | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDelete(Environment environment, BObject clientObj, BString exchangeName) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            channel.exchangeDelete(exchangeName.getValue());
            RabbitMQMetricsUtil.reportExchangeDeletion(channel, exchangeName.getValue());
            RabbitMQTracingUtil.traceExchangeResourceInvocation(channel, exchangeName.getValue(), environment);
        } catch (IOException | BError | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queuePurge(Environment environment, BObject clientObj, BString queueName) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            channel.queuePurge(queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue(), environment);
        } catch (IOException | BError | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_PURGE);
            return RabbitMQUtils.returnErrorValue("Error occurred while purging the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object close(BObject clientObj, Object closeCode, Object closeMessage) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.close((int) ((long) closeCode), closeMessage.toString());
            } else {
                channel.close();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
        } catch (BError | IOException | ArithmeticException | TimeoutException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CLOSE);
            return RabbitMQUtils.returnErrorValue("Error occurred while closing the channel: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object abort(BObject clientObj, Object closeCode, Object closeMessage) {
        Channel channel = (Channel) clientObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.abort((int) ((long) closeCode), closeMessage.toString());
            } else {
                channel.abort();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
            return null;
        } catch (BError | IOException | ArithmeticException | ShutdownSignalException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_ABORT);
            return RabbitMQUtils.
                    returnErrorValue("Error occurred while aborting the channel: " + exception.getMessage());
        }
    }

    private ChannelUtils() {
    }
}
