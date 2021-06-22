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
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.messaging.rabbitmq.RabbitMQConstants;
import org.ballerinalang.messaging.rabbitmq.RabbitMQUtils;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQMetricsUtil;
import org.ballerinalang.messaging.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Util class for RabbitMQ Connection handling.
 *
 * @since 0.995.0
 */
public class ConnectionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionUtils.class);
    private static final BigDecimal MILLISECOND_MULTIPLIER = new BigDecimal(1000);

    /**
     * Creates a RabbitMQ Connection using the given connection parameters.
     *
     * @param connectionConfig Parameters used to initialize the connection.
     * @return RabbitMQ Connection object.
     */
    public static Object createConnection(BString host, long port, BMap<BString, Object> connectionConfig) {
        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();

            // Enable TLS for the connection.
            @SuppressWarnings("unchecked")
            BMap<BString, Object> secureSocket = (BMap<BString, Object>) connectionConfig.getMapValue(
                    RabbitMQConstants.RABBITMQ_CONNECTION_SECURE_SOCKET);
            if (secureSocket != null) {
                SSLContext sslContext = getSSLContext(secureSocket);
                connectionFactory.useSslProtocol(sslContext);
                if (secureSocket.getBooleanValue(RabbitMQConstants.CONNECTION_VERIFY_HOST)) {
                    connectionFactory.enableHostnameVerification();
                }
                LOGGER.info("TLS enabled for the connection.");
            }

            connectionFactory.setHost(host.getValue());

            int portInt = Math.toIntExact(port);
            connectionFactory.setPort(portInt);

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
                connectionFactory.setConnectionTimeout(getTimeValuesInMillis((BDecimal) timeout));
            }
            Object handshakeTimeout = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_HANDSHAKE_TIMEOUT);
            if (handshakeTimeout != null) {
                connectionFactory.setHandshakeTimeout(getTimeValuesInMillis((BDecimal) handshakeTimeout));
            }
            Object shutdownTimeout = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_SHUTDOWN_TIMEOUT);
            if (shutdownTimeout != null) {
                connectionFactory.setShutdownTimeout(getTimeValuesInMillis((BDecimal) shutdownTimeout));
            }
            Object connectionHeartBeat = connectionConfig.get(RabbitMQConstants.RABBITMQ_CONNECTION_HEARTBEAT);
            if (connectionHeartBeat != null) {
                // Set in seconds.
                connectionFactory.setRequestedHeartbeat((int) ((BDecimal) connectionHeartBeat).intValue());
            }
            @SuppressWarnings("unchecked")
            BMap<BString, Object> authConfig = (BMap<BString, Object>) connectionConfig.getMapValue(
                    RabbitMQConstants.AUTH_CONFIG);
            if (authConfig != null) {
                connectionFactory.setCredentialsProvider(new DefaultCredentialsProvider(
                        authConfig.getStringValue(RabbitMQConstants.AUTH_USERNAME).getValue(),
                        authConfig.getStringValue(RabbitMQConstants.AUTH_PASSWORD).getValue()));
            }
            Connection connection = connectionFactory.newConnection();
            RabbitMQMetricsUtil.reportNewConnection(connection);
            return connection;
        } catch (IOException | TimeoutException exception) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_CONNECTION);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.CREATE_CONNECTION_ERROR
                    + exception.getMessage());
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                UnrecoverableKeyException e) {
            String errorMsg = "error occurred while setting up the connection. " +
                    (e.getMessage() != null ? e.getMessage() : "");
            return RabbitMQUtils.returnErrorValue(errorMsg);
        }
    }

    private static int getTimeValuesInMillis(BDecimal value) {
        BigDecimal valueInSeconds = value.decimalValue();
        return (valueInSeconds.multiply(MILLISECOND_MULTIPLIER)).intValue();
    }

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    private static SSLContext getSSLContext(BMap<BString, Object> secureSocket)
            throws IOException, CertificateException, KeyStoreException,
            UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException {
        // Keystore
        String keyFilePath = null;
        char[] keyPassphrase = null;
        char[] trustPassphrase;
        String trustFilePath;
        if (secureSocket.containsKey(RabbitMQConstants.CONNECTION_KEYSTORE)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> cryptoKeyStore =
                    (BMap<BString, Object>) secureSocket.getMapValue(RabbitMQConstants.CONNECTION_KEYSTORE);
            keyPassphrase = cryptoKeyStore.getStringValue(RabbitMQConstants.KEY_STORE_PASS).getValue().toCharArray();
            keyFilePath = cryptoKeyStore.getStringValue(RabbitMQConstants.KEY_STORE_PATH).getValue();
        }

        // Truststore
        @SuppressWarnings("unchecked")
        BMap<BString, Object> cryptoTrustStore =
                (BMap<BString, Object>) secureSocket.getMapValue(RabbitMQConstants.CONNECTION_TRUSTORE);
        trustPassphrase = cryptoTrustStore.getStringValue(RabbitMQConstants.KEY_STORE_PASS).getValue()
                .toCharArray();
        trustFilePath = cryptoTrustStore.getStringValue(RabbitMQConstants.KEY_STORE_PATH).getValue();

        // protocol
        String protocol = null;
        if (secureSocket.containsKey(RabbitMQConstants.CONNECTION_PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(RabbitMQConstants.CONNECTION_PROTOCOL);
            protocol = protocolRecord.getStringValue(RabbitMQConstants.CONNECTION_PROTOCOL_NAME).getValue();
        }
        SSLContext sslContext = createSSLContext(trustFilePath, trustPassphrase, keyFilePath, keyPassphrase, protocol);
        return sslContext;
    }


    public static KeyStore loadKeystore(String path, char[] pass) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        KeyStore store = KeyStore.getInstance(RabbitMQConstants.KEY_STORE_TYPE);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
            store.load(in, pass);
        }
        return store;
    }

    public static KeyManager[] createKeyManagers(String keyStorePath, char[] keyStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {
        KeyStore store = loadKeystore(keyStorePath, keyStorePass);
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, keyStorePass);
        return factory.getKeyManagers();
    }

    public static TrustManager[] createTrustManagers(String trustStorePath, char[] trustStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore store = loadKeystore(trustStorePath, trustStorePass);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    public static SSLContext createSSLContext(String trustStorePath, char[] trustStorePass, String keyStorePath,
                                              char[] keyStorePass, String protocol)
            throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException,
            NoSuchAlgorithmException, KeyManagementException {

        SSLContext ctx =
                SSLContext.getInstance(Objects.requireNonNullElse(protocol, RabbitMQConstants.DEFAULT_SSL_PROTOCOL));
        ctx.init(keyStorePath != null ? createKeyManagers(keyStorePath, keyStorePass) : null,
                createTrustManagers(trustStorePath, trustStorePass), new SecureRandom());
        return ctx;
    }

    private ConnectionUtils() {
    }
}
