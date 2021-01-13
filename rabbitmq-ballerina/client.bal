// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/java;
import ballerina/uuid;

# The Ballerina interface to provide AMQP Channel related functionality.
public client class Client {

    handle amqpChannel = JAVA_NULL;
    string connectorId = uuid:createType4AsString();

    # Initializes a `rabbitmq:Client` object.
    #
    # + connectionData - A connection configuration
    public isolated function init(ConnectionConfig connectionData = {}) returns Error? {
        handle|Error channelResult = createChannel(connectionData, self);
        if (channelResult is handle) {
            self.amqpChannel = channelResult;
            return;
        } else {
            return channelResult;
        }
    }

    # Declares a non-exclusive, auto-delete, or non-durable queue with the given configurations.
    # ```ballerina
    # string|rabbitmq:Error? queueResult = newClient->queueDeclare("MyQueue");
    # ```
    #
    # + name - Name of the queue
    # + config - Configurations required to declare a queue
    # + return - `()` if the queue was successfully generated or else a `rabbitmq:Error`
    #               if an I/O error is encountered
    isolated remote function queueDeclare(string name, QueueConfig? config = ()) returns Error? {
        return nativeQueueDeclare(name, config, self.amqpChannel);
    }

    # Declares a non-auto-delete, non-durable exchange with no extra arguments.
    # If the arguments are specified, then the exchange is declared accordingly.
    # ```ballerina
    # rabbitmq:Error? exchangeResult = newChannel->exchangeDeclare("MyExchange",
    #                                               rabbitmq:DIRECT_EXCHANGE);
    # ```
    #
    # + name - The name of the exchange
    # + exchangeType - The type of the exchange
    # + config - Configurations required to declare an exchange
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function exchangeDeclare(string name,
            ExchangeType exchangeType = DIRECT_EXCHANGE, ExchangeConfig? config = ()) returns Error? {
        return nativeExchangeDeclare(name, exchangeType, config, self.amqpChannel);
    }

    # Binds a queue to an exchange with the given binding key.
    # ```ballerina
    # rabbitmq:Error? bindResult = newChannel->queueBind("MyQueue", "MyExchange", "routing-key");
    # ```
    #
    # + queueName - Name of the queue
    # + exchangeName - Name of the exchange
    # + bindingKey - Binding key used to bind the queue to the exchange
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function queueBind(string queueName, string exchangeName, string bindingKey) returns Error? {
         return nativeQueueBind(queueName, exchangeName, bindingKey, self.amqpChannel);
    }

    # Publishes a message. Publishing to a non-existent exchange will result in a channel-level
    # protocol error, which closes the channel.
    # ```ballerina
    # rabbitmq:Error? sendResult = newChannel->basicPublish(messageInBytes, "MyQueue");
    # ```
    #
    # + data - The message body
    # + routingKey - The routing key
    # + exchangeName - The name of the exchange to which the message is published
    # + properties - Other properties for the message (routing headers, etc.)
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function basicPublish(@untainted byte[] data, string routingKey,
                        string exchangeName = "", BasicProperties? properties = ()) returns Error? {
        return nativeBasicPublish(data, routingKey, exchangeName, properties, self.amqpChannel, self);
    }

    # Deletes the queue with the given name although it is in use or has messages in it.
    # If the `ifUnused` or `ifEmpty` parameters are given, the queue is checked before deleting.
    # ```ballerina
    # rabbitmq:Error? deleteResult = newChannel->queueDelete("MyQueue");
    # ```
    #
    # + queueName - Name of the queue to be deleted
    # + ifUnused - True if the queue should be deleted only if it's not in use
    # + ifEmpty - True if the queue should be deleted only if it's empty
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function queueDelete(string queueName, boolean ifUnused = false, boolean ifEmpty = false)
                        returns Error? {
        return nativeQueueDelete(queueName, ifUnused, ifEmpty, self.amqpChannel);
    }

    # Deletes the exchange with the given name.
    # ```ballerina
    # rabbitmq:Error? deleteResult = newChannel->exchangeDelete("MyExchange");
    # ```
    #
    # + exchangeName - The name of the exchange
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function exchangeDelete(string exchangeName) returns Error? {
        return nativeExchangeDelete(exchangeName, self.amqpChannel);
    }

    # Purges the content of the given queue.
    # ```ballerina
    # rabbitmq:Error? purgeResult = newChannel->queuePurge("MyQueue");
    # ```
    #
    # + queueName - The name of the queue
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function queuePurge(string queueName) returns Error? {
        return nativeQueuePurge(queueName, self.amqpChannel);
    }

    # Closes the `rabbitmq:Client`.
    # ```ballerina
    # rabbitmq:Error? closeResult = newClient.close();
    # ```
    #
    # + closeCode - The close code (for information, go to the "Reply Codes" section in the
    #               [AMQP 0-9-1 specification] (#https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf))
    # + closeMessage - A message indicating the reason for closing the channel
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated function close(int? closeCode = (), string? closeMessage = ()) returns Error? {
        return nativeClientClose(closeCode, closeMessage, self.amqpChannel);
    }

    # Aborts the RabbitMQ `rabbitmq:Client`. Forces the `rabbitmq:Client` to close and waits for all the close operations
    # to complete. Any encountered exceptions in the close operations are discarded silently.
    # ```ballerina
    # rabbitmq:Error? abortResult = newClient.abort(320, "Client Aborted");
    # ```
    #
    # + closeCode - The close code (for information, go to the "Reply Codes" section in the
    #               [AMQP 0-9-1 specification] (#https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf))
    # + closeMessage - A message indicating the reason for closing the channel
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated function 'abort(int? closeCode = (), string? closeMessage = ()) returns Error? {
        return nativeClientAbort(closeCode, closeMessage, self.amqpChannel);
    }

    isolated function getChannel() returns handle {
        return self.amqpChannel;
    }
}

isolated function createChannel(ConnectionConfig config, Client channelObj) returns handle|Error =
@java:Method {
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeQueueDeclare(string name, QueueConfig? config, handle amqpChannel) returns Error? =
@java:Method {
    name: "queueDeclare",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeExchangeDeclare(string name, ExchangeType exchangeType, ExchangeConfig? config,
handle amqpChannel) returns Error? =
@java:Method {
    name: "exchangeDeclare",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeQueueBind(string queueName, string exchangeName, string bindingKey, handle amqpChannel)
returns Error? = @java:Method {
    name: "queueBind",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeBasicPublish(byte[] messageContent, string routingKey, string exchangeName,
BasicProperties? properties, handle amqpChannel, Client channelObj) returns Error? =
@java:Method {
    name: "basicPublish",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeQueueDelete(string queueName, boolean ifUnused, boolean ifEmpty, handle amqpChannel) returns
Error? = @java:Method {
    name: "queueDelete",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeExchangeDelete(string exchangeName, handle amqpChannel) returns Error? =
@java:Method {
    name: "exchangeDelete",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeQueuePurge(string queueName, handle amqpChannel) returns Error? =
@java:Method {
    name: "queuePurge",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeClientClose(int? closeCode, string? closeMessage, handle amqpChannel) returns Error? =
@java:Method {
    name: "close",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;

isolated function nativeClientAbort(int? closeCode, string? closeMessage, handle amqpChannel) returns Error? =
@java:Method {
    name: "abort",
    'class: "org.ballerinalang.messaging.rabbitmq.util.ChannelUtils"
} external;
