// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/websubhub;
import rabbitmqHub.config;
import rabbitmqHub.util;

// Producer which persist the current in-memory state of the Hub
public final rabbitmq:Client statePersistProducer = check new (rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

// Consumer which reads the persisted subscriber details
public final rabbitmq:Client subscribersConsumer = check new (rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

// Consumer which reads the persisted subscriber details
// TODO: NO AVAILABLE WAY TO DO THIS IN RABBITMQ! PERSISTING MESSAGES???
//kafka:ConsumerConfiguration registeredTopicsConsumerConfig = {
//    groupId: "consolidated--websub-topics-group-" + config:CONSTRUCTED_SERVER_ID,
//    offsetReset: "earliest",
//    topics: [ config:CONSOLIDATED_WEBSUB_TOPICS_TOPIC ]
//};
//public final kafka:Consumer registeredTopicsConsumer = check new (config:KAFKA_BOOTSTRAP_NODE, registeredTopicsConsumerConfig);

# Creates a `rabbitmq:Client` for a subscriber.
# 
# + message - The subscription details
# + return - `rabbitmq:Client` if succcessful or else `error`
public isolated function createMessageConsumer(websubhub:VerifiedSubscription message) returns rabbitmq:Client|error {
    string topicName = util:sanitizeTopicName(message.hubTopic);
    string groupName = util:generateGroupName(message.hubTopic, message.hubCallback);
    return check new (rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
}
