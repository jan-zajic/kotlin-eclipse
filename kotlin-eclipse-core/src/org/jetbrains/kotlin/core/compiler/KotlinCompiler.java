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
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.config.Services;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironmentKt;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.incremental.eclipse.IncrementalEclipseJvmCompilerRunnerKt;

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;

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
        System.setProperty("java.awt.headless", "true");
        K2JVMCompilerArguments commandLineArguments = configureCompilerArguments(javaProject, arguments,
                outputFolder.getLocation().toOSString());        
        if (arguments.isIncremental()) {
            return runIncrementalCompiler(javaProject, arguments.getCachesDir(), commandLineArguments);
        } else {
            return execKotlinCompiler(commandLineArguments);
        }
    }
    
    private KotlinCompilerResult runIncrementalCompiler(IJavaProject javaProject, File cachesDir, K2JVMCompilerArguments commandLineArguments) throws CoreException {
        KotlinLogger.logWarning("Using experimental Kotlin incremental compilation");
        EclipseICReporter icReporter = EclipseICReporter.get(javaProject, EclipseICReporter.IC_LOG_LEVEL_WARNING);
        EclipseMessageCollector messageCollector = new EclipseMessageCollector();
        String destination = commandLineArguments.getDestination();
        File classesDir = new File(destination);
        File kotlinClassesDir = new File(cachesDir, "classes");        
        File snapshotsFile = new File(cachesDir, "snapshots.bin");
        ExtensionsArea rootArea = Extensions.getRootArea();
        try {
            commandLineArguments.setDestination(kotlinClassesDir.getAbsolutePath());
            //https://github.com/JetBrains/kotlin/blob/1dadf84c40ccc30b8312f40194d1a13b6da1d203/libraries/tools/kotlin-maven-plugin/src/main/java/org/jetbrains/kotlin/maven/K2JVMCompileMojo.java
            IncrementalEclipseJvmCompilerRunnerKt.makeEclipseIncrementally(cachesDir, getSourceRoots(javaProject), commandLineArguments, messageCollector, icReporter);            
            KotlinLogger.logInfo("Compiled " + icReporter.getCompiledKotlinFiles().size() + " Kotlin files using incremental compiler");
        } catch (Throwable t) {
            t.printStackTrace();
            return new KotlinCompilerResult(ExitCode.INTERNAL_ERROR, messageCollector.getCompilerOutput());
        }
        
        ExitCode exitCode;
        if (messageCollector.hasErrors()) {
            exitCode =  ExitCode.COMPILATION_ERROR;
        } else {
            (new FileCopier()).syncDirs(kotlinClassesDir, classesDir, snapshotsFile);
            exitCode = ExitCode.OK;
        }
        return new KotlinCompilerResult(exitCode, messageCollector.getCompilerOutput());
    }
    
    private Iterable<? extends File> getSourceRoots(IJavaProject javaProject) throws CoreException {
        List<File> sourceList = new ArrayList<>();
        for (IClasspathEntry classpathEntry : javaProject.getResolvedClasspath(true)) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                sourceList.addAll(ProjectUtils.getFileByEntry(classpathEntry, javaProject));
            }
        }
        return sourceList;
    }

    public KotlinCompilerResult execKotlinCompiler(K2JVMCompilerArguments arguments) {
        EclipseMessageCollector messageCollector = new EclipseMessageCollector();
        ExitCode exitCode = execKotlinCompiler(messageCollector, arguments);
        return new KotlinCompilerResult(exitCode, messageCollector.getCompilerOutput());
    }
    
    public ExitCode execKotlinCompiler(MessageCollector messageCollector, K2JVMCompilerArguments arguments) {
        return new K2JVMCompiler().exec(messageCollector, Services.EMPTY, arguments);
    }
    
    @NotNull
    private K2JVMCompilerArguments configureCompilerArguments(@NotNull IJavaProject javaProject,
            @NotNull KotlinCompilerArguments arguments, @NotNull String outputDir) throws CoreException {
        K2JVMCompilerArguments args = new K2JVMCompilerArguments();
        // see K2JVMCompilerArguments.java
        args.setKotlinHome(ProjectUtils.KT_HOME);
        args.setNoJdk(true);
        args.setNoStdlib(true); // Because we add runtime into the classpath
        args.setModuleName("");
        
        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");
        
        if (arguments.launch) {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForLaunch(javaProject)) {
                classPath.append(file.getAbsolutePath()).append(pathSeparator);
            }
        } else if(arguments.incremental) {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForIncrementalBuild(javaProject)) {
                classPath.append(file.getAbsolutePath()).append(pathSeparator);
            }
        } else {
            for (File file : ProjectUtils.collectClasspathWithDependenciesForFullBuild(javaProject)) {
                classPath.append(file.getAbsolutePath()).append(pathSeparator);
            }
        }
        
        args.setClasspath(classPath.toString());
        args.setDestination(outputDir);
        
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            args.getFreeArgs().add(srcDirectory.getAbsolutePath());
        }
        
        args.setIntellijPluginRoot(KotlinEnvironmentKt.getKOTLIN_COMPILER_PATH());        
        
        return args;
    }
    
    public static class KotlinCompilerResult {
        public static KotlinCompilerResult EMPTY = new KotlinCompilerResult(ExitCode.INTERNAL_ERROR,
                new CompilerOutputData());
        
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
        private boolean incremental = false;
        private File cachesDir;
        
        private KotlinCompilerArguments() {
        }
        
        public File getCachesDir() {
            return cachesDir;
        }

        public boolean isLaunch() {
            return launch;
        }
        
        public boolean isIncremental() {
            return incremental;
        }
        
        public static KotlinCompilerArguments run() {
            return new KotlinCompilerArguments();
        }
        
        public static KotlinCompilerArguments fullBuild() {
            KotlinCompilerArguments args = new KotlinCompilerArguments();
            args.launch = false;
            args.incremental = false;
            return args;
        }
        
        public static KotlinCompilerArguments incrementalBuild(File cachesDir) {
            KotlinCompilerArguments args = new KotlinCompilerArguments();
            args.launch = false;
            args.incremental = true;
            args.cachesDir = cachesDir;
            return args;
        }
        
    }
    
}
