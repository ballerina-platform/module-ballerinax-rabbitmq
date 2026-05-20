// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
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

import ballerina/log;
import ballerinax/rabbitmq;

// Demonstrates connecting a rabbitmq:Listener to a cluster or with failover support.
// The primary host and port are tried first. If the primary is unreachable, the
// addresses listed in `addresses` are tried in order.
listener rabbitmq:Listener rabbitmqListener = check new (
    rabbitmq:DEFAULT_HOST,
    rabbitmq:DEFAULT_PORT,
    addresses = [
        {host: "rabbitmq-node-2", port: 5672},
        {host: "rabbitmq-node-3", port: 5672}
    ]
);

@rabbitmq:ServiceConfig {
    queueName: "ClusterQueue"
}
service rabbitmq:Service on rabbitmqListener {
    remote function onMessage(rabbitmq:BytesMessage message) {
        string|error content = string:fromBytes(message.content);
        if content is string {
            log:printInfo("Received message: " + content);
        }
    }
}
