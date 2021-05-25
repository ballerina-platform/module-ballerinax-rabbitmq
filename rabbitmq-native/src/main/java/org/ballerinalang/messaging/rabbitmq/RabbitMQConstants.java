/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.messaging.rabbitmq;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * RabbitMQ Connector Constants.
 *
 * @since 0.995.0
 */
public class RabbitMQConstants {

    // RabbitMQ package name constant fields
    public static final String ORG_NAME = "ballerinax";
    static final String RABBITMQ = "rabbitmq";

    // Queue configuration constant fields
    public static final BString QUEUE_NAME = StringUtils.fromString("queueName");
    public static final BString QUEUE_DURABLE = StringUtils.fromString("durable");
    public static final BString QUEUE_EXCLUSIVE = StringUtils.fromString("exclusive");
    public static final BString QUEUE_AUTO_DELETE = StringUtils.fromString("autoDelete");
    public static final BString QUEUE_ARGUMENTS = StringUtils.fromString("arguments");

    // Exchange configuration constant fields
    public static final BString EXCHANGE_DURABLE = StringUtils.fromString("durable");
    public static final BString EXCHANGE_AUTO_DELETE = StringUtils.fromString("autoDelete");
    public static final BString EXCHANGE_ARGUMENTS = StringUtils.fromString("arguments");

    public static final BString AUTH_CONFIG = StringUtils.fromString("auth");
    public static final BString AUTH_USERNAME = StringUtils.fromString("username");
    public static final BString AUTH_PASSWORD = StringUtils.fromString("password");

    // Warning suppression
    public static final String UNCHECKED = "unchecked";

    // Error constant fields
    static final String RABBITMQ_ERROR = "Error";

    // Connection errors
    public static final String CREATE_CONNECTION_ERROR = "Error occurred while connecting to the broker: ";
    public static final String CREATE_SECURE_CONNECTION_ERROR = "An error occurred while configuring " +
            "the SSL connection: ";

    // Channel errors
    public static final String CLOSE_CHANNEL_ERROR = "An error occurred while closing the channel: ";
    public static final String CHANNEL_CLOSED_ERROR = "Channel already closed, messages will no longer be received: ";

    // Connection constant fields
    public static final String CONNECTION_OBJECT = "Connection";
    public static final String CONNECTION_NATIVE_OBJECT = "rabbitmq_connection_object";

    // Connection configuration constant fields
    public static final BString RABBITMQ_CONNECTION_USER = StringUtils.fromString("username");
    public static final BString RABBITMQ_CONNECTION_PASS = StringUtils.fromString("password");
    public static final BString RABBITMQ_CONNECTION_TIMEOUT = StringUtils.fromString("connectionTimeout");
    public static final BString RABBITMQ_CONNECTION_HANDSHAKE_TIMEOUT = StringUtils.fromString(
            "handshakeTimeout");
    public static final BString RABBITMQ_CONNECTION_SHUTDOWN_TIMEOUT = StringUtils.fromString(
            "shutdownTimeout");
    public static final BString RABBITMQ_CONNECTION_HEARTBEAT = StringUtils.fromString("heartbeat");
    public static final BString RABBITMQ_CONNECTION_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString CONNECTION_KEYSTORE = StringUtils.fromString("key");
    public static final BString CONNECTION_TRUSTORE = StringUtils.fromString("cert");
    public static final BString CONNECTION_VERIFY_HOST = StringUtils.fromString("verifyHostName");
    public static final BString CONNECTION_PROTOCOL = StringUtils.fromString("protocol");
    public static final BString CONNECTION_PROTOCOL_NAME = StringUtils.fromString("name");
    public static final String KEY_STORE_TYPE = "PKCS12";
    public static final BString KEY_STORE_PASS = StringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = StringUtils.fromString("path");

    // Channel listener constant fields
    public static final String CONSUMER_SERVICES = "consumer_services";
    public static final String STARTED_SERVICES = "started_services";
    public static final String SERVICE_CONFIG = "ServiceConfig";
    public static final BString ALIAS_QUEUE_NAME = StringUtils.fromString("queueName");
    public static final BString AUTO_ACK = StringUtils.fromString("autoAck");
    public static final String MULTIPLE_ACK_ERROR = "Trying to acknowledge the same message multiple times";
    public static final String ACK_MODE_ERROR = "Trying to acknowledge messages in auto-ack mode";
    static final String THREAD_INTERRUPTED = "Error occurred in RabbitMQ service. " +
            "The current thread got interrupted";
    public static final String ACK_ERROR = "Error occurred while positively acknowledging the message: ";
    public static final String NACK_ERROR = "Error occurred while negatively acknowledging the message: ";
    static final String FUNC_ON_MESSAGE = "onMessage";
    static final String FUNC_ON_REQUEST = "onRequest";
    static final String FUNC_ON_ERROR = "onError";

    // Channel constant fields
    public static final String CHANNEL_NATIVE_OBJECT = "rabbitmq_channel_object";

    // Message constant fields
    public static final String MESSAGE_RECORD = "Message";
    public static final String CALLER_OBJECT = "Caller";
    public static final  String ACK_STATUS = "ackStatus";
    public static final String ACK_MODE = "ackMode";
    public static final BString MESSAGE_CONTENT = StringUtils.fromString("content");
    public static final BString DELIVERY_TAG = StringUtils.fromString("deliveryTag");
    public static final BString MESSAGE_EXCHANGE = StringUtils.fromString("exchange");
    public static final BString MESSAGE_ROUTING_KEY = StringUtils.fromString("routingKey");
    public static final BString BASIC_PROPERTIES = StringUtils.fromString("properties");
    static final String DISPATCH_ERROR = "Error occurred while dispatching the message. ";

    // Transaction constant fields
    public static final String RABBITMQ_TRANSACTION_CONTEXT = "rabbitmq_transactional_context";
    public static final BString CONNECTOR_ID = StringUtils.fromString("connectorId");
    static final String COMMIT_FAILED = "Transaction commit failed: ";
    static final String ROLLBACK_FAILED = "Transaction rollback failed: ";

    // Basic Properties constant fields
    public static final String RECORD_BASIC_PROPERTIES = "BasicProperties";
    public static final BString ALIAS_REPLY_TO = StringUtils.fromString("replyTo");
    public static final BString ALIAS_CONTENT_TYPE = StringUtils.fromString("contentType");
    public static final BString ALIAS_CONTENT_ENCODING = StringUtils.fromString("contentEncoding");
    public static final BString ALIAS_CORRELATION_ID = StringUtils.fromString("correlationId");

    private RabbitMQConstants() {
    }
}
