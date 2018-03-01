/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.jetbrains.kotlin.core.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class KotlinCompiler {
    public final static KotlinCompiler INSTANCE = new KotlinCompiler();
    
    private KotlinCompiler() {
    }
    
    @NotNull
    public KotlinCompilerResult compileKotlinFiles(@NotNull IJavaProject javaProject,
            @NotNull KotlinCompilerArguments arguments) throws CoreException {
        IFolder outputFolder = ProjectUtils.getOutputFolder(javaProject);
        if (outputFolder == null) {
            KotlinLogger.logError("There is no output folder for project: " + javaProject, null);
            return KotlinCompilerResult.EMPTY;
        }
        
        K2JVMCompilerArguments commandLineArguments = configureCompilerArguments(javaProject, arguments,
                outputFolder.getLocation().toOSString());
        
        return execKotlinCompiler(commandLineArguments);
    }
    
    public KotlinCompilerResult execKotlinCompiler(K2JVMCompilerArguments arguments) {
        final CompilerOutputData compilerOutput = new CompilerOutputData();
        final List<CompilerMessageSeverity> severities = new ArrayList<CompilerMessageSeverity>();
        
        ExitCode exitCode = execKotlinCompiler(new MessageCollector() {
            private boolean hasErrors = false;
            
            @Override
            public void report(@NotNull CompilerMessageSeverity messageSeverity, @NotNull String message,
                    @Nullable CompilerMessageLocation messageLocation) {
                hasErrors = hasErrors || messageSeverity.isError();
                severities.add(messageSeverity);
                compilerOutput.add(messageSeverity, message, messageLocation);
            }
            
            @Override
            public boolean hasErrors() {
                return hasErrors;
            }
            
            @Override
            public void clear() {
                hasErrors = false;
                
            }
        }, arguments);
        
        return new KotlinCompilerResult(exitCode, compilerOutput);
    }
    
    public ExitCode execKotlinCompiler(MessageCollector messageCollector, K2JVMCompilerArguments arguments) {
        return new K2JVMCompiler().exec(messageCollector, Services.EMPTY, arguments);
    }
    
    @NotNull
    private K2JVMCompilerArguments configureCompilerArguments(@NotNull IJavaProject javaProject,
            @NotNull KotlinCompilerArguments arguments, @NotNull String outputDir) throws CoreException {
        K2JVMCompilerArguments command = new K2JVMCompilerArguments();
        // see K2JVMCompilerArguments.java
        command.setKotlinHome(ProjectUtils.KT_HOME);
        command.setNoJdk(true);
        command.setNoStdlib(true); // Because we add runtime into the classpath
        
        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");
        
        if (arguments.launch) {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject)) {
                classPath.append(file.getAbsolutePath()).append(pathSeparator);
            }
        } else {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForFullBuild(javaProject)) {
                classPath.append(file.getAbsolutePath()).append(pathSeparator);
            }
        }
        
        command.setClasspath(classPath.toString());
        command.setDestination(outputDir);
        
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            command.getFreeArgs().add(srcDirectory.getAbsolutePath());
        }
        
        return command;
    }
  
    public static class KotlinCompilerResult {
        public static KotlinCompilerResult EMPTY = new KotlinCompilerResult(ExitCode.INTERNAL_ERROR, new CompilerOutputData());
        
        private final ExitCode result;
        private final CompilerOutputData compilerOutput;
        
        private KotlinCompilerResult(ExitCode exitCode, @NotNull CompilerOutputData compilerOutput) {
            this.result = exitCode;
            this.compilerOutput = compilerOutput;
        }
        
        public boolean compiledCorrectly() {
            return result == ExitCode.OK;
        }
        
        @NotNull
        public CompilerOutputData getCompilerOutput() {
            return compilerOutput;
        }
    }
    
    public static class KotlinCompilerArguments {
        private boolean launch = true;
                
        private KotlinCompilerArguments() {
        }
        
        public boolean isLaunch() {
            return launch;
        }
        
        public static KotlinCompilerArguments run() {
            return new KotlinCompilerArguments();
        }
        
        public static KotlinCompilerArguments fullBuild() {
            KotlinCompilerArguments args = new KotlinCompilerArguments();
            args.launch = false;
            return args;
        }
       
    }
    
}
