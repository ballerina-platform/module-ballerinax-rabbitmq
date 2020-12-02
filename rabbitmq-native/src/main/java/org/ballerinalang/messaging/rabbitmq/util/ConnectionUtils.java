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

package org.ballerinalang.messaging.rabbitmq.util;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Util class for RabbitMQ Connection handling.
 *
 * @since 0.995.0
 */
public class ConnectionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionUtils.class);

    /**
     * Creates a RabbitMQ Connection using the given connection parameters.
     *
     * @param connectionConfig Parameters used to initialize the connection.
     * @return RabbitMQ Connection object.
     */
    public static Connection createConnection(BMap<BString, Object> connectionConfig) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();

            String host = connectionConfig.getStringValue(RabbitMQConstants.RABBITMQ_CONNECTION_HOST).getValue();
            connectionFactory.setHost(host);

            int port = Math.toIntExact(connectionConfig.getIntValue(RabbitMQConstants.RABBITMQ_CONNECTION_PORT));
            connectionFactory.setPort(port);

            Object username = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_USER);
            if (username != null) {
                connectionFactory.setUsername(username.toString());
            }
            Object pass = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_PASS);
            if (pass != null) {
                connectionFactory.setPassword(pass.toString());
            }
            Object timeout = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_TIMEOUT);
            if (timeout != null) {
                connectionFactory.setConnectionTimeout(Integer.parseInt(timeout.toString()));
            }
            Object handshakeTimeout = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_HANDSHAKE_TIMEOUT);
            if (handshakeTimeout != null) {
                connectionFactory.setHandshakeTimeout(Integer.parseInt(handshakeTimeout.toString()));
            }
            Object shutdownTimeout = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_SHUTDOWN_TIMEOUT);
            if (shutdownTimeout != null) {
                connectionFactory.setShutdownTimeout(Integer.parseInt(shutdownTimeout.toString()));
            }
            Object connectionHeartBeat = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_HEARTBEAT);
            if (connectionHeartBeat != null) {
                connectionFactory.setRequestedHeartbeat(Integer.parseInt(connectionHeartBeat.toString()));
            }
            Connection connection = connectionFactory.newConnection();
            RabbitMQMetricsUtil.reportNewConnection(connection);
            return connection;
        } catch (IOException | TimeoutException exception) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_CONNECTION);
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.CREATE_CONNECTION_ERROR
                    + exception.getMessage());
        }
    }

    public static boolean isClosed(Connection connection) {
        return connection == null || !connection.isOpen();
    }

    public static Object handleCloseConnection(Object closeCode, Object closeMessage, Object timeout,
                                               Connection connection) {
        boolean validTimeout = timeout != null && RabbitMQUtils.checkIfInt(timeout);
        boolean validCloseCode = (closeCode != null && RabbitMQUtils.checkIfInt(closeCode)) &&
                (closeMessage != null && RabbitMQUtils.checkIfString(closeMessage));
        try {
            if (validTimeout && validCloseCode) {
                connection.close(Integer.parseInt(closeCode.toString()), closeMessage.toString(),
                        Integer.parseInt(timeout.toString()));
            } else if (validTimeout) {
                connection.close(Integer.parseInt(timeout.toString()));
            } else if (validCloseCode) {
                connection.close(Integer.parseInt(closeCode.toString()), closeMessage.toString());
            } else {
                connection.close();
            }
            RabbitMQMetricsUtil.reportConnectionClose(connection);
        } catch (IOException | ArithmeticException exception) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_CONNECTION_CLOSE);
            return RabbitMQUtils.returnErrorValue("Error occurred while closing the connection: "
                    + exception.getMessage());
        }
        return null;
    }

    public static void handleAbortConnection(Object closeCode, Object closeMessage, Object timeout,
                                             Connection connection) {
        boolean validTimeout = timeout != null && RabbitMQUtils.checkIfInt(timeout);
        boolean validCloseCode = (closeCode != null && RabbitMQUtils.checkIfInt(closeCode)) &&
                (closeMessage != null && RabbitMQUtils.checkIfString(closeMessage));
        if (validTimeout && validCloseCode) {
            connection.abort(Integer.parseInt(closeCode.toString()), closeMessage.toString(),
                    Integer.parseInt(timeout.toString()));
        } else if (validTimeout) {
            connection.abort(Integer.parseInt(timeout.toString()));
        } else if (validCloseCode) {
            connection.abort(Integer.parseInt(closeCode.toString()), closeMessage.toString());
        } else {
            connection.abort();
        }
        RabbitMQMetricsUtil.reportConnectionClose(connection);
    }

    private ConnectionUtils() {
    }
}
