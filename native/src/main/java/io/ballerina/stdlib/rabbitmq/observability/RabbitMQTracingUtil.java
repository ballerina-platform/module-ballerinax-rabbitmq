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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.deliveryTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.exchangeTemplate;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.messageTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.setQueueTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.setStandardTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.standardTags;
import static io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityUtil.traceHeadersOf;

/**
 * Providing tracing functionality to RabbitMQ. Trace context travels in the AMQP message headers.
 *
 * @since 1.2.0
 */
public class RabbitMQTracingUtil {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTracingUtil.class);

    // Unsettled deliveries per channel, for the links of a settlement.
    private static final Map<Channel, Deliveries> DELIVERIES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Channel> SHUTDOWN_LISTENED = Collections.newSetFromMap(
            Collections.synchronizedMap(new WeakHashMap<>()));

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
                                          GetResponse response, boolean autoAck) {
        if (!autoAck && response != null) {
            trackDelivery(channel, queueName, response.getEnvelope(), response.getProps());
        }
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
            if (!autoAck) {
                trackHandler(channel, queueName, envelope.getDeliveryTag(), span);
            }
            span.addTags(deliveryTags(queueName, envelope, response.getProps(), response.getBody().length));
            Map<String, String> traceContext = traceHeadersOf(response.getProps());
            if (!traceContext.isEmpty()) {
                new RabbitMQSpanLink(traceContext, linkAttributes(queueName, envelope.getDeliveryTag())).addTo(span);
            }
        } catch (Throwable t) {
            log.debug("Failed to trace a RabbitMQ pull", t);
        }
    }

    public static List<RabbitMQSpanLink> traceSettlement(Channel channel, String type, String outcome,
                                                         long deliveryTag, boolean multiple,
                                                         Environment environment) {
        if (!ObserveUtils.isTracingEnabled()) {
            return List.of();
        }
        Deliveries deliveries = DELIVERIES.get(channel);
        List<RabbitMQSpanLink> settled = settleDeliveries(channel, deliveryTag, multiple);
        try {
            ObserverContext observerContext = observedContext(environment);
            if (observerContext == null) {
                return settled;
            }
            setTags(observerContext, channel);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_TYPE, type);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_EVENT_STAGE,
                    RabbitMQObservabilityConstants.STAGE_SETTLED);
            observerContext.addTag(RabbitMQObservabilityConstants.TAG_EVENT_OUTCOME, outcome);
            BSpan span = observerContext.getSpan();
            if (span != null) {
                // The traced messages it settles; messages without a trace context are not tracked, and the count is
                // left out when the settlement covers a delivery dropped at the tracking limit.
                if (multiple && (deliveries == null || deliveries.isComplete(deliveryTag))) {
                    span.addTag(RabbitMQObservabilityConstants.TAG_EVENT_BATCH_SIZE, String.valueOf(settled.size()));
                }
                // No link to the handler of the settlement's own delivery: it is already the parent.
                Map<String, String> parentContext = traceContextOfParent(observerContext);
                settled.stream().filter(link -> !link.pointsTo(parentContext))
                        .limit(RabbitMQObservabilityConstants.MAX_SPAN_LINKS)
                        .forEach(link -> link.addTo(span));
            }
        } catch (Throwable t) {
            log.debug("Failed to trace a RabbitMQ settlement", t);
        }
        return settled;
    }

    public static void trackDelivery(Channel channel, String queueName, Envelope envelope,
                                     AMQP.BasicProperties properties) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        try {
            Map<String, String> traceContext = traceHeadersOf(properties);
            if (!traceContext.isEmpty()) {
                trackDelivery(channel, envelope.getDeliveryTag(), new RabbitMQSpanLink(traceContext,
                        linkAttributes(queueName, envelope.getDeliveryTag())));
            }
        } catch (Throwable t) {
            log.debug("Failed to track a RabbitMQ delivery", t);
        }
    }

    static void trackHandler(Channel channel, String queueName, long deliveryTag, BSpan handlerSpan) {
        trackDelivery(channel, deliveryTag, new RabbitMQSpanLink(handlerSpan.extractContextAsHttpHeaders(),
                linkAttributes(queueName, deliveryTag)));
    }

    // A rolled back transaction's settlements did not happen, so their deliveries are unsettled again.
    static void restoreDeliveries(Channel channel, List<RabbitMQSpanLink> links) {
        try {
            for (RabbitMQSpanLink link : links) {
                long deliveryTag = link.deliveryTag();
                if (deliveryTag >= 0) {
                    trackDelivery(channel, deliveryTag, link);
                }
            }
        } catch (Throwable t) {
            log.debug("Failed to restore the deliveries of a rolled back RabbitMQ transaction", t);
        }
    }

    public static List<RabbitMQSpanLink> publishLinks(Environment environment, String exchangeName,
                                                      String routingKey) {
        try {
            BSpan span = spanOf(observedContext(environment));
            if (span == null) {
                return List.of();
            }
            Map<String, String> attributes = new HashMap<>();
            attributes.put(RabbitMQObservabilityConstants.TAG_DESTINATION_NAME, exchangeTemplate(exchangeName));
            attributes.put(RabbitMQObservabilityConstants.TAG_EVENT_KEY, routingKey);
            return List.of(new RabbitMQSpanLink(span.extractContextAsHttpHeaders(), attributes));
        } catch (Throwable t) {
            log.debug("Failed to link a RabbitMQ publish to its transaction", t);
            return List.of();
        }
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

    private static Map<String, String> traceContextOfParent(ObserverContext observerContext) {
        BSpan parentSpan = spanOf(observerContext.getParent());
        return parentSpan == null ? Map.of() : parentSpan.extractContextAsHttpHeaders();
    }

    private static Map<String, String> linkAttributes(String queueName, long deliveryTag) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(RabbitMQObservabilityConstants.TAG_DESTINATION_NAME, queueName);
        attributes.put(RabbitMQObservabilityConstants.TAG_EVENT_POSITION, String.valueOf(deliveryTag));
        return attributes;
    }

    private static void trackDelivery(Channel channel, long deliveryTag, RabbitMQSpanLink link) {
        // A shut down channel's unacked messages go back to the queue, and a recovered channel restarts its tags.
        if (SHUTDOWN_LISTENED.add(channel)) {
            channel.addShutdownListener(cause -> DELIVERIES.remove(channel));
        }
        DELIVERIES.computeIfAbsent(channel, c -> new Deliveries()).track(deliveryTag, link);
    }

    static List<RabbitMQSpanLink> settleDeliveries(Channel channel, long deliveryTag, boolean multiple) {
        Deliveries deliveries = DELIVERIES.get(channel);
        return deliveries == null ? List.of() : deliveries.settle(deliveryTag, multiple);
    }

    private static void setTags(ObserverContext observerContext, Channel channel) {
        setStandardTags(observerContext, channel.getConnection(), RabbitMQObservabilityConstants.TYPE_CLIENT);
        observerContext.addTag(RabbitMQObservabilityConstants.TAG_CHANNEL,
                RabbitMQObservabilityUtil.getChannelName(channel));
    }

    private RabbitMQTracingUtil() {
    }

    /**
     * The unsettled deliveries of one channel, by delivery tag. The count is kept separately because the size of a
     * {@link ConcurrentSkipListMap} is computed by walking it.
     */
    static final class Deliveries {

        private final ConcurrentSkipListMap<Long, RabbitMQSpanLink> links = new ConcurrentSkipListMap<>();
        private final AtomicInteger size = new AtomicInteger();
        private final AtomicLong evictedUpTo = new AtomicLong(-1);

        void track(long deliveryTag, RabbitMQSpanLink link) {
            if (links.put(deliveryTag, link) == null) {
                size.incrementAndGet();
            }
            while (size.get() > RabbitMQObservabilityConstants.MAX_TRACKED_DELIVERIES) {
                Map.Entry<Long, RabbitMQSpanLink> oldest = links.pollFirstEntry();
                if (oldest == null) {
                    break;
                }
                size.decrementAndGet();
                evictedUpTo.accumulateAndGet(oldest.getKey(), Math::max);
            }
        }

        // A multiple settlement covers every delivery up to its tag; with tag 0, all of them.
        List<RabbitMQSpanLink> settle(long deliveryTag, boolean multiple) {
            if (!multiple) {
                RabbitMQSpanLink link = links.remove(deliveryTag);
                if (link == null) {
                    return List.of();
                }
                size.decrementAndGet();
                return List.of(link);
            }
            long upTo = deliveryTag;
            if (deliveryTag == 0) {
                Map.Entry<Long, RabbitMQSpanLink> last = links.lastEntry();
                if (last == null) {
                    return List.of();
                }
                upTo = last.getKey();
            }
            List<RabbitMQSpanLink> settled = new ArrayList<>();
            Map.Entry<Long, RabbitMQSpanLink> first;
            while ((first = links.firstEntry()) != null && first.getKey() <= upTo) {
                if (links.remove(first.getKey(), first.getValue())) {
                    size.decrementAndGet();
                    settled.add(first.getValue());
                }
            }
            return settled;
        }

        // Whether a multiple settlement up to this tag covers no delivery dropped at the tracking limit.
        boolean isComplete(long deliveryTag) {
            long evicted = evictedUpTo.get();
            return evicted < 0 || (deliveryTag != 0 && evicted > deliveryTag);
        }

        int size() {
            return size.get();
        }
    }
}
