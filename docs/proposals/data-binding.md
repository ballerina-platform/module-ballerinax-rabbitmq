# Proposal: Data binding support for RabbitMQ

_Owners_: @shafreenAnfar @aashikam @dilanSachi     
_Reviewers_: @shafreenAnfar @aashikam  
_Created_: 2022/05/12  
_Issues_: [#2818](https://github.com/ballerina-platform/ballerina-standard-library/issues/2818) [#2812](https://github.com/ballerina-platform/ballerina-standard-library/issues/2812) [#2880](https://github.com/ballerina-platform/ballerina-standard-library/issues/2880)

## Summary

Data binding helps to access the incoming and outgoing message data in the user's desired type. Similar to the Ballerina HTTP package, subtypes of `anydata` will be the supported types. This proposal discusses ways to provide data binding on the RabbitMQ client and consumer service side.

## Goals

- Improve user experience by adding data binding support for sending messages with `rabbitmq:Client` and receiving messages with `rabbitmq:Service`.

## Motivation

As of now, the Ballerina `rabbitmq` package does not provide direct data binding for sending and receiving messages. Only `rabbitmq:AnydataMessage` is the supported data type to send and receive messages which only support `byte[]` as the message content type. Therefore, users have to do data manipulations by themselves. With this new feature, the user experience can be improved by introducing data binding to reduce the burden of developers converting byte data to the desired format as discussed in the next section.

## Description

Currently, when sending a message using `rabbitmq:Client`, user has to convert the message to byte[].

```ballerina
type Person record {|
    string name;
    int age;
|};

Person person = {
    age: 10,
    name: "Harry"
};

// Publish a message 
check rabbitmqClient->publishMessage({ content: person.toString().toBytes(), routingKey: "MyQueue" });
```

When receiving the same message,

```ballerina
service "MyQueue" on rabbitmqListener {
    remote function onMessage(rabbitmq:Message message) returns rabbitmq:Error? {
        string messageContent = check string:fromBytes(message.content);
        Person person = check value:fromJsonStringWithType(messageContent);
    }
}
```

Receiving the message as a request,
```ballerina
service "MyQueue" on rabbitmqListener {
    remote function onRequest(rabbitmq:Message message) returns string|rabbitmq:Error? {
        string messageContent = check string:fromBytes(message.content);
        Person person = check value:fromJsonStringWithType(messageContent);
        return "New person received";
    }
}
```

Instead of this, if data binding support is introduced, user can easily send and receive the messages in the desired format.
For this purpose, we will introduce a new record for sending and receiving.
```ballerina
public type AnydataMessage record {|
    // The content of the message
    anydata content;
    // The routing key to which the message is sent
    string routingKey;
    // The exchange to which the message is sent. The default exchange is a direct exchange with no name (empty string) pre-declared by the broker.
    string exchange = "";
    // The delivery tag of the message
    int deliveryTag?;
    // Basic properties of the message - routing headers etc.
    BasicProperties properties?;
|};
```
With these, user can create user-defined subtypes of the above record to achieve data binding as shown below.
```ballerina
public type PersonMessage record {|
    *rabbitmq:AnydataMessage;
    Person content;
|};
// publish message
check rabbitmqClient->publishMessage(personMessage);

// consume message
PersonMessage receivedMessage = check rabbitmqClient->consumeMessage(RABBITMQ_QUEUE);
```

### Consuming messages with rabbitmq:Service

`rabbitmq:Listener` connects to the RabbitMQ server and allows the `rabbitmq:Service` to subscribe to a given subject to consume messages. The messages are received by the `onMessage` remote method, and the requests are received by the `onRequest` remote method in `rabbitmq:Service`.

```ballerina
remote function onMessage(rabbitmq:Message message) returns rabbitmq:Error? {}

remote function onRequest(rabbitmq:Message message) returns anydata|rabbitmq:Error? {}
```

This will be updated to accept the above-mentioned parameter types. User can create a subtype of `rabbitmq:AnydataMessage` and use it in the function signature or directly use a subtype of `anydata` to get the payload binded directly.
Therefore, following scenarios will be available for the user.

- **onMessage remote function**
```ballerina
remote function onMessage(anydata data) returns rabbitmq:Error? {}
```
```ballerina
remote function onMessage(rabbitmq:AnydataMessage message, anydata data) returns rabbitmq:Error? {}
```
```ballerina
remote function onMessage(rabbitmq:AnydataMessage message, rabbitmq:Caller caller, anydata data) returns rabbitmq:Error? {}
```
- **onRequest remote function**
```ballerina
remote function onRequest(anydata data) returns anydata|rabbitmq:Error? {}
```
```ballerina
remote function onRequest(rabbitmq:AnydataMessage message, anydata data) returns anydata|rabbitmq:Error? {}
```
```ballerina
remote function onRequest(rabbitmq:AnydataMessage message, rabbitmq:Caller caller, anydata data) returns anydata|rabbitmq:Error? {}
```
### Consuming messages with rabbitmq:Client

To consume messages, the `rabbitmq:Client` has `consumeMessage` API which returns a `rabbitmq:Message`.
```ballerina
isolated remote function consumeMessage(string queueName, boolean autoAck = true) returns rabbitmq:Message|Error;
```
This will be updated as follows.
```ballerina
# Retrieves a message synchronously from the given queue providing direct access to the messages in the queue.
#
# + queueName - The name of the queue
# + autoAck - If false, should manually acknowledge
# + T - Optional type description of the required data type
# + return - A `rabbitmq:Message` object containing the retrieved message data or else a`rabbitmq:Error` if an
#            I/O error occurred
isolated remote function consumeMessage(string queueName, boolean autoAck = true, typedesc<AnydataMessage> T = <>) returns T|Error;
```
For the cases where the user wants none of the `rabbitmq:AnydataMessage` information, `consumePayload()` API will be introduced.
```ballerina
isolated remote function consumePayload(string queueName, boolean autoAck = true, typedesc<anydata> T = <>) returns T|Error;
```
With this, user can do the following.
```ballerina
Person person = check rabbitmqClient->consumePayload("MyQueue");
```
### Producing messages with rabbitmq:Client

The `rabbitmq:Client` has `publishMessage(rabbitmq:Message message)` API to send data to the RabbitMQ server.
```ballerina
isolated remote function publishMessage(AnydataMessage message) returns Error?;
```
This will be updated as,
```ballerina
# Publishes a message. Publishing to a non-existent exchange will result in a channel-level protocol error, which closes the channel.
#
# + message - The message to be published
# + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
isolated remote function publishMessage(AnydataMessage message) returns Error?;
```
Whatever the data type given as the value will be converted to a byte[] internally and sent to the RabbitMQ server. If the data binding fails, a `rabbitmq:Error` will be returned from the API.

> With this new data binding improvement, the compiler plugin validation for `onMessage` and `onRequest` remote functions will also be updated to allow types other than `rabbitmq:Message`.

## Testing

- Testing the runtime data type conversions on `rabbitmq:Client` and `rabbitmq:Service`.
- Testing compiler plugin validation to accept new data types.
