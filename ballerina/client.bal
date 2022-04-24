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

import ballerina/jballerina.java;
import ballerina/uuid;

# The Ballerina interface to provide AMQP Channel related functionality.
public isolated client class Client {

    private string connectorId = uuid:createType4AsString();

    # Initializes a `rabbitmq:Client` object.
    # ```ballerina
    # rabbitmq:Client rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
    # ```
    #
    # + host - The host used for establishing the connection
    # + port - The port used for establishing the connection
    # + connectionData - The connection configurations
    public isolated function init(string host, int port, *ConnectionConfiguration connectionData) returns Error? {
        return createChannel(host, port, self, connectionData);
    }

    # Declares a non-exclusive, auto-delete, and non-durable queue with the given configurations.
    # ```ballerina
    # check rabbitmqClient->queueDeclare("MyQueue");
    # ```
    #
    # + name - The name of the queue
    # + config - The configurations required to declare a queue
    # + return - `()` if the queue was successfully generated or else a `rabbitmq:Error`
    #               if an I/O error occurred
    isolated remote function queueDeclare(string name, QueueConfig? config = ()) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Declares a queue with a server-generated name.
    # ```ballerina
    # string queueName = check rabbitmqClient->queueAutoGenerate();
    # ```
    #
    # + return - The name of the queue or else a `rabbitmq:Error`
    #             if an I/O error occurred
    isolated remote function queueAutoGenerate() returns string|Error =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Declares a non-auto-delete, non-durable exchange with no extra arguments.
    # If the arguments are specified, then the exchange is declared accordingly.
    # ```ballerina
    # check rabbitmqClient->exchangeDeclare("MyExchange", rabbitmq:DIRECT_EXCHANGE);
    # ```
    #
    # + name - The name of the exchange
    # + exchangeType - The type of the exchange
    # + config - The configurations required to declare an exchange
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function exchangeDeclare(string name,
            ExchangeType exchangeType = DIRECT_EXCHANGE, ExchangeConfig? config = ()) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Binds a queue to an exchange with the given binding key.
    # ```ballerina
    # check rabbitmqClient->queueBind("MyQueue", "MyExchange", "routing-key");
    # ```
    #
    # + queueName - The name of the queue
    # + exchangeName - The name of the exchange
    # + bindingKey - The binding key used to bind the queue to the exchange
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function queueBind(string queueName, string exchangeName, string bindingKey) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Publishes a message. Publishing to a non-existent exchange will result in a channel-level
    # protocol error, which closes the channel.
    # ```ballerina
    # check rabbitmqClient->publishMessage(messageInBytes, "MyQueue");
    # ```
    #
    # + message - The message to be published
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function publishMessage(AnydataMessage message) returns Error? {
        byte[] messageContent;
        anydata anydataContent = message.content;
        if anydataContent is byte[] {
            messageContent = anydataContent;
        } else if anydataContent is xml {
            messageContent = anydataContent.toString().toBytes();
        } else if anydataContent is string {
            messageContent = anydataContent.toBytes();
        } else {
            messageContent = anydataContent.toJsonString().toBytes();
        }
        int? tag = message.deliveryTag;
        BasicProperties? props = message.properties;
        if tag !is int && props !is BasicProperties {
            return publishNative(self, { content: messageContent, routingKey: message.routingKey, exchange: message.exchange});
        } else if tag !is int && props is BasicProperties {
            return publishNative(self, { content: messageContent, routingKey: message.routingKey, exchange: message.exchange, properties: props});
        } else if tag is int && props !is BasicProperties {
            return publishNative(self, { content: messageContent, routingKey: message.routingKey, exchange: message.exchange, deliveryTag: tag});
        }
        return;
    }

    # Retrieves a message synchronously from the given queue providing direct access to the messages in the queue.
    # ```ballerina
    # rabbitmq:AnydataMessage message = check rabbitmqClient->consumeMessage("MyQueue");
    # ```
    #
    # + queueName - The name of the queue
    # + autoAck - If false, should manually acknowledge
    # + return - A `rabbitmq:Message` object containing the retrieved message data or else a`rabbitmq:Error` if an
    #            I/O error occurred
    isolated remote function consumeMessage(string queueName, boolean autoAck = true, typedesc<AnydataMessage> T = <>)
        returns T|Error =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Acknowledges one or several received messages.
    # ```ballerina
    # check rabbitmqClient->basicAck(<message>);
    # ```
    #
    # + message - The message to be acknowledged
    # + multiple - Set to `true` to acknowledge all messages up to and including the called on message and
    #              `false` to acknowledge just the called on message
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function basicAck(Message message, boolean multiple = false) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Rejects one or several received messages.
    # ```ballerina
    # check rabbitmqClient->basicNack(<message>);
    # ```
    #
    # + message - The message to be rejected
    # + multiple - Set to `true` to reject all messages up to and including the called on message and
    #              `false` to reject just the called on message
    # + requeue - `true` if the rejected message(s) should be re-queued rather than discarded/dead-lettered
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function basicNack(Message message, boolean multiple = false, boolean requeue = true)
                            returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Deletes the queue with the given name although it is in use or has messages in it.
    # If the `ifUnused` or `ifEmpty` parameters are given, the queue is checked before deleting.
    # ```ballerina
    # check rabbitmqClient->queueDelete("MyQueue");
    # ```
    #
    # + queueName - The name of the queue to be deleted
    # + ifUnused - True if the queue should be deleted only if it's not in use
    # + ifEmpty - True if the queue should be deleted only if it's empty
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function queueDelete(string queueName, boolean ifUnused = false, boolean ifEmpty = false)
                        returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Deletes the exchange with the given name.
    # ```ballerina
    # check rabbitmqClient->exchangeDelete("MyExchange");
    # ```
    #
    # + exchangeName - The name of the exchange
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function exchangeDelete(string exchangeName) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Purges the content of the given queue.
    # ```ballerina
    # check rabbitmqClient->queuePurge("MyQueue");
    # ```
    #
    # + queueName - The name of the queue
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function queuePurge(string queueName) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Closes the `rabbitmq:Client`.
    # ```ballerina
    # check rabbitmqClient->close();
    # ```
    #
    # + closeCode - The close code (for information, go to the [Reply Codes]
    #               (#https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf))
    # + closeMessage - A message indicating the reason for closing the channel
    # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
    isolated remote function close(int? closeCode = (), string? closeMessage = ()) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;

    # Aborts the RabbitMQ `rabbitmq:Client`. Forces the `rabbitmq:Client` to close and waits for all the close operations
    # to complete. Any encountered exceptions in the close operations are discarded silently.
    # ```ballerina
    # check rabbitmqClient->'abort(320, "Client Aborted");
    # ```
    #
    # + closeCode - The close code (for information, go to the [Reply Codes]
    #               (#https://www.rabbitmq.com/resources/specs/amqp0-9-1.pdf))
    # + closeMessage - A message indicating the reason for closing the channel
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function 'abort(int? closeCode = (), string? closeMessage = ()) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
    } external;
}

isolated function createChannel(string host, int port, Client channelObj, *ConnectionConfiguration config)
returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
} external;

isolated function publishNative(Client clientObj, AnydataMessage message) returns Error? =
@java:Method {
    'class: "io.ballerina.stdlib.rabbitmq.util.ChannelUtils"
} external;
