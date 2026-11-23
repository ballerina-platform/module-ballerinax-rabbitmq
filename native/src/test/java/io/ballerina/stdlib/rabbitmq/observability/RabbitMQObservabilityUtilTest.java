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
import com.rabbitmq.client.Connection;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Tests that the url and channel tags never carry the user name.
 */
public class RabbitMQObservabilityUtilTest {

    // Contains "@host:port", which a string search for the host and port would match first.
    private static final String USER_NAME = "team@127.0.0.1:5672/alice";

    @Test
    public void testUserNameIsNotInTags() throws UnknownHostException {
        Connection connection = connection("amqp://" + USER_NAME + "@127.0.0.1:5672/");
        RabbitMQObservabilityUtil.registerVirtualHost(connection, "/");

        Assert.assertEquals(RabbitMQObservabilityUtil.getServerUrl(connection), "amqp://127.0.0.1:5672/");
        Assert.assertEquals(RabbitMQObservabilityUtil.getChannelName(channel(connection, 3)),
                "AMQChannel(amqp://127.0.0.1:5672/,3)");
    }

    @Test
    public void testVirtualHostIsKept() throws UnknownHostException {
        Connection connection = connection("amqp://" + USER_NAME + "@127.0.0.1:5672/orders");
        RabbitMQObservabilityUtil.registerVirtualHost(connection, "orders");

        Assert.assertEquals(RabbitMQObservabilityUtil.getServerUrl(connection), "amqp://127.0.0.1:5672/orders");
        Assert.assertEquals(RabbitMQObservabilityUtil.getChannelName(channel(connection, 1)),
                "AMQChannel(amqp://127.0.0.1:5672/orders,1)");
    }

    // A connection whose string form holds the raw user name, as the client library formats it.
    private static Connection connection(String text) throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAddress" -> address;
                    case "getPort" -> 5672;
                    case "toString" -> text;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static Channel channel(Connection connection, int number) {
        return (Channel) Proxy.newProxyInstance(Channel.class.getClassLoader(), new Class<?>[]{Channel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getConnection" -> connection;
                    case "getChannelNumber" -> number;
                    case "toString" -> "AMQChannel(" + connection + "," + number + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
