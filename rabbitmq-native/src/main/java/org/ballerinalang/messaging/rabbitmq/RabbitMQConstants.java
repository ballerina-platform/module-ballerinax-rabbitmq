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

import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.types.BPackage;

import static org.ballerinalang.jvm.util.BLangConstants.ORG_NAME_SEPARATOR;
import static org.ballerinalang.jvm.util.BLangConstants.VERSION_SEPARATOR;

/**
 * RabbitMQ Connector Constants.
 *
 * @since 0.995.0
 */
public class RabbitMQConstants {

    // RabbitMQ package name constant fields
    public static final String ORG_NAME = "ballerinax";
    static final String RABBITMQ = "rabbitmq";
    static final String RABBITMQ_VERSION = "1.0.3";
    public static final String PACKAGE_RABBITMQ =
            ORG_NAME + ORG_NAME_SEPARATOR + RABBITMQ + VERSION_SEPARATOR + RABBITMQ_VERSION;
    public static final String PACKAGE_RABBITMQ_FQN =
            ORG_NAME + ORG_NAME_SEPARATOR + RABBITMQ + VERSION_SEPARATOR + RABBITMQ_VERSION;
    public static final BPackage PACKAGE_ID_RABBITMQ =
            new BPackage(ORG_NAME, "rabbitmq", RABBITMQ_VERSION);

    // Queue configuration constant fields
    public static final BString QUEUE_NAME = BStringUtils.fromString("queueName");
    public static final BString QUEUE_DURABLE = BStringUtils.fromString("durable");
    public static final BString QUEUE_EXCLUSIVE = BStringUtils.fromString("exclusive");
    public static final BString QUEUE_AUTO_DELETE = BStringUtils.fromString("autoDelete");
    public static final BString QUEUE_ARGUMENTS = BStringUtils.fromString("arguments");

    // Exchange configuration constant fields
    public static final BString EXCHANGE_NAME = BStringUtils.fromString("exchangeName");
    public static final BString EXCHANGE_TYPE = BStringUtils.fromString("exchangeType");
    public static final BString EXCHANGE_DURABLE = BStringUtils.fromString("durable");
    public static final BString EXCHANGE_AUTO_DELETE = BStringUtils.fromString("autoDelete");
    public static final BString EXCHANGE_ARGUMENTS = BStringUtils.fromString("arguments");

    // Warning suppression
    public static final String UNCHECKED = "unchecked";

    // Error constant fields
    static final String RABBITMQ_ERROR = "RabbitMqError";
    static final String RABBITMQ_ERROR_DETAILS = "Detail";
    static final String RABBITMQ_ERROR_MESSAGE = "message";

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
    public static final BString RABBITMQ_CONNECTION_HOST = BStringUtils.fromString("host");
    public static final BString RABBITMQ_CONNECTION_PORT = BStringUtils.fromString("port");
    public static final BString RABBITMQ_CONNECTION_USER = BStringUtils.fromString("username");
    public static final BString RABBITMQ_CONNECTION_PASS = BStringUtils.fromString("password");
    public static final BString RABBITMQ_CONNECTION_TIMEOUT = BStringUtils.fromString("connectionTimeoutInMillis");
    public static final BString RABBITMQ_CONNECTION_HANDSHAKE_TIMEOUT = BStringUtils.fromString(
            "handshakeTimeoutMillis");
    public static final BString RABBITMQ_CONNECTION_SHUTDOWN_TIMEOUT = BStringUtils.fromString(
            "shutdownTimeoutInMillis");
    public static final BString RABBITMQ_CONNECTION_HEARTBEAT = BStringUtils.fromString("heartbeatInSeconds");
    public static final BString RABBITMQ_CONNECTION_SECURE_SOCKET = BStringUtils.fromString("secureSocket");
    public static final BString RABBITMQ_CONNECTION_KEYSTORE = BStringUtils.fromString("keyStore");
    public static final BString RABBITMQ_CONNECTION_TRUSTORE = BStringUtils.fromString("trustStore");
    public static final BString RABBITMQ_CONNECTION_VERIFY_HOST = BStringUtils.fromString("verifyHostname");
    public static final BString RABBITMQ_CONNECTION_TLS_VERSION = BStringUtils.fromString("tlsVersion");
    public static final String KEY_STORE_TYPE = "PKCS12";
    public static final BString KEY_STORE_PASS = BStringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = BStringUtils.fromString("path");

    // Channel listener constant fields
    public static final String CONSUMER_SERVICES = "consumer_services";
    public static final String STARTED_SERVICES = "started_services";
    public static final BString CHANNEL_REFERENCE = BStringUtils.fromString("amqpChannel");
    public static final String QOS_STATUS = "qos_status";
    static final String LISTENER_OBJECT = "Listener";
    public static final String SERVICE_CONFIG = "ServiceConfig";
    public static final BString ALIAS_QUEUE_CONFIG = BStringUtils.fromString("queueConfig");
    public static final BString ALIAS_ACK_MODE = BStringUtils.fromString("ackMode");
    public static final BString ALIAS_PREFETCH_COUNT = BStringUtils.fromString("prefetchCount");
    public static final BString ALIAS_PREFETCH_SIZE = BStringUtils.fromString("prefetchSize");
    public static final String AUTO_ACKMODE = "auto";
    public static final String CLIENT_ACKMODE = "client";
    public static final int DEFAULT_PREFETCH = 10;
    public static final String MULTIPLE_ACK_ERROR = "Trying to acknowledge the same message multiple times";
    public static final String ACK_MODE_ERROR = "Trying to acknowledge messages in auto-ack mode";
    static final String THREAD_INTERRUPTED = "Error occurred in RabbitMQ service. " +
            "The current thread got interrupted";
    public static final String ACK_ERROR = "Error occurred while positively acknowledging the message: ";
    public static final String NACK_ERROR = "Error occurred while negatively acknowledging the message: ";
    static final String FUNC_ON_MESSAGE = "onMessage";
    static final String FUNC_ON_ERROR = "onError";

    // Channel constant fields
    public static final String CHANNEL_NATIVE_OBJECT = "rabbitmq_channel_object";
    public static final BString JAVA_CLIENT_CHANNEL = BStringUtils.fromString("amqpChannel");

    // Message constant fields
    public static final String MESSAGE_OBJECT = "Message";
    static final String MESSAGE_OBJ_FULL_NAME = PACKAGE_RABBITMQ + ":" + MESSAGE_OBJECT;
    public static final BString MESSAGE_CONTENT = BStringUtils.fromString("messageContent");
    public static final BString DELIVERY_TAG = BStringUtils.fromString("deliveryTag");
    public static final BString MESSAGE_ACK_STATUS = BStringUtils.fromString("ackStatus");
    public static final BString AUTO_ACK_STATUS = BStringUtils.fromString("autoAck");
    public static final BString BASIC_PROPERTIES = BStringUtils.fromString("properties");
    public static final String XML_CONTENT_ERROR = "Error while retrieving the xml content of the message. ";
    public static final String JSON_CONTENT_ERROR = "Error while retrieving the json content of the message. ";
    public static final String TEXT_CONTENT_ERROR = "Error while retrieving the string content of the message. ";
    public static final String INT_CONTENT_ERROR = "Error while retrieving the int content of the message. ";
    public static final String FLOAT_CONTENT_ERROR = "Error while retrieving the float content of the message. ";
    static final String DISPATCH_ERROR = "Error occurred while dispatching the message. ";

    // Transaction constant fields
    public static final String RABBITMQ_TRANSACTION_CONTEXT = "rabbitmq_transactional_context";
    public static final BString CONNECTOR_ID = BStringUtils.fromString("connectorId");
    static final String COMMIT_FAILED = "Transaction commit failed: ";
    static final String ROLLBACK_FAILED = "Transaction rollback failed: ";

    // Basic Properties constant fields
    public static final String RECORD_BASIC_PROPERTIES = "BasicProperties";
    public static final BString ALIAS_REPLY_TO = BStringUtils.fromString("replyTo");
    public static final BString ALIAS_CONTENT_TYPE = BStringUtils.fromString("contentType");
    public static final BString ALIAS_CONTENT_ENCODING = BStringUtils.fromString("contentEncoding");
    public static final BString ALIAS_CORRELATION_ID = BStringUtils.fromString("correlationId");

    private RabbitMQConstants() {
    }
}
