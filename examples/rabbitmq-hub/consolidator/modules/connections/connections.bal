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

// Producer which persist the current consolidated in-memory state of the system
public final rabbitmq:Client statePersistProducer = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

// Consumer which reads the consolidated topic details
public final rabbitmq:Client consolidatedTopicsConsumer = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

// Consumer which reads the consolidated subscriber details
public final rabbitmq:Client consolidatedSubscriberConsumer = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

// Consumer which reads the persisted topic-registration/topic-deregistration/subscription/unsubscription events
//REGISTERED_WEBSUB_TOPICS_QUEUE
//WEBSUB_SUBSCRIBERS_QUEUE
public final rabbitmq:Client websubEventConsumer = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
