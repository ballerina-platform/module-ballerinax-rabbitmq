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
    public static final String MESSAGE = "Message";
    public static final String CALLER = "Caller";
    public static final String ERROR_PARAM = "Error";

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

    /**
     * Compilation errors.
     */
    enum CompilationErrors {
        ON_MESSAGE_OR_ON_REQUEST("Only one of either onMessage or onRequest is allowed.", "RABBITMQ_101"),
        NO_ON_MESSAGE_OR_ON_REQUEST("Service must have either remote function onMessage or onRequest.",
                "RABBITMQ_102"),
        INVALID_REMOTE_FUNCTION("Invalid remote function.", "RABBITMQ_103"),
        FUNCTION_SHOULD_BE_REMOTE("Function must have the remote qualifier.", "RABBITMQ_104"),
        MUST_HAVE_MESSAGE("Must have the function parameter rabbitmq:Message.", "RABBITMQ_105"),
        MUST_HAVE_MESSAGE_AND_ERROR("Must have the function parameters rabbitmq:Message and rabbitmq:Error.",
                "RABBITMQ_106"),
        INVALID_FUNCTION_PARAM_MESSAGE("Invalid function parameter. Only rabbitmq:Message is allowed.",
                "RABBITMQ_107"),
        INVALID_FUNCTION_PARAM_ERROR("Invalid function parameter. Only rabbitmq:Error is allowed.",
                "RABBITMQ_108"),
        INVALID_FUNCTION_PARAM_CALLER("Invalid function parameter. Only rabbitmq:Caller is allowed.",
                "RABBITMQ_109"),
        ONLY_PARAMS_ALLOWED("Invalid function parameter count. Only rabbitmq:Message and " +
                "rabbitmq:Caller are allowed.", "RABBITMQ_110"),
        ONLY_PARAMS_ALLOWED_ON_ERROR("Invalid function parameter count. Only rabbitmq:Message and " +
                "rabbitmq:Error are allowed.",
                "RABBITMQ_111"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or rabbitmq:Error? is allowed.",
                "RABBITMQ_112"),
        INVALID_RETURN_TYPE_ANY_DATA("Invalid return type. Only anydata or error is allowed.",
                "RABBITMQ_113"),
        INVALID_MULTIPLE_LISTENERS("Multiple listener attachments. Only one rabbitmq:Listener is allowed.",
                "RABBITMQ_114"),
        INVALID_ANNOTATION_NUMBER("Only one service config annotation is allowed.",
                "RABBITMQ_115"),
        NO_ANNOTATION("No @rabbitmq:ServiceConfig{} annotation is found.",
                "RABBITMQ_116"),
        INVALID_ANNOTATION("Invalid service config annotation. Only @rabbitmq:ServiceConfig{} is allowed.",
                "RABBITMQ_117"),
        INVALID_SERVICE_NAME("Invalid service name. Only string literals are allowed.",
                "RABBITMQ_118");

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
