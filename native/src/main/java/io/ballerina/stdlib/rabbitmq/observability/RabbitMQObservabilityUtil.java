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
import com.rabbitmq.client.Connection;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;

import java.util.regex.Pattern;

/**
 * Util class for Observability-related tasks for RabbitMQ.
 *
 * @since 1.2.0
 */
public class RabbitMQObservabilityUtil {

    private static final Pattern USER_INFO = Pattern.compile("://[^@/]*@");

    // The client library puts the user name in these: amqp://user@host:port/vhost.
    public static String getServerUrl(Connection connection) {
        return withoutUserInfo(connection.toString());
    }

    public static String getChannelName(Channel channel) {
        return withoutUserInfo(channel.toString());
    }

    public static String getQueueName(BObject service) {
        return (String) service.getNativeData(RabbitMQConstants.QUEUE_NAME.getValue());
    }

    private static String withoutUserInfo(String url) {
        return USER_INFO.matcher(url).replaceFirst("://");
    }
}
