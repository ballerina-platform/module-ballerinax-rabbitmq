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

import ballerina/test;

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testSslConnection() returns error? {
    SecureSocket secured = {
        cert: {
            path: "tests/server/certs/truststore.p12",
            password: "password"
        },
        key: {
            path: "tests/server/certs/keystore.p12",
            password: "password"
        },
        protocol: {
            name: TLS
        },
        verifyHostName: true
    };
    Client|Error newClient = new(DEFAULT_HOST, 5671, secureSocket = secured);
    if newClient is Error {
        test:assertFail("Error when trying to create a client with secure connection.");
    } else {
        check newClient->close();
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testSslConnection2() returns error? {
    SecureSocket secured = {
        cert: "tests/server/certs/server.crt",
        key: {
            certFile: "tests/server/certs/client.crt",
            keyFile: "tests/server/certs/client.key"
        },
        verifyHostName: false
    };
    Client|Error newClient = new(DEFAULT_HOST, 5671, secureSocket = secured);
    if newClient is Error {
        test:assertFail("Error when trying to create a client with secure connection.");
    } else {
        check newClient->close();
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testSslConnection3() returns error? {
    SecureSocket secured = {
        cert: "tests/server/certs/server1.crt",
        key: {
            certFile: "tests/server/certs/client.crt",
            keyFile: "tests/server/certs/client.key"
        },
        verifyHostName: false
    };
    Client|Error newClient = new(DEFAULT_HOST, 5671, secureSocket = secured);
    if !(newClient is Error) {
        test:assertFail("Error expected when trying to create a client with secure connection.");
        check newClient->close();
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testSslConnection4() returns error? {
    SecureSocket secured = {
        cert: "tests/server/certs/server.crt",
        key: {
            certFile: "tests/server/certs/client1.crt",
            keyFile: "tests/server/certs/client1.key"
        },
        verifyHostName: false
    };
    Client|Error newClient = new(DEFAULT_HOST, 5671, secureSocket = secured);
    if !(newClient is Error) {
        test:assertFail("Error expected when trying to create a client with secure connection.");
        check newClient->close();
    }
}

@test:Config {
    dependsOn: [testClient],
    groups: ["rabbitmq"]
}
public isolated function testSslConnection5() returns error? {
    SecureSocket secured = {
        cert: {
            path: "tests/server/certs/truststore.p12",
            password: "password"
        },
        key: {
            certFile: "tests/server/certs/client.crt",
            keyFile: "tests/server/certs/client.key"
        },
        verifyHostName: false
    };
    Client|Error newClient = new(DEFAULT_HOST, 5671, secureSocket = secured);
    if newClient is Error {
        test:assertFail("Error when trying to create a client with secure connection.");
    } else {
        check newClient->close();
    }
}
