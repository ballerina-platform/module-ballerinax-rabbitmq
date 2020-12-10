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
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.transactions.TransactionResourceManager;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQTransactionContext;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Util class for RabbitMQ Channel handling.
 *
 * @since 0.995.0
 */
public class ChannelUtils {
    public static Channel createChannel(BMap<BString, Object> connectionConfig, BObject channelObj) {
        Connection connection = ConnectionUtils.createConnection(connectionConfig);
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

    public static Object queueDeclare(Environment environment, BString queueName, Object queueConfig, Channel channel) {
        boolean durable = false;
        boolean exclusive = false;
        boolean autoDelete = true;
        Map<String, Object> argumentsMap = null;
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
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDeclare(Environment environment, BString exchangeName, BString exchangeType,
                                         Object exchangeConfig, Channel channel) {
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
        } catch (BError | IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DECLARE);
            return RabbitMQUtils.returnErrorValue("Error occurred while declaring the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueBind(Environment environment, BString queueName, BString exchangeName, BString bindingKey,
                                   Channel channel) {
        try {
            channel.queueBind(queueName.getValue(), exchangeName.getValue(), bindingKey.getValue(), null);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
            RabbitMQTracingUtil.traceQueueBindResourceInvocation(channel, queueName.getValue(), exchangeName.getValue(),
                                                                 bindingKey.getValue(), environment);
        } catch (IOException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_BIND);
            return RabbitMQUtils.returnErrorValue("Error occurred while binding the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object basicPublish(Environment environment, BArray messageContent, BString routingKey,
                                      BString exchangeName, Object properties, Channel channel, BObject channelObj) {
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
            byte[] messageContentBytes = messageContent.getBytes();
            channel.basicPublish(defaultExchangeName, routingKey.getValue(), basicProps, messageContentBytes);
            RabbitMQMetricsUtil.reportPublish(channel, defaultExchangeName, routingKey.getValue(),
                                              messageContentBytes.length);
            RabbitMQTracingUtil.tracePublishResourceInvocation(channel, defaultExchangeName, routingKey.getValue(),
                                                               environment);
            if (TransactionResourceManager.getInstance().isInTransaction()) {
                RabbitMQUtils.handleTransaction(channelObj);
            }
        } catch (IOException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_PUBLISH);
            return RabbitMQUtils.returnErrorValue("Error occurred while publishing the message: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queueDelete(Environment environment, BString queueName, boolean ifUnused, boolean ifEmpty,
                                     Channel channel) {
        try {
            channel.queueDelete(queueName.getValue(), ifUnused, ifEmpty);
            RabbitMQMetricsUtil.reportQueueDeletion(channel, queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue(), environment);
        } catch (IOException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object exchangeDelete(Environment environment, BString exchangeName, Channel channel) {
        try {
            channel.exchangeDelete(exchangeName.getValue());
            RabbitMQMetricsUtil.reportExchangeDeletion(channel, exchangeName.getValue());
            RabbitMQTracingUtil.traceExchangeResourceInvocation(channel, exchangeName.getValue(), environment);
        } catch (IOException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_EXCHANGE_DELETE);
            return RabbitMQUtils.returnErrorValue("Error occurred while deleting the exchange: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object queuePurge(Environment environment, BString queueName, Channel channel) {
        try {
            channel.queuePurge(queueName.getValue());
            RabbitMQTracingUtil.traceQueueResourceInvocation(channel, queueName.getValue(), environment);
        } catch (IOException | BError exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_QUEUE_PURGE);
            return RabbitMQUtils.returnErrorValue("Error occurred while purging the queue: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object close(Environment environment, Object closeCode, Object closeMessage, Channel channel) {
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.close((int) closeCode, closeMessage.toString());
            } else {
                channel.close();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
        } catch (BError | IOException | ArithmeticException | TimeoutException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_CHANNEL_CLOSE);
            return RabbitMQUtils.returnErrorValue("Error occurred while closing the channel: "
                                                          + exception.getMessage());
        }
        return null;
    }

    public static Object abort(Environment environment, Object closeCode, Object closeMessage, Channel channel) {
        try {
            boolean validCloseCode = closeCode != null && RabbitMQUtils.checkIfInt(closeCode);
            boolean validCloseMessage = closeMessage != null && RabbitMQUtils.checkIfString(closeMessage);
            if (validCloseCode && validCloseMessage) {
                channel.abort((int) closeCode, closeMessage.toString());
            } else {
                channel.abort();
            }
            RabbitMQMetricsUtil.reportChannelClose(channel);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
            return null;
        } catch (BError | IOException | ArithmeticException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_ABORT);
            return RabbitMQUtils.
                    returnErrorValue("Error occurred while aborting the channel: " + exception.getMessage());
        }
    }

    public static Object getConnection(Environment environment, Channel channel) {
        try {
            Connection connection = channel.getConnection();
            BObject connectionObject = ValueCreator.createObjectValue(RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                                      RabbitMQConstants.CONNECTION_OBJECT);
            connectionObject.addNativeData(RabbitMQConstants.CONNECTION_NATIVE_OBJECT, connection);
            RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
            return connectionObject;
        } catch (AlreadyClosedException exception) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_GET_CONNECTION);
            return RabbitMQUtils.returnErrorValue("Error occurred while retrieving the connection: "
                                                          + exception.getMessage());
        }
    }

    private ChannelUtils() {
    }
}
