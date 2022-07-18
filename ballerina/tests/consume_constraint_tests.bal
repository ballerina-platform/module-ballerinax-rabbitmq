// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/constraint;
import ballerina/log;
import ballerina/lang.runtime;

public type StringConstraintMessage record {|
    *AnydataMessage;
    @constraint:String {
        minLength: 2,
        maxLength: 10
    }
    string content;
|};

public type IntConstraintMessage record {|
    @constraint:Int {
        maxValue: 100,
        minValue: 10
    }
    int content;
    string routingKey;
    string exchange = "";
    int deliveryTag?;
    BasicProperties properties?;
|};

@constraint:Float {
    maxValue: 100,
    minValue: 10
}
public type Price float;

@constraint:Number {
    maxValue: 100,
    minValue: 10
}
public type Weight decimal;

@constraint:Array {
    minLength: 2,
    maxLength: 5
}
public type NameList int[];

public type Child record {|
    @constraint:String {
        length: 10
    }
    string name;
    int age;
|};

string receivedIntMaxValueConstraintError = "";
string receivedIntMinValueConstraintError = "";
string receivedNumberMaxValueConstraintError = "";
string receivedNumberMinValueConstraintError = "";

@test:Config {}
function stringConstraintMessageValidTest() returns error? {
    string message = "Hello";
    check produceMessage(message.toString(), CONSTRAINT_VALID_STRING_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    StringConstraintMessage|error result = 'client->consumeMessage(CONSTRAINT_VALID_STRING_QUEUE);
    if result is error {
        test:assertFail(result.message());
    } else {
        test:assertEquals(result.content, message);
    }
    check 'client->close();
}

@test:Config {}
function numberConstraintMessageValidTest() returns error? {
    decimal message = 12.453;
    check produceMessage(message.toString(), CONSTRAINT_VALID_NUMBER_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Weight|error result = 'client->consumePayload(CONSTRAINT_VALID_NUMBER_QUEUE);
    if result is error {
        test:assertFail(result.message());
    } else {
        test:assertEquals(result, message);
    }
    check 'client->close();
}

@test:Config {}
function stringMaxLengthConstraintMessageTest() returns error? {
    string message = "This is a data binding related message";
    check produceMessage(message.toString(), CONSTRAINT_STRING_MAX_LENGTH_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    StringConstraintMessage|error result = 'client->consumeMessage(CONSTRAINT_STRING_MAX_LENGTH_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'maxLength' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {}
function stringMinLengthConstraintMessageTest() returns error? {
    string message = "M";
    check produceMessage(message.toString(), CONSTRAINT_STRING_MIN_LENGTH_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    StringConstraintMessage|error result = 'client->consumeMessage(CONSTRAINT_STRING_MIN_LENGTH_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'minLength' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {}
function stringLengthConstraintRecordTest() returns error? {
    Child child = {
        name: "Phil Dunphy",
        age: 20
    };
    check produceMessage(child.toString(), CONSTRAINT_STRING_LENGTH_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Child|error result = 'client->consumePayload(CONSTRAINT_STRING_LENGTH_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'length' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {enable: true}
function floatMaxValueConstraintPayloadTest() returns error? {
    check produceMessage(1010.45.toString(), CONSTRAINT_FLOAT_MAX_VALUE_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Price|error result = 'client->consumePayload(CONSTRAINT_FLOAT_MAX_VALUE_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'maxValue' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {enable: true}
function floatMinValueConstraintPayloadTest() returns error? {
    check produceMessage(1.3.toString(), CONSTRAINT_FLOAT_MIN_VALUE_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Price|error result = 'client->consumePayload(CONSTRAINT_FLOAT_MIN_VALUE_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'minValue' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {enable: true}
function arrayMaxLengthConstraintPayloadTest() returns error? {
    check produceMessage([1, 2, 3, 4, 5, 6].toString(), CONSTRAINT_ARRAY_MAX_LENGTH_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    NameList|error result = 'client->consumePayload(CONSTRAINT_ARRAY_MAX_LENGTH_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'maxLength' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {enable: true}
function arrayMinLengthConstraintPayloadTest() returns error? {
    check produceMessage([1].toString(), CONSTRAINT_ARRAY_MIN_LENGTH_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    NameList|error result = 'client->consumePayload(CONSTRAINT_ARRAY_MIN_LENGTH_QUEUE);
    if result is PayloadValidationError {
        test:assertEquals(result.message(), "Validation failed for 'minLength' constraint(s).");
    } else {
        test:assertFail("Expected a constraint validation error");
    }
    check 'client->close();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function intMinValueConstraintListenerMessageTest() returns error? {
    Service intService =
    @ServiceConfig {
        queueName: CONSTRAINT_INT_MIN_VALUE_QUEUE
    }
    service object {
        remote function onMessage(IntConstraintMessage message) {
            log:printInfo("The message received: " + message.toString());
        }

        remote function onError(Message msg, Error e) {
            if e is PayloadValidationError {
                receivedIntMinValueConstraintError = e.message();
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };

    check produceMessage(1.toString(), CONSTRAINT_INT_MIN_VALUE_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedIntMinValueConstraintError, "Validation failed for 'minValue' constraint(s).");
    check channelListener.gracefulStop();
}

@test:Config {enable: true}
function intMaxValueConstraintListenerMessageTest() returns error? {
    Service intConstraintService =
    @ServiceConfig {
        queueName: CONSTRAINT_INT_MAX_VALUE_QUEUE
    }
    service object {
        remote function onMessage(IntConstraintMessage message) returns error? {
            log:printInfo("The message received: " + message.toString());
        }

        remote function onError(Message msg, Error e) {
            if e is PayloadValidationError {
                receivedIntMaxValueConstraintError = e.message();
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };
    check produceMessage(1200.toString(), CONSTRAINT_INT_MAX_VALUE_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intConstraintService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedIntMaxValueConstraintError, "Validation failed for 'maxValue' constraint(s).");
    check channelListener.gracefulStop();
}

@test:Config {enable: true}
function numberMaxValueConstraintListenerPayloadTest() returns error? {
    Service numberConstraintService =
    @ServiceConfig {
        queueName: CONSTRAINT_NUMBER_MAX_VALUE_QUEUE
    }
    service object {
        remote function onMessage(Weight message) returns error? {
            log:printInfo("The message received: " + message.toString());
        }

        remote function onError(Message msg, Error e) {
            if e is PayloadValidationError {
                receivedNumberMaxValueConstraintError = e.message();
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };
    check produceMessage(1099.999.toString(), CONSTRAINT_NUMBER_MAX_VALUE_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(numberConstraintService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedNumberMaxValueConstraintError, "Validation failed for 'maxValue' constraint(s).");
    check channelListener.gracefulStop();
}

@test:Config {enable: true}
function numberMinValueConstraintListenerPayloadTest() returns error? {
    Service numberConstraintService =
    @ServiceConfig {
        queueName: CONSTRAINT_NUMBER_MIN_VALUE_QUEUE
    }
    service object {
        remote function onMessage(Weight message) returns error? {
            log:printInfo("The message received: " + message.toString());
        }

        remote function onError(Message msg, Error e) {
            if e is PayloadValidationError {
                receivedNumberMinValueConstraintError = e.message();
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };
    check produceMessage(1.999.toString(), CONSTRAINT_NUMBER_MIN_VALUE_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(numberConstraintService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedNumberMinValueConstraintError, "Validation failed for 'minValue' constraint(s).");
    check channelListener.gracefulStop();
}

@test:Config {}
function recordConstraintMessageValidTest() returns error? {
    Child child = {name: "PhilDunphy", age: 27};
    check produceMessage(child.toString(), CONSTRAINT_VALID_RECORD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Child|error result = 'client->consumePayload(CONSTRAINT_VALID_RECORD_QUEUE);
    if result is error {
        test:assertFail(result.message());
    } else {
        test:assertEquals(result, child);
    }
    check 'client->close();
}
