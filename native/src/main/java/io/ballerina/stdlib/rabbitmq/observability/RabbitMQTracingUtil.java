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

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;
import io.ballerina.runtime.observability.tracer.BSpan;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.deliveryTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.exchangeTemplate;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.headersOf;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.messageTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.setQueueTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.setStandardTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.standardTags;

/**
 * Providing tracing functionality to RabbitMQ. Trace context travels in the AMQP message headers.
 *
 * @since 1.2.0
 */
public class RabbitMQTracingUtil {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTracingUtil.class);

    public static void traceResourceInvocation(Channel channel, Environment environment) {
        ObserverContext observerContext = observedContext(environment);
        if (observerContext != null) {
            setTags(observerContext, channel);
        }
    }

    public static void traceQueueResourceInvocation(Channel channel, String queueName, Environment environment) {
        ObserverContext observerContext = observedContext(environment);
        if (observerContext != null) {
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_QUEUE, queueName);
            setTags(observerContext, channel);
        }
    }

    public static void traceExchangeResourceInvocation(Channel channel, String exchangeName,
                                                       Environment environment) {
        ObserverContext observerContext = observedContext(environment);
        if (observerContext != null) {
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_EXCHANGE, exchangeName);
            setTags(observerContext, channel);
        }
    }

    public static void traceQueueBindResourceInvocation(Channel channel, String queueName, String exchangeName,
                                                        String routingKey, Environment environment) {
        ObserverContext observerContext = observedContext(environment);
        if (observerContext != null) {
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_QUEUE, queueName);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_EXCHANGE, exchangeName);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_ROUTING_KEY, routingKey);
            setTags(observerContext, channel);
        }
    }

    public static void tracePublishResourceInvocation(Channel channel, String exchangeName, String routingKey,
                                                      Environment environment) {
        tracePublishResourceInvocation(channel, exchangeName, routingKey, null, -1, environment);
    }

    public static void tracePublishResourceInvocation(Channel channel, String exchangeName, String routingKey,
                                                      String correlationId, int size, Environment environment) {
        try {
            ObserverContext observerContext = observedContext(environment);
            if (observerContext == null) {
                return;
            }
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_EXCHANGE, exchangeName);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_ROUTING_KEY, routingKey);
            setTags(observerContext, channel);
            String exchange = exchangeTemplate(exchangeName);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_DESTINATION_KIND,
                    RabbitMQObservabilityConstants.DESTINATION_KIND_EXCHANGE);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_DESTINATION_TEMPLATE, exchange);
            BSpan span = observerContext.getSpan();
            if (span != null) {
                span.addTags(messageTags(exchange, routingKey, correlationId, size));
            }
        } catch (Throwable t) {
            log.debug("Failed to trace a RabbitMQ publish", t);
        }
    }

    public static void injectTraceContext(Environment environment, Map<String, Object> headers) {
        try {
            BSpan span = spanOf(observedContext(environment));
            if (span != null) {
                span.extractContextAsHttpHeaders().forEach(headers::putIfAbsent);
            }
        } catch (Throwable t) {
            log.debug("Failed to inject trace context into a RabbitMQ message", t);
        }
    }

    public static Map<String, Object> traceContextOf(BSpan span) {
        Map<String, Object> headers = new HashMap<>();
        try {
            if (span != null) {
                headers.putAll(span.extractContextAsHttpHeaders());
            }
        } catch (Throwable t) {
            log.debug("Failed to read the trace context of a RabbitMQ span", t);
        }
        return headers;
    }

    public static void traceClientReceive(Environment environment, Channel channel, String queueName,
                                          GetResponse response) {
        try {
            ObserverContext observerContext = observedContext(environment);
            if (observerContext == null) {
                return;
            }
            setTags(observerContext, channel);
            setQueueTags(observerContext, queueName);
            BSpan span = observerContext.getSpan();
            if (span == null || response == null) {
                return;
            }
            Envelope envelope = response.getEnvelope();
            span.addTags(deliveryTags(queueName, envelope, response.getProps(), response.getBody().length));
            Map<String, String> traceContext = headersOf(response.getProps());
            if (!traceContext.isEmpty()) {
                new RabbitMQSpanLink(traceContext, linkAttributes(queueName, envelope.getDeliveryTag())).addTo(span);
            }
        } catch (Throwable t) {
            log.debug("Failed to trace a RabbitMQ pull", t);
        }
    }

    public static void traceSettlement(Channel channel, String type, String outcome, Environment environment) {
        ObserverContext observerContext = observedContext(environment);
        if (observerContext == null) {
            return;
        }
        setTags(observerContext, channel);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_TYPE, type);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_EVENT_STAGE,
                RabbitMQObservabilityConstants.STAGE_SETTLED);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_EVENT_OUTCOME, outcome);
    }

    // The reply is published after onRequest returns, so this span starts after its parent ends.
    public static BSpan startReplySpan(ObserverContext handlerContext, Channel channel, String exchangeName,
                                       String replyTo, String correlationId, int size) {
        try {
            BSpan parent = ObserveUtils.isTracingEnabled() ? spanOf(handlerContext) : null;
            if (parent == null) {
                return null;
            }
            BSpan span = BSpan.start(parent, handlerContext.getServiceName(),
                    RabbitMQObservabilityUtil.operationName(RabbitMQObservabilityConstants.OBJECT_LISTENER,
                            RabbitMQObservabilityConstants.ACTION_REPLY), true);
            String exchange = exchangeTemplate(exchangeName);
            Map<String, String> tags = standardTags(channel.getConnection(),
                    RabbitMQObservabilityConstants.TYPE_LISTENER);
            tags.putAll(messageTags(exchange, replyTo, correlationId, size));
            tags.put(RabbitMQObservabilityConstants.TAG_EXCHANGE, exchangeName);
            tags.put(RabbitMQObservabilityConstants.TAG_ROUTING_KEY, replyTo);
            tags.put(RabbitMQObservabilityConstants.TAG_DESTINATION_KIND,
                    RabbitMQObservabilityConstants.DESTINATION_KIND_EXCHANGE);
            tags.put(RabbitMQObservabilityConstants.TAG_DESTINATION_TEMPLATE, exchange);
            span.addTags(tags);
            return span;
        } catch (Throwable t) {
            log.debug("Failed to start the span of a RabbitMQ reply", t);
            return null;
        }
    }

    public static void finishSpan(BSpan span, boolean failed) {
        if (span == null) {
            return;
        }
        try {
            if (failed) {
                span.addTag(ObservabilityConstants.TAG_KEY_ERROR, ObservabilityConstants.TAG_TRUE_VALUE);
                span.setStatus(StatusCode.ERROR);
            }
            span.finishSpan();
        } catch (Throwable t) {
            log.debug("Failed to finish a RabbitMQ span", t);
        }
    }

    public static String nackOutcome(boolean requeue) {
        return requeue ? RabbitMQObservabilityConstants.OUTCOME_RELEASED
                : RabbitMQObservabilityConstants.OUTCOME_REJECTED;
    }

    // Null when tracing is off or the call is not observed, such as a Listener method. A new context is not created:
    // one without a span becomes the parent of the next observed call on the strand, which then fails to start.
    private static ObserverContext observedContext(Environment environment) {
        return ObserveUtils.isTracingEnabled() ? ObserveUtils.getObserverContextOfCurrentFrame(environment) : null;
    }

    private static BSpan spanOf(ObserverContext observerContext) {
        return observerContext == null ? null : observerContext.getSpan();
    }

    private static Map<String, String> linkAttributes(String queueName, long deliveryTag) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(RabbitMQObservabilityConstants.TAG_DESTINATION_NAME, queueName);
        attributes.put(RabbitMQObservabilityConstants.TAG_EVENT_POSITION, String.valueOf(deliveryTag));
        return attributes;
    }

    private static void setTags(ObserverContext observerContext, Channel channel) {
        setStandardTags(observerContext, channel.getConnection(), RabbitMQObservabilityConstants.TYPE_CLIENT);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_CHANNEL,
                RabbitMQObservabilityUtil.getChannelName(channel));
    }

    private RabbitMQTracingUtil() {
    }
}
