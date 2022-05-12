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

public type JsonMessage record {|
    *rabbitmq:AnydataMessage;
    json content;
|};

public type Person record {|
    string name;
    int age;
    boolean married;
|};

public type RandomPayload record {|
    string content;
    string routingKey;
    string exchange = "";
    int deliveryTag?;
    record {|
        string replyTo?;
        string contentType?;
        string contentEncoding?;
        string correlationId?;
    |} properties?;
|};

listener rabbitmq:Listener channelListener =
        new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage message, string payload) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage message, @rabbitmq:Payload RandomPayload payload) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(JsonMessage message, rabbitmq:Caller caller, string payload) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(@rabbitmq:Payload RandomPayload payload) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(@rabbitmq:Payload RandomPayload & readonly payload) returns string {
        return "Hello";
    }
}

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmq:Service on channelListener {
    remote function onRequest(string & readonly message) returns string {
        return "Hello";
    }
}
