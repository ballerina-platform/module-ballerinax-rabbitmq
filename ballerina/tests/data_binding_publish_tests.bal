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

import ballerina/lang.value;
import ballerina/test;

@test:Config {}
function stringPublishTest() returns error? {
    string content = "This is a data binding related message";
    StringMessage message = {
        content,
        routingKey: DATA_BINDING_STRING_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_STRING_PUBLISH_QUEUE);
    string receivedContent = check string:fromBytes(receivedMessage.content);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function intPublishTest() returns error? {
    int content = 550;
    IntMessage message = {
        content,
        routingKey: DATA_BINDING_INT_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_INT_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    int receivedContent = check int:fromString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function floatPublishTest() returns error? {
    float content = 43.201;
    FloatMessage message = {
        content,
        routingKey: DATA_BINDING_FLOAT_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_FLOAT_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    float receivedContent = check float:fromString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function decimalPublishTest() returns error? {
    decimal content = 59.382;
    DecimalMessage message = {
        content,
        routingKey: DATA_BINDING_DECIMAL_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_DECIMAL_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    decimal receivedContent = check decimal:fromString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function booleanPublishTest() returns error? {
    boolean content = true;
    BooleanMessage message = {
        content,
        routingKey: DATA_BINDING_BOOLEAN_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_BOOLEAN_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    boolean receivedContent = check boolean:fromString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function recordPublishTest() returns error? {
    RecordMessage message = {
        content: personRecord,
        routingKey: DATA_BINDING_RECORD_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_RECORD_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    Person receivedContent = check value:fromJsonStringWithType(receivedString);
    test:assertEquals(receivedContent, personRecord);
    check 'client->close();
}

@test:Config {}
function mapPublishTest() returns error? {
    MapMessage message = {
        content: personMap,
        routingKey: DATA_BINDING_MAP_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_MAP_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    map<Person> receivedContent = check value:fromJsonStringWithType(receivedString);
    test:assertEquals(receivedContent, personMap);
    check 'client->close();
}

@test:Config {}
function tablePublishTest() returns error? {
    table<Person> content = table [];
    content.add(personRecord);

    TableMessage message = {
        content,
        routingKey: DATA_BINDING_TABLE_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_TABLE_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    table<Person> receivedContent = check value:fromJsonStringWithType(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function xmlPublishTest() returns error? {
    xml content = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    XmlMessage message = {
        content,
        routingKey: DATA_BINDING_XML_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_XML_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    xml receivedContent = check xml:fromString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function jsonPublishTest() returns error? {
    json content = personMap.toJson();

    JsonMessage message = {
        content,
        routingKey: DATA_BINDING_JSON_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_JSON_PUBLISH_QUEUE);
    string receivedString = check string:fromBytes(receivedMessage.content);
    json receivedContent = check value:fromJsonString(receivedString);
    test:assertEquals(receivedContent, content);
    check 'client->close();
}

@test:Config {}
function bytesPublishTest() returns error? {
    byte[] content = "Test message".toBytes();

    BytesMessage message = {
        content,
        routingKey: DATA_BINDING_BYTES_PUBLISH_QUEUE
    };
    Client 'client = check new (DEFAULT_HOST, DEFAULT_PORT);
    check 'client->publishMessage(message);
    BytesMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_BYTES_PUBLISH_QUEUE);
    test:assertEquals(receivedMessage.content, content);
    check 'client->close();
}
