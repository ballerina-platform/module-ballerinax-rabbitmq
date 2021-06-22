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

import com.rabbitmq.client.Channel;
import io.ballerina.runtime.transactions.BallerinaTransactionContext;
import io.ballerina.runtime.transactions.TransactionLocalContext;
import io.ballerina.runtime.transactions.TransactionResourceManager;

import java.io.IOException;
import java.util.Objects;

import javax.transaction.xa.XAResource;

/**
 * Channel wrapper class to handle Ballerina transaction logic.
 *
 * @since 0.995
 */
public class RabbitMQTransactionContext implements BallerinaTransactionContext {
    private final Channel channel;
    private final String connectorId;

    /**
     * Initializes the rabbitmq transaction context.
     *
     * @param channel     RabbitMQ Channel.
     * @param connectorId Connector ID.
     */
    public RabbitMQTransactionContext(Channel channel, String connectorId) {
        this.channel = channel;
        this.connectorId = connectorId;
    }

    @Override
    public void commit() {
        try {
            channel.txCommit();
        } catch (IOException exception) {
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.COMMIT_FAILED
                    + exception.getMessage());
        }
    }

    @Override
    public void rollback() {
        try {
            channel.txRollback();
        } catch (IOException exception) {
            throw RabbitMQUtils.returnErrorValue(RabbitMQConstants.ROLLBACK_FAILED
                    + exception.getMessage());
        }

    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public XAResource getXAResource() {
        // Not supported
        return null;
    }

    /**
     * Handles the transaction block if the context is in transaction.
     */
    void handleTransactionBlock() {
        TransactionResourceManager trxResourceManager = TransactionResourceManager.getInstance();
        TransactionLocalContext transactionLocalContext = trxResourceManager.getCurrentTransactionContext();
        BallerinaTransactionContext txContext = transactionLocalContext.getTransactionContext(connectorId);
        if (Objects.isNull(txContext)) {
            try {
                channel.txSelect();
            } catch (IOException exception) {
                throw RabbitMQUtils.returnErrorValue("I/O Error occurred while initiating the transaction."
                        + exception.getMessage());
            }
            transactionLocalContext.registerTransactionContext(connectorId, this);
            String globalTxId = transactionLocalContext.getGlobalTransactionId();
            String currentTxBlockId = transactionLocalContext.getCurrentTransactionBlockId();
            TransactionResourceManager.getInstance().register(globalTxId, currentTxBlockId, this);
        }
    }
}
