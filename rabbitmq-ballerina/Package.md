## Module Overview

This module contains the functionality to support messaging with the RabbitMQ broker (AMQP 0-9-1). It assumes that the most recent version of Ballerina is used and is built on top of the basics.

[RabbitMQ](https://www.rabbitmq.com/) is one of the most popular open-source enterprise messaging systems modelled on the Advanced Message Queuing Protocol (AMQP) standard. This guide covers the Ballerina RabbitMQ module and its public API. 
 
Key sections include: 
- Connecting to RabbitMQ
- Using Exchanges and Queues
- Publishing Messages
- Consuming Messages Using Consumer Services
- Client Acknowledgements 

### Connecting to RabbitMQ

The following code connects to a RabbitMQ node with default host and port: 

```ballerina
   rabbitmq:Client newClient = new;
```

The `rabbitmq:Client` can now be used to send and receive messages as described in the subsequent sections. 

#### Disconnecting from RabbitMQ

To disconnect, simply close the open channels and the connections: 

```ballerina
   rabbitmq:Error? closeResult = newClient.close();
```

### Using exchanges and queues

Client applications work with exchanges and queues, which are the high-level building blocks of the AMQP protocol. These must be declared before they can be used. The following code declares an exchange and a server-named queue and then binds them together. 

```ballerina
   rabbitmq:Error? exchangeResult = newClient->exchangeDeclare("MyExchange", 
                                                                rabbitmq:DIRECT_EXCHANGE);
   
   string|rabbitmq:Error? queueResult = newClient->queueDeclare("MyQueue");
   if (queueResult is string) {
        rabbitmq:Error? bindResult = newClient->queueBind("MyQueue", "MyExchange", "routing-key");
   }
```

This sample code will declare,
- a durable auto-delete exchange of the type `rabbitmq:DIRECT_EXCHANGE`
- a non-durable, exclusive auto-delete queue with an auto-generated name

Next, the above function is called to bind the queue to the exchange with the given routing key. 

```ballerina
   rabbitmq:Error? exchangeResult = newClient->exchangeDeclare("MyExchange",
                                                        rabbitmq:DIRECT_EXCHANGE);
   
   string|rabbitmq:Error? queueResult = newClient->queueDeclare("MyQueue", 
                                                { durable: true,
                                                  exclusive: false,
                                                  autoDelete: false });

   rabbitmq:Error? bindResult = newClient->queueBind("MyQueue", "MyExchange", "routing-key");
```

This sample code will declare,
 - a durable auto-delete exchange of the type `rabbitmq:DIRECT_EXCHANGE`
 - a durable, non-exclusive non-auto-delete queue with a well-known name

#### Deleting entities and purging queues

- Delete a queue:
```ballerina
   rabbitmq:Error? deleteResult = newClient->queueDelete("MyQueue");
```
- Delete a queue only if it is empty:
```ballerina
   rabbitmq:Error? deleteResult = newClient->queueDelete("MyQueue", false, true);
```
- Delete a queue only if it is unused (does not have any consumers):
```ballerina
   rabbitmq:Error? deleteResult = newClient->queueDelete("MyQueue", true, false);
```
- Delete an exchange:
```ballerina
   rabbitmq:Error? deleteResult = newClient->exchangeDelete("MyExchange");
```
- Purge a queue (delete all of its messages):
```ballerina
   rabbitmq:Error? purgeResult = newClient->queuePurge("MyQueue");
```

### Publishing messages

To publish a message to an exchange, use the `basicPublish()` function as follows:

```ballerina
    string message = "Hello from Ballerina";
   rabbitmq:Error? sendResult = newClient->basicPublish(message.toBytes(), "MyQueue");
``` 
Setting other properties of the message such as routing headers can be done by using the `BasicProperties` record with the appropriate values. 

### Consuming messages using consumer services

The most efficient way to receive messages is to set up a subscription using a Ballerina RabbitMQ `rabbitmq:Listener` and any number of consumer services. The messages will then be delivered automatically as they arrive rather than having to be explicitly requested. 

Multiple consumer services can be bound to one Ballerina RabbitMQ `rabbitmq:Listener`. The queue to which the service is listening is configured in the `rabbitmq:ServiceConfig` annotation of the service. 

```ballerina
listener rabbitmq:Listener channelListener= new;

@rabbitmq:ServiceConfig {
    queueName: "MyQueue"
}
service rabbitmqConsumer on channelListener {
    remote function onMessage(rabbitmq:Message message) {
        // Do something with the message here 
    }
}
```
The `rabbitmq:Message` record received can be used to retrieve its contents. 

### Client acknowledgements

The message consuming is supported by mainly two types of acknowledgement modes, which are auto acknowledgements and client acknowledgements. 
Client acknowledgements can further be divided into to two different types as positive and negative acknowledgements. 
The default acknowledgement mode is auto-ack (messages are acknowledged immediately after consuming).
> WARNING: To ensure the reliability of receiving messages, use the client-ack mode. 

The negatively-acknowledged (rejected) messages can be re-queued. 


>**Note:** The default thread pool size used in Ballerina is the number of processors available * 2. You can configure the thread pool size by using the `BALLERINA_MAX_POOL_SIZE` environment variable.

For information on the operations, which you can perform with this module, see the below **Functions**. 

For examples on the usage of the connector, see the following.
* [Producer Example](https://ballerina.io/swan-lake/learn/by-example/rabbitmq-producer.html).
* [Consumer Example](https://ballerina.io/swan-lake/learn/by-example/rabbitmq-consumer.html)
* [Client Acknowledgements Example](https://ballerina.io/swan-lake/learn/by-example/rabbitmq-consumer-with-client-acknowledgement.html)
* [QoS Settings Example](https://ballerina.io/swan-lake/learn/by-example/rabbitmq-consumer-with-qos-settings.html)
