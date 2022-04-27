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
const QUEUE2 = "MyQueue2";
const REQ_QUEUE = "OnRequestQueue";
const ACK_QUEUE = "MyAckQueue";
const ACK_QUEUE2 = "MyAckQueue2";
const ACK_QUEUE3 = "MyAckQueue3";
const NACK_QUEUE2 = "MyNackQueue2";
const NACK_QUEUE3 = "MyNackQueue3";
const READONLY_MESSAGE_QUEUE = "ReadOnlyMessage";
const READONLY_MESSAGE_QUEUE_CALLER = "ReadOnlyMessageCaller";
const READONLY_REQUEST_QUEUE = "ReadOnlyRequest";
const READONLY_REQUEST_QUEUE_CALLER = "ReadOnlyRequestCaller";
boolean negativeAck = false;
boolean negativeAck2 = false;
boolean negativeNack = false;
boolean negativeNack2 = false;
const NACK_QUEUE = "MyNackQueue";
const MOCK_QUEUE = "MockQueue";
const DIRECT_EXCHANGE_NAME = "MyDirectExchange";
const TOPIC_EXCHANGE_NAME = "MyTopicExchange";
const FANOUT_EXCHANGE_NAME = "MyFanoutExchange";
const SYNC_NEGATIVE_QUEUE = "MySyncNegativeQueue";
const DATA_BINDING_STRING_LISTENER_QUEUE = "StringListenerQueue";
const DATA_BINDING_INT_LISTENER_QUEUE = "IntListenerQueue";
const DATA_BINDING_DECIMAL_LISTENER_QUEUE = "DecimalListenerQueue";
const DATA_BINDING_FLOAT_LISTENER_QUEUE = "FloatListenerQueue";
const DATA_BINDING_BOOLEAN_LISTENER_QUEUE = "BooleanListenerQueue";
const DATA_BINDING_MAP_LISTENER_QUEUE = "MapListenerQueue";
const DATA_BINDING_RECORD_LISTENER_QUEUE = "RecordListenerQueue";
const DATA_BINDING_TABLE_LISTENER_QUEUE = "TableListenerQueue";
const DATA_BINDING_XML_LISTENER_QUEUE = "XmlListenerQueue";
const DATA_BINDING_JSON_LISTENER_QUEUE = "JsonListenerQueue";
const DATA_BINDING_ERROR_QUEUE = "ErrorQueue";
const DATA_BINDING_STRING_PUBLISH_QUEUE = "StringPublishQueue";
const DATA_BINDING_INT_PUBLISH_QUEUE = "IntPublishQueue";
const DATA_BINDING_DECIMAL_PUBLISH_QUEUE = "DecimalPublishQueue";
const DATA_BINDING_FLOAT_PUBLISH_QUEUE = "FloatPublishQueue";
const DATA_BINDING_BOOLEAN_PUBLISH_QUEUE = "BooleanPublishQueue";
const DATA_BINDING_MAP_PUBLISH_QUEUE = "MapPublishQueue";
const DATA_BINDING_RECORD_PUBLISH_QUEUE = "RecordPublishQueue";
const DATA_BINDING_TABLE_PUBLISH_QUEUE = "TablePublishQueue";
const DATA_BINDING_XML_PUBLISH_QUEUE = "XmlPublishQueue";
const DATA_BINDING_JSON_PUBLISH_QUEUE = "JsonPublishQueue";
const DATA_BINDING_STRING_CONSUME_QUEUE = "StringConsumeQueue";
const DATA_BINDING_INT_CONSUME_QUEUE = "IntConsumeQueue";
const DATA_BINDING_DECIMAL_CONSUME_QUEUE = "DecimalConsumeQueue";
const DATA_BINDING_FLOAT_CONSUME_QUEUE = "FloatConsumeQueue";
const DATA_BINDING_BOOLEAN_CONSUME_QUEUE = "BooleanConsumeQueue";
const DATA_BINDING_MAP_CONSUME_QUEUE = "MapConsumeQueue";
const DATA_BINDING_RECORD_CONSUME_QUEUE = "RecordConsumeQueue";
const DATA_BINDING_TABLE_CONSUME_QUEUE = "TableConsumeQueue";
const DATA_BINDING_XML_CONSUME_QUEUE = "XmlConsumeQueue";
const DATA_BINDING_JSON_CONSUME_QUEUE = "JsonQueue";
const DATA_BINDING_REPLY_QUEUE = "DataBindingReplyQueue";
string asyncConsumerMessage = "";
string asyncConsumerMessage2 = "";
string onRequestMessage = "";
string replyMessage = "";
string reqReplyMessage = "";
string REPLYTO = "replyHere";
string REQ_REPLYTO = "onReqReply";

@test:BeforeSuite
function setup() returns error? {
    log:printInfo("Creating a ballerina RabbitMQ channel.");
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    rabbitmqChannel = newClient;
    Client? clientObj = rabbitmqChannel;
    if clientObj is Client {
        check clientObj->queueDeclare(QUEUE);
        check clientObj->queueDeclare(QUEUE2);
        check clientObj->queueDeclare(READONLY_MESSAGE_QUEUE);
        check clientObj->queueDeclare(READONLY_MESSAGE_QUEUE_CALLER);
        check clientObj->queueDeclare(READONLY_REQUEST_QUEUE);
        check clientObj->queueDeclare(READONLY_REQUEST_QUEUE_CALLER);
        check clientObj->queueDeclare(DATA_BINDING_STRING_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_INT_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_DECIMAL_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_FLOAT_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_BOOLEAN_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_MAP_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_RECORD_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_TABLE_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_XML_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_JSON_LISTENER_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_STRING_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_INT_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_DECIMAL_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_FLOAT_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_BOOLEAN_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_MAP_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_RECORD_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_TABLE_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_XML_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_JSON_PUBLISH_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_STRING_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_INT_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_DECIMAL_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_FLOAT_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_BOOLEAN_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_MAP_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_RECORD_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_TABLE_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_XML_CONSUME_QUEUE);
        check clientObj->queueDeclare(DATA_BINDING_JSON_CONSUME_QUEUE);
        check clientObj->queueDeclare(SYNC_NEGATIVE_QUEUE);
        check clientObj->queueDeclare(ACK_QUEUE);
        check clientObj->queueDeclare(ACK_QUEUE2);
        check clientObj->queueDeclare(ACK_QUEUE3);
        check clientObj->queueDeclare(NACK_QUEUE3);
        check clientObj->queueDeclare(NACK_QUEUE2);
        check clientObj->queueDeclare(NACK_QUEUE);
        check clientObj->queueDeclare(REQ_QUEUE);
        check clientObj->queueDeclare(REQ_REPLYTO);
        check clientObj->queueDeclare(REPLYTO);
    }
    Listener lis = check new(DEFAULT_HOST, DEFAULT_PORT);
    rabbitmqListener = lis;
    return;
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testClient() returns error? {
    boolean flag = false;
    Client? con = rabbitmqChannel;
    if con is Client {
        flag = true;
    }
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->close();
    test:assertTrue(flag, msg = "RabbitMQ Connection creation failed.");
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public function testProducer() returns error? {
    Client? channelObj = rabbitmqChannel;
    if channelObj is Client {
        string message = "Hello from Ballerina";
        check channelObj->publishMessage({ content: message.toBytes(), routingKey: QUEUE });
        check channelObj->queuePurge(QUEUE);
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testProducerTransactional() returns error? {
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
    check newClient->close();
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testProducerTransactionalRollback() returns error? {
    string queue = "testProducerTransactionalRollback";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    error? rollbackError = rabbitMQTransactionFail(queue);
    Message|Error consumeResult = newClient->consumeMessage(queue, false);
    if consumeResult is Message {
        test:assertFail("Rolled back message is in queue.");
    }
    check newClient->close();
    return;
}

isolated function rabbitMQTransactionFail(string queue) returns error? {
    string message = "Test producing transactional and rollback";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->queueDeclare(queue);
    do {
        transaction {
            check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
            check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
            check newClient->publishMessage({ content: message.toBytes(), routingKey: queue });
            check failTransaction();
            check commit;
        }
    } on fail var e {
        check newClient->close();
        return e;
    }
    return;
}

isolated function failTransaction() returns error {
    return error("Fail!");
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListener() {
    boolean flag = false;
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        flag = true;
    }
    test:assertTrue(flag, msg = "RabbitMQ Listener creation failed.");
}

@test:Config {
    groups: ["rabbitmq"]
}
public isolated function testListenerWithQos() {
    Listener|Error qosListener1 = new(DEFAULT_HOST, DEFAULT_PORT, qosSettings = { prefetchCount: 10 });
    if qosListener1 is Error {
        test:assertFail("RabbitMQ Listener initialization with qos settings failed.");
    }
    Listener|Error qosListener2 = new(DEFAULT_HOST, DEFAULT_PORT, qosSettings =
                                                            { prefetchCount: 10, prefetchSize: 0, global: true });
    if qosListener2 is Error {
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
    if channelObj is Client {
        Message getResult = check channelObj->consumeMessage(QUEUE);
        string messageContent = check 'string:fromBytes(getResult.content);
        test:assertEquals(messageContent, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testAsyncConsumer() returns error? {
    string message = "Testing Async Consumer";
    check produceMessage(message, QUEUE);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(asyncTestService);
        check channelListener.attach(replyService);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(asyncConsumerMessage, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAsyncConsumerWithoutServiceConfig() returns error? {
    string message = "Testing Async Consumer Without Queue Config";
    check produceMessage(message, QUEUE2);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(asyncTestService3, QUEUE2);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(asyncConsumerMessage2, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testAsyncConsumer2() returns error? {
    string message = "Testing Async Consumer With onRequest";
    check produceMessage(message, REQ_QUEUE, REQ_REPLYTO);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(asyncTestService2);
        check channelListener.attach(replyService2);
        check channelListener.'start();
        runtime:sleep(5);
        string replyMsg = "Hello back from ballerina!";
        test:assertEquals(onRequestMessage, message, msg = "Message received does not match.");
        test:assertEquals(reqReplyMessage, replyMsg, msg = "Message received does not match.");
    }
    return;
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
    return;
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
    return;
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
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
    check newClient->close();
    return;
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
        check newClient->close();
    }
    return;
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
    check newClient->close();
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testConnectionConfig() returns error? {
    ConnectionConfiguration connConfig = {
        username: "guest",
        password: "guest",
        connectionTimeout: 5,
        handshakeTimeout: 5,
        shutdownTimeout: 5,
        heartbeat: 5
    };
    Client|error newClient = new(DEFAULT_HOST, DEFAULT_PORT, connectionData = connConfig);
    if newClient is error {
        test:assertFail("Error when trying to connect.");
    } else {
        check newClient->close();
    }
    return;
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
    check newClient->close();
    return;
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
    check newClient->close(200, "Client closed");
    return;
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
    check newClient->close(200, "Client closed");
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testExchangeDeleteNegative() returns error? {
    string exchange = "testExchangeDeleteNegative";
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check newClient->exchangeDeclare(exchange, DIRECT_EXCHANGE);
    check newClient->close(200, "Client closed");
    Error? deleteExchange = newClient->exchangeDelete(exchange);
    if !(deleteExchange is error) {
        test:assertFail("Error expected when deleting with closed channel.");
    }
    return;
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testClientAbort() returns error? {
    Client newClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    Error? abortResult = newClient->'abort(200, "Client aborted");
    if abortResult is Error {
        test:assertFail("Error when trying to abort a client.");
    }
    return;
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
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements() returns error? {
    string message = "Testing Message Acknowledgements";
    check produceMessage(message, ACK_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(ackTestService);
        runtime:sleep(2);
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements2() returns error? {
    string message = "Testing Negative Message Acknowledgements";
    check produceMessage(message, ACK_QUEUE2);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(ackTestService2);
        runtime:sleep(3);
    }
    test:assertTrue(negativeAck, msg = "Negative acknoeledgement failed.");
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements3() returns error? {
    string message = "Testing Negative Message Acknowledgements";
    check produceMessage(message, ACK_QUEUE3);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(ackTestService3);
        runtime:sleep(3);
    }
    test:assertTrue(negativeAck2, msg = "Negative acknoeledgement failed.");
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements4() returns error? {
    string message = "Testing Negative Message Acknowledgements";
    check produceMessage(message, NACK_QUEUE2);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(nackTestService2);
        runtime:sleep(3);
    }
    test:assertTrue(negativeNack, msg = "Negative acknoeledgement failed.");
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testAcknowledgements5() returns error? {
    string message = "Testing Negative Message Acknowledgements";
    check produceMessage(message, NACK_QUEUE3);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(nackTestService3);
        runtime:sleep(3);
    }
    test:assertTrue(negativeNack2, msg = "Negative acknoeledgement failed.");
    return;
}

@test:Config {
    dependsOn: [testListener, testAsyncConsumer],
    groups: ["rabbitmq"]
}
public function testNegativeAcknowledgements() returns error? {
    string message = "Testing Message Rejection";
    check produceMessage(message, NACK_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(nackTestService);
        runtime:sleep(2);
    }
    return;
}

@test:Config {
    dependsOn: [testAsyncConsumer, testAcknowledgements],
    groups: ["rabbitmq"]
}
public function testOnRequest() returns error? {
    string message = "Hello from the other side!";
    check produceMessage(message, QUEUE, REPLYTO);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        runtime:sleep(5);
        test:assertEquals(asyncConsumerMessage, message, msg = "Message received does not match.");
        test:assertEquals(replyMessage, "Hello Back!!", msg = "Reply message received does not match.");

    }
    return;
}

Service asyncTestService =
@ServiceConfig {
    queueName: QUEUE
}
service object {
    remote function onMessage(Message message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            asyncConsumerMessage = messageContent;
            log:printInfo("The message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }

    remote function onRequest(Message message) returns string {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            asyncConsumerMessage = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
        return "Hello Back!!";
    }
};

Service asyncTestService2 =
@ServiceConfig {
    queueName: REQ_QUEUE
}
service object {
    remote function onRequest(Message message, Caller caller) returns string {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            onRequestMessage = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
        return "Hello back from ballerina!";
    }
};

Service asyncTestService3 =
service object {
    remote function onMessage(Message message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            asyncConsumerMessage2 = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

Service ackTestService =
@ServiceConfig {
    queueName: ACK_QUEUE,
    autoAck: false
}
service object {
    remote isolated function onMessage(Message message, Caller caller) {
        checkpanic caller->basicAck();
    }
};

Service ackTestService2 =
@ServiceConfig {
    queueName: ACK_QUEUE2,
    autoAck: true
}
service object {
    remote function onMessage(Message message, Caller caller) {
        error? ackResult = caller->basicAck();
        if ackResult is error {
            negativeAck = true;
        }
    }
};

Service ackTestService3 =
@ServiceConfig {
    queueName: ACK_QUEUE3,
    autoAck: false
}
service object {
    remote function onMessage(Message message, Caller caller) {
        error? ackResult = caller->basicAck();
        error? ackResult2 = caller->basicAck();
        if ackResult2 is error {
            negativeAck2 = true;
        }
    }
};

Service nackTestService2 =
@ServiceConfig {
    queueName: NACK_QUEUE2,
    autoAck: true
}
service object {
    remote function onMessage(Message message, Caller caller) {
        error? ackResult = caller->basicNack(false, false);
        if ackResult is error {
            negativeNack = true;
        }
    }
};

Service nackTestService3 =
@ServiceConfig {
    queueName: NACK_QUEUE3,
    autoAck: false
}
service object {
    remote function onMessage(Message message, Caller caller) {
        error? ackResult = caller->basicNack(false, false);
        error? ackResult2 = caller->basicNack(false, false);
        if ackResult2 is error {
            negativeNack2 = true;
        }
    }
};

Service nackTestService =
@ServiceConfig {
    queueName: NACK_QUEUE,
    autoAck: false
}
service object {
    remote isolated function onMessage(Message message, Caller caller) {
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
        if messageContent is string {
            replyMessage = messageContent;
            log:printInfo("The reply message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

Service replyService2 =
@ServiceConfig {
    queueName: REQ_REPLYTO
}
service object {
    remote function onMessage(Message message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            reqReplyMessage = messageContent;
            log:printInfo("The reply message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

function produceMessage(string message, string queueName, string? replyToQueue = ()) returns error? {
    Client? clientObj = rabbitmqChannel;
    if clientObj is Client {
        if replyToQueue is string {
            check clientObj->publishMessage({ content: message.toBytes(), routingKey: queueName,
                    properties: { replyTo: replyToQueue }});
        } else {
            check clientObj->publishMessage({ content: message.toBytes(), routingKey: queueName });
        }
    }
    return;
}
