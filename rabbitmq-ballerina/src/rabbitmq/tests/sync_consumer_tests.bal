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

import ballerina/test;

// Retrieve messages from a queue with messages.
@test:Config {
    dependsOn: ["testPublish"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGet() {
    string message = "Testing Basic Get";
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        produceMessage(message, QUEUE);
        Message|Error getResult = channelObj->basicGet(QUEUE, AUTO_ACK);
        if (getResult is Error) {
            test:assertFail("Pulling a message from the broker caused an error.");
        } else {
            string messageReceived = checkpanic getResult.getTextContent();
            test:assertEquals(messageReceived, message, msg = "Message received does not match.");
        }
    }
}

// Retrieve messages from a queue without messages.
@test:Config {
    dependsOn: ["testQueueDeclare"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetNegative() {
    string queueName = "testBasicGetNegative";
    Channel channelObj = new (successConfig);
    declareQueue(queueName);
    Message|Error getResult = channelObj->basicGet(queueName, AUTO_ACK);
    if (getResult is Error) {
        test:assertEquals(getResult.message(), "No messages are found in the queue.",
            msg = "Error does not match.");
    } else {
        test:assertFail("Pulling a message from the broker caused an error.");
    }
}

// Retrieve messages from a non-declared queue.
@test:Config {
    dependsOn: ["testChannelWithConnection"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetNegative2() {
    string queueName = "testBasicGetNegative2";
    Channel channelObj = new (successConfig);
    Message|Error getResult = channelObj->basicGet(queueName, AUTO_ACK);
    if (getResult is Error) {
        test:assertEquals(getResult.message(), "Error occurred while retrieving the message: Queue does not exist.",
            msg = "Error does not match.");
    } else {
        test:assertFail("Error expected.");
    }
}

// Retrieve messages using a closed channel.
@test:Config {
    dependsOn: ["testChannelClose"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetNegative3() {
    string expected = "Error occurred while retrieving the message: channel is already closed due to clean channel " +
        "shutdown; protocol method: #method<channel.close>(reply-code=200, reply-text=OK, class-id=0, method-id=0)";
    string queueName = "testBasicGetNegative3";
    Channel channelObj = new (successConfig);
    checkpanic channelObj.close();
    Message|Error getResult = channelObj->basicGet(queueName, AUTO_ACK);
    if (getResult is Error) {
        test:assertEquals(getResult.message(), expected, msg = "Error does not match.");
    } else {
        test:assertFail("Error expected.");
    }
}

// Retrieve messages with manual acknowledgment.
@test:Config {
    dependsOn: ["testPublish"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetClientAck() {
    string message = "Testing Basic Get With Client Ack";
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        produceMessage(message, QUEUE);
        Message getResult = checkpanic channelObj->basicGet(QUEUE, CLIENT_ACK);
        Error? ackResult = getResult->basicAck();
        if (ackResult is Error) {
            test:assertFail("Acknowledging a message from the broker caused an error.");
        } else {
            string messageReceived = checkpanic getResult.getTextContent();
            test:assertEquals(messageReceived, message, msg = "Message received does not match.");
        }
    }
}

// Retrieve messages with auto-ack mode and manual acknowledge.
@test:Config {
    dependsOn: ["testPublish", "testChannelClose"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetAutoAckNegative() {
    string queue = "testBasicGetAutoAckNegative";
    string message = "Testing Basic Get With Auto Ack Negative";
    Channel channelObj = new (successConfig);
    declareQueue(queue);
    produceMessage(message, queue);
    Message getResult = checkpanic channelObj->basicGet(queue, AUTO_ACK);
    Error? ackResult = getResult->basicAck();
    if (ackResult is Error) {
        test:assertEquals(ackResult.message(), "Trying to acknowledge messages in auto-ack mode.",
            msg = "Error does not match.");
    } else {
        test:assertFail("Acknowledging a message from the broker caused an error.");
    }
    checkpanic channelObj.close();
}

// Retrieve messages with client-ack mode and manually negative acknowledge.
@test:Config {
    dependsOn: ["testPublish"],
    groups: ["rabbitmq-sync"]
}
public function testBasicGetClientNack() {
    string message = "Testing Basic Get With Client Nack";
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        produceMessage(message, QUEUE);
        Message getResult = checkpanic channelObj->basicGet(QUEUE, CLIENT_ACK);
        Error? ackResult = getResult->basicNack();
        if (ackResult is Error) {
            test:assertFail("Acknowledging a message from the broker caused an error.");
        } else {
            Message result = checkpanic channelObj->basicGet(QUEUE, AUTO_ACK);
            string messageReceived = checkpanic result.getTextContent();
            test:assertEquals(messageReceived, message, msg = "Message received does not match.");
        }
    }
}

// Send message to the reply-header of the message received.
@test:Config {
    dependsOn: ["testPublish"],
    groups: ["rabbitmq-sync"]
}
public function testGetProperties() {
    string message = "Testing Get Properties";
    string queueName = "getProps";
    string replyTo = "getPropsReply";
    Channel channelObj = new (successConfig);
    declareQueue(queueName);
    BasicProperties props = { replyTo: replyTo };
    checkpanic channelObj->basicPublish(message, queueName, "", props);
    Message|Error getResult = channelObj->basicGet(queueName, AUTO_ACK);
    if (getResult is Error) {
        test:assertFail("Pulling a message from the broker caused an error.");
    } else {
        string messageReceived = checkpanic getResult.getTextContent();
        test:assertEquals(messageReceived, message, msg = "Message received does not match.");
        BasicProperties|Error receivedMessageProps = getResult.getProperties();
        if (receivedMessageProps is Error) {
            test:assertFail("Getting message properties caused an error.");
        } else {
            string? receivedReplyTo = receivedMessageProps?.replyTo;
            if (receivedReplyTo is string) {
                test:assertEquals(receivedReplyTo, replyTo, msg = "ReplyTo queue name does not match.");
            } else {
                test:assertFail("Getting replyTo queue name caused an error.");
            }
        }
    }
    checkpanic channelObj.close();
}

// Retrieve the delivery tag of the message received.
@test:Config {
    dependsOn: ["testPublish"],
    groups: ["rabbitmq-sync"]
}
public function testGetDeliveryTag() {
    string message = "Testing Get Delivery Tag";
    string queueName = "deliveryTag";
    Channel channelObj = new (successConfig);
    declareQueue(queueName);
    produceMessage(message, queueName);
    Message|Error getResult = channelObj->basicGet(queueName, AUTO_ACK);
    if (getResult is Error) {
        test:assertFail("Pulling a message from the broker caused an error.");
    } else {
        string messageReceived = checkpanic getResult.getTextContent();
        int deliveryTag = <@untainted> getResult.getDeliveryTag();
        test:assertEquals(deliveryTag, 1, msg = "Delivery tag does not match.");
        test:assertEquals(messageReceived, message, msg = "Message received does not match.");
    }
    checkpanic channelObj.close();
}
