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

@test:Config {}
function stringConsumeMessageTest() returns error? {
    string message = "This is a data binding related message";
    check produceMessage(message.toString(), DATA_BINDING_STRING_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    StringMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_STRING_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function intConsumeMessageTest() returns error? {
    int message = 445;
    check produceMessage(message.toString(), DATA_BINDING_INT_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    IntMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_INT_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function floatConsumeMessageTest() returns error? {
    float message = 43.201;
    check produceMessage(message.toString(), DATA_BINDING_FLOAT_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    FloatMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_FLOAT_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function decimalConsumeMessageTest() returns error? {
    decimal message = 59.382;
    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    DecimalMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_DECIMAL_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function booleanConsumeMessageTest() returns error? {
    boolean message = true;
    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    BooleanMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_BOOLEAN_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function recordConsumeMessageTest() returns error? {
    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    RecordMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_RECORD_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, personRecord);
    check 'client->close();
}

@test:Config {}
function mapConsumeMessageTest() returns error? {
    check produceMessage(personMap.toString(), DATA_BINDING_MAP_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    MapMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_MAP_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, personMap);
    check 'client->close();
}

@test:Config {}
function tableConsumeMessageTest() returns error? {
    table<Person> message = table [];
    message.add(personRecord);
    check produceMessage(message.toString(), DATA_BINDING_TABLE_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    TableMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_TABLE_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function xmlConsumeMessageTest() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    check produceMessage(message.toString(), DATA_BINDING_XML_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    XmlMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_XML_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function jsonConsumeMessageTest() returns error? {
    json message = personMap.toJson();
    check produceMessage(message.toString(), DATA_BINDING_JSON_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    JsonMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_JSON_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function bytesConsumeMessageTest() returns error? {
    string message = "Test message";
    check produceMessage(message.toString(), DATA_BINDING_BYTES_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_BYTES_CONSUME_QUEUE);
    test:assertEquals(string:fromBytes(receivedMessage.content), message);
    check 'client->close();
}

@test:Config {}
function anydataConsumeMessageTest() returns error? {
    string message = "Test message";
    check produceMessage(message.toString(), DATA_BINDING_ANYDATA_CONSUME_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    AnydataMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_ANYDATA_CONSUME_QUEUE);
    if receivedMessage.content is byte[] {
        test:assertEquals(string:fromBytes(<byte[]>receivedMessage.content), message);
    } else {
        test:assertFail("Expected a byte array");
    }
    check 'client->close();
}

@test:Config {}
function dataBindingConsumeMessageErrorTest() returns error? {
    json message = personMap.toJson();
    check produceMessage(message.toString(), DATA_BINDING_ERROR_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    IntMessage|Error result = 'client->consumeMessage(DATA_BINDING_ERROR_QUEUE);
    if result is PayloadBindingError {
        test:assertTrue(result.message().startsWith("Data binding failed:"));
    } else {
        test:assertFail("Expected an error");
    }
    check 'client->close();
}

@test:Config {}
function stringConsumePayloadTest() returns error? {
    string message = "This is a data binding related message";
    check produceMessage(message.toString(), DATA_BINDING_STRING_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    string receivedPayload = check 'client->consumePayload(DATA_BINDING_STRING_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function intConsumePayloadTest() returns error? {
    int message = 451;
    check produceMessage(message.toString(), DATA_BINDING_INT_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    int receivedPayload = check 'client->consumePayload(DATA_BINDING_INT_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function floatConsumePayloadTest() returns error? {
    float message = 43.201;
    check produceMessage(message.toString(), DATA_BINDING_FLOAT_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    float receivedPayload = check 'client->consumePayload(DATA_BINDING_FLOAT_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function decimalConsumePayloadTest() returns error? {
    decimal message = 59.382;
    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    decimal receivedPayload = check 'client->consumePayload(DATA_BINDING_DECIMAL_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function booleanConsumePayloadTest() returns error? {
    boolean message = true;
    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    boolean receivedPayload = check 'client->consumePayload(DATA_BINDING_BOOLEAN_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function recordConsumePayloadTest() returns error? {
    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    Person receivedPayload = check 'client->consumePayload(DATA_BINDING_RECORD_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, personRecord);
    check 'client->close();
}

@test:Config {}
function mapConsumePayloadTest() returns error? {
    check produceMessage(personMap.toString(), DATA_BINDING_MAP_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    map<Person> receivedPayload = check 'client->consumePayload(DATA_BINDING_MAP_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, personMap);
    check 'client->close();
}

@test:Config {}
function tableConsumePayloadTest() returns error? {
    table<Person> message = table [];
    message.add(personRecord);
    check produceMessage(message.toString(), DATA_BINDING_TABLE_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    table<Person> receivedPayload = check 'client->consumePayload(DATA_BINDING_TABLE_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function xmlConsumePayloadTest() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    check produceMessage(message.toString(), DATA_BINDING_XML_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    xml receivedPayload = check 'client->consumePayload(DATA_BINDING_XML_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function jsonConsumePayloadTest() returns error? {
    json message = personMap.toJson();
    check produceMessage(message.toString(), DATA_BINDING_JSON_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    json receivedPayload = check 'client->consumePayload(DATA_BINDING_JSON_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function unionConsumePayloadTest() returns error? {
    string message = "Hello";
    check produceMessage(message.toString(), DATA_BINDING_UNION_PAYLOAD_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    int|string receivedPayload = check 'client->consumePayload(DATA_BINDING_UNION_PAYLOAD_QUEUE);
    test:assertEquals(receivedPayload, message);
    check 'client->close();
}

@test:Config {}
function consumePayloadErrorTest() returns error? {
    json message = personMap.toJson();
    check produceMessage(message.toString(), DATA_BINDING_ERROR_QUEUE);
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    int|Error result = 'client->consumePayload(DATA_BINDING_ERROR_QUEUE);
    if result is PayloadBindingError {
        test:assertTrue(result.message().startsWith("Data binding failed:"));
    } else {
        test:assertFail("Expected an error");
    }
    check 'client->close();
}
