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
6. [Listening for Messages](#6-listening-for-messages)
7. [Retrieving Individual Messages ](#7-retrieving-individual-messages )
8. [Client Acknowledgements](#8-client-acknowledgements)
9. [Samples](#9-samples)
    * 9.1. [Publish-Subscribe](#91-publish-subscribe)
    * 9.2. [Request-Reply](#92-request-reply)

## 1. Overview

This specification elaborates on the usage of RabbitMQ library client and services/listener. RabbitMQ is lightweight and easy to deploy on premises and in the cloud.
The client API exposes key entities in the AMQP 0-9-1 protocol model, with additional abstractions for ease of use. Protocol operations are available through the `rabbitmq:Client` client object. 

## 2. Connection
Connections with the RabbitMQ server can be established through the RabbitMQ library client and the listener. There are multiple ways to connect.

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

A queue or an exchange can be explicitly deleted using following ways. 

1. Delete a queue.
```ballerina
   check rabbitmqClient->queueDelete("MyQueue");
```
2. Delete a queue only if it is empty.
```ballerina
   check rabbitmqClient->queueDelete("MyQueue", false, true);
```
3. Delete a queue only if it is unused (does not have any consumers).
```ballerina
   check rabbitmqClient->queueDelete("MyQueue", true, false);
```
4. Delete an exchange.
```ballerina
   check rabbitmqClient->exchangeDelete("MyExchange");
```
5. Purge a queue (delete all of its messages).
```ballerina
   check rabbitmqClient->queuePurge("MyQueue");
```

## 5. Publishing

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

## 6. Listening for Messages

The most efficient way to receive messages is to set up a subscription using a Ballerina RabbitMQ `rabbitmq:Listener` and any number of consumer services. The messages will then be delivered automatically as they arrive rather than having to be explicitly requested. Multiple consumer services can be bound to one Ballerina RabbitMQ `rabbitmq:Listener`. The queue to which the service is listening is configured in the `rabbitmq:ServiceConfig` annotation of the service or else as the name of the service.

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

The `rabbitmq:Message` record received can be used to retrieve its contents.

## 7. Retrieving Individual Messages 

It is also possible to retrieve individual messages on demand ("pull API" a.k.a. polling). This approach to consumption is highly inefficient as it is effectively polling and applications repeatedly have to ask for results even if the vast majority of the requests yield no results. To "pull" a message, use the `consumeMessage` function.

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
