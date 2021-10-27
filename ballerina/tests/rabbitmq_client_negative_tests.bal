// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

@test:Config {
    groups: ["rabbitmq"]
}
public isolated function testConnectionNegative() returns error? {
    Client|error newClient = new(DEFAULT_HOST, 5000);
    if !(newClient is error) {
        test:assertFail("Error expected for connection with incorrect port.");
    }
    return;
}

@test:Config {
    groups: ["rabbitmq"]
}
public isolated function testConnectionNegative2() returns error? {
    Listener|error newLis = new(DEFAULT_HOST, 5000);
    if !(newLis is error) {
        test:assertFail("Error expected for connection with incorrect port.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueDeleteNegative() returns error? {
    string queue = "testQueueDeleteNegative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->close(200, "Client closed");
    error? deleteResult = newClient->queueDelete(queue);
    if !(deleteResult is error) {
        test:assertFail("Error expected when trying to delete a non existent queue.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueConfigNegative() returns error? {
    string queueName = "testQueueConfigNegative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    QueueConfig queueConfig = { durable: true, exclusive: true, autoDelete: false };
    check newClient->close();
    Error? result = newClient->queueDeclare(queueName, config = queueConfig);
    if !(result is error) {
       test:assertFail("Error expected when when trying to create a queue.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testExchangeDeclareNegative() returns error? {
    string name = "testExchangeDeclareNegative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->close();
    Error? result = newClient->exchangeDeclare(name, DIRECT_EXCHANGE);
    if !(result is error) {
       test:assertFail("Error expected when trying to create a direct exchange.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueAutoGenerateNegative() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->close();
    Error|string queueResult = newClient->queueAutoGenerate();
    if !(queueResult is error) {
        test:assertFail("Error expected when  when trying to create an auto generated queue.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientConsumeNegative() returns error? {
    string queue = "testClientConsumeNegative";
    string message = "Test client consume negative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    check newClient->close();
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        test:assertFail("Error expected when trying to consume messages using client.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientBasicAckNegative() returns error? {
    string queue = "testClientBasicAckNegative";
    string message = "Test client basic ack negative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        string messageContent = check 'string:fromBytes(consumeResult.content);
        log:printInfo("The message received: " + messageContent);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
        check newClient->close();
        Error? ackResult = newClient->basicAck(consumeResult, false);
        if !(ackResult is Error) {
            test:assertFail("Error expected when trying to acknowledge the message using client.");
        }
    } else {
        test:assertFail("Error when trying to consume messages using client.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientBasicNackNegative() returns error? {
    string queue = "testClientBasicNackNegative";
    string message = "Test client basic nack negative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        string messageContent = check 'string:fromBytes(consumeResult.content);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
        log:printInfo("The message received: " + messageContent);
        check newClient->close();
        Error? ackResult = newClient->basicNack(consumeResult, false, false);
        if !(ackResult is Error) {
            test:assertFail("Error expected when trying to acknowledge the message using client.");
        }
    } else {
        test:assertFail("Error when trying to consume messages using client.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueBindNegative() returns error? {
    string exchange = "testQueueBindNegativeExchange";
    string queue = "testQueueBindNegativeQueue";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->exchangeDeclare(exchange, DIRECT_EXCHANGE);
    check newClient->queueDeclare(queue);
    check newClient->close();
    Error? result = newClient->queueBind(queue, exchange, "myBinding");
    if !(result is error) {
        test:assertFail("Error expected when trying to bind a queue.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testPublishNegative() returns error? {
    string queue = "testPublishNegative";
    string message = "Test client publish negative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->close();
    error? pubResult = newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    if !(pubResult is error) {
        test:assertFail("Error expected when trying to publish messages using client.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testCloseNegative() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->close();
    error? closeResult = newClient->close();
    if !(closeResult is error) {
       test:assertFail("Error expected when trying to close the client twice.");
    }
    return;
}


