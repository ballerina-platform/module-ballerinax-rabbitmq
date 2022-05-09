/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.rabbitmq.plugin;

/**
 * RabbitMQ compiler plugin constants.
 */
public class PluginConstants {
    // compiler plugin constants
    public static final String PACKAGE_PREFIX = "rabbitmq";
    public static final String PACKAGE_ORG = "ballerinax";
    public static final String REMOTE_QUALIFIER = "REMOTE";
    public static final String ON_MESSAGE_FUNC = "onMessage";
    public static final String ON_REQUEST_FUNC = "onRequest";
    public static final String ON_ERROR_FUNC = "onError";

    // parameters
    public static final String CALLER = "Caller";
    public static final String ERROR_PARAM = "Error";

    // message fields
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_ROUTING_KEY = "routingKey";
    public static final String MESSAGE_EXCHANGE = "exchange";
    public static final String MESSAGE_DELIVERY_TAG = "deliveryTag";
    public static final String MESSAGE_PROPERTIES = "properties";
    public static final String MESSAGE_REPLY_TO = "replyTo";
    public static final String MESSAGE_CONTENT_TYPE = "contentType";
    public static final String MESSAGE_CONTENT_ENCODING = "contentEncoding";
    public static final String MESSAGE_CORRELATION_ID = "correlationId";

    // return types error or nil
    public static final String ERROR = "error";
    public static final String RABBITMQ_ERROR = PACKAGE_PREFIX + ":" + ERROR_PARAM;
    public static final String NIL = "?";
    public static final String ERROR_OR_NIL = ERROR + NIL;
    public static final String NIL_OR_ERROR = "()|" + ERROR;
    public static final String RABBITMQ_ERROR_OR_NIL = RABBITMQ_ERROR + NIL;
    public static final String NIL_OR_RABBITMQ_ERROR = "()|" + RABBITMQ_ERROR;
    static final String[] ANY_DATA_RETURN_VALUES = {ERROR, RABBITMQ_ERROR, ERROR_OR_NIL, RABBITMQ_ERROR_OR_NIL,
            NIL_OR_ERROR, NIL_OR_RABBITMQ_ERROR, "string", "int", "float", "decimal", "boolean", "xml", "anydata",
            "string|error", "int|error", "float|error", "decimal|error", "boolean|error", "xml|error", "anydata|error",
            "error|string", "error|int", "float|error", "error|decimal", "error|boolean", "error|boolean",
            "error|anydata", "string?", "anydata?", "int?", "float?", "decimal?", "xml?", "boolean?"};

    public static final String PAYLOAD_ANNOTATION = "rabbitmq:Payload ";

    /**
     * Compilation errors.
     */
    enum CompilationErrors {
        ON_MESSAGE_OR_ON_REQUEST("Only one of either onMessage or onRequest is allowed.", "RABBITMQ_101"),
        NO_ON_MESSAGE_OR_ON_REQUEST("Service must have either remote method onMessage or onRequest.",
                "RABBITMQ_102"),
        INVALID_REMOTE_FUNCTION("Invalid remote method.", "RABBITMQ_103"),
        INVALID_FUNCTION("Resource functions are not allowed.", "RABBITMQ_104"),
        FUNCTION_SHOULD_BE_REMOTE("Method must have the remote qualifier.", "RABBITMQ_105"),
        MUST_HAVE_MESSAGE("Must have the method parameter rabbitmq:AnydataMessage.", "RABBITMQ_106"),
        MUST_HAVE_MESSAGE_AND_ERROR("Must have the method parameters rabbitmq:Message and rabbitmq:Error.",
                "RABBITMQ_107"),
        INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD("Invalid method parameter. Only subtypes of " +
                "rabbitmq:AnydataMessage or subtypes of anydata are allowed.", "RABBITMQ_108"),
        INVALID_FUNCTION_PARAM_MESSAGE("Invalid method parameter. Only subtypes of rabbitmq:AnydataMessage " +
                "is allowed.", "RABBITMQ_109"),
        INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER("Invalid method parameter. Only subtypes of anydata " +
                "or rabbitmq:Caller is allowed.", "RABBITMQ_110"),
        INVALID_FUNCTION_PARAM_ERROR("Invalid method parameter. Only rabbitmq:Error is allowed.",
                "RABBITMQ_111"),
        INVALID_FUNCTION_PARAM_CALLER("Invalid method parameter. Only rabbitmq:Caller is allowed.",
                "RABBITMQ_112"),
        INVALID_FUNCTION_PARAM_PAYLOAD("Invalid method parameter. Only subtypes of anydata is allowed.",
                "RABBITMQ_113"),
        ONLY_PARAMS_ALLOWED("Invalid method parameter count. Only subtypes of rabbitmq:AnydataMessage, " +
                "subtypes of anydata and rabbitmq:Caller are allowed.", "RABBITMQ_114"),
        ONLY_PARAMS_ALLOWED_ON_ERROR("Invalid method parameter count. Only rabbitmq:Message and " +
                "rabbitmq:Error are allowed.", "RABBITMQ_115"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or rabbitmq:Error? is allowed.",
                "RABBITMQ_116"),
        INVALID_RETURN_TYPE_ANY_DATA("Invalid return type. Only anydata or error is allowed.",
                "RABBITMQ_117"),
        INVALID_MULTIPLE_LISTENERS("Multiple listener attachments. Only one rabbitmq:Listener is allowed.",
                "RABBITMQ_118"),
        INVALID_ANNOTATION_NUMBER("Only one service config annotation is allowed.",
                "RABBITMQ_119"),
        NO_ANNOTATION("No @rabbitmq:ServiceConfig{} annotation is found.",
                "RABBITMQ_120"),
        INVALID_ANNOTATION("Invalid service config annotation. Only @rabbitmq:ServiceConfig{} is allowed.",
                "RABBITMQ_121"),
        INVALID_SERVICE_ATTACH_POINT("Invalid service attach point. Only string literals are allowed.",
                "RABBITMQ_122"),
        TEMPLATE_CODE_GENERATION_HINT("Template generation for empty service", "RABBITMQ_123");

        private final String error;
        private final String errorCode;

        CompilationErrors(String error, String errorCode) {
            this.error = error;
            this.errorCode = errorCode;
        }

        String getError() {
            return error;
        }

        String getErrorCode() {
            return errorCode;
        }
    }

    private PluginConstants() {
    }
}
