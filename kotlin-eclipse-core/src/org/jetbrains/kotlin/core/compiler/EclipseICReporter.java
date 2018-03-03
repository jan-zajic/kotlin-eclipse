package org.jetbrains.kotlin.core.compiler;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.incremental.ICReporter;

import kotlin.jvm.functions.Function0;

public class EclipseICReporter implements ICReporter {
    
    public static final String IC_LOG_LEVEL_INFO = "info";
    public static final String IC_LOG_LEVEL_WARNING = "warn";

    public static EclipseICReporter get(@NotNull IJavaProject javaProject, @NotNull String logLevel) {      
        if (logLevel.equalsIgnoreCase(IC_LOG_LEVEL_INFO)) {
            return EclipseICReporter.info(javaProject);
        }
        else if (logLevel.equalsIgnoreCase(IC_LOG_LEVEL_WARNING)) {
            return EclipseICReporter.warning(javaProject);
        }
        else {
            return EclipseICReporter.noLog(javaProject);
        }
    }

    private static EclipseICReporter info(IJavaProject javaProject) {
        return new EclipseICReporter(javaProject) {
            @Override
            protected boolean isLogEnabled() {
                return true;
            }

            @Override
            protected void log(String str) {
                KotlinLogger.logInfo(str);
            }
        };
    }

    private static EclipseICReporter warning(IJavaProject javaProject) {
        return new EclipseICReporter(javaProject) {
            @Override
            protected boolean isLogEnabled() {
                return true;
            }

            @Override
            protected void log(String str) {
                KotlinLogger.logWarning(str);
            }
        };
    }

    private static EclipseICReporter noLog(IJavaProject javaProject) {
        return new EclipseICReporter(javaProject);
    }

    @NotNull
    private final Set<File> compiledKotlinFiles = new HashSet<>();
    private final IJavaProject javaProject;

    protected boolean isLogEnabled() {
        return false;
    }

    protected void log(String str) {
    }

    private EclipseICReporter(IJavaProject javaProject) {
        this.javaProject = javaProject;
    }

    @Override
    public void report(Function0<String> getMessage) {
        if (isLogEnabled()) {
            log(getMessage.invoke());
        }
    }

    @Override
    public void reportCompileIteration(Collection<? extends File> sourceFiles, ExitCode exitCode) {
        compiledKotlinFiles.addAll(sourceFiles);
        if (isLogEnabled()) {
            log("Kotlin compile iteration: " + pathsAsString(sourceFiles));
            log("Exit code: " + exitCode.toString());
        }
    }

    @NotNull
    @Override
    public String pathsAsString(Iterable<? extends File> files) {
        return ICReporter.DefaultImpls.pathsAsString(this, files);
    }

    @NotNull
    @Override
    public String pathsAsString(File... files) {
        return ICReporter.DefaultImpls.pathsAsString(this, files);
    }

    @NotNull
    public Set<File> getCompiledKotlinFiles() {
        return compiledKotlinFiles;
    }
    
    public IJavaProject getJavaProject() {
        return javaProject;
    }
    
    public IProject getProject() {
        return javaProject.getProject();
    }
    
}
