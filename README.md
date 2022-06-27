Ballerina RabbitMQ Library
===================

[![Build](https://github.com/ballerina-platform/module-ballerinax-rabbitmq/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-rabbitmq/actions/workflows/build-timestamped-master.yml)
[![Trivy](https://github.com/ballerina-platform/module-ballerinax-rabbitmq/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-rabbitmq/actions/workflows/trivy-scan.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerinax-rabbitmq.svg)](https://github.com/ballerina-platform/module-ballerinax-rabbitmq/commits/master)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-rabbitmq/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-rabbitmq)

This library provides the capability to send and receive messages by connecting to the RabbitMQ server.

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

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the library.

## Build from the source

### Set up the prerequisites

* Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

   * [OpenJDK](https://adoptium.net/)

        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.

2. Download and install Docker as follows. (The RabbitMQ library is tested with a docker-based integration test
 environment. The before suite initializes the docker container before executing the tests).

   * Installing Docker on Linux

        > **Note:** These commands retrieve content from the `get.docker.com` website in a quiet output-document mode and installs it.
   
          wget -qO- https://get.docker.com/ | sh
   
   * For instructions on installing Docker on Mac, go to <a target="_blank" href="https://docs.docker.com/docker-for-mac/">Get Started with Docker for Mac</a>.
  
   * For information on installing Docker on Windows, goo to <a target="_blank" href="https://docs.docker.com/docker-for-windows/">Get Started with Docker for Windows</a>.

### Build the source

Execute the commands below to build from source.

1. To build the library:
   ```    
   ./gradlew clean build
   ```

2. To run the tests:
   ```
   ./gradlew clean test
   ```
3. To build the library without the tests:
   ```
   ./gradlew clean build -x test
   ```
4. To debug package implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```
5. To debug the library with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```
6. Publish ZIP artifact to the local `.m2` repository:
   ```
   ./gradlew clean build publishToMavenLocal
   ```
7. Publish the generated artifacts to the local Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToLocalCentral=true
   ```
8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`rabbitmq` library](https://lib.ballerina.io/ballerinax/rabbitmq/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
