/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.instrumentation.extensions.property

import com.google.testing.compile.Compilation
import org.gradle.internal.instrumentation.InstrumentationCodeGenTest

import static com.google.testing.compile.CompilationSubject.assertThat

class PropertyUpgradeCustomInterceptorCodeGenTest extends InstrumentationCodeGenTest {

    def "should generate bridge class for upgraded property with custom adapter"() {
        given:
        def givenSource = source """
            package org.gradle.test;

            import org.gradle.api.provider.Property;
            import org.gradle.internal.instrumentation.api.annotations.ReplacesEagerProperty;
            import org.gradle.internal.instrumentation.api.annotations.BytecodeUpgrade;

            public abstract class Task {
                @ReplacesEagerProperty(adapter = Task.GetMaxErrorsAdapter.class)
                public abstract Property<Integer> getMaxErrors();

                static class GetMaxErrorsAdapter {
                    @BytecodeUpgrade
                    static int maxErrors(Task task) {
                        return 0;
                    }

                    @BytecodeUpgrade
                    static int getMaxErrors(Task task) {
                        return 0;
                    }

                    @BytecodeUpgrade
                    static Task maxErrors(Task task, int maxErrors) {
                        return task;
                    }

                    @BytecodeUpgrade
                    static void setMaxErrors(Task task, int maxErrors) {
                    }
                }
            }
        """

        when:
        Compilation compilation = compile(givenSource)

        then:
        def expectedGeneratedClass = source """
             package org.gradle.test;
             import org.gradle.api.Generated;

             @Generated
             public final class \$\$BridgeFor\$\$Task\$\$GetMaxErrorsAdapter {
                 public static int access_get_getMaxErrors(Task task) {
                     ${getDefaultPropertyUpgradeDeprecation("Task", "maxErrors")}
                     return Task.GetMaxErrorsAdapter.getMaxErrors(task);
                 }
                 public static int access_get_maxErrors(Task task) {
                     ${getDefaultPropertyUpgradeDeprecation("Task", "maxErrors")}
                     return Task.GetMaxErrorsAdapter.maxErrors(task);
                 }
                 public static Task access_set_maxErrors(Task task, int maxErrors) {
                     ${getDefaultPropertyUpgradeDeprecation("Task", "maxErrors")}
                     return Task.GetMaxErrorsAdapter.maxErrors(task, maxErrors);
                 }
                 public static void access_set_setMaxErrors(Task task, int maxErrors) {
                     ${getDefaultPropertyUpgradeDeprecation("Task", "maxErrors")}
                     Task.GetMaxErrorsAdapter.setMaxErrors(task, maxErrors);
                 }
             }
        """
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile(fqName(expectedGeneratedClass))
            .containsElementsIn(expectedGeneratedClass)
    }

    def "should generate interceptor for custom adapter"() {
        given:
        def givenSource = source """
            package org.gradle.test;

            import org.gradle.api.provider.Property;
            import org.gradle.internal.instrumentation.api.annotations.ReplacesEagerProperty;
            import org.gradle.internal.instrumentation.api.annotations.BytecodeUpgrade;

            public abstract class Task {
                @ReplacesEagerProperty(adapter = Task.GetMaxErrorsAdapter.class)
                public abstract Property<Integer> getMaxErrors();

                static class GetMaxErrorsAdapter {
                    @BytecodeUpgrade
                    static int getMaxErrors(Task task) {
                        return 0;
                    }

                    @BytecodeUpgrade
                    static Task maxErrors(Task task, int maxErrors) {
                        return task;
                    }
                }
            }
        """

        when:
        Compilation compilation = compile(givenSource)

        then:
        def expectedGeneratedClass = source """
            package org.gradle.internal.classpath.generated;

            @Generated
            public class InterceptorDeclaration_PropertyUpgradesJvmBytecode_TestProject implements JvmBytecodeCallInterceptor, FilterableBytecodeInterceptor.BytecodeUpgradeInterceptor {
                 @Override
                 public boolean visitMethodInsn(String className, int opcode, String owner, String name,
                         String descriptor, boolean isInterface, Supplier<MethodNode> readMethodNode) {
                     if (metadata.isInstanceOf(owner, "org/gradle/test/Task")) {
                         if (name.equals("getMaxErrors") && descriptor.equals("()I") && (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE)) {
                             mv._INVOKESTATIC(\$\$_BRIDGE_FOR\$\$_TASK\$\$_GET_MAX_ERRORS_ADAPTER_TYPE, "access_get_getMaxErrors", "(Lorg/gradle/test/Task;)I");
                             return true;
                         }
                         if (name.equals("maxErrors") && descriptor.equals("(I)Lorg/gradle/test/Task;") && (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE)) {
                             mv._INVOKESTATIC(\$\$_BRIDGE_FOR\$\$_TASK\$\$_GET_MAX_ERRORS_ADAPTER_TYPE, "access_set_maxErrors", "(Lorg/gradle/test/Task;I)Lorg/gradle/test/Task;");
                             return true;
                         }
                     }
                     return false;
                 }
            }
        """
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile(fqName(expectedGeneratedClass))
            .containsElementsIn(expectedGeneratedClass)
    }

    def "should fail compilation if adapter and it's methods are not package-private"() {
        given:
        def givenSource = source """
            package org.gradle.test;

            import org.gradle.api.provider.Property;
            import org.gradle.internal.instrumentation.api.annotations.ReplacesEagerProperty;
            import org.gradle.internal.instrumentation.api.annotations.BytecodeUpgrade;

            public abstract class Task {
                @ReplacesEagerProperty(adapter = Task.GetMaxErrorsAdapter.class)
                public abstract Property<Integer> getMaxErrors();

                public static class GetMaxErrorsAdapter {
                    @BytecodeUpgrade
                    public static int firstMethod(Task task) {
                        return 0;
                    }

                    @BytecodeUpgrade
                    int secondMethod(Task task) {
                        return 0;
                    }

                    @BytecodeUpgrade
                    private Task thirdMethod(Task task, int maxErrors) {
                        return task;
                    }

                    @BytecodeUpgrade
                    static int forthMethod() {
                        return 0;
                    }

                    @BytecodeUpgrade
                    static int fifthMethod(int param) {
                        return 0;
                    }
                }
            }
        """

        when:
        Compilation compilation = compile(givenSource)

        then:
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation).hadErrorContaining("Adapter class 'org.gradle.test.Task.GetMaxErrorsAdapter' should be package private, but it's not.")
        assertThat(compilation).hadErrorContaining("Adapter method 'org.gradle.test.Task.GetMaxErrorsAdapter.firstMethod(org.gradle.test.Task)' should be package-private but it's not.")
        assertThat(compilation).hadErrorContaining("Adapter method 'org.gradle.test.Task.GetMaxErrorsAdapter.secondMethod(org.gradle.test.Task)' should be static but it's not.")
        assertThat(compilation).hadErrorContaining("Adapter method 'org.gradle.test.Task.GetMaxErrorsAdapter.thirdMethod(org.gradle.test.Task,int)' should be package-private but it's not.")
        assertThat(compilation).hadErrorContaining("Adapter method 'org.gradle.test.Task.GetMaxErrorsAdapter.thirdMethod(org.gradle.test.Task,int)' should be static but it's not.")
        assertThat(compilation).hadErrorContaining("'org.gradle.test.Task.GetMaxErrorsAdapter.forthMethod()' has no parameters, but it should have at least one of type 'org.gradle.test.Task'.")
        assertThat(compilation).hadErrorContaining("Adapter method 'org.gradle.test.Task.GetMaxErrorsAdapter.fifthMethod(int)' should have first parameter of type 'org.gradle.test.Task', but first parameter is of type 'int'.")
    }
}
