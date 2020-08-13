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

// Successful creation of a connection.
@test:Config {
    groups: ["rabbitmq-connection"]
}
public function testConnection() {
    Connection|error newConnection = trap new (successConfig);
    if (newConnection is Connection) {
        sharedConnection = newConnection;
    } else {
        log:printError("Connection refused. Check if the docker container has started properly.", newConnection);
        test:assertFail("RabbitMQ Connection creation failed.");
    }
}

// Connection creation with correct authentication details.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testConnectionCorrectAuthentication() {
    Connection|error newConnection = trap new (correctAuthConfig);
    if (newConnection is error) {
        log:printError("Connection refused. Check if the docker container has started properly.", newConnection);
        test:assertFail("RabbitMQ Connection creation failed.");
    }
}

// Connection creation with incorrect authentication details.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testConnectionIncorrectAuthentication() {
    Connection|error newConnection = trap new (incorrectAuthConfig);
    if (newConnection is error) {
        string message = newConnection.message();
        string expected = "Error occurred while connecting to the broker: ACCESS_REFUSED - " +
        "Login was refused using authentication mechanism PLAIN. For details see the broker logfile.";
        test:assertEquals(message, expected, msg = "Error occurred does not match.");
    }
}

// Checking status of an active connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testIsClosedActive() {
    Connection? connection = sharedConnection;
    if (connection is Connection) {
        boolean flag = connection.isClosed();
        test:assertFalse(flag, msg = "Status of the active connection returned as closed.");
    }
}

// Closing an active connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testConnectionClose() {
    Connection closeConnection = new (successConfig);
    isClosedConnection = closeConnection; // Required param for `testIsClosed`
    Error? closeResult = closeConnection.close();
    if (closeResult is Error) {
        log:printError(closeResult.message());
        test:assertFail("Closing the connection failed.");
    }
}

// Closing an active connection with parameters.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testConnectionCloseWithParams() {
    Connection closeConnection = new (successConfig);
    Error? closeResult = closeConnection.close(CLOSE_CODE, "Closing Connection.");
    if (closeResult is Error) {
        log:printError(closeResult.message());
        test:assertFail("Closing the connection failed.");
    }
}

// Closing an already closed connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testConnectionCloseNegative() {
    string expectedConnectionClosed = "Error occurred while closing the connection: connection is already closed " +
        "due to clean connection shutdown; protocol method: #method<connection.close>(reply-code=200, reply-text=OK, " +
        "class-id=0, method-id=0)";
    Connection closeConnection = new (successConfig);
    Error? closeResult = closeConnection.close();
    Error? closeAgainResult = closeConnection.close();
    if (closeAgainResult is Error) {
        string message = closeAgainResult.message();
        test:assertEquals(message, expectedConnectionClosed, msg = "Error occurred does not match.");
    }
}

// Checking status of an closed connection.
@test:Config {
    dependsOn: ["testConnectionClose"],
    groups: ["rabbitmq-connection"]
}
public function testIsClosed() {
    Connection? connection = isClosedConnection;
    if (connection is Connection) {
        boolean flag = connection.isClosed();
        test:assertTrue(flag, msg = "Status of the closed connection returned as active.");
    }
}

// Aborting a connection.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testAbortConnection() {
    Connection abortConnection = new (successConfig);
    error? abortResult = trap abortConnection.abortConnection();
    if (abortResult is error) {
        log:printError(abortResult.message());
        test:assertFail("Aborting the connection failed.");
    }
}

// Aborting a connection with parameters.
@test:Config {
    dependsOn: ["testConnection"],
    groups: ["rabbitmq-connection"]
}
public function testAbortConnectionWithParams() {
    Connection abortConnection = new (successConfig);
    error? abortResult = trap abortConnection.abortConnection(CLOSE_CODE, "Aborting Connection.");
    if (abortResult is error) {
        log:printError(abortResult.message());
        test:assertFail("Aborting the connection failed.");
    }
}
