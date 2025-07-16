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
import ballerina/uuid;
import ballerina/log;

# Represents the RabbitMQ store client configuration.
public type StoreClientConfiguration record {|
    # The RabbitMQ server host. Defaults to "localhost"
    string host = DEFAULT_HOST;
    # The RabbitMQ server port. Defaults to 5672
    int port = DEFAULT_PORT;
    # The RabbitMQ connection configuration
    ConnectionConfiguration connectionData = {};
    # The RabbitMQ publish configuration
    StoreClientPublishConfiguration publishConfig = {};
    # The RabbitMQ queue declaration configuration
    StoreClientDeclareQueueConfiguration declareQueue = {};
|};

# Represents the RabbitMQ message queue declaration configuration.
public type StoreClientDeclareQueueConfiguration record {|
    # Enable or disable the queue declaration. Defaults to true
    boolean enabled = true;
    # Additional queue configuration to match the RabbitMQ queue declaration
    QueueConfig queueConfig?;
|};

# Represents a RabbitMQ store client configuration for publishing messages.
public type StoreClientPublishConfiguration record {|
    # The RabbitMQ exchange to publish messages to. Defaults to an empty string
    string exchange = "";
    # The RabbitMQ delivery tag for the message. Optional
    int deliveryTag?;
    # The RabbitMQ message properties. Optional
    BasicProperties properties?;
|};

# Represents a RabbitMQ message store implementation.
public isolated client class MessageStore {
    *messaging:Store;

    private final Client rabbitMqClient;
    private final readonly & StoreClientPublishConfiguration publishConfig;
    private final string queueName;
    private map<int?> consumedMessageDeliverTags;
    private final readonly & StoreClientConfiguration clientConfig;

    # Initializes a new instance of the RabbitMqStore class.
    #
    # + queueName - The name of the RabbitMQ queue to use for storing messages
    # + clientConfig - The RabbitMQ client configuration
    public isolated function init(string queueName, *StoreClientConfiguration clientConfig) returns error? {
        self.clientConfig = clientConfig.cloneReadOnly();
        self.rabbitMqClient = check new (clientConfig.host, clientConfig.port, clientConfig.connectionData);
        self.publishConfig = clientConfig.publishConfig.cloneReadOnly();
        self.queueName = queueName;
        self.consumedMessageDeliverTags = {};

        if clientConfig.declareQueue.enabled {
            check self.rabbitMqClient->queueDeclare(queueName, clientConfig.declareQueue.queueConfig);
        }
    }

    # Stores a message in the RabbitMQ message store.
    #
    # + payload - The message payload to be stored
    # + return - An error if the message could not be stored, or `()`
    isolated remote function store(anydata payload) returns error? {
        error? result = self.rabbitMqClient->publishMessage({
            content: payload,
            routingKey: self.queueName,
            ...self.publishConfig
        });

        if result is error {
            return error("Failed to store message in RabbitMQ", cause = result);
        }
    }

    # Retrieves the top message from the RabbitMQ message store without removing it. Retrieving
    # concurrently without acknowledgment will return the next message in the queue if the previous one
    # is not acknowledged.
    #
    # + return - The retrieved message, or `()` if the store is empty, or an error if an error occurs
    isolated remote function retrieve() returns messaging:Message|error? {
        lock {
            AnydataMessage|error message = self.rabbitMqClient->consumeMessage(self.queueName, false);
            if message is error {
                // TODO: Ideally this should be a specific error type
                if message.message().startsWith("No messages are found") {
                    return; // No messages available in the queue
                }
                return error("Failed to retrieve message from RabbitMQ", cause = message);
            }

            anydata payload = message.content;
            // The payload is always a byte[] when bound to anydata type. For better experience, 
            // convert it to string or json
            // TODO: Check for a better way to handle this, may be use the content type
            if payload is byte[] {
                string|error fromBytes = string:fromBytes(payload);
                if fromBytes is string {
                    payload = fromBytes;
                    json|error fromJsonString = fromBytes.fromJsonString();
                    if fromJsonString is json {
                        payload = fromJsonString.clone();
                    }
                }
            }
            string id = uuid:createType1AsString();
            int? deliveryTag = message.deliveryTag;
            self.consumedMessageDeliverTags[id] = deliveryTag;
            return {id, payload: payload.clone()};
        }
    }

    # Acknowledges the processing of a message. When acknowledged with success, the message is
    # removed from the store.
    #
    # + id - The unique identifier of the message to acknowledge
    # + success - Indicates whether the message was processed successfully
    # + return - An error if the acknowledgment could not be processed, or `()`
    isolated remote function acknowledge(string id, boolean success = true) returns error? {
        lock {
            if !self.consumedMessageDeliverTags.hasKey(id) {
                return error("Message with the given ID is not consumed or does not exist");
            }
            int? deliverTag = self.consumedMessageDeliverTags.get(id);
            if deliverTag is () {
                log:printWarn("message with the given ID does not have a delivery tag, cannot acknowledge",
                    msgId = id);
                return;
            }
            error? result;
            if success {
                result = self.rabbitMqClient.basicAckWithDeliveryTag(deliverTag);
            } else {
                result = self.rabbitMqClient.basicNackWithDeliveryTag(deliverTag);
            }
            _ = self.consumedMessageDeliverTags.remove(id);
            if result is error {
                return error("Failed to acknowledge message from RabbitMQ", cause = result);
            }
        }
    }
}
