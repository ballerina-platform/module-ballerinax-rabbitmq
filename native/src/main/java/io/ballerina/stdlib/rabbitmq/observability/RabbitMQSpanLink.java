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

import io.ballerina.runtime.observability.tracer.BSpan;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A span link to a message: the trace context it points to and the tags that identify the message.
 *
 * @since 3.7.0
 */
public final class RabbitMQSpanLink {

    private static final String TRACEPARENT = "traceparent";
    // Span links need Ballerina 2201.14.2 or later. On earlier runtimes they are skipped.
    private static final AtomicBoolean SUPPORTED = new AtomicBoolean(true);

    private final Map<String, String> traceContext;
    private final Map<String, String> attributes;

    RabbitMQSpanLink(Map<String, String> traceContext, Map<String, String> attributes) {
        this.traceContext = Map.copyOf(traceContext);
        this.attributes = Map.copyOf(attributes);
    }

    void addTo(BSpan span) {
        if (!SUPPORTED.get()) {
            return;
        }
        try {
            span.addLink(traceContext, attributes);
        } catch (NoSuchMethodError e) {
            SUPPORTED.set(false);
        }
    }

    boolean pointsTo(Map<String, String> otherContext) {
        String traceParent = traceContext.get(TRACEPARENT);
        String otherTraceParent = otherContext.get(TRACEPARENT);
        if (traceParent == null || otherTraceParent == null) {
            return false;
        }
        String[] ids = traceParent.split("-");
        String[] otherIds = otherTraceParent.split("-");
        return ids.length > 2 && otherIds.length > 2 && ids[1].equals(otherIds[1]) && ids[2].equals(otherIds[2]);
    }
}
