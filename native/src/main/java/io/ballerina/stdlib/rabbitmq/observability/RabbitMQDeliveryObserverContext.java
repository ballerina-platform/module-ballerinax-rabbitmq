/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.tracer.BSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Observer context of one listener delivery, which makes the handler span a child of the publish span.
 *
 * @since 3.7.0
 */
public class RabbitMQDeliveryObserverContext extends RabbitMQObserverContext {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQDeliveryObserverContext.class);

    private final Map<String, String> deliveryTags;
    private final WeakReference<Channel> channel;
    private final String queueName;
    private final long deliveryTag;
    private boolean trackSettlement = false;

    public RabbitMQDeliveryObserverContext(Channel channel, String queueName, String handlerName, Envelope envelope,
                                           AMQP.BasicProperties properties, int size) {
        RabbitMQObservabilityUtil.setStandardTags(this, channel.getConnection(),
                RabbitMQObservabilityConstants.TYPE_LISTENER);
        RabbitMQObservabilityUtil.setQueueTags(this, queueName);
        addTag(RabbitMQObservabilityConstants.TAG_CHANNEL, RabbitMQObservabilityUtil.getChannelName(channel));
        addTag(RabbitMQObservabilityConstants.TAG_HANDLER_NAME, handlerName);
        this.deliveryTags = RabbitMQObservabilityUtil.deliveryTags(queueName, envelope, properties, size);
        this.channel = new WeakReference<>(channel);
        this.queueName = queueName;
        this.deliveryTag = envelope.getDeliveryTag();
        Map<String, String> traceContext = RabbitMQObservabilityUtil.headersOf(properties);
        if (!traceContext.isEmpty()) {
            addProperty(ObservabilityConstants.PROPERTY_TRACE_PROPERTIES, traceContext);
        }
    }

    public void trackSettlement() {
        this.trackSettlement = true;
    }

    @Override
    public void setSpan(BSpan span) {
        super.setSpan(span);
        if (span == null) {
            return;
        }
        try {
            span.addTags(deliveryTags);
            Channel deliveryChannel = channel.get();
            if (trackSettlement && deliveryChannel != null) {
                RabbitMQTracingUtil.trackHandler(deliveryChannel, queueName, deliveryTag, span);
            }
        } catch (Throwable t) {
            log.debug("Failed to tag a RabbitMQ handler span", t);
        }
    }
}
