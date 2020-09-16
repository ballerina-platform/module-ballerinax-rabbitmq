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

import org.ballerinalang.jvm.TypeChecker;
import org.ballerinalang.jvm.api.BErrorCreator;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BError;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.jvm.scheduling.Strand;
import org.ballerinalang.jvm.types.TypeTags;

import java.util.ArrayList;

/**
 * Util class used to bridge the RabbitMQ connector's native code and the Ballerina API.
 *
 * @since 0.995.0
 */
public class RabbitMQUtils {

    public static BError returnErrorValue(String errorMessage) {
        return BErrorCreator.createDistinctError(RabbitMQConstants.RABBITMQ_ERROR,
                                                 RabbitMQConstants.PACKAGE_ID_RABBITMQ,
                                                 BStringUtils.fromString(errorMessage));
    }

    public static boolean checkIfInt(Object object) {
        return TypeChecker.getType(object).getTag() == TypeTags.INT_TAG;
    }

    public static boolean checkIfString(Object object) {
        return TypeChecker.getType(object).getTag() == TypeTags.STRING_TAG;
    }

    static ArrayList<BObject> addToList(ArrayList<BObject> arrayList, BObject objectValue) {
        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }
        arrayList.add(objectValue);
        return arrayList;
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

    public static void handleTransaction(BObject objectValue, Strand strand) {
        RabbitMQTransactionContext transactionContext =
                (RabbitMQTransactionContext) objectValue.getNativeData(RabbitMQConstants.RABBITMQ_TRANSACTION_CONTEXT);
        if (transactionContext != null) {
            transactionContext.handleTransactionBlock(strand);
        }
    }

    private RabbitMQUtils() {
    }
}
