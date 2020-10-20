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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.ValueCreator;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.scheduling.Scheduler;
import io.ballerina.runtime.scheduling.Strand;
import io.ballerina.runtime.util.exceptions.BallerinaException;
import io.ballerina.runtime.values.ArrayValue;
import io.ballerina.runtime.values.HandleValue;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConnectorException;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQTransactionContext;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public static Channel createChannel(Connection connection, BObject channelObj) {
        try {
            Channel channel = connection.createChannel();
            RabbitMQMetricsUtil.reportNewChannel(channel);
            String connectorId = channelObj.getStringValue(RabbitMQConstants.CONNECTOR_ID).getValue();
            channelObj.addNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT,
                                     new RabbitMQTransactionContext(channel, connectorId));
            return channel;
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(connection, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CREATE);
            throw RabbitMQUtils.returnErrorValue("Error occurred while initializing the channel: "
                                                         + exception.getMessage());
        }
    }

    public static Object queueDeclare(Object queueConfig, Channel channel) {
        try {
            if (queueConfig == null) {
                RabbitMQMetricsUtil.reportNewQueue(channel, RabbitMQObservabilityConstants.UNKNOWN);
                return StringUtils.fromString(channel.queueDeclare().getQueue());
            }
            @SuppressWarnings(RabbitMQConstants.UNCHECKED)
            BMap<BString, Object> config = (BMap<BString, Object>) queueConfig;
            String queueName = config.getStringValue(RabbitMQConstants.QUEUE_NAME).getValue();
            boolean durable = config.getBooleanValue(RabbitMQConstants.QUEUE_DURABLE);
            boolean exclusive = config.getBooleanValue(RabbitMQConstants.QUEUE_EXCLUSIVE);
            boolean autoDelete = config.getBooleanValue(RabbitMQConstants.QUEUE_AUTO_DELETE);
            Map<String, Object> argumentsMap = null;
            if (config.getMapValue(RabbitMQConstants.QUEUE_ARGUMENTS) != null) {
                argumentsMap = (HashMap<String, Object>) config.getMapValue(RabbitMQConstants.QUEUE_ARGUMENTS);
            }
            channel.queueDeclare(queueName, durable, exclusive, autoDelete, argumentsMap);
            RabbitMQMetricsUtil.reportNewQueue(channel, queueName);
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName);
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDeclare(BMap<BString, Object> exchangeConfig, Channel channel) {
        RabbitMQTracingUtil.traceResourceInvocation(channel);
        try {
            String exchangeName = exchangeConfig.getStringValue(RabbitMQConstants.EXCHANGE_NAME).getValue();
            String exchangeType = exchangeConfig.getStringValue(RabbitMQConstants.EXCHANGE_TYPE).getValue();
            boolean durable = exchangeConfig.getBooleanValue(RabbitMQConstants.EXCHANGE_DURABLE);
            boolean autoDelete = exchangeConfig.getBooleanValue(RabbitMQConstants.EXCHANGE_AUTO_DELETE);
            Map<String, Object> argumentsMap = null;
            if (exchangeConfig.getMapValue(RabbitMQConstants.EXCHANGE_ARGUMENTS) != null) {
                argumentsMap =
                        (HashMap<String, Object>) exchangeConfig.getMapValue(RabbitMQConstants.EXCHANGE_ARGUMENTS);
            }
            channel.exchangeDeclare(exchangeName, exchangeType, durable, autoDelete, argumentsMap);
            RabbitMQMetricsUtil.reportNewExchange(channel, exchangeName);
            RabbitMQTracingUtil.traceExchangeResourceInvocation(channel, exchangeName);
        } catch (RabbitMQConnectorException | IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueBind(BString queueName, BString exchangeName, BString bindingKey, Channel channel) {
        try {
            channel.queueBind(queueName.getValue(), exchangeName.getValue(), bindingKey.getValue(), null);
            RabbitMQTracingUtil.traceResourceInvocation(channel);
            RabbitMQTracingUtil.traceQueueBindResourceInvocation(channel, queueName.getValue(), exchangeName.getValue(),
                                                                 bindingKey.getValue());
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_BIND);
            return RabbitMQUtils.returnErrorValue("Error occurred while binding the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object basicPublish(Object messageContent, BString routingKey, BString exchangeName,
                                      Object properties, Channel channel, BObject channelObj) {
        Strand strand = Scheduler.getStrand();
        String defaultExchangeName = "";
        if (exchangeName != null) {
            defaultExchangeName = exchangeName.getValue();
        }
        try {
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            if (properties != null) {
                @SuppressWarnings(RabbitMQConstants.UNCHECKED)
                BMap<BString, Object> basicPropsMap = (BMap) properties;
                String replyTo = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_REPLY_TO).getValue();
                String contentType = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CONTENT_TYPE).getValue();
                String contentEncoding = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CONTENT_ENCODING)
                        .getValue();
                String correlationId = basicPropsMap.getStringValue(RabbitMQConstants.ALIAS_CORRELATION_ID).getValue();
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
            AMQP.BasicProperties basicProps = builder.build();
            byte[] messageContentBytes = messageContent.toString().getBytes(StandardCharsets.UTF_8);
            channel.basicPublish(defaultExchangeName, routingKey.getValue(), basicProps, messageContentBytes);
            RabbitMQMetricsUtil.reportPublish(channel, defaultExchangeName, routingKey.getValue(),
                                              messageContentBytes.length);
            RabbitMQTracingUtil.tracePublishResourceInvocation(channel, defaultExchangeName, routingKey.getValue());
            if (strand.isInTransaction()) {
                RabbitMQUtils.handleTransaction(channelObj, strand);
            }
        } catch (IOException | RabbitMQConnectorException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_PUBLISH);
            return RabbitMQUtils.returnErrorValue("Error occurred while publishing the message: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueDelete(BString queueName, boolean ifUnused, boolean ifEmpty, Channel channel) {
        try {
            channel.queueDelete(queueName.getValue(), ifUnused, ifEmpty);
            RabbitMQMetricsUtil.reportQueueDeletion(channel, queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue());
        } catch (IOException | RabbitMQConnectorException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDelete(BString exchangeName, Channel channel) {
        try {
            channel.exchangeDelete(exchangeName.getValue());
            RabbitMQMetricsUtil.reportExchangeDeletion(channel, exchangeName.getValue());
            RabbitMQTracingUtil.traceExchangeResourceInvocation(channel, exchangeName.getValue());
        } catch (IOException | BallerinaException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queuePurge(BString queueName, Channel channel) {
        try {
            channel.queuePurge(queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue());
        } catch (IOException | RabbitMQConnectorException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_PURGE);
            return RabbitMQUtils.returnErrorValue("Error occurred while purging the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object close(Object closeCode, Object closeMessage, Channel channel) {
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.close((int) closeCode, closeMessage.toString());
            } else {
                channel.close();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
            RabbitMQTracingUtil.traceResourceInvocation(channel);
        } catch (RabbitMQConnectorException | IOException | ArithmeticException | TimeoutException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CLOSE);
            return RabbitMQUtils.returnErrorValue("Error occurred while closing the channel: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object abort(Object closeCode, Object closeMessage, Channel channel) {
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.abort((int) closeCode, closeMessage.toString());
            } else {
                channel.abort();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
            RabbitMQTracingUtil.traceResourceInvocation(channel);
            return null;
        } catch (RabbitMQConnectorException | IOException | ArithmeticException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_ABORT);
            return RabbitMQUtils.
                    returnErrorValue("Error occurred while aborting the channel: " + exception.getMessage());
        }
    }

    public static Object getConnection(Channel channel) {
        try {
            Connection connection = channel.getConnection();
            BObject connectionObject = ValueCreator.createObjectValue(RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                                       RabbitMQConstants.CONNECTION_OBJECT);
            connectionObject.addNativeData(RabbitMQConstants.CONNECTION_NATIVE_OBJECT, connection);
            RabbitMQTracingUtil.traceResourceInvocation(channel);
            return connectionObject;
        } catch (AlreadyClosedException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_GET_CONNECTION);
            return RabbitMQUtils.returnErrorValue("Error occurred while retrieving the connection: "
                                                          + exception.getMessage());
        }
    }

    public static Object basicGet(BString queueName, boolean ackMode, Channel channel) {
        try {
            GetResponse response = channel.basicGet(queueName.getValue(), ackMode);
            if (Objects.isNull(response)) {
                return RabbitMQUtils.returnErrorValue("No messages are found in the queue.");
            }
            BObject messageBObject = createAndPopulateMessageBObject(response, channel, ackMode);
            ArrayValue messageContent = (ArrayValue) messageBObject.get(RabbitMQConstants.MESSAGE_CONTENT);
            RabbitMQMetricsUtil.reportConsume(channel, queueName.getValue(), messageContent.getBytes().length,
                                              RabbitMQObservabilityConstants.CONSUME_TYPE_CHANNEL);
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue());
            return messageBObject;
        } catch (IOException e) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_BASIC_GET);
            return RabbitMQUtils.returnErrorValue("Error occurred while retrieving the message: " +
                                                          e.getMessage());
        }
    }

    private static BObject createAndPopulateMessageBObject(GetResponse response, Channel channel,
                                                                   boolean autoAck) {
        BObject messageBObject = ValueCreator.createObjectValue(RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                                           RabbitMQConstants.MESSAGE_OBJECT);
        messageBObject.set(RabbitMQConstants.DELIVERY_TAG, response.getEnvelope().getDeliveryTag());
        messageBObject.set(RabbitMQConstants.JAVA_CLIENT_CHANNEL, new HandleValue(channel));
        messageBObject.set(RabbitMQConstants.MESSAGE_CONTENT,
                               ValueCreator.createArrayValue(response.getBody()));
        messageBObject.set(RabbitMQConstants.AUTO_ACK_STATUS, autoAck);
        messageBObject.set(RabbitMQConstants.MESSAGE_ACK_STATUS, false);
        AMQP.BasicProperties properties = response.getProps();
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

    private ChannelUtils() {
    }
}
