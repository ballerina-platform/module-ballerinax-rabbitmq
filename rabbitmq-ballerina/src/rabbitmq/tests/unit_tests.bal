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

Connection? connection = ();
Channel? rabbitmqChannel = ();
const QUEUE_NAME = "MyQueue";

@test:BeforeSuite
function setup() {
    log:printInfo("Creating a ballerina RabbitMQ connection.");
    Connection newConnection = new ({host: "", port: 5672, username: "guest", password: "guest"});

    log:printInfo("Creating a ballerina RabbitMQ channel.");
    rabbitmqChannel = new (newConnection);
    connection = newConnection;
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        string? queueResult = checkpanic channelObj->queueDeclare({queueName: QUEUE_NAME});
    }
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
        Error? producerResult = channelObj->basicPublish("Hello from Ballerina", QUEUE_NAME);
        if (producerResult is Error) {
            test:assertFail("Producing a message to the broker caused an error.");
        }
    }
}

@test:Config {
    dependsOn: ["testChannel", "testProducer"],
    groups: ["rabbitmq"]
}
public function testSyncConsumer() {
    string message = "Hello from Ballerina";
    Channel? channelObj = rabbitmqChannel;
    if (channelObj is Channel) {
        Error? producerResult = channelObj->basicPublish(message, QUEUE_NAME);
        if (producerResult is ()) {
            Message|Error getResult = channelObj->basicGet(QUEUE_NAME, AUTO_ACK);
            if (getResult is Error) {
                test:assertFail("Pulling a message from the broker caused an error.");
            } else {
                string|Error messageReceived = getResult.getTextContent();
                if (messageReceived is string) {
                    test:assertEquals(messageReceived, message, msg = "Message received does not match.");
                } else {
                    test:assertFail("Retrieving text content of the message failed.");
                }
            }
        } else {
            test:assertFail("Producing a message to the broker caused an error.");
        }
    }
}

@test:AfterSuite
function cleanUp() {
    Connection? con = connection;
    if (con is Connection) {
        log:printInfo("Closing the active resources.");
        checkpanic con.close();
    }
}
