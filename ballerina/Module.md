## Overview

This module provides the capability to send and receive messages by connecting to the RabbitMQ server.

RabbitMQ gives your applications a common platform to send and receive messages and a safe place for your messages to live until received. RabbitMQ is one of the most popular open-source message brokers. It is lightweight and easy to deploy on-premise and in the cloud.

### Basic usage

#### Set up the connection

First, you need to set up the connection with the RabbitMQ server. The following ways can be used to connect to a
RabbitMQ server.

1. Connect to a RabbitMQ node with the default host and port:
```ballerina
    rabbitmq:Client rabbitmqClient = check new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
```

2. Connect to a RabbitMQ node with a custom host and port:
```ballerina
    rabbitmq:Client rabbitmqClient = check new("localhost", 5672);
```

3. Connect to a RabbitMQ node with host, port, and additional configurations:
```ballerina
    rabbitmq:ConnectionConfiguration config = {
        username: "ballerina",
        password: "password"
    };
    rabbitmq:Client rabbitmqClient = check new("localhost", 5672, configs);
```

The `rabbitmq:Client` can now be used to send and receive messages as described in the subsequent sections.

#### Exchanges and queues

Client applications work with exchanges and queues, which are the high-level building blocks of the AMQP protocol. These must be declared before they can be used. The following code declares an exchange and a server-named queue and then binds them together.

```ballerina
    check rabbitmqClient->exchangeDeclare("MyExchange", rabbitmq:DIRECT_EXCHANGE);
    check rabbitmqClient->queueDeclare("MyQueue");
    check rabbitmqClient->queueBind("MyQueue", "MyExchange", "routing-key");
```

This sample code will declare,
- a durable auto-delete exchange of the type `rabbitmq:DIRECT_EXCHANGE`
- a non-durable, exclusive auto-delete queue with an auto-generated name

Next, the `queueBind` function is called to bind the queue to the exchange with the given routing key.

```ballerina
    check rabbitmqClient->exchangeDeclare("MyExchange", rabbitmq:DIRECT_EXCHANGE);
    check rabbitmqClient->queueDeclare("MyQueue", { durable: true,
                                                   exclusive: false,
                                                   autoDelete: false });
    check rabbitmqClient->queueBind("MyQueue", "MyExchange", "routing-key");
```

This sample code will declare,
- a durable auto-delete exchange of the type `rabbitmq:DIRECT_EXCHANGE`
- a durable, non-exclusive, non-auto-delete queue with a well-known name

#### Delete entities and purge queues

- Delete a queue:
```ballerina
    check rabbitmqClient->queueDelete("MyQueue");
```
- Delete a queue only if it is empty:
```ballerina
    check rabbitmqClient->queueDelete("MyQueue", false, true);
```
- Delete a queue only if it is unused (does not have any consumers):
```ballerina
    check rabbitmqClient->queueDelete("MyQueue", true, false);
```
- Delete an exchange:
```ballerina
    check rabbitmqClient->exchangeDelete("MyExchange");
```
- Purge a queue (delete all of its messages):
```ballerina
    check rabbitmqClient->queuePurge("MyQueue");
```

#### Publish messages

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

#### Consume messages using consumer services

The most efficient way to receive messages is to set up a subscription using a Ballerina RabbitMQ `rabbitmq:Listener` and any number of consumer services. The messages will then be delivered automatically as they arrive rather than having to be explicitly requested. Multiple consumer services can be bound to one Ballerina RabbitMQ `rabbitmq:Listener`. The queue to which the service is listening is configured in the `rabbitmq:ServiceConfig` annotation of the service or else as the name of the service.

1. Listen to incoming messages with the `onMessage` remote method:

```ballerina
    listener rabbitmq:Listener channelListener= new(rabbitmq:DEFAULT_HOST, rabbitmq:DEFAULT_PORT);
    
    @rabbitmq:ServiceConfig {
        queueName: "MyQueue"
    }
    service rabbitmq:Service on channelListener {
        remote function onMessage(rabbitmq:AnydataMessage message) {
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
        remote function onRequest(rabbitmq:BytesMessage message) returns string {
            return "Hello Back!";
        }
    }
```

The `rabbitmq:BytesMessage` record received can be used to retrieve its contents.

### Advanced usage

#### Client acknowledgements

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
        remote function onMessage(rabbitmq:BytesMessage message, rabbitmq:Caller caller) {
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
        remote function onMessage(rabbitmq:BytesMessage message) {
            rabbitmq:Error? result = caller->basicNack(true, requeue = false);
        }
    }
```

The negatively-acknowledged (rejected) messages can be re-queued by setting the `requeue` to `true`.
