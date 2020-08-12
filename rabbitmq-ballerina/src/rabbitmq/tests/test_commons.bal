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

Connection? sharedConnection = ();
Channel? sharedChannel = ();
Listener? sharedListener = ();
Connection? isClosedConnection = ();
const CONNECTION_CLOSE_CODE = 200;
const QUEUE = "MyQueue";

ConnectionConfiguration successConfig = { host: "0.0.0.0",
                                          port: 5672 };

ConnectionConfiguration correctAuthConfig = { host: "0.0.0.0",
                                              port: 5672,
                                              username: "guest",
                                              password: "guest" };

ConnectionConfiguration incorrectAuthConfig = { host: "0.0.0.0",
                                              port: 5672,
                                              username: "1234",
                                              password: "1234" };

@test:BeforeSuite
function setup() {
    log:printInfo("Starting RabbitMQ Docker Container...");
    var dockerStartResult = system:exec("docker", {}, "/", "run", "-d", "--name", "rabbit-tests", "-p", "15672:15672",
    "-p", "5672:5672", "rabbitmq:3-management");
    runtime:sleep(20000);
}

@test:AfterSuite
function cleanUp() {
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        checkpanic channelObj->queuePurge(QUEUE);
    }
    Connection? con = sharedConnection;
    if (con is Connection) {
        log:printInfo("Closing the active resources...");
        checkpanic con.close();
    }
    log:printInfo("Stopping RabbitMQ Docker Container...");
    var dockerStopResult = system:exec("docker", {}, "/", "stop", "rabbit-tests");
    var dockerRmResult = system:exec("docker", {}, "/", "rm", "rabbit-tests");
}

function produceMessage(string message, string queueName) {
    Channel? channelObj = sharedChannel;
    if (channelObj is Channel) {
        checkpanic channelObj->basicPublish(message, queueName);
    }
}
