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

package io.ballerina.stdlib.rabbitmq.util;

import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.transactions.TransactionResourceManager;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;
import io.ballerina.stdlib.rabbitmq.RabbitMQUtils;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQTracingUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Util class for RabbitMQ Message handling.
 *
 * @since 1.1.0
 */
public class MessageUtils {

    public static Object basicAck(Environment environment, BObject callerObj, boolean multiple) {
        Channel channel = (Channel) callerObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        int deliveryTag =
                Integer.parseInt(callerObj.getNativeData(RabbitMQConstants.DELIVERY_TAG.getValue()).toString());
        boolean ackStatus = (boolean) callerObj.getNativeData(RabbitMQConstants.ACK_STATUS);
        boolean ackMode = (boolean) callerObj.getNativeData(RabbitMQConstants.ACK_MODE);
        if (ackStatus) {
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.MULTIPLE_ACK_ERROR);
        } else if (ackMode) {
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.ACK_MODE_ERROR);
        } else {
            try {
                channel.basicAck(deliveryTag, multiple);
                callerObj.addNativeData(RabbitMQConstants.ACK_STATUS, true);
                RabbitMQMetricsUtil.reportAcknowledgement(channel, RabbitMQObservabilityConstants.ACK);
                RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
                if (TransactionResourceManager.getInstance().isInTransaction()) {
                    RabbitMQUtils.handleTransaction(callerObj);
                }
            } catch (IOException | AlreadyClosedException exception) {
                RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_ACK);
                return RabbitMQUtils.returnErrorValue(RabbitMQConstants.ACK_ERROR + exception.getMessage());
            }
        }
        return null;
    }

    public static Object basicNack(Environment environment, BObject callerObj, boolean multiple, boolean requeue) {
        Channel channel = (Channel) callerObj.getNativeData(RabbitMQConstants.CHANNEL_NATIVE_OBJECT);
        boolean ackStatus = (boolean) callerObj.getNativeData(RabbitMQConstants.ACK_STATUS);
        boolean ackMode = (boolean) callerObj.getNativeData(RabbitMQConstants.ACK_MODE);
        int deliveryTag =
                Integer.parseInt(callerObj.getNativeData(RabbitMQConstants.DELIVERY_TAG.getValue()).toString());
        if (ackStatus) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_NACK);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.MULTIPLE_ACK_ERROR);
        } else if (ackMode) {
            RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_NACK);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.ACK_MODE_ERROR);
        } else {
            try {
                channel.basicNack(deliveryTag, multiple, requeue);
                callerObj.addNativeData(RabbitMQConstants.ACK_STATUS, true);
                RabbitMQMetricsUtil.reportAcknowledgement(channel, RabbitMQObservabilityConstants.NACK);
                RabbitMQTracingUtil.traceResourceInvocation(channel, environment);
                if (TransactionResourceManager.getInstance().isInTransaction()) {
                    RabbitMQUtils.handleTransaction(callerObj);
                }
            } catch (IOException | AlreadyClosedException exception) {
                RabbitMQMetricsUtil.reportError(channel, RabbitMQObservabilityConstants.ERROR_TYPE_NACK);
                return RabbitMQUtils.returnErrorValue(RabbitMQConstants.NACK_ERROR
                        + exception.getMessage());
            }
        }
        return null;
    }

    public static byte[] convertDataIntoByteArray(Object data) {
        int typeTag = TypeUtils.getType(data).getTag();
        if (typeTag == TypeTags.STRING_TAG) {
            return ((BString) data).getValue().getBytes(StandardCharsets.UTF_8);
        } else {
            return ((BArray) data).getBytes();
        }
    }

    private MessageUtils() {
    }
}
