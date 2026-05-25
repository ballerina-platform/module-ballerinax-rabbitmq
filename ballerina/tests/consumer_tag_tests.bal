// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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

import ballerina/http;
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/test;

const CONSUMER_TAG_QUEUE = "ConsumerTagQueue";
const CUSTOM_CONSUMER_TAG = "my-custom-consumer-tag";
string consumerTagReceivedMessage = "";

type ConsumerEntry record {
    string consumer_tag;
    record {string name;} queue;
};

function getConsumerTagsOnQueue(string queueName) returns string[]|error {
    http:Client mgmtClient = check new ("http://localhost:15672",
        {auth: {username: "guest", password: "guest"}, httpVersion: http:HTTP_1_1}
    );
    ConsumerEntry[] consumers = check mgmtClient->/api/consumers.get();
    return consumers
        .filter(c => c.queue.name == queueName)
        .map(c => c.consumer_tag);
}

Service consumerTagService =
@ServiceConfig {
    queueName: CONSUMER_TAG_QUEUE,
    consumerTag: CUSTOM_CONSUMER_TAG
}
service object {
    remote function onMessage(BytesMessage message) returns error? {
        consumerTagReceivedMessage = check string:fromBytes(message.content);
    }
};

@test:Config {
    groups: ["rabbitmq", "consumerTag"]
}
public function testCustomConsumerTagIsRegisteredOnBroker() returns error? {
    Client publisher = check new (DEFAULT_HOST, DEFAULT_PORT);
    check publisher->queueDeclare(CONSUMER_TAG_QUEUE);
    Listener tagListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check tagListener.attach(consumerTagService);
    check tagListener.'start();

    // Give the broker a moment to register the consumer
    runtime:sleep(5);

    string[] tags = check getConsumerTagsOnQueue(CONSUMER_TAG_QUEUE);
    log:printInfo("Registered consumer tags on " + CONSUMER_TAG_QUEUE + ": " + tags.toString());
    test:assertTrue(tags.indexOf(CUSTOM_CONSUMER_TAG) !is (),
            msg = "Expected consumer tag '" + CUSTOM_CONSUMER_TAG + "' was not found on the broker. Got: " + tags.toString());

    check tagListener.gracefulStop();
    check publisher->close();
}

@test:Config {
    dependsOn: [testCustomConsumerTagIsRegisteredOnBroker],
    groups: ["rabbitmq", "consumerTag"]
}
public function testCustomConsumerTagReceivesMessages() returns error? {
    Client publisher = check new (DEFAULT_HOST, DEFAULT_PORT);
    check publisher->queueDeclare(CONSUMER_TAG_QUEUE);
    Listener tagListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check tagListener.attach(consumerTagService);
    check tagListener.'start();

    string testMsg = "Hello with custom consumer tag";
    check publisher->publishMessage({content: testMsg.toBytes(), routingKey: CONSUMER_TAG_QUEUE});

    runtime:sleep(3);
    test:assertEquals(consumerTagReceivedMessage, testMsg,
            msg = "Service with custom consumerTag did not receive the expected message.");

    check tagListener.gracefulStop();
    check publisher->close();
}
