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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static io.ballerina.stdlib.rabbitmq.plugin.CompilerPluginTestUtils.BALLERINA_SOURCES;
import static io.ballerina.stdlib.rabbitmq.plugin.CompilerPluginTestUtils.RESOURCE_DIRECTORY;
import static io.ballerina.stdlib.rabbitmq.plugin.CompilerPluginTestUtils.getEnvironmentBuilder;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.FUNCTION_SHOULD_BE_REMOTE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_CALLER;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_FUNCTION_PARAM_PAYLOAD;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_MULTIPLE_LISTENERS;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_REMOTE_FUNCTION;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ANY_DATA;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.INVALID_RETURN_TYPE_ERROR_OR_NIL;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.MUST_HAVE_MESSAGE_AND_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.NO_ANNOTATION;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.NO_ON_MESSAGE_OR_ON_REQUEST;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ONLY_PARAMS_ALLOWED_ON_ERROR;
import static io.ballerina.stdlib.rabbitmq.plugin.PluginConstants.CompilationErrors.ON_MESSAGE_OR_ON_REQUEST;

/**
 * Tests for NATS package compiler plugin.
 */
public class RabbitmqCompilerPluginTest {

    @Test
    public void testValidService1() {
        Package currentPackage = loadPackage("valid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService2() {
        Package currentPackage = loadPackage("valid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService3() {
        Package currentPackage = loadPackage("valid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService4() {
        Package currentPackage = loadPackage("valid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService5() {
        Package currentPackage = loadPackage("valid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService6() {
        Package currentPackage = loadPackage("valid_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService7() {
        Package currentPackage = loadPackage("valid_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService8() {
        Package currentPackage = loadPackage("valid_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testValidService9() {
        Package currentPackage = loadPackage("valid_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test(description = "Validate `rabbitmq:Service` with `display` annotation")
    public void testValidService10() {
        Package currentPackage = loadPackage("valid_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 0);
    }

    @Test
    public void testInvalidService1() {
        Package currentPackage = loadPackage("invalid_service_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, NO_ON_MESSAGE_OR_ON_REQUEST);
    }

    @Test
    public void testInvalidService2() {
        Package currentPackage = loadPackage("invalid_service_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ON_MESSAGE_OR_ON_REQUEST);
    }

    @Test
    public void testInvalidService3() {
        Package currentPackage = loadPackage("invalid_service_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_REMOTE_FUNCTION);
    }

    @Test
    public void testInvalidService4() {
        Package currentPackage = loadPackage("invalid_service_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 3);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, FUNCTION_SHOULD_BE_REMOTE);
        }
    }

    @Test
    public void testInvalidService5() {
        Package currentPackage = loadPackage("invalid_service_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, MUST_HAVE_MESSAGE);
        }
    }

    @Test
    public void testInvalidService6() {
        Package currentPackage = loadPackage("invalid_service_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, ONLY_PARAMS_ALLOWED);
        }
    }

    @Test
    public void testInvalidService7() {
        Package currentPackage = loadPackage("invalid_service_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD);
        }
    }

    @Test
    public void testInvalidService8() {
        Package currentPackage = loadPackage("invalid_service_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ERROR_OR_NIL);
        }
    }

    @Test
    public void testInvalidService9() {
        Package currentPackage = loadPackage("invalid_service_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_MESSAGE);
        }
    }

    @Test
    public void testInvalidService10() {
        Package currentPackage = loadPackage("invalid_service_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_RETURN_TYPE_ANY_DATA);
        }
    }

    @Test
    public void testInvalidService11() {
        Package currentPackage = loadPackage("invalid_service_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, MUST_HAVE_MESSAGE_AND_ERROR);
    }

    @Test
    public void testInvalidService12() {
        Package currentPackage = loadPackage("invalid_service_12");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, ONLY_PARAMS_ALLOWED_ON_ERROR);
    }

    @Test
    public void testInvalidService13() {
        Package currentPackage = loadPackage("invalid_service_13");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_ERROR);
        }
    }

    @Test
    public void testInvalidService14() {
        Package currentPackage = loadPackage("invalid_service_14");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_MULTIPLE_LISTENERS);
    }

    @Test
    public void testInvalidService15() {
        Package currentPackage = loadPackage("invalid_service_15");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, INVALID_FUNCTION);
    }

    @Test
    public void testInvalidService16() {
        Package currentPackage = loadPackage("invalid_service_16");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_MESSAGE_OR_PAYLOAD);
        }
    }

    @Test
    public void testInvalidService17() {
        Package currentPackage = loadPackage("invalid_service_17");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER);
        }
    }

    @Test
    public void testInvalidService18() {
        Package currentPackage = loadPackage("invalid_service_18");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_CALLER);
        }
    }

    @Test
    public void testInvalidService19() {
        Package currentPackage = loadPackage("invalid_service_19");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_PAYLOAD);
        }
    }

    @Test
    public void testInvalidService20() {
        Package currentPackage = loadPackage("invalid_service_20");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_MESSAGE_OR_CALLER);
        }
    }

    @Test
    public void testInvalidService21() {
        Package currentPackage = loadPackage("invalid_service_21");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 2);
        Object[] diagnostics = diagnosticResult.errors().toArray();
        for (Object obj : diagnostics) {
            Diagnostic diagnostic = (Diagnostic) obj;
            assertDiagnostic(diagnostic, INVALID_FUNCTION_PARAM_PAYLOAD);
        }
    }

    @Test(description = "Validate `rabbitmq:Service` no service name or service config annotation")
    public void testInvalidService22() {
        Package currentPackage = loadPackage("invalid_service_22");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errorCount(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        assertDiagnostic(diagnostic, NO_ANNOTATION);
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(BALLERINA_SOURCES).resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    private void assertDiagnostic(Diagnostic diagnostic, PluginConstants.CompilationErrors error) {
        Assert.assertEquals(diagnostic.diagnosticInfo().code(), error.getErrorCode());
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                error.getError());
    }
}
