// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/log;
import ballerina/lang.runtime;

public type StringMessage record {|
    *AnydataMessage;
    string content;
|};

public type IntMessage record {|
    *AnydataMessage;
    int content;
|};

public type FloatMessage record {|
    *AnydataMessage;
    float content;
|};

public type DecimalMessage record {|
    *AnydataMessage;
    decimal content;
|};

public type BooleanMessage record {|
    *AnydataMessage;
    boolean content;
|};

public type RecordMessage record {|
    *AnydataMessage;
    Person content;
|};

public type MapMessage record {|
    *AnydataMessage;
    map<Person> content;
|};

public type TableMessage record {|
    *AnydataMessage;
    table<Person> content;
|};

public type XmlMessage record {|
    *AnydataMessage;
    xml content;
|};

public type JsonMessage record {|
    *AnydataMessage;
    json content;
|};

public type Person record {|
    string name;
    int age;
    boolean married;
|};

string receivedStringValue = "";
string receivedStringReqValue = "";
int receivedIntValue = 0;
int receivedIntReqValue = 0;
decimal receivedDecimalValue = 0;
decimal receivedDecimalReqValue = 0;
float receivedFloatValue = 0;
float receivedFloatReqValue = 0;
boolean receivedBooleanValue = false;
boolean receivedBooleanReqValue = false;
Person? receivedRecordValue = ();
Person? receivedRecordReqValue = ();
map<Person> receivedMapValue = {};
map<Person> receivedMapReqValue = {};
table<Person> receivedTableValue = table [];
table<Person> receivedTableReqValue = table [];
xml receivedXmlValue = xml ``;
xml receivedXmlReqValue = xml ``;
json receivedJsonValue = "";
json receivedJsonReqValue = "";
int receivedIntPayload = 0;
int receivedIntReqPayload = 0;
float receivedFloatPayload = 0;
float receivedFloatReqPayload = 0;
decimal receivedDecimalPayload = 0;
decimal receivedDecimalReqPayload = 0;
boolean receivedBooleanPayload = false;
boolean receivedBooleanReqPayload = false;
string receivedStringPayload = "";
string receivedStringReqPayload = "";
xml receivedXmlPayload = xml ``;
xml receivedXmlReqPayload = xml ``;
Person? receivedPersonPayload = ();
Person? receivedPersonReqPayload = ();
map<Person> receivedMapPayload = {};
map<Person> receivedMapReqPayload = {};
table<Person> receivedTablePayload = table [];
table<Person> receivedTableReqPayload = table [];
json receivedJsonPayload = {};
json receivedJsonReqPayload = {};
json receivedRandomPayloadValue = {};
json receivedRandomPayloadReqValue = {};
boolean readOnlyReceived = false;
int receivedErrorCount = 0;

Person personRecord = {
    name: "Phil Dunphy",
    age: 40,
    married: true
};

map<Person> personMap = {
    "P1": personRecord,
    "P2": personRecord,
    "P3": personRecord
};

public type RandomPayload record {|
    string content;
    string routingKey;
    string exchange = "";
    int deliveryTag?;
    record {|
        string replyTo?;
        string contentType?;
        string contentEncoding?;
        string correlationId?;
    |} properties?;
|};

RandomPayload payloadMsg = {
    content: "Hello",
    deliveryTag: 1,
    exchange: "mock-exchange",
    properties: {
        contentEncoding: "mock-encoding",
        contentType: "mock-type",
        correlationId: "mock-id",
        replyTo: "mock-queue"
    },
    routingKey: "mock-key"
};

public type PayloadMessage record {|
    *AnydataMessage;
    RandomPayload content;
|};

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerStringBinding() returns error? {
    string message = "This is a data binding related message";

    Service stringService =
    @ServiceConfig {
        queueName: DATA_BINDING_STRING_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(StringMessage stringMessage) {
            receivedStringValue = stringMessage.content;
            log:printInfo("The message received: " + stringMessage.toString());
        }
    };

    check produceMessage(message, DATA_BINDING_STRING_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(stringService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedStringValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerStringRequestBinding() returns error? {
    string message = "This is a data binding related message";

    Service stringRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_STRING_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(StringMessage stringMessage) returns string {
            receivedStringReqValue = stringMessage.content;
            log:printInfo("The message received in onRequest: " + stringMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message, DATA_BINDING_STRING_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(stringRequestService);
    check channelListener.'start();
    runtime:sleep(5);
    test:assertEquals(receivedStringReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerIntBinding() returns error? {
    int message = 510;

    Service intService =
    @ServiceConfig {
        queueName: DATA_BINDING_INT_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(IntMessage intMessage) {
            receivedIntValue = intMessage.content;
            log:printInfo("The message received: " + intMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_INT_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedIntValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerIntRequestBinding() returns error? {
    int message = 510;

    Service intRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_INT_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(IntMessage intMessage) returns string {
            receivedIntReqValue = intMessage.content;
            log:printInfo("The message received in onRequest: " + intMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_INT_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedIntReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerDecimalBinding() returns error? {
    decimal message = 510;

    Service decimalService =
    @ServiceConfig {
        queueName: DATA_BINDING_DECIMAL_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(DecimalMessage decimalMessage) {
            receivedDecimalValue = decimalMessage.content;
            log:printInfo("The message received: " + decimalMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(decimalService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedDecimalValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerDecimalRequestBinding() returns error? {
    decimal message = 510;

    Service decimalRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_DECIMAL_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(DecimalMessage decimalMessage) returns string {
            receivedDecimalReqValue = decimalMessage.content;
            log:printInfo("The message received in onRequest: " + decimalMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(decimalRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedDecimalReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerFloatBinding() returns error? {
    float message = 41.258;

    Service floatService =
    @ServiceConfig {
        queueName: DATA_BINDING_FLOAT_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(FloatMessage floatMessage) {
            receivedFloatValue = floatMessage.content;
            log:printInfo("The message received: " + floatMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_FLOAT_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(floatService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedFloatValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerFloatRequestBinding() returns error? {
    float message = 41.258;

    Service floatRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_FLOAT_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(FloatMessage floatMessage) returns string {
            receivedFloatReqValue = floatMessage.content;
            log:printInfo("The message received in onRequest: " + floatMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_FLOAT_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(floatRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedFloatReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerBooleanBinding() returns error? {
    boolean message = true;

    Service booleanService =
    @ServiceConfig {
        queueName: DATA_BINDING_BOOLEAN_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(BooleanMessage booleanMessage) {
            receivedBooleanValue = booleanMessage.content;
            log:printInfo("The message received: " + booleanMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(booleanService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedBooleanValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerBooleanRequestBinding() returns error? {
    boolean message = true;

    Service booleanRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_BOOLEAN_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(BooleanMessage booleanMessage) returns string {
            receivedBooleanReqValue = booleanMessage.content;
            log:printInfo("The message received in onRequest: " + booleanMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(booleanRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedBooleanReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerRecordBinding() returns error? {
    Service recordService =
    @ServiceConfig {
        queueName: DATA_BINDING_RECORD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(RecordMessage recordMessage) {
            receivedRecordValue = recordMessage.content;
            log:printInfo("The message received: " + recordMessage.toString());
        }
    };

    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(recordService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedRecordValue, personRecord, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerRecordRequestBinding() returns error? {
    Service recordRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_RECORD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(RecordMessage recordMessage) returns string {
            receivedRecordReqValue = recordMessage.content;
            log:printInfo("The message received in onRequest: " + recordMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(recordRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedRecordValue, personRecord, msg = "Message received does not match.");
    test:assertEquals(receivedRecordReqValue, personRecord, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerMapBinding() returns error? {
    Service mapService =
    @ServiceConfig {
        queueName: DATA_BINDING_MAP_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(MapMessage mapMessage) {
            receivedMapValue = mapMessage.content;
            log:printInfo("The message received: " + mapMessage.toString());
        }
    };

    check produceMessage(personMap.toString(), DATA_BINDING_MAP_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(mapService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedMapValue, personMap, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerMapRequestBinding() returns error? {
    Service mapRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_MAP_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(MapMessage mapMessage) returns string {
            receivedMapReqValue = mapMessage.content;
            log:printInfo("The message received in onRequest: " + mapMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(personMap.toString(), DATA_BINDING_MAP_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(mapRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedMapReqValue, personMap, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerTableBinding() returns error? {
    table<Person> message = table [];
    message.add(personRecord);

    Service tableService =
    @ServiceConfig {
        queueName: DATA_BINDING_TABLE_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(TableMessage tableMessage) {
            receivedTableValue = tableMessage.content;
            log:printInfo("The message received: " + tableMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_TABLE_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(tableService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedTableValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerTableRequestBinding() returns error? {
    table<Person> message = table [];
    message.add(personRecord);

    Service tableRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_TABLE_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(TableMessage tableMessage) returns string {
            receivedTableReqValue = tableMessage.content;
            log:printInfo("The message received in onRequest: " + tableMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_TABLE_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(tableRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedTableReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerXmlBinding() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;

    Service xmlService =
    @ServiceConfig {
        queueName: DATA_BINDING_XML_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(XmlMessage xmlMessage) {
            receivedXmlValue = xmlMessage.content;
            log:printInfo("The message received: " + xmlMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_XML_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(xmlService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedXmlValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerXmlRequestBinding() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;

    Service xmlRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_XML_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(XmlMessage xmlMessage) returns string {
            receivedXmlReqValue = xmlMessage.content;
            log:printInfo("The message received in onRequest: " + xmlMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_XML_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(xmlRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedXmlReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerJsonBinding() returns error? {
    json message = personMap.toJson();

    Service jsonService =
    @ServiceConfig {
        queueName: DATA_BINDING_JSON_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(JsonMessage jsonMessage, Caller caller) {
            receivedJsonValue = jsonMessage.content;
            log:printInfo("The message received: " + jsonMessage.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_JSON_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedJsonValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerJsonRequestBinding() returns error? {
    json message = personMap.toJson();

    Service jsonRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_JSON_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(JsonMessage jsonMessage, Caller caller) returns string {
            receivedJsonReqValue = jsonMessage.content;
            log:printInfo("The message received in onRequest: " + jsonMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_JSON_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedJsonReqValue, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerDataBindingError() returns error? {
    json message = personMap.toJson();

    Service jsonService =
    @ServiceConfig {
        queueName: DATA_BINDING_ERROR_QUEUE
    }
    service object {
        remote function onMessage(IntMessage intMessage) {
            receivedJsonValue = intMessage.content;
            log:printInfo("The message received: " + intMessage.toString());
        }

        remote function onError(Message msg, Error e) returns Error? {
            if e.message().includes("ConversionError", 0) {
                receivedErrorCount += 1;
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_ERROR_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedErrorCount, 1);
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"],
    dependsOn: [testListenerDataBindingError]
}
public function testListenerRequestDataBindingError() returns error? {
    json message = personMap.toJson();

    Service jsonRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_ERROR_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(IntMessage intMessage) returns string {
            receivedJsonReqValue = intMessage.content;
            log:printInfo("The message received in onRequest: " + intMessage.toString());
            return "Hello Back!!";
        }

        remote function onError(Message msg, Error e) returns Error? {
            if e.message().includes("ConversionError", 0) {
                receivedErrorCount += 1;
            }
            log:printInfo("An error received in onError: " + e.message());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_ERROR_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedErrorCount, 2);
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerReadonlyJsonBinding() returns error? {
    json message = personMap.toJson();

    Service jsonRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_JSON_LISTENER_READONLY_QUEUE
    }
    service object {
        remote function onRequest(JsonMessage & readonly jsonMessage, Caller caller) returns string {
            readOnlyReceived = jsonMessage.isReadOnly();
            log:printInfo("The message received in onRequest: " + jsonMessage.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_JSON_LISTENER_READONLY_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertTrue(readOnlyReceived);
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerStringPayloadBinding() returns error? {
    string message = "This is a data binding related message";

    Service stringService =
    @ServiceConfig {
        queueName: DATA_BINDING_STRING_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(string payload) {
            receivedStringPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message, DATA_BINDING_STRING_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(stringService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedStringPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerStringPayloadRequestBinding() returns error? {
    string message = "This is a data binding related message";

    Service stringRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_STRING_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(string payload) returns string {
            receivedStringReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload);
            return "Hello Back!!";
        }
    };

    check produceMessage(message, DATA_BINDING_STRING_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(stringRequestService);
    check channelListener.'start();
    runtime:sleep(5);
    test:assertEquals(receivedStringReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerIntPayloadBinding() returns error? {
    int message = 510;

    Service intService =
    @ServiceConfig {
        queueName: DATA_BINDING_INT_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(int payload) {
            receivedIntPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_INT_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intService);
    check channelListener.'start();
    runtime:sleep(5);
    test:assertEquals(receivedIntPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerIntPayloadRequestBinding() returns error? {
    int message = 510;

    Service intRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_INT_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(int payload) returns string {
            receivedIntReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_INT_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(intRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedIntReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerDecimalPayloadBinding() returns error? {
    decimal message = 510;

    Service decimalService =
    @ServiceConfig {
        queueName: DATA_BINDING_DECIMAL_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(decimal payload) {
            receivedDecimalPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(decimalService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedDecimalPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerDecimalPayloadRequestBinding() returns error? {
    decimal message = 510;

    Service decimalRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_DECIMAL_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(decimal payload) returns string {
            receivedDecimalReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(decimalRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedDecimalReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerFloatPayloadBinding() returns error? {
    float message = 41.258;

    Service floatService =
    @ServiceConfig {
        queueName: DATA_BINDING_FLOAT_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(float payload) {
            receivedFloatPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_FLOAT_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(floatService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedFloatPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerFloatPayloadRequestBinding() returns error? {
    float message = 41.258;

    Service floatRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_FLOAT_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(float payload) returns string {
            receivedFloatReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_FLOAT_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(floatRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedFloatReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerBooleanPayloadBinding() returns error? {
    boolean message = true;

    Service booleanService =
    @ServiceConfig {
        queueName: DATA_BINDING_BOOLEAN_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(boolean payload) {
            receivedBooleanPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }

        remote function onError(Error e) {
            log:printError(e.message());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(booleanService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedBooleanPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerBooleanPayloadRequestBinding() returns error? {
    boolean message = true;

    Service booleanRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_BOOLEAN_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(boolean payload) returns string {
            receivedBooleanReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }

        remote function onError(Error e) {
            log:printError(e.message());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(booleanRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedBooleanReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerRecordPayloadBinding() returns error? {
    Service recordService =
    @ServiceConfig {
        queueName: DATA_BINDING_RECORD_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(Person payload) {
            receivedPersonPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(recordService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedPersonPayload, personRecord, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerRecordPayloadRequestBinding() returns error? {
    Service recordRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_RECORD_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(Person payload) returns string {
            receivedPersonReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(recordRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedPersonReqPayload, personRecord, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerMapPayloadBinding() returns error? {
    Service mapService =
    @ServiceConfig {
        queueName: DATA_BINDING_MAP_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(map<Person> payload) {
            receivedMapPayload= payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(personMap.toString(), DATA_BINDING_MAP_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(mapService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedMapPayload, personMap, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerMapPayloadRequestBinding() returns error? {
    Service mapRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_MAP_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(map<Person> payload) returns string {
            receivedMapReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(personMap.toString(), DATA_BINDING_MAP_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(mapRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedMapReqPayload, personMap, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerTablePayloadBinding() returns error? {
    table<Person> message = table [];
    message.add(personRecord);

    Service tableService =
    @ServiceConfig {
        queueName: DATA_BINDING_TABLE_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(table<Person> payload) {
            receivedTablePayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_TABLE_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(tableService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedTablePayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerTablePayloadRequestBinding() returns error? {
    table<Person> message = table [];
    message.add(personRecord);

    Service tableRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_TABLE_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(table<Person> payload) returns string {
            receivedTableReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_TABLE_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(tableRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedTableReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerXmlPayloadBinding() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;

    Service xmlService =
    @ServiceConfig {
        queueName: DATA_BINDING_XML_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(xml payload) {
            receivedXmlPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_XML_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(xmlService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedXmlPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerXmlPayloadRequestBinding() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;

    Service xmlRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_XML_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(xml payload) returns string {
            receivedXmlReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_XML_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(xmlRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedXmlReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerJsonPayloadBinding() returns error? {
    json message = personMap.toJson();

    Service jsonService =
    @ServiceConfig {
        queueName: DATA_BINDING_JSON_PAYLOAD_LISTENER_QUEUE
    }
    service object {
        remote function onMessage(json payload, Caller caller) {
            receivedJsonPayload = payload;
            log:printInfo("The message received: " + payload.toString());
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_JSON_PAYLOAD_LISTENER_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedJsonPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerJsonPayloadRequestBinding() returns error? {
    json message = personMap.toJson();

    Service jsonRequestService =
    @ServiceConfig {
        queueName: DATA_BINDING_JSON_PAYLOAD_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(json payload, Caller caller) returns string {
            receivedJsonReqPayload = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(message.toString(), DATA_BINDING_JSON_PAYLOAD_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(jsonRequestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedJsonReqPayload, message, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}

@test:Config {
    groups: ["rabbitmq"]
}
public function testListenerJsonPayloadMessageRequestBinding() returns error? {
    Service requestService =
    @ServiceConfig {
        queueName: DATA_BINDING_RANDOM_PAYLOAD_MESSAGE_LISTENER_REQUEST_QUEUE
    }
    service object {
        remote function onRequest(@Payload RandomPayload payload, Caller caller, PayloadMessage message) returns string {
            receivedRandomPayloadValue = payload;
            log:printInfo("The message received in onRequest: " + payload.toString());
            return "Hello Back!!";
        }
    };

    check produceMessage(payloadMsg.toString(), DATA_BINDING_RANDOM_PAYLOAD_MESSAGE_LISTENER_REQUEST_QUEUE, DATA_BINDING_REPLY_QUEUE);
    Listener channelListener = check new (DEFAULT_HOST, DEFAULT_PORT);
    check channelListener.attach(requestService);
    check channelListener.'start();
    runtime:sleep(2);
    test:assertEquals(receivedRandomPayloadValue, payloadMsg, msg = "Message received does not match.");
    check channelListener.gracefulStop();
}
