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
import ballerina/runtime;
import ballerina/system;
import ballerina/test;

Connection? connection = ();
Channel? rabbitmqChannel = ();
Listener? rabbitmqListener = ();
const QUEUE = "MyQueue";
const ACK_QUEUE = "MyAckQueue";
const SYNC_NEGATIVE_QUEUE = "MySyncNegativeQueue";
const DATA_BINDING_QUEUE = "MyDataQueue";
string asyncConsumerMessage = "";
string dataBindingMessage = "";

@test:BeforeSuite
function setup() {
    log:printInfo("Starting RabbitMQ Docker Container.");
    var dockerStartResult = system:exec("docker", {}, "/", "run", "-d", "--name", "rabbit-tests", "-p", "15672:15672",
    "-p", "5672:5672", "rabbitmq:3-management");
    runtime:sleep(20000);
    log:printInfo("Creating a ballerina RabbitMQ connection.");
    Connection newConnection = new ({host: "0.0.0.0", port: 5672});

    log:printInfo("Creating a ballerina RabbitMQ channel.");
    rabbitmqChannel = new (newConnection);
    connection = newConnection;
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        string? queue = checkpanic channelObj->queueDeclare({queueName: QUEUE});
        string? dataBindingQueue = checkpanic channelObj->queueDeclare({queueName: DATA_BINDING_QUEUE});
        string? syncNegativeQueue = checkpanic channelObj->queueDeclare({queueName: SYNC_NEGATIVE_QUEUE});
        string? ackQueue = checkpanic channelObj->queueDeclare({queueName: ACK_QUEUE});
    }
    rabbitmqListener = new (newConnection);
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testConnection() {
    boolean flag = false;
    Connection? con = connection;
    if (con is Connection) {
        flag = true;
    }
    test:assertTrue(flag, msg = "RabbitMQ Connection creation failed.");
}

@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq"]
}
public function testChannel() {
    boolean flag = false;
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        flag = true;
    }
    test:assertTrue(flag, msg = "RabbitMQ Channel creation failed.");
}

@test:Config {
    dependsOn: ["testChannel"],
    groups: ["rabbitmq"]
}
public function testProducer() {
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        Error? producerResult = channelObj->basicPublish("Hello from Ballerina", QUEUE);
        if (producerResult is Error) {
            test:assertFail("Producing a message to the broker caused an error.");
        }
        checkpanic channelObj->queuePurge(QUEUE);
    }
}

@test:Config {
    dependsOn: ["testChannel", "testProducer"],
    groups: ["rabbitmq"]
}
public function testSyncConsumer() {
    string message = "Testing Sync Consumer";
    Channel? channelObj = rabbitmqChannel;
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

@test:Config {
    dependsOn: ["testChannel", "testProducer"],
    groups: ["rabbitmq"]
}
public function testNegativeSyncConsumer() {
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        Message|Error getResult = channelObj->basicGet(SYNC_NEGATIVE_QUEUE, AUTO_ACK);
        if (getResult is Error) {
            test:assertEquals(getResult.message(), "No messages are found in the queue.",
             msg = "Error message does not match.");
        }
    }
}

@test:Config {
    dependsOn: ["testConnection"],
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
    dependsOn: ["testListener", "testSyncConsumer"],
    groups: ["rabbitmq"]
}
public function testAsyncConsumer() {
    string message = "Testing Async Consumer";
    produceMessage(message, QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        checkpanic channelListener.__attach(asyncTestService);
        checkpanic channelListener.__start();
        runtime:sleep(2000);
        test:assertEquals(asyncConsumerMessage, message, msg = "Message received does not match.");
    }
}

@test:Config {
    dependsOn: ["testListener", "testAsyncConsumer"],
    groups: ["rabbitmq"]
}
public function testAcknowledgements() {
    string message = "Testing Message Acknowledgements";
    produceMessage(message, ACK_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        checkpanic channelListener.__attach(ackTestService);
        runtime:sleep(2000);
    }
}

@test:Config {
    dependsOn: ["testListener", "testAsyncConsumer"],
    groups: ["rabbitmq"]
}
public function testAsyncConsumerWithDataBinding() {
    string message = "Testing Async Consumer with Data Binding";
    produceMessage(message, DATA_BINDING_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if (channelListener is Listener) {
        checkpanic channelListener.__attach(dataBindingTestService);
        runtime:sleep(2000);
        test:assertEquals(dataBindingMessage, message, msg = "Message received does not match.");
    }
}

service asyncTestService =
@ServiceConfig {
    queueConfig: {
        queueName: QUEUE
    }
}
service {
    resource function onMessage(Message message) {
        var messageContent = message.getTextContent();
        if (messageContent is string) {
            asyncConsumerMessage = <@untainted> messageContent;
            log:printInfo("The message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.");
        }
    }
};

service ackTestService =
@ServiceConfig {
    queueConfig: {
        queueName: ACK_QUEUE
    },
    ackMode: CLIENT_ACK
}
service {
    resource function onMessage(Message message) {
        checkpanic message->basicAck();
    }
};

service dataBindingTestService =
@ServiceConfig {
    queueConfig: {
        queueName: DATA_BINDING_QUEUE
    }
}
service {
    resource function onMessage(Message message, string data) {
        dataBindingMessage = <@untainted> data;
        log:printInfo("The message received from data binding: " + data);
    }
};

@test:AfterSuite {}
function cleanUp() {
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        checkpanic channelObj->queuePurge(QUEUE);
        checkpanic channelObj->queuePurge(DATA_BINDING_QUEUE);
    }
    Connection? con = connection;
    if (con is Connection) {
        log:printInfo("Closing the active resources.");
        checkpanic con.close();
    }
    var dockerStopResult = system:exec("docker", {}, "/", "stop", "rabbit-tests");
    var dockerRmResult = system:exec("docker", {}, "/", "rm", "rabbit-tests");
}

function produceMessage(string message, string queueName) {
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        checkpanic channelObj->basicPublish(message, queueName);
    }
}
