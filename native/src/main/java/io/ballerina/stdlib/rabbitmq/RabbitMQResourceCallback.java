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

package io.ballerina.stdlib.rabbitmq;

import com.rabbitmq.client.Channel;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import io.ballerina.stdlib.rabbitmq.util.MessageUtils;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * The resource call back implementation for RabbitMQ async consumer.
 *
 * @since 0.995.0
 */
public class RabbitMQResourceCallback implements Callback {
    private final CountDownLatch countDownLatch;
    private final Channel channel;
    private final String queueName;
    private final int size;
    private String replyTo = null;
    private String exchange = null;

    RabbitMQResourceCallback(CountDownLatch countDownLatch, Channel channel, String queueName, int size) {
        this.countDownLatch = countDownLatch;
        this.channel = channel;
        this.queueName = queueName;
        this.size = size;
    }

    RabbitMQResourceCallback(CountDownLatch countDownLatch, Channel channel, String queueName, int size,
                             String replyTo, String exchange) {
        this.countDownLatch = countDownLatch;
        this.channel = channel;
        this.queueName = queueName;
        this.size = size;
        this.replyTo = replyTo;
        this.exchange = exchange;

    }

    @Override
    public void notifySuccess(Object obj) {
        if (obj instanceof BError) {
            ((BError) obj).printStackTrace();
        } else if (replyTo != null) {
            try {
                channel.basicPublish(exchange, replyTo, null, MessageUtils.convertDataIntoByteArray(obj));
            } catch (IOException e) {
                throw RabbitMQUtils.returnErrorValue("error occurred while replying to the message");
            }
        }
        RabbitMQMetricsUtil.reportConsume(channel, queueName, size,
                                          RabbitMQObservabilityConstants.CONSUME_TYPE_SERVICE);
        countDownLatch.countDown();
    }

    @Override
    public void notifyFailure(io.ballerina.runtime.api.values.BError error) {
        countDownLatch.countDown();
        RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_DISPATCH);
        error.printStackTrace();
    }
}
