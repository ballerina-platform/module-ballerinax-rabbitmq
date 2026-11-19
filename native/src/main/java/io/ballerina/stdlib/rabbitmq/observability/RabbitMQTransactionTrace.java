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

import com.rabbitmq.client.Channel;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;
import io.ballerina.runtime.observability.tracer.BSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Traces the transaction of a channel: the commit or rollback is a span that links to the transaction's publishes
 * and settlements. As in AMQP, a channel has one transaction at a time; strands that share a channel inside
 * transactions share this state.
 *
 * @since 3.7.0
 */
public final class RabbitMQTransactionTrace {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQTransactionTrace.class);

    private final String type;
    private final List<RabbitMQSpanLink> links = new ArrayList<>();
    private int operations = 0;
    private boolean started = false;
    private BSpan parentSpan = null;
    private String serviceName = null;

    public RabbitMQTransactionTrace(String type) {
        this.type = type;
    }

    public synchronized void record(Environment environment, List<RabbitMQSpanLink> operationLinks) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        try {
            if (!started) {
                started = true;
                ObserverContext observerContext = ObserveUtils.getObserverContextOfCurrentFrame(environment);
                ObserverContext parent = observerContext == null ? null : observerContext.getParent();
                parentSpan = parent == null ? null : parent.getSpan();
                serviceName = parent != null ? parent.getServiceName()
                        : observerContext != null ? observerContext.getServiceName() : null;
            }
            for (RabbitMQSpanLink link : operationLinks) {
                operations++;
                if (links.size() < RabbitMQObservabilityConstants.MAX_SPAN_LINKS) {
                    links.add(link);
                }
            }
        } catch (Throwable t) {
            log.debug("Failed to trace an operation of a RabbitMQ transaction", t);
        }
    }

    public synchronized void finish(Channel channel, boolean committed, boolean failed) {
        try {
            if (!started) {
                return;
            }
            String action = committed ? RabbitMQObservabilityConstants.ACTION_COMMIT
                    : RabbitMQObservabilityConstants.ACTION_ROLLBACK;
            String operation = RabbitMQObservabilityUtil.operationName(
                    RabbitMQObservabilityConstants.OBJECT_TRANSACTION, action);
            String service = serviceName != null ? serviceName : RabbitMQObservabilityConstants.MODULE_RABBITMQ;
            BSpan span = parentSpan != null ? BSpan.start(parentSpan, service, operation, true)
                    : BSpan.start(service, operation, true);
            Map<String, String> tags = RabbitMQObservabilityUtil.standardTags(channel.getConnection(), type);
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_STAGE, RabbitMQObservabilityConstants.STAGE_SETTLED);
            if (!failed) {
                tags.put(RabbitMQObservabilityConstants.TAG_EVENT_OUTCOME, committed
                        ? RabbitMQObservabilityConstants.OUTCOME_COMMITTED
                        : RabbitMQObservabilityConstants.OUTCOME_ROLLED_BACK);
            }
            tags.put(RabbitMQObservabilityConstants.TAG_EVENT_BATCH_SIZE, String.valueOf(operations));
            span.addTags(tags);
            links.forEach(link -> link.addTo(span));
            RabbitMQTracingUtil.finishSpan(span, failed);
            if (!committed && !failed) {
                RabbitMQTracingUtil.restoreDeliveries(channel, links);
            }
        } catch (Throwable t) {
            log.debug("Failed to trace the end of a RabbitMQ transaction", t);
        } finally {
            links.clear();
            operations = 0;
            started = false;
            parentSpan = null;
            serviceName = null;
        }
    }
}
