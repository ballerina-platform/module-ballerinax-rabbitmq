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

package io.ballerina.stdlib.rabbitmq.util;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.crypto.nativeimpl.Decode;
import io.ballerina.stdlib.rabbitmq.RabbitMQConstants;
import io.ballerina.stdlib.rabbitmq.RabbitMQUtils;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQMetricsUtil;
import io.ballerina.stdlib.rabbitmq.observability.RabbitMQObservabilityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.UUID;

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
                SSLContext sslContext = getSslContext(secureSocket);
                connectionFactory.useSslProtocol(sslContext);
                if (secureSocket.getBooleanValue(RabbitMQConstants.VERIFY_HOST)) {
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
        } catch (Exception exception) {
            RabbitMQMetricsUtil.reportError(RabbitMQObservabilityConstants.ERROR_TYPE_CONNECTION);
            return RabbitMQUtils.returnErrorValue(RabbitMQConstants.CREATE_CONNECTION_ERROR
                    + exception.getMessage());
        }
    }

    private static int getTimeValuesInMillis(BDecimal value) {
        BigDecimal valueInSeconds = value.decimalValue();
        return (valueInSeconds.multiply(MILLISECOND_MULTIPLIER)).intValue();
    }

    private static SSLContext getSslContext(BMap<BString, ?> secureSocket) throws Exception {
        // protocol
        String protocol = null;
        if (secureSocket.containsKey(RabbitMQConstants.PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(RabbitMQConstants.PROTOCOL);
            protocol = protocolRecord.getStringValue(RabbitMQConstants.PROTOCOL_NAME).getValue();
        }

        Object cert = secureSocket.get(RabbitMQConstants.CERT);
        @SuppressWarnings("unchecked")
        BMap<BString, BString> key = (BMap<BString, BString>) getBMapValueIfPresent(secureSocket,
                RabbitMQConstants.KEY);

        KeyManagerFactory kmf;
        TrustManagerFactory tmf;
        if (cert instanceof BString) {
            if (key != null) {
                if (key.containsKey(RabbitMQConstants.CERT_FILE)) {
                    BString certFile = key.get(RabbitMQConstants.CERT_FILE);
                    BString keyFile = key.get(RabbitMQConstants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, RabbitMQConstants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        if (cert instanceof BMap) {
            BMap<BString, BString> trustStore = (BMap<BString, BString>) cert;
            if (key != null) {
                if (key.containsKey(RabbitMQConstants.CERT_FILE)) {
                    BString certFile = key.get(RabbitMQConstants.CERT_FILE);
                    BString keyFile = key.get(RabbitMQConstants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, RabbitMQConstants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        return null;
    }

    private static TrustManagerFactory getTrustManagerFactory(BString cert) throws Exception {
        Object publicKeyMap = Decode.decodeRsaPublicKeyFromCertFile(cert);
        if (publicKeyMap instanceof BMap) {
            X509Certificate x509Certificate = (X509Certificate) ((BMap<BString, Object>) publicKeyMap).getNativeData(
                    RabbitMQConstants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            KeyStore ts = KeyStore.getInstance(RabbitMQConstants.PKCS12);
            ts.load(null, "".toCharArray());
            ts.setCertificateEntry(UUID.randomUUID().toString(), x509Certificate);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            return tmf;
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKeyMap).getErrorMessage().getValue());
        }
    }

    private static TrustManagerFactory getTrustManagerFactory(BMap<BString, BString> trustStore) throws Exception {
        BString trustStorePath = trustStore.getStringValue(RabbitMQConstants.KEY_STORE_PATH);
        BString trustStorePassword = trustStore.getStringValue(RabbitMQConstants.KEY_STORE_PASS);
        KeyStore ts = getKeyStore(trustStorePath, trustStorePassword);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BMap<BString, BString> keyStore) throws Exception {
        BString keyStorePath = keyStore.getStringValue(RabbitMQConstants.KEY_STORE_PATH);
        BString keyStorePassword = keyStore.getStringValue(RabbitMQConstants.KEY_STORE_PASS);
        KeyStore ks = getKeyStore(keyStorePath, keyStorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.getValue().toCharArray());
        return kmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BString certFile, BString keyFile, BString keyPassword)
            throws Exception {
        Object publicKey = Decode.decodeRsaPublicKeyFromCertFile(certFile);
        if (publicKey instanceof BMap) {
            X509Certificate publicCert = (X509Certificate) ((BMap<BString, Object>) publicKey).getNativeData(
                    RabbitMQConstants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            Object privateKeyMap = Decode.decodeRsaPrivateKeyFromKeyFile(keyFile, keyPassword);
            if (privateKeyMap instanceof BMap) {
                PrivateKey privateKey = (PrivateKey) ((BMap<BString, Object>) privateKeyMap).getNativeData(
                        RabbitMQConstants.NATIVE_DATA_PRIVATE_KEY);
                KeyStore ks = KeyStore.getInstance(RabbitMQConstants.PKCS12);
                ks.load(null, "".toCharArray());
                ks.setKeyEntry(UUID.randomUUID().toString(), privateKey, "".toCharArray(),
                        new X509Certificate[]{publicCert});
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "".toCharArray());
                return kmf;
            } else {
                throw new Exception("Failed to get the private key from Crypto API. " +
                        ((BError) privateKeyMap).getErrorMessage().getValue());
            }
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKey).getErrorMessage().getValue());
        }
    }

    private static KeyStore getKeyStore(BString path, BString password) throws Exception {
        try (FileInputStream is = new FileInputStream(path.getValue())) {
            char[] passphrase = password.getValue().toCharArray();
            KeyStore ks = KeyStore.getInstance(RabbitMQConstants.PKCS12);
            ks.load(is, passphrase);
            return ks;
        }
    }

    private static SSLContext buildSslContext(KeyManager[] keyManagers, TrustManager[] trustManagers,
                                              String protocol) throws Exception {
        SSLContext sslContext =
                SSLContext.getInstance(Objects.requireNonNullElse(protocol, RabbitMQConstants.DEFAULT_SSL_PROTOCOL));
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    private static BMap<BString, ?> getBMapValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? (BMap<BString, ?>) config.getMapValue(key) : null;
    }

    private static BString getBStringValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? config.getStringValue(key) : null;
    }

    private ConnectionUtils() {
    }
}
