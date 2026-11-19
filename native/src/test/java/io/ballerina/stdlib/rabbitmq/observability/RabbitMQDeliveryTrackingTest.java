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
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests the tracking of unsettled deliveries that settlement links come from.
 */
public class RabbitMQDeliveryTrackingTest {

    private static final int LIMIT = RabbitMQObservabilityConstants.MAX_TRACKED_DELIVERIES;

    @Test
    public void testSparseDeliveryIsKeptBelowTheLimit() {
        RabbitMQTracingUtil.Deliveries deliveries = new RabbitMQTracingUtil.Deliveries();
        deliveries.track(1, link(1));
        for (long tag = 2; tag <= LIMIT; tag++) {
            deliveries.track(tag, link(tag));
            deliveries.settle(tag, false);
        }
        deliveries.track(LIMIT + 1, link(LIMIT + 1));

        Assert.assertEquals(deliveries.size(), 2);
        Assert.assertEquals(deliveries.settle(LIMIT + 1, true).size(), 2);
        Assert.assertTrue(deliveries.isComplete(LIMIT + 1));
        Assert.assertEquals(deliveries.size(), 0);
    }

    @Test
    public void testSettlementCoveringAnEvictedDeliveryIsIncomplete() {
        RabbitMQTracingUtil.Deliveries deliveries = new RabbitMQTracingUtil.Deliveries();
        for (long tag = 1; tag <= LIMIT + 1; tag++) {
            deliveries.track(tag, link(tag));
        }

        Assert.assertEquals(deliveries.size(), LIMIT);
        Assert.assertFalse(deliveries.isComplete(LIMIT + 1));
        Assert.assertEquals(deliveries.settle(LIMIT + 1, true).size(), LIMIT);
    }

    @Test
    public void testRollbackRestoresMoreThanTheSpanLinkLimit() {
        Channel channel = channel();
        int count = RabbitMQObservabilityConstants.MAX_SPAN_LINKS + 72;
        List<RabbitMQSpanLink> links = new ArrayList<>();
        for (long tag = 1; tag <= count; tag++) {
            links.add(link(tag));
        }
        RabbitMQTransactionTrace trace = new RabbitMQTransactionTrace(RabbitMQObservabilityConstants.TYPE_LISTENER);
        trace.recordLinks(links);

        trace.finish(channel, false, false);

        Assert.assertEquals(RabbitMQTracingUtil.settleDeliveries(channel, 0, true).size(), count);
    }

    private static RabbitMQSpanLink link(long deliveryTag) {
        return new RabbitMQSpanLink(Map.of("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"),
                Map.of(RabbitMQObservabilityConstants.TAG_EVENT_POSITION, String.valueOf(deliveryTag)));
    }

    private static Channel channel() {
        return (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class<?>[]{Channel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "addShutdownListener" -> null;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "test channel";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
