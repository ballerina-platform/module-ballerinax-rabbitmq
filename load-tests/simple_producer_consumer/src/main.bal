// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/lang.runtime;
import ballerina/log;
import ballerina/http;
import ballerina/time;
import ballerina/lang.value;

const string QUEUE_NAME = "PerfQueue";
Message SENDING_MESSAGE = {
    id: 12501,
    name: "User",
    content: "This is the message content of the load test.",
    extra: "This contains the extra content of load test message record."
};
Message FINAL_MESSAGE = {
    id: 12501,
    name: "User",
    content: "This is the ending message content of the load test.",
    extra: "This contains the final extra content of load test message record."
};

int errorCount = 0;
int sentCount = 0;
int receivedCount = 0;
time:Utc startedTime = time:utcNow();
time:Utc endedTime = time:utcNow();
boolean finished = false;

service /rabbitmq on new http:Listener(9100) {
    function init() {
        log:printInfo("Service starting...");
    }

    resource function get publish() returns boolean {
        error? result = startListener();
        if result is error {
            return false;
        }
        errorCount = 0;
        sentCount = 0;
        receivedCount = 0;
        startedTime = time:utcNow();
        endedTime = time:utcNow();
        finished = false;
        _ = start publishMessages();
        return true;
    }

    resource function get getResults() returns boolean|map<string> {
        if finished {
            return {
                errorCount: errorCount.toString(),
                time: time:utcDiffSeconds(endedTime, startedTime).toString(),
                sentCount: sentCount.toString(),
                receivedCount: receivedCount.toString()
            };
        }
        return false;
    }
}

function publishMessages() returns error? {
    startedTime = time:utcNow();
    // Publishing messages for 1 hour
    int endingTimeInSecs = startedTime[0] + 3600;
    rabbitmq:Client|error rabbitmqClient = new("rabbitmq", 5672);
    if (rabbitmqClient is error) {
        log:printInfo("Error occurred when creating the rabbitmq client connection.");
    } else {
        check rabbitmqClient->queueDeclare(QUEUE_NAME);
        while time:utcNow()[0] <= endingTimeInSecs {
            error? result = rabbitmqClient->publishMessage({ content:SENDING_MESSAGE.toString().toBytes(),
                                routingKey:QUEUE_NAME });
            if result is error {
                lock {
                    errorCount += 1;
                }
            } else {
                sentCount +=1;
            }
            runtime:sleep(0.1);
        }
        error? result = rabbitmqClient->publishMessage({ content:FINAL_MESSAGE.toString().toBytes(),
                                            routingKey:QUEUE_NAME });
        if result is error {
            lock {
                errorCount += 1;
            }
        } else {
            sentCount +=1;
        }
    }
}

function startListener() returns error? {
    rabbitmq:Listener|error rabbitmqListener = new("rabbitmq", 5672);
    if (rabbitmqListener is error) {
        log:printInfo("Error occurred when creating the rabbitmq listener connection.");
    } else {
        check rabbitmqListener.attach(rabbitmqService);
        check rabbitmqListener.start();
        runtime:registerListener(rabbitmqListener);
    }
}

rabbitmq:Service rabbitmqService =
@rabbitmq:ServiceConfig {
    queueName: QUEUE_NAME
}
service object {
    remote function onMessage(rabbitmq:BytesMessage message) returns error? {
        string|error messageContent = 'string:fromBytes(message.content);
        if messageContent is error {
            lock {
                errorCount += 1;
            }
        } else {
            json|error jsonContent = value:fromJsonString(messageContent);
            if jsonContent is error {
                lock {
                    errorCount += 1;
                }
            } else {
                Message|error receivedMessage = jsonContent.cloneReadOnly().ensureType(Message);
                if receivedMessage is error {
                    lock {
                        errorCount += 1;
                    }
                } else {
                    if receivedMessage == SENDING_MESSAGE {
                        receivedCount += 1;
                    } else if receivedMessage == FINAL_MESSAGE {
                        finished = true;
                        endedTime = time:utcNow();
                    } else {
                        lock {
                            errorCount += 1;
                        }
                    }
                }
            }
        }
    }
};

public type Message record {|
    int id;
    string name;
    string content;
    string extra;
|};
