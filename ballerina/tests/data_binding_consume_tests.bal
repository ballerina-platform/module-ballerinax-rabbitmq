import ballerina/test;

@test:Config {}
function stringConsumeTest() returns error? {
    string message = "This is a data binding related message";
    check produceMessage(message.toString(), DATA_BINDING_STRING_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    StringMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_STRING_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function intConsumeTest() returns error? {
    int message = 445;
    check produceMessage(message.toString(), DATA_BINDING_INT_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    IntMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_INT_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function floatConsumeTest() returns error? {
    float message = 43.201;
    check produceMessage(message.toString(), DATA_BINDING_FLOAT_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    FloatMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_FLOAT_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function decimalConsumeTest() returns error? {
    decimal message = 59.382;
    check produceMessage(message.toString(), DATA_BINDING_DECIMAL_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    DecimalMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_DECIMAL_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function booleanConsumeTest() returns error? {
    boolean message = true;
    check produceMessage(message.toString(), DATA_BINDING_BOOLEAN_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    BooleanMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_BOOLEAN_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function recordConsumeTest() returns error? {
    check produceMessage(personRecord.toString(), DATA_BINDING_RECORD_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    RecordMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_RECORD_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, personRecord);
    check 'client->close();
}

@test:Config {}
function mapConsumeTest() returns error? {
    check produceMessage(personMap.toString(), DATA_BINDING_MAP_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    MapMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_MAP_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, personMap);
    check 'client->close();
}

@test:Config {}
function tableConsumeTest() returns error? {
    table<Person> message = table [];
    message.add(personRecord);
    check produceMessage(message.toString(), DATA_BINDING_TABLE_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    TableMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_TABLE_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function xmlConsumeTest() returns error? {
    xml message = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    check produceMessage(message.toString(), DATA_BINDING_XML_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    XmlMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_XML_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}

@test:Config {}
function jsonConsumeTest() returns error? {
    json message = personMap.toJson();
    check produceMessage(message.toString(), DATA_BINDING_JSON_CONSUME_QUEUE);
    Client 'client = check new(DEFAULT_HOST, DEFAULT_PORT);
    JsonMessage receivedMessage = check 'client->consumeMessage(DATA_BINDING_JSON_CONSUME_QUEUE);
    test:assertEquals(receivedMessage.content, message);
    check 'client->close();
}