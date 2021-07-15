// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/crypto;
import ballerina/jballerina.java;

final handle JAVA_NULL = java:createNull();

# Constant for the default host.
public const DEFAULT_HOST = "localhost";

# Constant for the default port.
public const DEFAULT_PORT = 5672;

# Types of exchanges supported by the Ballerina RabbitMQ Connector.
public type ExchangeType "direct"|"fanout"|"topic"|"headers";

# Constant for the RabbitMQ Direct Exchange type.
public const DIRECT_EXCHANGE = "direct";

# Constant for the RabbitMQ Fan-out Exchange type.
public const FANOUT_EXCHANGE = "fanout";

# Constant for the RabbitMQ Topic Exchange type.
public const TOPIC_EXCHANGE = "topic";

# Basic properties of the message - routing headers etc.
#
# + replyTo - The queue name to which the reply should be sent
# + contentType - The content type of the message
# + contentEncoding - The content encoding of the message
# + correlationId - The client-specific ID that can be used to mark or identify messages between clients
public type BasicProperties record {|
    string replyTo?;
    string contentType?;
    string contentEncoding?;
    string correlationId?;
|};

# Additional configurations used to declare a queue.
#
# + durable - Set to true if declaring a durable queue
# + exclusive - Set to true if declaring an exclusive queue
# + autoDelete - Set to true if declaring an auto-delete queue
# + arguments - Other properties (construction arguments) of the queue
public type QueueConfig record {|
    boolean durable = false;
    boolean exclusive = false;
    boolean autoDelete = true;
    map<anydata> arguments?;
|};

# Additional configurations used to declare an exchange.
#
# + durable - Set to `true` if a durable exchange is declared
# + autoDelete - Set to `true` if an autodelete exchange is declared
# + arguments - Other properties (construction arguments) for the queue
public type ExchangeConfig record {|
    boolean durable = false;
    boolean autoDelete = false;
    map<anydata> arguments?;
|};

# Configurations related to initializing the RabbitMQ client and listener.
#
# + username - The username used for establishing the connection
# + password - The password used for establishing the connection
# + connectionTimeout - Connection TCP establishment timeout in seconds and zero for infinite
# + handshakeTimeout -  The AMQP 0-9-1 protocol handshake timeout in seconds
# + shutdownTimeout - Shutdown timeout in seconds, zero for infinite, and the default value is 10. If the consumers exceed
#                     this timeout, then any remaining queued deliveries (and other Consumer callbacks) will be lost
# + heartbeat - The initially-requested heartbeat timeout in seconds and zero for none
# + secureSocket - Configurations for facilitating secure connections
# + auth - Configurations releated to authentication
public type ConnectionConfiguration record {|
    string username?;
    string password?;
    decimal connectionTimeout?;
    decimal handshakeTimeout?;
    decimal shutdownTimeout?;
    decimal heartbeat?;
    SecureSocket secureSocket?;
    Credentials auth?;
|};

# QoS settings to limit the number of unacknowledged
# messages on a channel.
#
# + prefetchCount - The maximum number of messages that the server will deliver.
#                   Give the value as 0 if unlimited
# + prefetchSize - The maximum amount of content (measured in octets)
#                   that the server will deliver and 0 if unlimited
# + global - `true` if the settings should be shared among all the consumers
public type QosSettings record {|
   int prefetchCount;
   int prefetchSize?;
   boolean global = false;
|};

# Configurations for facilitating secure connections.
#
# + cert - Configurations associated with `crypto:TrustStore` or single certificate file that the client trusts
# + key - Configurations associated with `crypto:KeyStore` or combination of certificate and private key of the client
# + protocol - SSL/TLS protocol related options
# + verifyHostName - Enable/disable host name verification
public type SecureSocket record {|
    crypto:TrustStore|string cert;
    crypto:KeyStore|CertKey key?;
    record {|
        Protocol name;
    |} protocol?;
    boolean verifyHostName = true;
|};

# Represents combination of certificate, private key and private key password if encrypted.
#
# + certFile - A file containing the certificate
# + keyFile - A file containing the private key in PKCS8 format
# + keyPassword - Password of the private key if it is encrypted
public type CertKey record {|
    string certFile;
    string keyFile;
    string keyPassword?;
|};

# Represents protocol options.
public enum Protocol {
   SSL,
   TLS,
   DTLS
}

# Configurations related to authentication.
#
# + username - Username to use for authentication
# + password - Password/secret/token to use for authentication
public type Credentials record {|
    string username;
    string password;
|};
