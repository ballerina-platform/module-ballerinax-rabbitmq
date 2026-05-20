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

import ballerina/io;
import ballerinax/rabbitmq;

// Demonstrates connecting a rabbitmq:Client to a cluster or with failover support.
// The primary host and port are tried first. If the primary is unreachable, the
// addresses listed in `failoverAddresses` are tried in order.
public function main() returns error? {
    rabbitmq:Client rabbitmqClient = check new (
        rabbitmq:DEFAULT_HOST,
        rabbitmq:DEFAULT_PORT,
        failoverAddresses = [
            {host: "rabbitmq-node-2", port: 5672},
            {host: "rabbitmq-node-3", port: 5672}
        ]
    );

    check rabbitmqClient->queueDeclare("ClusterQueue");

    check rabbitmqClient->publishMessage({
        content: "Hello from cluster publisher!".toBytes(),
        routingKey: "ClusterQueue"
    });

    io:println("Message published to ClusterQueue.");
    check rabbitmqClient->close();
}
