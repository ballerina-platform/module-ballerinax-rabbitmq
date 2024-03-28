// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


# Represents the anydata message, which a RabbitMQ server sends to its subscribed services.
#
# + content - The content of the message
# + routingKey - The routing key to which the message is sent
# + exchange - The exchange to which the message is sent. The default exchange is a direct exchange with no name (empty string) pre-declared by the broker.
# + deliveryTag - The delivery tag of the message
# + properties - Basic properties of the message - routing headers etc.
public type AnydataMessage record {|
    anydata content;
    string routingKey;
    string exchange = "";
    int deliveryTag?;
    BasicProperties properties?;
|};

# Represents the subtype of `rabbitmq:AnydataMessage` record where the message content is a byte array.
#
# + content - Message content in bytes
public type BytesMessage record {|
    *AnydataMessage;
    byte[] content;
|};
