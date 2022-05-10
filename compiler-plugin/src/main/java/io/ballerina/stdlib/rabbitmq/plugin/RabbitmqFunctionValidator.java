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

import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ArrayTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IntersectionTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.compiler.api.symbols.TypeDescKind.ANYDATA;
import static io.ballerina.compiler.api.symbols.TypeDescKind.ARRAY;
import static io.ballerina.compiler.api.symbols.TypeDescKind.BOOLEAN;
import static io.ballerina.compiler.api.symbols.TypeDescKind.DECIMAL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.FLOAT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.INT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.JSON;
import static io.ballerina.compiler.api.symbols.TypeDescKind.MAP;
import static io.ballerina.compiler.api.symbols.TypeDescKind.NIL;
import static io.ballerina.compiler.api.symbols.TypeDescKind.OBJECT;
import static io.ballerina.compiler.api.symbols.TypeDescKind.RECORD;
import static io.ballerina.compiler.api.symbols.TypeDescKind.STRING;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TABLE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.TYPE_REFERENCE;
import static io.ballerina.compiler.api.symbols.TypeDescKind.UNION;
import static io.ballerina.compiler.api.symbols.TypeDescKind.XML;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ANYDATA_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.ARRAY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BOOLEAN_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.BYTE_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.DECIMAL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.FLOAT_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.INTERSECTION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.INT_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.JSON_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.MAP_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.NIL_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.PARENTHESISED_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.QUALIFIED_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.READONLY_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.RECORD_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.SIMPLE_NAME_REFERENCE;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.STRING_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.TABLE_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.UNION_TYPE_DESC;
import static io.ballerina.compiler.syntax.tree.SyntaxKind.XML_TYPE_DESC;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.FUNCTION_SHOULD_BE_REMOTE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_CALLER;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_PAYLOAD;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ANY_DATA;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE_AND_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.NO_ON_MESSAGE_OR_ON_REQUEST;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED_ON_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ON_MESSAGE_OR_ON_REQUEST;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_CONTENT;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_CONTENT_ENCODING;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_CONTENT_TYPE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_CORRELATION_ID;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_DELIVERY_TAG;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_EXCHANGE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_PROPERTIES;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_REPLY_TO;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.MESSAGE_ROUTING_KEY;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.PAYLOAD_ANNOTATION;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginUtils.getMethodSymbol;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginUtils.validateModuleId;

/**
 * RabbitMQ remote function validator.
 */
public class RabbitmqFunctionValidator {

    private final SyntaxNodeAnalysisContext context;
    private final ServiceDeclarationNode serviceDeclarationNode;
    FunctionDefinitionNode onMessage;
    FunctionDefinitionNode onRequest;
    FunctionDefinitionNode onError;

    public RabbitmqFunctionValidator(SyntaxNodeAnalysisContext context, FunctionDefinitionNode onMessage,
                                     FunctionDefinitionNode onRequest, FunctionDefinitionNode onError) {
        this.context = context;
        this.serviceDeclarationNode = (ServiceDeclarationNode) context.node();
        this.onMessage = onMessage;
        this.onRequest = onRequest;
        this.onError = onError;
    }

    public void validate() {
        validateMandatoryFunction();
        if (Objects.nonNull(onMessage)) {
            validateOnMessage();
        }
        if (Objects.nonNull(onRequest)) {
            validateOnRequest();
        }
        if (Objects.nonNull(onError)) {
            validateOnError();
        }
    }

    private void validateMandatoryFunction() {
        if (Objects.isNull(onMessage) && Objects.isNull(onRequest)) {
            reportErrorDiagnostic(NO_ON_MESSAGE_OR_ON_REQUEST, serviceDeclarationNode.location());
        } else if (!Objects.isNull(onMessage) && !Objects.isNull(onRequest)) {
            reportErrorDiagnostic(ON_MESSAGE_OR_ON_REQUEST, serviceDeclarationNode.location());
        }
    }

    private void validateOnMessage() {
        if (!PluginUtils.isRemoteFunction(context, onMessage)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onMessage.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onMessage.functionSignature().parameters();
        validateFunctionParameters(parameters, onMessage);
        validateReturnTypeErrorOrNil(onMessage);
    }

    private void validateOnRequest() {
        if (!PluginUtils.isRemoteFunction(context, onRequest)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onRequest.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onRequest.functionSignature().parameters();
        validateFunctionParameters(parameters, onRequest);
        validateOnRequestReturnType(onRequest);
    }

    private void validateOnError() {
        if (!PluginUtils.isRemoteFunction(context, onError)) {
            reportErrorDiagnostic(FUNCTION_SHOULD_BE_REMOTE, onError.functionSignature().location());
        }
        SeparatedNodeList<ParameterNode> parameters = onError.functionSignature().parameters();
        validateOnErrorFunctionParameters(parameters, onError);
        validateReturnTypeErrorOrNil(onError);
    }

    private void validateFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                            FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 1) {
            validateSingleParam(parameters.get(0));
        } else if (parameters.size() == 2) {
            validateFirstParamInTwoParamScenario(parameters.get(0));
            validateSecondParamInTwoParamScenario(parameters.get(1));
        } else if (parameters.size() == 3) {
            validateFirstParamInThreeParamScenario(parameters.get(0));
            validateSecondParamInThreeParamScenario(parameters.get(1));
            validateThirdParamInThreeParamScenario(parameters.get(2));
        } else if (parameters.size() < 1) {
            reportErrorDiagnostic(MUST_HAVE_MESSAGE, functionDefinitionNode.functionSignature().location());
        } else {
            reportErrorDiagnostic(ONLY_PARAMS_ALLOWED, functionDefinitionNode.functionSignature().location());
        }
    }

    private void validateSingleParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        if (!isMessageParam(requiredParameterNode)) {
            if (!validatePayload(requiredParameterNode.typeName())) {
                reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD, parameterNode.location());
            }
        }
    }

    private void validateFirstParamInThreeParamScenario(ParameterNode parameterNode) {
        if (!isMessageParam((RequiredParameterNode) parameterNode)) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE, parameterNode.location());
        }
    }

    private void validateSecondParamInThreeParamScenario(ParameterNode parameterNode) {
        if (!validateCaller(parameterNode)) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_CALLER, parameterNode.location());
        }
    }

    private void validateFirstParamInTwoParamScenario(ParameterNode parameterNode) {
        if (!isMessageParam((RequiredParameterNode) parameterNode)) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE, parameterNode.location());
        }
    }

    private void validateSecondParamInTwoParamScenario(ParameterNode parameterNode) {
        if (!validateCaller(parameterNode) && (isMessageParam((RequiredParameterNode) parameterNode) ||
                !validatePayload(((RequiredParameterNode) parameterNode).typeName()))) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER, parameterNode.location());
        }
    }

    private void validateOnErrorFunctionParameters(SeparatedNodeList<ParameterNode> parameters,
                                                   FunctionDefinitionNode functionDefinitionNode) {
        if (parameters.size() == 2) {
            validateFirstParamInTwoParamScenario(parameters.get(0));
            validateErrorParam(parameters.get(1));
        } else if (parameters.size() < 2) {
            reportErrorDiagnostic(MUST_HAVE_MESSAGE_AND_ERROR, functionDefinitionNode.functionSignature().location());
        } else {
            reportErrorDiagnostic(ONLY_PARAMS_ALLOWED_ON_ERROR, functionDefinitionNode.functionSignature().location());
        }
    }

    private boolean isMessageParam(RequiredParameterNode requiredParameterNode) {
        boolean hasPayloadAnnotation = requiredParameterNode.annotations().stream()
                .anyMatch(annotationNode -> annotationNode.annotReference().toString().equals(PAYLOAD_ANNOTATION));
        if (hasPayloadAnnotation) {
            return false;
        }
        Node node;
        if (requiredParameterNode.typeName().kind() == INTERSECTION_TYPE_DESC) {
            IntersectionTypeDescriptorNode intersectionNode = (IntersectionTypeDescriptorNode) requiredParameterNode
                    .typeName();
            if (intersectionNode.leftTypeDesc().kind() == SIMPLE_NAME_REFERENCE ||
                    intersectionNode.leftTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
                node = intersectionNode.leftTypeDesc();
            } else if (intersectionNode.rightTypeDesc().kind() == SIMPLE_NAME_REFERENCE ||
                    intersectionNode.rightTypeDesc().kind() == QUALIFIED_NAME_REFERENCE) {
                node = intersectionNode.rightTypeDesc();
            } else {
                return false;
            }
        } else if (requiredParameterNode.typeName().kind() != SIMPLE_NAME_REFERENCE &&
                requiredParameterNode.typeName().kind() != QUALIFIED_NAME_REFERENCE) {
            return false;
        } else {
            node = requiredParameterNode.typeName();
        }
        return isMessageType((TypeSymbol) context.semanticModel().symbol(node).get());
    }

    private boolean validateCaller(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (parameterTypeNode.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode callerNode = (QualifiedNameReferenceNode) parameterTypeNode;
            Optional<Symbol> paramSymbol = context.semanticModel().symbol(callerNode);
            if (paramSymbol.isPresent()) {
                Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
                if (moduleSymbol.isPresent()) {
                    String paramName = paramSymbol.get().getName().isPresent() ?
                            paramSymbol.get().getName().get() : "";
                    if (!validateModuleId(moduleSymbol.get()) ||
                            !paramName.equals(PluginConstants.CALLER)) {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private void validateThirdParamInThreeParamScenario(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        if (isMessageParam((RequiredParameterNode) parameterNode) ||
                !validatePayload(requiredParameterNode.typeName())) {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_PAYLOAD, requiredParameterNode.location());
        }
    }

    private boolean validatePayload(Node node) {
        SyntaxKind syntaxKind = node.kind();
        if (syntaxKind == INTERSECTION_TYPE_DESC) {
            IntersectionTypeDescriptorNode intersectionNode = (IntersectionTypeDescriptorNode) node;
            if (intersectionNode.leftTypeDesc().kind() != READONLY_TYPE_DESC) {
                return validatePayload(intersectionNode.leftTypeDesc());
            } else if (intersectionNode.rightTypeDesc().kind() != READONLY_TYPE_DESC) {
                return validatePayload(intersectionNode.rightTypeDesc());
            } else {
                return false;
            }
        } else if (syntaxKind == PARENTHESISED_TYPE_DESC) {
            ParenthesisedTypeDescriptorNode parenthesisedNode = (ParenthesisedTypeDescriptorNode) node;
            return validatePayload(parenthesisedNode.typedesc());
        } else if (syntaxKind == UNION_TYPE_DESC) {
            UnionTypeDescriptorNode unionNode = (UnionTypeDescriptorNode) node;
            return validatePayload(unionNode.leftTypeDesc()) &&
                    validatePayload(unionNode.rightTypeDesc());
        } else if (syntaxKind == QUALIFIED_NAME_REFERENCE) {
            TypeReferenceTypeSymbol typeSymbol = (TypeReferenceTypeSymbol) context.semanticModel().symbol(node).get();
            return typeSymbol.typeDescriptor().typeKind() != OBJECT;
        } else if (syntaxKind == ARRAY_TYPE_DESC) {
            return validatePayload(((ArrayTypeDescriptorNode) node).memberTypeDesc());
        }
        return syntaxKind == INT_TYPE_DESC || syntaxKind == STRING_TYPE_DESC || syntaxKind == BOOLEAN_TYPE_DESC ||
                syntaxKind == FLOAT_TYPE_DESC || syntaxKind == DECIMAL_TYPE_DESC || syntaxKind == RECORD_TYPE_DESC ||
                syntaxKind == MAP_TYPE_DESC || syntaxKind == BYTE_TYPE_DESC || syntaxKind == TABLE_TYPE_DESC ||
                syntaxKind == JSON_TYPE_DESC || syntaxKind == XML_TYPE_DESC || syntaxKind == ANYDATA_TYPE_DESC ||
                syntaxKind == NIL_TYPE_DESC || syntaxKind == SIMPLE_NAME_REFERENCE;
    }

    private boolean isMessageType(TypeSymbol typeSymbol) {
        RecordTypeSymbol recordTypeSymbol;
        if (typeSymbol.typeKind() == TYPE_REFERENCE) {
            if (((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor().typeKind() == RECORD) {
                recordTypeSymbol = (RecordTypeSymbol) ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            } else {
                return false;
            }
        } else {
            recordTypeSymbol = (RecordTypeSymbol) typeSymbol;
        }
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        return validateMessageFields(fieldDescriptors);
    }

    private boolean validateMessageFields(Map<String, RecordFieldSymbol> fieldDescriptors) {
        if (fieldDescriptors.size() != 5 || !fieldDescriptors.containsKey(MESSAGE_CONTENT) ||
                !fieldDescriptors.containsKey(MESSAGE_ROUTING_KEY) ||
                !fieldDescriptors.containsKey(MESSAGE_EXCHANGE) ||
                !fieldDescriptors.containsKey(MESSAGE_DELIVERY_TAG) ||
                !fieldDescriptors.containsKey(MESSAGE_PROPERTIES)) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_ROUTING_KEY).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_EXCHANGE).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_DELIVERY_TAG).typeDescriptor().typeKind() != INT) {
            return false;
        }
        if (fieldDescriptors.get(MESSAGE_PROPERTIES).typeDescriptor().typeKind() != TYPE_REFERENCE &&
                fieldDescriptors.get(MESSAGE_PROPERTIES).typeDescriptor().typeKind() != RECORD) {
            return false;
        }
        if (!validatePropertiesField(fieldDescriptors.get(MESSAGE_PROPERTIES).typeDescriptor())) {
            return false;
        }
        if (!validateAnydataFields(fieldDescriptors.get(MESSAGE_CONTENT).typeDescriptor())) {
            return false;
        }
        return true;
    }

    private boolean validateAnydataFields(TypeSymbol typeSymbol) {
        TypeDescKind symbolTypeKind = typeSymbol.typeKind();
        return symbolTypeKind == ANYDATA || symbolTypeKind == ARRAY || symbolTypeKind == BOOLEAN ||
                symbolTypeKind == JSON || symbolTypeKind == INT || symbolTypeKind == STRING ||
                symbolTypeKind == FLOAT || symbolTypeKind == DECIMAL || symbolTypeKind == RECORD ||
                symbolTypeKind == TABLE || symbolTypeKind == XML || symbolTypeKind == UNION ||
                symbolTypeKind == MAP || symbolTypeKind == NIL || symbolTypeKind == TYPE_REFERENCE;
    }

    private boolean validatePropertiesField(TypeSymbol propertiesTypeSymbol) {
        RecordTypeSymbol propertiesRecordSymbol;
        if (propertiesTypeSymbol.typeKind() == TYPE_REFERENCE) {
            if (((TypeReferenceTypeSymbol) propertiesTypeSymbol).typeDescriptor().typeKind() == RECORD) {
                propertiesRecordSymbol = (RecordTypeSymbol) ((TypeReferenceTypeSymbol) propertiesTypeSymbol)
                        .typeDescriptor();
            } else {
                return false;
            }
        } else {
            propertiesRecordSymbol = (RecordTypeSymbol) propertiesTypeSymbol;
        }
        Map<String, RecordFieldSymbol> propertiesFieldDescriptors = propertiesRecordSymbol.fieldDescriptors();
        if (propertiesFieldDescriptors.size() != 4 || !propertiesFieldDescriptors.containsKey(MESSAGE_REPLY_TO) ||
                !propertiesFieldDescriptors.containsKey(MESSAGE_CONTENT_TYPE) ||
                !propertiesFieldDescriptors.containsKey(MESSAGE_CONTENT_ENCODING) ||
                !propertiesFieldDescriptors.containsKey(MESSAGE_CORRELATION_ID)) {
            return false;
        }
        if (propertiesFieldDescriptors.get(MESSAGE_REPLY_TO).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (propertiesFieldDescriptors.get(MESSAGE_CONTENT_TYPE).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (propertiesFieldDescriptors.get(MESSAGE_CONTENT_ENCODING).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        if (propertiesFieldDescriptors.get(MESSAGE_CORRELATION_ID).typeDescriptor().typeKind() != STRING) {
            return false;
        }
        return true;
    }

    private void validateReturnTypeErrorOrNil(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol != null) {
            Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDesc.isPresent()) {
                if (returnTypeDesc.get().typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> returnTypeMembers =
                            ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
                    for (TypeSymbol returnType : returnTypeMembers) {
                        if (returnType.typeKind() != TypeDescKind.NIL) {
                            if (returnType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                                if (!returnType.signature().equals(PluginConstants.ERROR) &&
                                        !validateModuleId(returnType.getModule().get())) {
                                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                            functionDefinitionNode.location());
                                }
                            } else if (returnType.typeKind() != TypeDescKind.ERROR) {
                                reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                        functionDefinitionNode.location());
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                            functionDefinitionNode.location());
                }
            }
        }
    }

    private void validateOnRequestReturnType(FunctionDefinitionNode functionDefinitionNode) {
        MethodSymbol methodSymbol = getMethodSymbol(context, functionDefinitionNode);
        if (methodSymbol != null) {
            Optional<TypeSymbol> returnTypeDesc = methodSymbol.typeDescriptor().returnTypeDescriptor();
            if (returnTypeDesc.isPresent()) {
                if (returnTypeDesc.get().typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> returnTypeMembers =
                            ((UnionTypeSymbol) returnTypeDesc.get()).memberTypeDescriptors();
                    for (TypeSymbol returnType : returnTypeMembers) {
                        if (returnType.typeKind() != TypeDescKind.NIL) {
                            if (returnType.typeKind() == TypeDescKind.ERROR) {
                                if (!returnType.signature().equals(PluginConstants.ERROR) &&
                                        !validateModuleId(returnType.getModule().get())) {
                                    reportErrorDiagnostic(INVALID_RETURN_TYPE_ERROR_OR_NIL,
                                            functionDefinitionNode.location());
                                }
                            } else {
                                validateAnyDataReturnType(returnTypeDesc.get().signature(), functionDefinitionNode);
                            }
                        }
                    }
                } else if (returnTypeDesc.get().typeKind() != TypeDescKind.NIL) {
                    validateAnyDataReturnType(returnTypeDesc.get().signature(), functionDefinitionNode);
                }
            }
        }
    }

    private void validateAnyDataReturnType(String returnType, FunctionDefinitionNode functionDefinitionNode) {
        if (!Arrays.asList(PluginConstants.ANY_DATA_RETURN_VALUES).contains(returnType)) {
            reportErrorDiagnostic(INVALID_RETURN_TYPE_ANY_DATA, functionDefinitionNode.location());
        }
    }

    private void validateErrorParam(ParameterNode parameterNode) {
        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
        Node parameterTypeNode = requiredParameterNode.typeName();
        if (parameterTypeNode.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE) {
            QualifiedNameReferenceNode errorNode = (QualifiedNameReferenceNode) parameterTypeNode;
            Optional<Symbol> paramSymbol = context.semanticModel().symbol(errorNode);
            if (paramSymbol.isPresent()) {
                String paramName = paramSymbol.get().getName().isPresent() ? paramSymbol.get().getName().get() : "";
                Optional<ModuleSymbol> moduleSymbol = paramSymbol.get().getModule();
                if (moduleSymbol.isPresent()) {
                    if (!validateModuleId(moduleSymbol.get()) ||
                            !(paramName.equals(PluginConstants.ERROR_PARAM))) {
                        reportErrorDiagnostic(INVALID_FUNCTION_PARAM_ERROR, requiredParameterNode.location());
                    }
                }
            }
        } else {
            reportErrorDiagnostic(INVALID_FUNCTION_PARAM_ERROR, requiredParameterNode.location());
        }
    }

    private void reportErrorDiagnostic(CompilationErrors error, Location location) {
        context.reportDiagnostic(PluginUtils.getDiagnostic(error, DiagnosticSeverity.ERROR, location));
    }
}
