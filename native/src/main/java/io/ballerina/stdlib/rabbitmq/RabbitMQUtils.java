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

package io.ballerina.stdlib.rabbitmq;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.rabbitmq.util.ModuleUtils;

import java.util.ArrayList;

/**
 * Util class used to bridge the RabbitMQ connector's native code and the Ballerina API.
 *
 * @since 0.995.0
 */
public class RabbitMQUtils {

    public static BError returnErrorValue(String errorMessage) {
        return ErrorCreator.createDistinctError(RabbitMQConstants.RABBITMQ_ERROR,
                                                ModuleUtils.getModule(),
                                                StringUtils.fromString(errorMessage));
    }

    public static boolean checkIfInt(Object object) {
        return TypeUtils.getType(object).getTag() == TypeTags.INT_TAG;
    }

    public static boolean checkIfString(Object object) {
        return TypeUtils.getType(object).getTag() == TypeTags.STRING_TAG;
    }

    /**
     * Removes a given element from the provided array list and returns the resulting list.
     *
     * @param arrayList   The original list
     * @param objectValue Element to be removed
     * @return Resulting list after removing the element
     */
    public static ArrayList<BObject> removeFromList(ArrayList<BObject> arrayList, BObject objectValue) {
        if (arrayList != null) {
            arrayList.remove(objectValue);
        }
        return arrayList;
    }

    public static void handleTransaction(BObject objectValue) {
        RabbitMQTransactionContext transactionContext =
                (RabbitMQTransactionContext) objectValue.getNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT);
        if (transactionContext != null) {
            transactionContext.handleTransactionBlock();
        }
    }

    private RabbitMQUtils() {
    }
}
