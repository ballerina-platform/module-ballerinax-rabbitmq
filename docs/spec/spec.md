# Specification: Ballerina RabbitMQ Library

_Owners_: @aashikam @shafreenAnfar  
_Reviewers_: @shafreenAnfar  
_Created_: 2020/10/28
_Updated_: 2021/11/29  
_Issue_: [#2223](https://github.com/ballerina-platform/ballerina-standard-library/issues/2223)

# Introduction
This is the specification for RabbitMQ standard library which is used to send and receive messages by connecting to the RabbitMQ server.
This library is programmed in the [Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the cloud
that makes it easier to use, combine, and create network services.

# Contents

1. [Overview](#1-overview)
2. [Connection](#2-connection)
3. [Exchanges and Queues](#3-exchanges-and-queues)
4. [Deleting and Purging](#4-deleting-and-purging)
5. [Publishing](#5-publishing)
6. [Subscribing](#6-subscribing)
7. [Retrieving Individual Messages](#7-retrieving-individual-messages)
8. [Client Acknowledgements](#8-client-acknowledgements)
9. [Samples](#9-samples)
    * 9.1. [Publish-Subscribe](#91-publish-subscribe)
    * 9.2. [Request-Reply](#92-request-reply)

## 1. Overview

This specification elaborates on the usage of RabbitMQ library client and services/listener. RabbitMQ is lightweight and easy to deploy on premises and in the cloud.
The client API exposes key entities in the AMQP 0-9-1 protocol model, with additional abstractions for ease of use. Protocol operations are available through the `rabbitmq:Client` client object. 

## 2. Connection
Connections with the RabbitMQ server can be established through the RabbitMQ library client and the listener. There are multiple ways to connect.

- `rabbitmq:Client`: Interface to an AMQ connection and other protocol operations. 
```ballerina
    # Initializes a `rabbitmq:Client` object.
    #
    # + host - The host used for establishing the connection
    # + port - The port used for establishing the connection
    # + connectionData - The connection configurations
    public isolated function init(string host, int port, *ConnectionConfiguration connectionData) returns Error?;
```

- `rabbitmq:Listener`: Represents a single network connection. A subscription service should be bound to a listener in order to receive messages.
```ballerina
    # Initializes a Listener object with the given connection configuration. Sets the global QoS settings,
    # which will be applied to the entire `rabbitmq:Listener`.
    #
    # + host - The host used for establishing the connection
    # + port - The port used for establishing the connection
    # + qosSettings - The consumer prefetch settings
    # + connectionData - The connection configuration
    public isolated function init(string host, int port, QosSettings? qosSettings = (),
                                    *ConnectionConfiguration connectionData) returns Error?;
```

**Configurations available for initializing the RabbitMQ client and listener**

- Connection related configurations:
```ballerina
   public type ConnectionConfiguration record {|
      # The username used for establishing the connection.
      string username?;
      # The password used for establishing the connection.
      string password?;
      # Connection TCP establishment timeout in seconds and zero for infinite.
      decimal connectionTimeout?;
      # The AMQP 0-9-1 protocol handshake timeout in seconds.
      decimal handshakeTimeout?;
      # Shutdown timeout in seconds, zero for infinite, and the default value is 10. If the consumers exceed
      # this timeout, then any remaining queued deliveries (and other Consumer callbacks) will be lost.
      decimal shutdownTimeout?;
      # The initially-requested heartbeat timeout in seconds and zero for none.
      decimal heartbeat?;
      # Configurations for facilitating secure connections. 
      SecureSocket secureSocket?;
      # Configurations releated to authentication.
      Credentials auth?;
   |};
```

- Configurations for facilitating secure connections:
```ballerina
   public type SecureSocket record {|
      # Configurations associated with `crypto:TrustStore` or single certificate file that the client trusts.
      crypto:TrustStore|string cert;
      # Configurations associated with `crypto:KeyStore` or combination of certificate and private key of the client.
      crypto:KeyStore|CertKey key?;
      # SSL/TLS protocol related options. 
      record {|
        Protocol name;
      |} protocol?;
      # Enable/disable host name verification.
      boolean verifyHostName = true;
   |};
```

- Combination of certificate and private key of the client:
```ballerina
   public type CertKey record {|
      # A file containing the certificate.
      string certFile;
      # A file containing the private key in PKCS8 format.
      string keyFile;
      # Password of the private key if it is encrypted.
      string keyPassword?;
   |};
```

- SSL/TLS protocol related options:
```ballerina
   public enum Protocol {
      SSL,
      TLS,
      DTLS
   }
```

1. Connect to a RabbitMQ node with the default host and port.
```ballerina
   // Connecting using the RabbitMQ client.
   rabbitmq:Client rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   // Connecting using the RabbitMQ listener.
   rabbitmq:Listener rabbitMQListener = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
```

2. Connect to a RabbitMQ node with a custom host and port.
```ballerina
   // Connecting using the RabbitMQ client.
   rabbitmq:Client rabbitmqClient = check new("localhost", 5672);
   
   // Connecting using the RabbitMQ listener.
   rabbitmq:Listener rabbitMQListener = check new("localhost", 5672);
```

3. Connect to a RabbitMQ node with host, port, and additional configurations.
```ballerina
   rabbitmq:ConnectionConfiguration config = {
      username: "ballerina",
      password: "password"
   };
   
   // Connecting using the RabbitMQ client.
   rabbitmq:Client rabbitmqClient = check new("localhost", 5672, configs);
   
   // Connecting using the RabbitMQ listener.
   rabbitmq:Listener rabbitMQListener = check new("localhost", 5672, configs);
```

4. Secured connections.

Connections can be secured using following approaches. All the given approaches are supported by both the client and the listener.

```ballerina
   // Connect using username/password credentials. 
   rabbitmq:Client rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, 5672,
      auth = {
          username: "alice",
          password: "alice@123"
      }
   );
   
   // Connect with SSL/TLS enabled. 5671 is the default port in the server for connections that use TLS. 
   rabbitmq:SecureSocket secured = {
      cert: "../resource/path/to/public.crt"
   };
   rabbitmq:Listener rabbitMQListener =check new(rabbitmq:DEFAULT_HOST, 5671, secureSocket = secured);
```

## 3. Exchanges and Queues

Client applications work with exchanges and queues, the high-level building blocks of the protocol. These must be declared before they can be used. Declaring either type of object simply ensures that one of that name exists, creating it if necessary. For more details on RabbitMQ concepts and exchange types see [here](https://www.rabbitmq.com/tutorials/amqp-concepts.html).

- Types of exchanges supported by the Ballerina RabbitMQ Connector:
```ballerina
   public type ExchangeType "direct"|"fanout"|"topic"|"headers";
   
   # Constant for the RabbitMQ Direct Exchange type.
   public const DIRECT_EXCHANGE = "direct";
   
   # Constant for the RabbitMQ Fan-out Exchange type.
   public const FANOUT_EXCHANGE = "fanout";
   
   # Constant for the RabbitMQ Topic Exchange type.
   public const TOPIC_EXCHANGE = "topic";
```

- Configurations related to declaring an exchange:
```ballerina
   public type ExchangeConfig record {|
      # Set to `true` if a durable exchange is declared.
      boolean durable = false;
      # Set to `true` if an autodelete exchange is declared.
      boolean autoDelete = false;
      # Other properties (construction arguments) for the queue.
      map<anydata> arguments?;
   |};
```

- Configurations related to declaring an queue:
```ballerina
   public type QueueConfig record {|
      # Set to true if declaring a durable queue.
      boolean durable = false;
      # Set to true if declaring an exclusive queue.
      boolean exclusive = false;
      # Set to true if declaring an auto-delete queue. 
      boolean autoDelete = true;
      # Other properties (construction arguments) of the queue. 
      map<anydata> arguments?;
   |};
```

Following methods can be used to declare the exchanges, queues and to bind them.

- `exchangeDeclare`
```ballerina
   # Declares a non-auto-delete, non-durable exchange with no extra arguments.
   # If the arguments are specified, then the exchange is declared accordingly.
   #
   # + name - The name of the exchange
   # + exchangeType - The type of the exchange
   # + config - The configurations required to declare an exchange
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function exchangeDeclare(string name,
         ExchangeType exchangeType = DIRECT_EXCHANGE, ExchangeConfig? config = ()) returns Error?;
```

- `queueDeclare`
```ballerina
   # Declares a non-exclusive, auto-delete, or non-durable queue with the given configurations.
   #
   # + name - The name of the queue
   # + config - The configurations required to declare a queue
   # + return - `()` if the queue was successfully generated or else a `rabbitmq:Error`
   #               if an I/O error occurred
   isolated remote function queueDeclare(string name, QueueConfig? config = ()) returns Error?;
```

- `queueAutoGenerate`
```ballerina
   # Declares a queue with a server-generated name.
   #
   # + return - The name of the queue or else a `rabbitmq:Error`
   #             if an I/O error occurred
   isolated remote function queueAutoGenerate() returns string|Error;
```

- `queueBind`
```ballerina
   # Binds a queue to an exchange with the given binding key.
   #
   # + queueName - The name of the queue
   # + exchangeName - The name of the exchange
   # + bindingKey - The binding key used to bind the queue to the exchange
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function queueBind(string queueName, string exchangeName, string bindingKey) returns Error?;
```

- Usage:

```ballerina
   check rabbitmqClient->exchangeDeclare("MyExchange", rabbitmq:DIRECT_EXCHANGE);
   check rabbitmqClient->queueDeclare("MyQueue");
   check rabbitmqClient->queueBind("MyQueue", "MyExchange", "routing-key");
```

This code will declare,
- a durable auto-delete exchange of the type `rabbitmq:DIRECT_EXCHANGE`.
- a non-durable, exclusive auto-delete queue.

```ballerina
   check rabbitmqClient->exchangeDeclare("MyExchange", rabbitmq:TOPIC_EXCHANGE);
   check rabbitmqClient->queueDeclare("MyQueue", { durable: true,
                                                exclusive: false,
                                                autoDelete: false });
   check rabbitmqClient->queueBind("MyQueue", "MyExchange", "routing-key");
```

This sample code will declare,
- a durable auto-delete exchange of the type `rabbitmq:TOPIC_EXCHANGE`.
- a durable, non-exclusive, non-auto-delete queue.

The `queueBind` function is called to bind the queue to the exchange with the given routing key. See the API docs for the complete list of supported configurations.

## 4. Deleting and Purging

A queue or an exchange can be explicitly deleted or purged using following methods. 

- `queueDelete`:
```ballerina
   # Deletes the queue with the given name although it is in use or has messages in it.
   # If the `ifUnused` or `ifEmpty` parameters are given, the queue is checked before deleting.
   #
   # + queueName - The name of the queue to be deleted
   # + ifUnused - True if the queue should be deleted only if it's not in use
   # + ifEmpty - True if the queue should be deleted only if it's empty
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function queueDelete(string queueName, boolean ifUnused = false, boolean ifEmpty = false)
                     returns Error?;
```
- `exchangeDelete`: 
```ballerina
   # Deletes the exchange with the given name.
   #
   # + exchangeName - The name of the exchange
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function exchangeDelete(string exchangeName) returns Error?;
```

- `queuePurge`:
```ballerina
   # Purges the content of the given queue.
   #
   # + queueName - The name of the queue
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function queuePurge(string queueName) returns Error?;
```

- Usage
```ballerina
   // Delete a queue only if it is empty.
   check rabbitmqClient->queueDelete("MyQueue", false, true);
   
   // Delete a queue only if it is unused (does not have any consumers).
   check rabbitmqClient->queueDelete("MyQueue", true, false);
   
   // Delete an exchange.
   check rabbitmqClient->exchangeDelete("MyExchange");
   
   // Purge a queue (delete all of its messages).
   check rabbitmqClient->queuePurge("MyQueue");
```

## 5. Publishing

- `publishMessage`:
```ballerina
   # Publishes a message. Publishing to a non-existent exchange will result in a channel-level
   # protocol error, which closes the channel.
   #
   # + message - The message to be published
   # + return - A `rabbitmq:Error` if an I/O error occurred or else `()`
   isolated remote function publishMessage(Message message) returns Error?;
```

- Configurations related to publishing:
```ballerina
   public type BasicProperties record {|
      # The queue name to which the reply should be sent.
      string replyTo?;
      # The content type of the message.
      string contentType?;
      # The content encoding of the message. 
      string contentEncoding?;
      # The client-specific ID that can be used to mark or identify messages between clients.
      string correlationId?;
   |};
```

To publish a message to an exchange, use the `publishMessage()` function as follows:

```ballerina
   string message = "Hello from Ballerina";
   check rabbitmqClient->publishMessage({ content: message.toBytes(), routingKey: queueName });
``` 
Setting other properties of the message such as routing headers can be done by using the `BasicProperties` record with the appropriate values.

```ballerina
   rabbitmq:BasicProperties props = {
    replyTo: "reply-queue"  
   };
   string message = "Hello from Ballerina";
   check rabbitmqClient->publishMessage({ content: message.toBytes(), routingKey: queueName, properties: props });
```

## 6. Subscribing

The most efficient way to receive messages is to set up a subscription using a Ballerina RabbitMQ `rabbitmq:Listener` and any number of consumer services. The messages will then be delivered automatically as they arrive rather than having to be explicitly requested. Multiple consumer services can be bound to one Ballerina RabbitMQ `rabbitmq:Listener`. The queue to which the service is listening is configured in the `rabbitmq:ServiceConfig` annotation of the service or else as the name of the service.

- Attach the service to the listener directly

1. Listen to incoming messages with the `onMessage` remote method:

```ballerina
   listener rabbitmq:Listener channelListener= new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   @rabbitmq:ServiceConfig {
      queueName: "MyQueue"
   }
   service rabbitmq:Service on channelListener {
      remote function onMessage(rabbitmq:Message message) {
      }
   }
```

2. Listen to incoming messages and reply directly with the `onRequest` remote method:

```ballerina
   listener rabbitmq:Listener channelListener= new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   @rabbitmq:ServiceConfig {
      queueName: "MyQueue"
   }
   service rabbitmq:Service on channelListener {
      remote function onRequest(rabbitmq:Message message) returns string {
         return "Hello Back!";
      }
   }
```

- Attach the service dynamically.
```ballerina
   // Create a service object 
   rabbitmq:Service listenerService =
   @rabbitmq:ServiceConfig {
      queueName: "MyQueue"
   }
   service object {
      remote function onRequest(rabbitmq:Message message) returns string {
         return "Hello Back!";
      }
   };
```

The `rabbitmq:Message` record received can be used to retrieve its contents.
```ballerina
   public type Message record {|
      # The content of the message.
      byte[] content;
      # The routing key to which the message is sent . 
      string routingKey;
      # The exchange to which the message is sent. The default exchange is a direct exchange with no name (empty string) pre-declared by the broker.
      string exchange = "";
      # The delivery tag of the message. 
      int deliveryTag?;
      # Basic properties of the message - routing headers etc. 
      BasicProperties properties?;
   |};
```

**The Listener has the following functions to manage a service:**
* `attach()` - can be used to attach a service to the listener dynamically.
```ballerina
   # Attaches the service to the `rabbitmq:Listener` endpoint.
   #
   # + s - The type descriptor of the service
   # + name - The name of the service
   # + return - `()` or else a `rabbitmq:Error` upon failure to register the service
   public isolated function attach(Service s, string[]|string? name = ()) returns error?;
```

* `detach()` - can be used to detach a service from the listener.
```ballerina
   # Stops consuming messages and detaches the service from the `rabbitmq:Listener` endpoint.
   #
   # + s - The type descriptor of the service
   # + return - `()` or else  a `rabbitmq:Error` upon failure to detach the service
   public isolated function detach(Service s) returns error?;
```

* `start()` - needs to be called to start the listener.
```ballerina
   # Starts consuming the messages on all the attached services.
   #
   # + return - `()` or else a `rabbitmq:Error` upon failure to start
   public isolated function 'start() returns error?;
```

* `gracefulStop()` - can be used to gracefully stop the listener from consuming messages.
```ballerina
   # Stops consuming messages through all consumer services by terminating the connection and all its channels.
   #
   # + return - `()` or else  a `rabbitmq:Error` upon failure to close the `ChannelListener`
   public isolated function gracefulStop() returns error?;
```

* `immediateStop()` - can be used to immediately stop the listener from consuming messages.
```ballerina
   # Stops consuming messages through all the consumer services and terminates the connection
   # with the server.
   #
   # + return - `()` or else  a `rabbitmq:Error` upon failure to close ChannelListener.
   public isolated function immediateStop() returns error?;
```

## 7. Retrieving Individual Messages 

It is also possible to retrieve individual messages on demand ("pull API" a.k.a. polling). This approach to consumption is highly inefficient as it is effectively polling and applications repeatedly have to ask for results even if the vast majority of the requests yield no results. To "pull" a message, use the `consumeMessage` function.

- `consumeMessage`:
```ballerina
   # Retrieves a message synchronously from the given queue providing direct access to the messages in the queue.
   #
   # + queueName - The name of the queue
   # + autoAck - If false, should manually acknowledge
   # + return - A `rabbitmq:Message` object containing the retrieved message data or else a`rabbitmq:Error` if an
   #            I/O error occurred
   isolated remote function consumeMessage(string queueName, boolean autoAck = true)
     returns Message|Error;
```

- Usage:

```ballerina
   // Pulls a single message from MyQueue. 
   rabbitmq:Message message = check rabbitmqClient->consumeMessage("MyQueue");
   
   // Pulls a message with auto acknowledgements turned off. 
   rabbitmq:Message message = check rabbitmqClient->consumeMessage("MyQueue", false);
```

## 8. Client Acknowledgements

The message consuming is supported by mainly two types of acknowledgement modes, which are auto acknowledgements and client acknowledgements.
Client acknowledgements can further be divided into two different types as positive and negative acknowledgements.
The default acknowledgement mode is auto-ack (messages are acknowledged immediately after consuming). The following examples show the usage of positive and negative acknowledgements.
> WARNING: To ensure the reliability of receiving messages, use the client-ack mode.

- `basicAck`:
```ballerina
   # Acknowledges one or several received messages.
   #
   # + multiple - Set to `true` to acknowledge all messages up to and including the called on message and
   #              `false` to acknowledge just the called on message
   # + return - A `rabbitmq:Error` if an I/O error occurred
   isolated remote function basicAck(boolean multiple = false) returns Error?;
```

- `basicNack`:
```ballerina
   # Rejects one or several received messages.
   #
   # + multiple - Set to `true` to reject all messages up to and including the called on message and
   #              `false` to reject just the called on message
   # + requeue - `true` if the rejected message(s) should be re-queued rather than discarded/dead-lettered
   # + return - A `rabbitmq:Error` if an I/O error is encountered or else `()`
   isolated remote function basicNack(boolean multiple = false, boolean requeue = true) returns Error?;
```

1. Positive client acknowledgement:
```ballerina
   listener rabbitmq:Listener channelListener= new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   @rabbitmq:ServiceConfig {
      queueName: "MyQueue",
      autoAck: false
   }
   service rabbitmq:Service on channelListener {
      remote function onMessage(rabbitmq:Message message, rabbitmq:Caller caller) {
         rabbitmq:Error? result = caller->basicAck();
      }
   }
```

2. Negative client acknowledgement:
```ballerina
   listener rabbitmq:Listener channelListener= new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   @rabbitmq:ServiceConfig {
      queueName: "MyQueue",
      autoAck: false
   }
   service rabbitmq:Service on channelListener {
      remote function onMessage(rabbitmq:Message message) {
         rabbitmq:Error? result = caller->basicNack(true, requeue = false);
      }
   }
```

The negatively-acknowledged (rejected) messages can be re-queued by setting the `requeue` to `true`.

## 9. Samples

### 9.1. Publish-Subscribe
* Publisher
```ballerina
   import ballerinax/rabbitmq;
   
   public function main() returns error? {
       rabbitmq:Client newClient =
                   check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
       check newClient->queueDeclare("MyQueue");
       string message = "Hello from Ballerina";
       check newClient->publishMessage({ content: message.toBytes(), routingKey: "MyQueue" });
   }
```
* Subscriber
```ballerina
   import ballerina/log;
   import ballerinax/rabbitmq;
   
   listener rabbitmq:Listener channelListener =
           new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);

   @rabbitmq:ServiceConfig {
       queueName: "MyQueue"
   }
   service rabbitmq:Service on channelListener {
       remote function onMessage(rabbitmq:Message message) {
           string|error messageContent = string:fromBytes(message.content);
           if messageContent is string {
               log:printInfo("Received message: " + messageContent);
           }
       }
   }
```

### 9.2. Request-Reply
* Publisher
```ballerina
   import ballerinax/rabbitmq;
   
   public function main() returns error? {
      rabbitmq:Client newClient =
             check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
      check newClient->queueDeclare("MyQueue");
      
      string message = "Hello from Ballerina";
      rabbitmq:BasicProperties props = {
         replyTo: "reply-queue"  
      };
      check newClient->publishMessage({ content: message.toBytes(), routingKey: queueName, 
                  properties: props });
   }
```

* Subscriber
```ballerina
   import ballerina/log;
   import ballerinax/rabbitmq;
   
   listener rabbitmq:Listener channelListener =
           new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
   
   @rabbitmq:ServiceConfig {
       queueName: "MyQueue"
   }
   service rabbitmq:Service on channelListener {
       remote function onRequest(rabbitmq:Message message) returns string {
           string|error messageContent = string:fromBytes(message.content);
           if messageContent is string {
               log:printInfo("Received message: " + messageContent);
           } 
           return "Hello back from ballerina!";
       }
   }
```
