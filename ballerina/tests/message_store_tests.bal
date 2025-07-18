// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/messaging;
import ballerina/test;
import ballerina/lang.runtime;
import ballerina/log;

const MESSAGE_STORE_QUEUE_NAME_1 = "MessageStoreQueue1";
final MessageStore messageStore1 = check new(MESSAGE_STORE_QUEUE_NAME_1);

const MESSAGE_STORE_QUEUE_NAME_2 = "MessageStoreQueue2";
final MessageStore messageStore2 = check new(MESSAGE_STORE_QUEUE_NAME_2);

const MESSAGE_STORE_QUEUE_NAME_3 = "MessageStoreQueue3";
final MessageStore messageStore3 = check new(MESSAGE_STORE_QUEUE_NAME_3);

const MESSAGE_STORE_RECEIVER_QUEUE_NAME_1 = "MessageStoreReceiverQueue1";
final MessageStore messageStoreReceiver1 = check new(MESSAGE_STORE_RECEIVER_QUEUE_NAME_1);

const MESSAGE_STORE_RECEIVER_QUEUE_NAME_2 = "MessageStoreReceiverQueue2";
final MessageStore messageStoreReceiver2 = check new(MESSAGE_STORE_RECEIVER_QUEUE_NAME_2);

const MESSAGE_STORE_DEAD_LETTER_QUEUE_NAME = "MessageStoreDeadLetterQueue";
final MessageStore messageStoreDeadLetter = check new(MESSAGE_STORE_DEAD_LETTER_QUEUE_NAME);

listener messaging:StoreListener messageStoreListenerWithDLS = new (messageStore1,
    pollingInterval = 3,
    maxRetries = 2,
    retryInterval = 1, 
    deadLetterStore = messageStoreDeadLetter
);

listener messaging:StoreListener messageStoreDefaultListener = new (messageStore2,
    pollingInterval = 3,
    maxRetries = 2,
    retryInterval = 1
);

listener messaging:StoreListener messageStoreListenerWithDrop = new (messageStore3,
    pollingInterval = 3,
    maxRetries = 2,
    retryInterval = 1,
    dropMessageAfterMaxRetries = true
);

service on messageStoreListenerWithDLS {

    isolated remote function onMessage(anydata payload) returns error? {
        log:printInfo("message received", payload = payload.toString());
        if payload == "fail" {
            // Simulate a failure to test dead-lettering
            return error("Simulated failure for testing dead-lettering");
        }
        return messageStoreReceiver1->store(payload);
    }
}

isolated int attempts = 0;

service on messageStoreDefaultListener {

    isolated remote function onMessage(anydata payload) returns error? {
        lock {
            if attempts <= 3 {
                attempts += 1;
                // Simulate failure for testing message drop
                return error("Simulated failure for testing message drop");
            }
        }
        return messageStoreReceiver2->store(payload);
    }
}

service on messageStoreListenerWithDrop {

    isolated remote function onMessage(anydata payload) returns error {
        // Simulate failure for testing message drop
        return error("Simulated failure for testing message drop");
    }
}

@test:Config {
    groups: ["message_store_listener"]
}
function testMessageStoreListenerWithDLSSuccess() returns error? {
    Client messageStoreClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    anydata payload = { id: "test-message", content: "This is a test message" };
    check messageStoreClient->publishMessage({
        content:  payload,
        routingKey: MESSAGE_STORE_QUEUE_NAME_1
        });
    runtime:sleep(5);

    AnydataMessage|error consumedMessage = messageStoreClient->consumeMessage(MESSAGE_STORE_QUEUE_NAME_1);
    if consumedMessage is AnydataMessage {
        test:assertFail("Message should be consumed by the listener, so it should not be available in the store");
    }

    Client messageStoreReceiverClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    map<json> receivedMessage = check messageStoreReceiverClient->consumePayload(MESSAGE_STORE_RECEIVER_QUEUE_NAME_1);
    test:assertEquals(receivedMessage, payload);
}

@test:Config {
    groups: ["message_store_listener"]
}
function testMessageStoreListenerWithDLSFailure() returns error? {
    Client messageStoreClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    anydata payload = "fail"; // This will trigger a failure in the service
    check messageStoreClient->publishMessage({
        content: payload,
        routingKey: MESSAGE_STORE_QUEUE_NAME_1
    });
    runtime:sleep(10);

    AnydataMessage|error consumedMessage = messageStoreClient->consumeMessage(MESSAGE_STORE_QUEUE_NAME_1);
    if consumedMessage is AnydataMessage {
        test:assertFail("Message should be consumed by the listener, so it should not be available in the store");
    }

    Client messageStoreDeadLetterClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    string deadLetterMessage = check messageStoreDeadLetterClient->consumePayload(MESSAGE_STORE_DEAD_LETTER_QUEUE_NAME);
    test:assertEquals(deadLetterMessage, payload);
}

@test:Config {
    groups: ["message_store_listener"]
}
function testMessageStoreListenerWithoutDrop() returns error? {
    Client messageStoreClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check messageStoreClient->publishMessage({
        content: "This message will not be dropped",
        routingKey: MESSAGE_STORE_QUEUE_NAME_2
    });
    runtime:sleep(20);

    AnydataMessage|error consumedMessage = messageStoreClient->consumeMessage(MESSAGE_STORE_QUEUE_NAME_2);
    if consumedMessage is AnydataMessage {
        test:assertFail("Message should be consumed by the listener second time, so it should not be available in the store");
    }

    Client messageStoreReceiverClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    string receivedMessage = check messageStoreReceiverClient->consumePayload(MESSAGE_STORE_RECEIVER_QUEUE_NAME_2);
    test:assertEquals(receivedMessage, "This message will not be dropped");
}

@test:Config {
    groups: ["message_store_listener"]
}
function testMessageStoreListenerWithDrop() returns error? {
    Client messageStoreClient = check new(DEFAULT_HOST, DEFAULT_PORT);
    check messageStoreClient->publishMessage({
        content: "This message will be dropped",
        routingKey: MESSAGE_STORE_QUEUE_NAME_3
    });
    runtime:sleep(5);

    AnydataMessage|error consumedMessage = messageStoreClient->consumeMessage(MESSAGE_STORE_QUEUE_NAME_3);
    if consumedMessage is AnydataMessage {
        test:assertFail("Message should be dropped by the listener, so it should not be available in the store");
    }
}

@test:Config {
    groups: ["message_store"]
}
function testMessageStoreBasicFunctions() returns error? {
    MessageStore store = check new("TestMessageStore");

    messaging:Message? retrievedMessage = check store->retrieve();
    if retrievedMessage is messaging:Message {
        test:assertFail("Expected no message to be retrieved from an empty store");
    }

    check store->store("testMessage1");
    check store->store("testMessage2");

    retrievedMessage = check store->retrieve();
    if retrievedMessage is () {
        test:assertFail("Expected a message to be retrieved from the store");
    }
    test:assertEquals(retrievedMessage.payload, "testMessage1");
    string firstMessageId = retrievedMessage.id;

    retrievedMessage = check store->retrieve();
    if retrievedMessage is () {
        test:assertFail("Expected a second message to be retrieved from the store");
    }
    test:assertEquals(retrievedMessage.payload, "testMessage2");
    string secondMessageId = retrievedMessage.id;

    retrievedMessage = check store->retrieve();
    if retrievedMessage is messaging:Message {
        test:assertFail("Expected no message to be retrieved from the store after retrieving two messages");
    }

    check store->acknowledge(firstMessageId, true);

    retrievedMessage = check store->retrieve();
    if retrievedMessage is messaging:Message {
        test:assertFail("Expected no message to be retrieved after acknowledging the first message");
    }

    check store->acknowledge(secondMessageId, false);

    retrievedMessage = check store->retrieve();
    if retrievedMessage is () {
        test:assertFail("Expected the second message to still be in the store after acknowledging it with failure");
    }
    test:assertEquals(retrievedMessage.payload, "testMessage2");

    error? ackResult = store->acknowledge("randomId", true);
    if ackResult is () {
        test:assertFail("Expected acknowledgment to fail for a non-existent message");
    }
    test:assertEquals(ackResult.message(), "Message with the given ID is not consumed or does not exist");
}
