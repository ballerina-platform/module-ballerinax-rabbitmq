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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.constraint.Constraints;
import org.ballerinalang.langlib.value.CloneReadOnly;
import org.ballerinalang.langlib.value.CloneWithType;
import org.ballerinalang.langlib.value.FromJsonWithType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static io.ballerina.runtime.api.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.TypeTags.STRING_TAG;
import static io.ballerina.runtime.api.TypeTags.UNION_TAG;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.MESSAGE_CONTENT_FIELD;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.MESSAGE_DELIVERY_TAG_FIELD;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.MESSAGE_EXCHANGE_FIELD;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.MESSAGE_PROPERTIES_FIELD;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.MESSAGE_ROUTINE_KEY_FIELD;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.PAYLOAD_BINDING_ERROR;
import static io.ballerina.stdlib.rabbitmq.RabbitMQConstants.PAYLOAD_VALIDATION_ERROR;
import static io.ballerina.stdlib.rabbitmq.util.ModuleUtils.getModule;

/**
 * Util class used to bridge the RabbitMQ connector's native code and the Ballerina API.
 *
 * @since 0.995.0
 */
public class RabbitMQUtils {

    public static BError returnErrorValue(String errorMessage) {
        return ErrorCreator.createDistinctError(RabbitMQConstants.RABBITMQ_ERROR,
                                                getModule(),
                                                StringUtils.fromString(errorMessage));
    }

    public static BError createPayloadValidationError(String message, Object results) {
        return ErrorCreator.createError(getModule(), PAYLOAD_VALIDATION_ERROR, StringUtils.fromString(message),
                ErrorCreator.createError(StringUtils.fromString(results.toString())), null);
    }

    public static BError createPayloadBindingError(String message, BError cause) {
        return ErrorCreator.createError(getModule(), PAYLOAD_BINDING_ERROR, StringUtils.fromString(message),
                cause, null);
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

    public static BMap<BString, Object> createAndPopulateMessageRecord(byte[] message, Envelope envelope,
                                                                       AMQP.BasicProperties properties,
                                                                       Type messageType) {
        RecordType recordType = getRecordType(messageType);
        Type intendedType = TypeUtils.getReferredType(recordType.getFields().get(MESSAGE_CONTENT_FIELD).getFieldType());
        BMap<BString, Object> messageRecord = ValueCreator.createRecordValue(recordType);
        Object messageContent = getValueWithIntendedType(intendedType, message);
        if (messageContent instanceof BError) {
            throw createPayloadBindingError(String.format("Data binding failed: %s", ((BError) messageContent)
                    .getMessage()), (BError) messageContent);
        }
        messageRecord.put(StringUtils.fromString(MESSAGE_CONTENT_FIELD), messageContent);
        messageRecord.put(StringUtils.fromString(MESSAGE_ROUTINE_KEY_FIELD), StringUtils.fromString(
                envelope.getRoutingKey()));
        messageRecord.put(StringUtils.fromString(MESSAGE_EXCHANGE_FIELD), StringUtils.fromString(
                envelope.getExchange()));
        messageRecord.put(StringUtils.fromString(MESSAGE_DELIVERY_TAG_FIELD), envelope.getDeliveryTag());

        if (properties != null) {
            String replyTo = properties.getReplyTo();
            String contentType = properties.getContentType();
            String contentEncoding = properties.getContentEncoding();
            String correlationId = properties.getCorrelationId();
            BMap<BString, Object> basicProperties =
                    ValueCreator.createRecordValue(getModule(),
                            RabbitMQConstants.RECORD_BASIC_PROPERTIES);
            Object[] propValues = new Object[4];
            propValues[0] = replyTo;
            propValues[1] = contentType;
            propValues[2] = contentEncoding;
            propValues[3] = correlationId;
            messageRecord.put(StringUtils.fromString(MESSAGE_PROPERTIES_FIELD), ValueCreator
                    .createRecordValue(basicProperties, propValues));
        }
        if (messageType.getTag() == TypeTags.INTERSECTION_TAG) {
            messageRecord.freezeDirect();
        }
        return messageRecord;
    }

    public static Object createPayload(byte[] message, Type payloadType) {
        Object messageContent = getValueWithIntendedType(getPayloadType(payloadType), message);
        if (messageContent instanceof BError) {
            throw createPayloadBindingError(String.format("Data binding failed: %s", ((BError) messageContent)
                    .getMessage()), (BError) messageContent);
        }
        if (payloadType.isReadOnly()) {
            return CloneReadOnly.cloneReadOnly(messageContent);
        }
        return messageContent;
    }

    public static Object getValueWithIntendedType(Type type, byte[] value) throws BError {
        String strValue = new String(value, StandardCharsets.UTF_8);
        try {
            switch (type.getTag()) {
                case TypeTags.STRING_TAG:
                    return StringUtils.fromString(strValue);
                case TypeTags.XML_TAG:
                    return XmlUtils.parse(strValue);
                case TypeTags.ANYDATA_TAG:
                    return ValueCreator.createArrayValue(value);
                case TypeTags.RECORD_TYPE_TAG:
                    return CloneWithType.convert(type, JsonUtils.parse(strValue));
                case UNION_TAG:
                    if (hasStringType((UnionType) type)) {
                        return StringUtils.fromString(strValue);
                    }
                    return getValueFromJson(type, strValue);
                case TypeTags.ARRAY_TAG:
                    if (TypeUtils.getReferredType(((ArrayType) type).getElementType()).getTag() == TypeTags.BYTE_TAG) {
                        return ValueCreator.createArrayValue(value);
                    }
                    /*-fallthrough*/
                default:
                    return getValueFromJson(type, strValue);
            }
        } catch (BError bError) {
            throw createPayloadBindingError(String.format("Data binding failed: %s", bError.getMessage()), bError);
        }
    }

    private static boolean hasStringType(UnionType type) {
        return type.getMemberTypes().stream().anyMatch(memberType -> {
            if (memberType.getTag() == STRING_TAG) {
                return true;
            }
            return false;
        });
    }

    private static Object getValueFromJson(Type type, String stringValue) {
        BTypedesc typeDesc = ValueCreator.createTypedescValue(type);
        return FromJsonWithType.fromJsonWithType(JsonUtils.parse(stringValue), typeDesc);
    }

    public static RecordType getRecordType(BTypedesc bTypedesc) {
        RecordType recordType;
        if (bTypedesc.getDescribingType().isReadOnly()) {
            recordType = (RecordType) ((IntersectionType) (bTypedesc.getDescribingType())).getConstituentTypes().get(0);
        } else {
            recordType = (RecordType) bTypedesc.getDescribingType();
        }
        return recordType;
    }

    public static RecordType getRecordType(Type type) {
        if (type.getTag() == TypeTags.INTERSECTION_TAG) {
            return (RecordType) ((IntersectionType) (type)).getConstituentTypes().get(0);
        }
        return (RecordType) type;
    }

    private static Type getPayloadType(Type definedType) {
        if (definedType.getTag() == INTERSECTION_TAG) {
            return  ((IntersectionType) definedType).getConstituentTypes().get(0);
        }
        return definedType;
    }

    public static Object validateConstraints(Object value, BTypedesc bTypedesc, boolean constraintValidation) {
        if (constraintValidation) {
            Object validationResult = Constraints.validate(value, bTypedesc);
            if (validationResult instanceof BError) {
                throw createPayloadValidationError(((BError) validationResult).getMessage(), value);
            }
        }
        return value;
    }

    public static BTypedesc getElementTypeDescFromArrayTypeDesc(BTypedesc bTypedesc) {
        if (bTypedesc.getDescribingType().getTag() == INTERSECTION_TAG) {
            return ValueCreator.createTypedescValue((((IntersectionType) bTypedesc.getDescribingType())
                    .getConstituentTypes().get(0)));
        }
        return ValueCreator.createTypedescValue((bTypedesc.getDescribingType()));
    }

    private RabbitMQUtils() {
    }
}
