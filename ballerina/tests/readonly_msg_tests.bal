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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.'string;
import ballerina/log;
import ballerina/lang.runtime as runtime;
import ballerina/test;

string readOnlyConsumerMessage = "";
string readOnlyConsumerMessageCaller = "";
string readOnlyConsumerRequest = "";
string readOnlyConsumerRequestCaller = "";

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testConsumerReadOnlyMessage() returns error? {
    string message = "Testing Async Consumer with ReadOnly BytesMessage";
    check produceMessage(message, READONLY_MESSAGE_QUEUE);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(readOnlyMessageService);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(readOnlyConsumerMessage, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testConsumerReadOnlyMessage2() returns error? {
    string message = "Testing Async Consumer with ReadOnly Message and Caller";
    check produceMessage(message, READONLY_MESSAGE_QUEUE_CALLER);
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(readOnlyMessageService2);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(readOnlyConsumerMessageCaller, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testConsumerReadOnlyRequest() returns error? {
    string message = "Testing Async Consumer with ReadOnly Request";
    check produceMessage(message, READONLY_REQUEST_QUEUE, "ReadOnlyReplyQueue");
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(readOnlyMessageServiceRequest);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(readOnlyConsumerRequest, message, msg = "Message received does not match.");
    }
    return;
}

@test:Config {
    dependsOn: [testListener, testSyncConsumer],
    groups: ["rabbitmq"]
}
public function testConsumerReadOnlyRequest2() returns error? {
    string message = "Testing Async Consumer with ReadOnly Request and Caller";
    check produceMessage(message, READONLY_REQUEST_QUEUE_CALLER, "ReadOnlyReplyQueue");
    Listener? channelListener = rabbitmqListener;
    if channelListener is Listener {
        check channelListener.attach(readOnlyMessageServiceRequest2);
        check channelListener.'start();
        runtime:sleep(5);
        test:assertEquals(readOnlyConsumerRequestCaller, message, msg = "Message received does not match.");
    }
    return;
}

Service readOnlyMessageService =
@ServiceConfig {
    queueName: READONLY_MESSAGE_QUEUE
}
service object {
    remote function onMessage(readonly & BytesMessage message) {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            readOnlyConsumerMessage = messageContent;
            log:printInfo("The message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

Service readOnlyMessageService2 =
@ServiceConfig {
    queueName: READONLY_MESSAGE_QUEUE_CALLER
}
service object {
    remote function onMessage(readonly & BytesMessage message, Caller caller) {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            readOnlyConsumerMessageCaller = messageContent;
            log:printInfo("The message received: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
    }
};

Service readOnlyMessageServiceRequest =
@ServiceConfig {
    queueName: READONLY_REQUEST_QUEUE
}
service object {
    remote function onRequest(readonly & BytesMessage message) returns string {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            readOnlyConsumerRequest = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
        return "Hello Back!!";
    }
};

Service readOnlyMessageServiceRequest2 =
@ServiceConfig {
    queueName: READONLY_REQUEST_QUEUE_CALLER
}
service object {
    remote function onRequest(readonly & BytesMessage message, Caller caller) returns string {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is string {
            readOnlyConsumerRequestCaller = messageContent;
            log:printInfo("The message received in onRequest: " + messageContent);
        } else {
            log:printError("Error occurred while retrieving the message content.", 'error = messageContent);
        }
        return "Hello Back!!";
    }
};
