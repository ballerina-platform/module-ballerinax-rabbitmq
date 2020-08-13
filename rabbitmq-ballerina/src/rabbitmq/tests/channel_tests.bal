// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;
import ballerina/test;

// Successful creation of a channel using an already created connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelWithConnection() {
    Connection? connection = sharedConnection;
    if (connection is Connection) {
        Channel|error newChannel = new (connection);
        if (newChannel is error) {
            log:printError("Error occurred while creating the channel.", newChannel);
            test:assertFail("RabbitMQ Channel creation failed.");
        } else {
            sharedChannel = newChannel;
        }
    }
}

// Successful creation of a channel using the connection configuration.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelWithConnectionConfig() {
    Channel|error newChannel = new (successConfig);
    if (newChannel is error) {
        log:printError("Error occurred while creating the channel.", newChannel);
        test:assertFail("RabbitMQ Channel creation failed.");
    }
}

// Creation of a channel using a closed connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelWithClosedConnection() {
    string expectedClosed = "Error occurred while initializing the channel: connection is already closed due to " +
        "clean connection shutdown; protocol method: #method<connection.close>(reply-code=200, reply-text=OK, " +
        "class-id=0, method-id=0)";
    Connection? connection = isClosedConnection;
    if (connection is Connection) {
        Channel|error newChannel = trap new (connection);
        if (newChannel is error) {
            string message = newChannel.message();
            test:assertEquals(message, expectedClosed, msg = "Error occurred does not match.");
        }
    }
}

// Creating a queue with auto-generated queue name.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testAutoGenQueue() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        string|Error? autoGenQueue = rabbitChannel->queueDeclare();
        if (autoGenQueue is string) {
            log:printInfo("Server generated queue name in `testAutoGenQueue`: " + autoGenQueue);
        } else {
            test:assertFail("Creating a queue with server generated name failed.");
        }
    }
}

// Creating a queue with a given queue name.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testQueueDeclare() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        string|Error? autoGenQueue = rabbitChannel->queueDeclare({queueName: QUEUE});
        if (autoGenQueue is Error) {
            test:assertFail("Creating a queue with a given name failed.");
        }
    }
}

// Declaring an exchange of type DIRECT.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testDeclareDirectExchange() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Error? exchangeResult = rabbitChannel->exchangeDeclare({ exchangeName: DIRECT_EXCHANGE_NAME,
                                                                 exchangeType: DIRECT_EXCHANGE });
        if (exchangeResult is Error) {
            test:assertFail("Creating a direct exchange with a given name failed.");
        }
    }
}

// Declaring an exchange of type TOPIC.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testDeclareTopicExchange() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Error? exchangeResult = rabbitChannel->exchangeDeclare({ exchangeName: TOPIC_EXCHANGE_NAME,
                                                                 exchangeType: TOPIC_EXCHANGE });
        if (exchangeResult is Error) {
            test:assertFail("Creating a direct exchange with a given name failed.");
        }
    }
}

// Declaring an exchange of type FANOUT.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testDeclareFanoutExchange() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Error? exchangeResult = rabbitChannel->exchangeDeclare({ exchangeName: FANOUT_EXCHANGE_NAME,
                                                                 exchangeType: FANOUT_EXCHANGE });
        if (exchangeResult is Error) {
            test:assertFail("Creating a direct exchange with a given name failed.");
        }
    }
}

// Retrieve the underlying connection of the channel.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testGetConnection() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Connection|Error connection = rabbitChannel.getConnection();
        if (connection is Error) {
            test:assertFail("Retrieving the connection from the channel failed.");
        }
    }
}

// Bind already declared queue and exchange.
@test:Config {
    dependsOn: ["testChannelWithConnection", "testQueueDeclare", "testDeclareDirectExchange"],
    groups: ["rabbitmq-channel"]
}
public function testQueueBind() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Error? bindResult = rabbitChannel->queueBind(QUEUE, DIRECT_EXCHANGE_NAME, QUEUE);
        if (bindResult is Error) {
            test:assertFail("Binding the existing queue failed.");
        }
    }
}

// Bind non existing queue and exchange.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testQueueBindNegative() {
    string expected = "Error occurred while binding the queue: Given queue or exchange does not exist.";
    Channel rabbitChannel = new (successConfig);
    Error? bindResult = rabbitChannel->queueBind("1234", "1234", "1234");
    if (bindResult is Error) {
        string message = bindResult.message();
        test:assertEquals(message, expected, msg = "Error message does not match.");
    }
}

// Purge existing queue.
@test:Config {
    dependsOn: ["testChannelWithConnection", "testQueueDeclare"],
    groups: ["rabbitmq-channel"]
}
public function testQueuePurge() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        Error? bindResult = rabbitChannel->queuePurge(QUEUE);
        if (bindResult is Error) {
            test:assertFail("Binding the existing queue failed.");
        }
    }
}

// Purge non-declared queue.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testQueuePurgeNegative() {
    string expected = "Error occurred while purging the queue: Queue does not exist.";
    Channel rabbitChannel = new (successConfig);
    Error? bindResult = rabbitChannel->queuePurge("4321");
    if (bindResult is Error) {
        string message = bindResult.message();
        test:assertEquals(message, expected, msg = "Error message does not match.");
    }
}

// Publish messages to an existing queue.
@test:Config {
    dependsOn: ["testChannelWithConnection", "testQueuePurge"],
    groups: ["rabbitmq-channel"]
}
public function testPublish() {
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        Error? producerResult = channelObj->basicPublish("Hello from Ballerina", QUEUE);
        if (producerResult is Error) {
            test:assertFail("Producing a message to the broker caused an error.");
        }
        checkpanic channelObj->queuePurge(QUEUE);
    }
}

// Publish messages to a non-declared queue.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testPublishNegative() {
    string expected = "";
    Channel channelObj = new (successConfig);
    Error? producerResult = channelObj->basicPublish("Hello from Ballerina", "WhatQueue");
    if (producerResult is Error) {
        string message = producerResult.message();
        test:assertEquals(message, expected, msg = "Error message does not match.");
    }
}

// Publish messages with a closed channel.
@test:Config {
    dependsOn: ["testChannelWithConnection", "testChannelClose"],
    groups: ["rabbitmq-channel"]
}
public function testPublishNegative2() {
    string expectedChannelClosed = "Error occurred while publishing the message: channel is already closed due to " +
        "clean channel shutdown; protocol method: #method<channel.close>(reply-code=200, reply-text=OK, class-id=0, " +
        "method-id=0)";
    Channel channelObj = new (successConfig);
    checkpanic channelObj.close();
    Error? producerResult = channelObj->basicPublish("Hello from Ballerina", QUEUE);
    if (producerResult is Error) {
        string message = producerResult.message();
        test:assertEquals(message, expectedChannelClosed, msg = "Error message does not match.");
    }
}

// Close an active channel.
@test:Config {
     dependsOn: ["testChannelWithConnection"],
     groups: ["rabbitmq-channel"]
}
public function testChannelClose() {
    Channel channelObj = new (successConfig);
    Error? closeResult = channelObj.close();
    if (closeResult is Error) {
        test:assertFail("Closing an active channel failed.");
    }
}

// Close an active channel with parameters.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelCloseWithParams() {
    Channel channelObj = new (successConfig);
    Error? closeResult = channelObj.close(CLOSE_CODE, "Closing the channel.");
    if (closeResult is Error) {
        test:assertFail("Closing an active channel failed.");
    }
}

// Close an already closed channel.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelCloseNegative() {
    string expectedMessage = "Error occurred while closing the channel: channel is already closed due to clean " +
    "channel shutdown; protocol method: #method<channel.close>(reply-code=200, reply-text=OK, class-id=0, method-id=0)";
    Channel channelObj = new (successConfig);
    Error? closeResult = channelObj.close();
    if (closeResult is Error) {
        test:assertFail("Closing an active channel failed.");
    } else {
        Error? closeResult2 = channelObj.close();
        if (closeResult2 is Error) {
            string message = closeResult2.message();
            test:assertEquals(message, expectedMessage, msg = "Error message does not match.");
        } else {
            test:assertFail("Error expected.");
        }
    }
}

// Abort an active channel.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelAbort() {
    Channel channelObj = new (successConfig);
    Error? closeResult = channelObj.abortChannel();
    if (closeResult is Error) {
        test:assertFail("Aborting an active channel failed.");
    }
}

// Abort an active channel with parameters.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testChannelAbortWithParams() {
    Channel channelObj = new (successConfig);
    Error? closeResult = channelObj.close(CLOSE_CODE, "Aborting the channel.");
    if (closeResult is Error) {
        test:assertFail("Aborting an active channel failed.");
    }
}

// Delete an already existing queue.
@test:Config {
    dependsOn: ["testQueueDeclare"],
    groups: ["rabbitmq-channel"]
}
public function testQueueDelete() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        string? result = checkpanic rabbitChannel->queueDeclare({queueName: DELETE_QUEUE});
        Error? deleteResult = rabbitChannel->queueDelete(DELETE_QUEUE);
        if (deleteResult is Error) {
            test:assertFail("Deleting the existing queue failed.");
        }
    }
}

// Delete a non-declared queue.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testQueueDeleteNegative() {
    string expected = "";
    Channel channelObj = new (successConfig);
    Error? deleteResult = channelObj->queueDelete("del1234");
    if (deleteResult is Error) {
        string message = deleteResult.message();
        test:assertEquals(message, expected, msg = "Error message does not match.");
    } else {
        test:assertFail("Error expected.");
    }
}

// Delete an already existing exchange.
@test:Config {
    dependsOn: ["testDeclareDirectExchange"],
    groups: ["rabbitmq-channel"]
}
public function testExchangeDelete() {
    Channel? rabbitChannel = sharedChannel;
    if (rabbitChannel is Channel) {
        checkpanic rabbitChannel->exchangeDeclare({ exchangeName: DELETE_EXCHANGE_NAME,
                                                    exchangeType: DIRECT_EXCHANGE });
        Error? deleteResult = rabbitChannel->exchangeDelete(DELETE_EXCHANGE_NAME);
        if (deleteResult is Error) {
            test:assertFail("Deleting the existing exchange failed.");
        }
    }
}

// Delete a non-declared exchange.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-channel"]
}
public function testExchangeDeleteNegative() {
    string expected = "";
    Channel channelObj = new (successConfig);
    Error? deleteResult = channelObj->exchangeDelete(DELETE_EXCHANGE_NAME);
    if (deleteResult is Error) {
        string message = deleteResult.message();
        test:assertEquals(message, expected, msg = "Error message does not match.");
    } else {
        test:assertFail("Error expected.");
    }
}
