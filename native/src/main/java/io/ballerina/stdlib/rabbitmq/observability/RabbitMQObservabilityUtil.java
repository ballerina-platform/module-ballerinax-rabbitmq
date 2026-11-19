/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.rabbitmq.observability;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.ObserverContext;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;
import io.ballerina.stdlib.rabbitmq.util.ModuleUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Util class for Observability-related tasks for RabbitMQ.
 *
 * @since 1.2.0
 */
public class RabbitMQObservabilityUtil {

    private static final String TRACEPARENT = "traceparent";
    private static final List<String> TRACE_HEADERS = List.of(TRACEPARENT, "tracestate", "baggage");
    // The client library does not expose a connection's virtual host, so the connector records it.
    private static final Map<Connection, String> VIRTUAL_HOSTS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void registerVirtualHost(Connection connection, String virtualHost) {
        VIRTUAL_HOSTS.put(connection, virtualHost);
    }

    // Built from the connection's fields, as the client library formats them: its string form also holds the raw
    // user name (amqp://user@host:port/vhost).
    public static String getServerUrl(Connection connection) {
        String virtualHost = VIRTUAL_HOSTS.getOrDefault(connection, "/");
        return "amqp://" + connection.getAddress().getHostAddress() + ":" + connection.getPort()
                + ("/".equals(virtualHost) ? virtualHost : "/" + virtualHost);
    }

    public static String getChannelName(Channel channel) {
        return "AMQChannel(" + getServerUrl(channel.getConnection()) + "," + channel.getChannelNumber() + ")";
    }

    public static String getQueueName(BObject service) {
        return (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
    }

    // Named like the spans the runtime starts for the module's calls: <org>/<module>/<object>:<action>.
    static String operationName(String objectName, String action) {
        Module module = ModuleUtils.getModule();
        String prefix = module == null ? RabbitMQObservabilityConstants.MODULE_RABBITMQ
                : module.getOrg() + "/" + module.getName();
        return prefix + "/" + objectName + ":" + action;
    }

    static void setStandardTags(ObserverContext observerContext, Connection connection, String type) {
        standardTags(connection, type).forEach(observerContext::addTag);
    }

    static void setQueueTags(ObserverContext observerContext, String queueName) {
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_QUEUE, queueName);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_DESTINATION_KIND,
                RabbitMQObservabilityConstants.DESTINATION_KIND_QUEUE);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_DESTINATION_TEMPLATE, queueTemplate(queueName));
    }

    static Map<String, String> standardTags(Connection connection, String type) {
        Map<String, String> tags = new HashMap<>();
        tags.put(RabbitMQObservabilityConstants.TAG_URL, getServerUrl(connection));
        tags.put(RabbitMQObservabilityConstants.TAG_MODULE, RabbitMQObservabilityConstants.MODULE_RABBITMQ);
        tags.put(RabbitMQObservabilityConstants.TAG_PROTOCOL, RabbitMQObservabilityConstants.PROTOCOL_AMQP);
        tags.put(RabbitMQObservabilityConstants.TAG_TYPE, type);
        tags.put(RabbitMQObservabilityConstants.TAG_SERVER_ADDRESS, connection.getAddress().getHostAddress());
        tags.put(RabbitMQObservabilityConstants.TAG_SERVER_PORT, String.valueOf(connection.getPort()));
        return tags;
    }

    static Map<String, String> messageTags(String exchange, String routingKey, String correlationId, int size) {
        Map<String, String> tags = new HashMap<>();
        tags.put(RabbitMQObservabilityConstants.TAG_DESTINATION_NAME, exchange);
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_TYPE, RabbitMQObservabilityConstants.EVENT_TYPE_MESSAGE);
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_KEY, routingKey);
        if (correlationId != null) {
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_CORRELATION_ID, correlationId);
        }
        if (size >= 0) {
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_SIZE, String.valueOf(size));
        }
        return tags;
    }

    // Span tags only: as observer context tags they would also tag the runtime's metrics, one series per message.
    static Map<String, String> deliveryTags(String queueName, Envelope envelope, AMQP.BasicProperties properties,
                                            int size) {
        Map<String, String> tags = new HashMap<>();
        tags.put(RabbitMQObservabilityConstants.TAG_DESTINATION_NAME, queueName);
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_TYPE, RabbitMQObservabilityConstants.EVENT_TYPE_MESSAGE);
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_SIZE, String.valueOf(size));
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_KEY, envelope.getRoutingKey());
        tags.put(RabbitMQObservabilityConstants.TAG_EVENT_POSITION, String.valueOf(envelope.getDeliveryTag()));
        if (properties == null) {
            return tags;
        }
        if (properties.getMessageId() != null) {
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_ID, properties.getMessageId());
        }
        if (properties.getCorrelationId() != null) {
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_CORRELATION_ID, properties.getCorrelationId());
        }
        if (properties.getTimestamp() != null) {
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_CREATED,
                    String.valueOf(properties.getTimestamp().getTime()));
        }
        Map<String, Object> headers = properties.getHeaders();
        if (headers != null) {
            Object deliveryCount = headers.get("x-delivery-count");
            if (deliveryCount != null) {
                tags.put(RabbitMQObservabilityConstants.TAG_EVENT_REDELIVERY_COUNT, headerValue(deliveryCount));
            }
            Object deathReason = headers.get("x-first-death-reason");
            if (deathReason != null) {
                tags.put(RabbitMQObservabilityConstants.TAG_EVENT_OUTCOME_REASON, headerValue(deathReason));
            }
        }
        return tags;
    }

    // The trace context headers of a received message; empty when it carries no traceparent.
    static Map<String, String> traceHeadersOf(AMQP.BasicProperties properties) {
        Map<String, String> traceHeaders = new HashMap<>();
        Map<String, Object> headers = properties == null ? null : properties.getHeaders();
        if (headers == null || headers.get(TRACEPARENT) == null) {
            return traceHeaders;
        }
        for (String key : TRACE_HEADERS) {
            Object value = headers.get(key);
            if (value != null) {
                traceHeaders.put(key, headerValue(value));
            }
        }
        return traceHeaders;
    }

    static String queueTemplate(String queueName) {
        return queueName.startsWith(RabbitMQObservabilityConstants.SERVER_NAMED_QUEUE_PREFIX)
                ? RabbitMQObservabilityConstants.SERVER_NAMED_QUEUE_PREFIX + "*" : queueName;
    }

    static String exchangeTemplate(String exchangeName) {
        return exchangeName == null || exchangeName.isEmpty()
                ? RabbitMQObservabilityConstants.DEFAULT_EXCHANGE : exchangeName;
    }

    // AMQP delivers string headers as LongString, whose toString is the text.
    private static String headerValue(Object value) {
        return value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(value);
    }
}
