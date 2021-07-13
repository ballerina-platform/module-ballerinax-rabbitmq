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

import ballerina/jballerina.java;
import ballerina/uuid;

# Ballerina RabbitMQ Message Listener.
# Provides a listener to consume messages from the RabbitMQ server.
public isolated class Listener {

    private string connectorId = uuid:createType4AsString();

    # Initializes a Listener object with the given connection configuration. Sets the global QoS settings,
    # which will be applied to the entire `rabbitmq:Listener`.
    # ```ballerina
    # rabbitmq:Listener rabbitmqListener = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
    # ```
    #
    # + host - The host used for establishing the connection
    # + port - The port used for establishing the connection
    # + qosSettings - The consumer prefetch settings
    # + connectionData - The connection configuration
    public isolated function init(string host, int port, QosSettings? qosSettings = (),
                                    *ConnectionConfiguration connectionData) returns Error? {
        Error? initResult = externInit(host, port, self, connectionData);
        if !(initResult is Error) {
            if (qosSettings is QosSettings) {
                checkpanic nativeSetQosSettings(qosSettings.prefetchCount, qosSettings?.prefetchSize,
                    qosSettings.global, self);
            }
        } else {
            return initResult;
        }
    }

    # Attaches the service to the `rabbitmq:Listener` endpoint.
    # ```ballerina
    # check rabbitmqListener.attach(service, "serviceName");
    # ```
    #
    # + s - The type descriptor of the service
    # + name - The name of the service
    # + return - `()` or else a `rabbitmq:Error` upon failure to register the service
    public isolated function attach(Service s, string[]|string? name = ()) returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
    } external;

    # Starts consuming the messages on all the attached services.
    # ```ballerina
    # check rabbitmqListener.'start();
    # ```
    #
    # + return - `()` or else a `rabbitmq:Error` upon failure to start
    public isolated function 'start() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
    } external;

    # Stops consuming messages and detaches the service from the `rabbitmq:Listener` endpoint.
    # ```ballerina
    # check rabbitmqListener.detach(service);
    # ```
    #
    # + s - The type descriptor of the service
    # + return - `()` or else  a `rabbitmq:Error` upon failure to detach the service
    public isolated function detach(Service s) returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
    } external;

    # Stops consuming messages through all consumer services by terminating the connection and all its channels.
    # ```ballerina
    # check rabbitmqListener.gracefulStop();
    # ```
    #
    # + return - `()` or else  a `rabbitmq:Error` upon failure to close the `ChannelListener`
    public isolated function gracefulStop() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
    } external;

    # Stops consuming messages through all the consumer services and terminates the connection
    # with the server.
    # ```ballerina
    # check rabbitmqListener.immediateStop();
    # ```
    #
    # + return - `()` or else  a `rabbitmq:Error` upon failure to close ChannelListener.
    public isolated function immediateStop() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
    } external;
}

# Configurations required to create a subscription.
#
# + queueName - The name of the queue to be subscribed
# + autoAck - If false, should manually acknowledge
public type RabbitMQServiceConfig record {|
    string queueName;
    boolean autoAck = true;
|};

# The annotation, which is used to configure the subscription.
public annotation RabbitMQServiceConfig ServiceConfig on service, class;

isolated function externInit(string host, int port, Listener lis, *ConnectionConfiguration connectionData)
returns Error? = @java:Method {
    name: "init",
    'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
} external;

isolated function nativeSetQosSettings(int count, int? size, boolean global, Listener lis) returns Error? =
@java:Method {
    name: "setQosSettings",
    'class: "io.ballerina.stdlib.rabbitmq.util.ListenerUtils"
} external;
