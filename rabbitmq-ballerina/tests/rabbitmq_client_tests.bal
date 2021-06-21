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

import ballerina/lang.'string;
import ballerina/log;
import ballerina/lang.runtime as runtime;
import ballerina/test;

Client? rabbitmqChannel = ();
Listener? rabbitmqListener = ();
const QUEUE = "MyQueue";
const ACK_QUEUE = "MyAckQueue";
const NACK_QUEUE = "MyNackQueue";
const MOCK_QUEUE = "MockQueue";
const DIRECT_EXCHANGE_NAME = "MyDirectExchange";
const TOPIC_EXCHANGE_NAME = "MyTopicExchange";
const FANOUT_EXCHANGE_NAME = "MyFanoutExchange";
const SYNC_NEGATIVE_QUEUE = "MySyncNegativeQueue";
const DATA_BINDING_QUEUE = "MyDataQueue";
string asyncConsumerMessage = "";
string replyMessage = "";
string dataBindingMessage = "";
string REPLYTO = "replyHere";

@test:BeforeSuite
function setup() returns error? {
    log:printInfo("Creating a ballerina RabbitMQ channel.");
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    rabbitmqChannel = newClient;
    Client? clientObj = rabbitmqChannel;
    if (clientObj is Client) {
        check clientObj->queueDeclare(QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_QUEUE);
        check clientObj->queueDeclare(SYNC_NEGATIVE_QUEUE);
        check clientObj->queueDeclare(ACK_QUEUE);
        check clientObj->queueDeclare(NACK_QUEUE);
        check clientObj->queueDeclare(REPLYTO);
    }
    Listener lis = check new(DEFAULT_HOST, DEFAULT_PORT);
    rabbitmqListener = lis;
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testClient() returns error? {
    boolean flag = false;
    Client? con = rabbitmqChannel;
    if (con is Client) {
        flag = true;
    }
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient.close();
    test:assertTrue(flag, msg = "RabbitMQ Connection creation failed.");
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public function testProducer() returns error? {
    Client? channelObj = rabbitmqChannel;
    if (channelObj is Client) {
        string message = "Hello from Ballerina";
        check channelObj->publishMessage({ content: message.toBytes(), routingKey: QUEUE });
        check channelObj->queuePurge(QUEUE);
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public function testProducerTransactional() returns error? {
    string queue = "testProducerTransactional";
    string message = "Test producing transactionally";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    transaction {
        check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
        var commitResult = commit;
        if !(commitResult is ()) {
            test:assertFail(msg = "Commit failed for transactional producer.");
        }
    }
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        string messageContent = check 'string:fromBytes(consumeResult.content);
        log:printInfo("The message received: " + messageContent);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
    } else {
        test:assertFail("Error when trying to consume messages using client.");
    }
    check newClient.close();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListener() {
    boolean flag = false;
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        flag = true;
    }
    test:assertTrue(flag, msg = "RabbitMQ Listener creation failed.");
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerWithQos() {
    Listener|Error qosListener1 = new(DEFAULT_HOST, DEFAULT_PORT, qosSettings = { prefetchCount: 10 });
    if (qosListener1 is Error) {
        test:assertFail("RabbitMQ Listener initialization with qos settings failed.");
    }
    Listener|Error qosListener2 = new(DEFAULT_HOST, DEFAULT_PORT, qosSettings =
                                                            { prefetchCount: 10, prefetchSize: 0, global: true });
    if (qosListener2 is Error) {
        test:assertFail("RabbitMQ Listener initialization with qos settings failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["rabbitmq"]
}
public function testSyncConsumer() returns error? {
    string message = "Testing Sync Consumer";
    check produceMessage(message, QUEUE);
    Client? channelObj = rabbitmqChannel;
    if (channelObj is Client) {
        Message getResult = check channelObj->consumeMessage(QUEUE);
        string messageContent = check 'string:fromBytes(getResult.content);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
    }
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testAsyncConsumer() returns error? {
    string message = "Testing Async Consumer";
    check produceMessage(message, QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        check channelListener.attach(asyncTestService);
        check channelListener.attach(replyService);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(asyncConsumerMessage, message, msg = "Message received does not match.");
    }
}

@test:Config {
    dependsOn: [testListener],
    groups: ["rabbitmq"]
}
public isolated function testGracefulStop() returns error? {
    Listener channelListener = check new(DEFAULT_HOST, DEFAULT_PORT);
    error? stopResult = channelListener.gracefulStop();
    if stopResult is error {
        test:assertFail("Error when trying to close the listener gracefully.");
    }
}

@test:Config {
    dependsOn: [testListener],
    groups: ["rabbitmq"]
}
public isolated function testImmediateStop() returns error? {
    Listener channelListener = check new(DEFAULT_HOST, DEFAULT_PORT);
    error? stopResult = channelListener.immediateStop();
    if stopResult is error {
        test:assertFail("Error when trying to close the listener immediately.");
    }
}

@test:Config {
    dependsOn: [testListener],
    groups: ["rabbitmq"]
}
public function testListenerDetach() returns error? {
    Listener channelListener = check new(DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(mockService);
    check channelListener.'start();
    error? detachResult = channelListener.detach(mockService);
    if detachResult is error {
        test:assertFail("Error when trying to detach a service from the listener.");
    }
    check channelListener.immediateStop();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueAutoGenerate() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error|string queueResult = newClient->queueAutoGenerate();
    if queueResult is error {
        test:assertFail("Error when trying to create an auto generated queue.");
    } else {
        log:printInfo("Auto generated queue name: " + queueResult);
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testDirectExchangeDeclare() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error? result = newClient->exchangeDeclare(DIRECT_EXCHANGE_NAME, DIRECT_EXCHANGE);
    if result is error {
       test:assertFail("Error when trying to create a direct exchange.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testTopicExchangeDeclare() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error? result = newClient->exchangeDeclare(TOPIC_EXCHANGE_NAME, TOPIC_EXCHANGE);
    if result is error {
       test:assertFail("Error when trying to create a topic exchange.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testFanoutExchangeDeclare() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error? result = newClient->exchangeDeclare(FANOUT_EXCHANGE_NAME, FANOUT_EXCHANGE);
    if result is error {
       test:assertFail("Error when trying to create a fanout exchange.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient, testQueueDelete],
    groups: ["rabbitmq"]
}
public isolated function testQueueConfig() returns error? {
    string queueName = "testQueueConfig";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    QueueConfig queueConfig = { durable: true, exclusive: true, autoDelete: false };
    Error? result = newClient->queueDeclare(queueName, config = queueConfig);
    if result is error {
       test:assertFail("Error when trying to create a queue with config.");
    }
    check newClient->queueDelete(queueName);
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient, testExchangeDelete],
    groups: ["rabbitmq"]
}
public isolated function testExchangeConfig() returns error? {
    string exchangeName = "testExchangeConfig";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    ExchangeConfig exchangeConfig = { durable: true, autoDelete: true};
    Error? result = newClient->exchangeDeclare(exchangeName, DIRECT_EXCHANGE, config = exchangeConfig);
    if result is error {
       test:assertFail("Error when trying to create an exchange with options.");
    }
    check newClient->exchangeDelete(exchangeName);
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueBind() returns error? {
    string exchange = "QueueBindTestExchange";
    string queue = "QueueBindTest";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->exchangeDeclare(exchange, DIRECT_EXCHANGE);
    check newClient->queueDeclare(queue);
    Error? result = newClient->queueBind(queue, exchange, "myBinding");
    if result is error {
        test:assertFail("Error when trying to bind a queue.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testAuthentication() returns error? {
    Credentials credentials = {
        username: "user",
        password: "pass"
    };
    Client|Error newClient = check new(DEFAULT_HOST, 5673, auth = credentials);
    if newClient is Error {
        test:assertFail("Error when trying to initialize a client with auth.");
    } else {
        check newClient.close();
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientBasicAck() returns error? {
    string queue = "testClientBasicAck";
    string message = "Test client basic ack";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        string messageContent = check 'string:fromBytes(consumeResult.content);
        log:printInfo("The message received: " + messageContent);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
        Error? ackResult = newClient->basicAck(consumeResult, false);
        if ackResult is Error {
            test:assertFail("Error when trying to acknowledge the message using client.");
        }
    } else {
        test:assertFail("Error when trying to consume messages using client.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientBasicNack() returns error? {
    string queue = "testClientBasicNack";
    string message = "Test client basic nack";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        string messageContent = check 'string:fromBytes(consumeResult.content);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
        log:printInfo("The message received: " + messageContent);
        Error? ackResult = newClient->basicNack(consumeResult, false, false);
        if ackResult is Error {
            test:assertFail("Error when trying to acknowledge the message using client.");
        }
    } else {
        test:assertFail("Error when trying to consume messages using client.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testQueueDelete() returns error? {
    string queue = "testQueueDelete";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    Error? deleteResult = newClient->queueDelete(queue);
    if deleteResult is Error {
        test:assertFail("Error when trying to delete a queue.");
    }
    check newClient.close(200, "Client closed");
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testExchangeDelete() returns error? {
    string exchange = "testExchangeDelete";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->exchangeDeclare(exchange, DIRECT_EXCHANGE);
    Error? deleteExchange = newClient->exchangeDelete(exchange);
    check newClient.close(200, "Client closed");
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientAbort() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error? abortResult = newClient.'abort(200, "Client aborted");
    if abortResult is Error {
        test:assertFail("Error when trying to abort a client.");
    }
}

@test:Config {
    dependsOn: [testClient, testQueueDelete],
    groups: ["rabbitmq"]
}
public function testQueuePurge() returns error? {
    string queue = "testQueuePurge";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    check produceMessage("Hello world 1", queue);
    check produceMessage("Hello world 2", queue);
    Error? deleteResult = newClient->queuePurge(queue);
    if deleteResult is Error {
        test:assertFail("Error when trying to purge a queue.");
    }
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements() returns error? {
    string message = "Testing Message Acknowledgements";
    check produceMessage(message, ACK_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        check channelListener.attach(ackTestService);
        runtime:sleep(2);
    }
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testNegativeAcknowledgements() returns error? {
    string message = "Testing Message Rejection";
    check produceMessage(message, NACK_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        check channelListener.attach(nackTestService);
        runtime:sleep(2);
    }
}

@test:Config {
    dependsOn: [testAsyncConsumer, testAcknowledgements],
    groups: ["rabbitmq"]
}
public function testOnRequest() returns error? {
    string message = "Hello from the other side!";
    check produceMessage(message, QUEUE, REPLYTO);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        runtime:sleep(5);
        test:assertEquals(asyncConsumerMessage, message, msg = "Message received does not match.");
        test:assertEquals(replyMessage, "Hello Back!!", msg = "Reply message received does not match.");

    }
}

Service asyncTestService =
@ServiceConfig {
    queueName: QUEUE
}
service object {
    remote function onMessage(Message message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if (messageContent is string) {
            asyncConsumerMessage = messageContent;
            log:printInfo("The message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }

    remote function onRequest(Message message) returns string {
        string|error messageContent = 'string:fromBytes(message.content);
        if (messageContent is string) {
            asyncConsumerMessage = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
        return "Hello Back!!";
    }
};

Service ackTestService =
@ServiceConfig {
    queueName: ACK_QUEUE,
    autoAck: false
}
service object {
    remote function onMessage(Message message, Caller caller) {
        checkpanic caller->basicAck();
    }
};

Service nackTestService =
@ServiceConfig {
    queueName: NACK_QUEUE,
    autoAck: false
}
service object {
    remote function onMessage(Message message, Caller caller) {
        checkpanic caller->basicNack(false, false);
    }
};

Service mockService =
@ServiceConfig {
    queueName: MOCK_QUEUE
}
service object {
    remote function onMessage(Message message) {
    }
};

Service replyService =
@ServiceConfig {
    queueName: REPLYTO
}
service object {
    remote function onMessage(Message message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if (messageContent is string) {
            replyMessage = messageContent;
            log:printInfo("The reply message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

function produceMessage(string message, string queueName, string? replyToQueue = ()) returns error? {
    Client? clientObj = rabbitmqChannel;
    if (clientObj is Client) {
        if (replyToQueue is string) {
            check clientObj->publishMessage({ content: message.toBytes(), routingKey: queueName,
                    properties: { replyTo: replyToQueue }});
        } else {
            check clientObj->publishMessage({ content: message.toBytes(), routingKey: queueName });
        }
    }
}
