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

import ballerinax/rabbitmq;

public type IntMessage record {|
    *rabbitmq:AnydataMessage;
    int content;
|};

public type FloatMessage record {|
    *rabbitmq:AnydataMessage;
    float content;
|};

public type DecimalMessage record {|
    *rabbitmq:AnydataMessage;
    decimal content;
|};

public type BooleanMessage record {|
    *rabbitmq:AnydataMessage;
    boolean content;
|};

public type RecordMessage record {|
    *rabbitmq:AnydataMessage;
    Person content;
|};

public type MapMessage record {|
    *rabbitmq:AnydataMessage;
    map<Person> content;
|};

public type TableMessage record {|
    *rabbitmq:AnydataMessage;
    table<Person> content;
|};

public type XmlMessage record {|
    *rabbitmq:AnydataMessage;
    xml content;
|};

public type JsonMessage record {|
    *rabbitmq:AnydataMessage;
    json content;
|};

public type Person record {|
    string name;
    int age;
    boolean married;
|};

listener rabbitmq:Listener channelListener =
        new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(IntMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(FloatMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(DecimalMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(BooleanMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(RecordMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(MapMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(TableMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(XmlMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(JsonMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(readonly & TableMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(readonly & XmlMessage message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(JsonMessage & readonly message) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(readonly & TableMessage message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(readonly & XmlMessage message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(JsonMessage & readonly message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(TableMessage message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(XmlMessage message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onMessage(JsonMessage message, rabbitmq:Caller caller) {
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(IntMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(FloatMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(DecimalMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(BooleanMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(RecordMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(MapMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(TableMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(XmlMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(readonly & TableMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(readonly & XmlMessage message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage & readonly message) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(readonly & TableMessage message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(readonly & XmlMessage message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage & readonly message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(TableMessage message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(XmlMessage message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage message, rabbitmq:Caller caller) returns string {
        return "Hello";
    }
}
