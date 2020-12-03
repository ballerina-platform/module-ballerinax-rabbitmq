// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;

# Provides the functionality to manipulate the messages received by the consumer services.
public client class Caller {
    private boolean ackStatus = false;
    private boolean autoAck = true;

    # Acknowledges one or several received messages.
    # ```ballerina
    # rabbitmq:Error? ackResult = caller->basicAck(true);
    # ```
    #
    # + multiple - `true` to acknowledge all messages up to and including the called on message and
    #              `false` to acknowledge just the called on message
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function basicAck(boolean multiple = false) returns Error? {
        var result = nativeBasicAck(multiple, self.autoAck, self.ackStatus, self);
        self.ackStatus = true;
        return result;
    }

    # Rejects one or several received messages.
    # ```ballerina
    # rabbitmq:Error? nackResult = caller->basicNack(true, requeue = false);
    # ```
    #
    # + multiple - `true` to reject all messages up to and including the called on message and
    #              `false` to reject just the called on message
    # + requeue - `true` if the rejected message(s) should be re-queued rather than discarded/dead-lettered
    # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
    isolated remote function basicNack(boolean multiple = false, boolean requeue = true)
                            returns Error? {
        var result = nativeBasicNack(self.autoAck, self.ackStatus,
                                multiple, requeue, self);
        self.ackStatus = true;
        return result;
    }
}

isolated function nativeBasicAck(boolean multiple, boolean ackMode, boolean ackStatus, Caller caller) returns Error? =
@java:Method {
    name: "basicAck",
    'class: "org.ballerinalang.messaging.rabbitmq.util.MessageUtils"
} external;

isolated function nativeBasicNack(boolean ackMode, boolean ackStatus, boolean multiple, boolean requeue, Caller caller)
 returns Error? = @java:Method {
    name: "basicNack",
    'class: "org.ballerinalang.messaging.rabbitmq.util.MessageUtils"
} external;
