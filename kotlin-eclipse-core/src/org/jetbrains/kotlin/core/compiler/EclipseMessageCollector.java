package org.jetbrains.kotlin.core.compiler;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;

public class EclipseMessageCollector implements MessageCollector {
    
    private boolean hasErrors = false;
    
    final CompilerOutputData compilerOutput = new CompilerOutputData();
    final List<CompilerMessageSeverity> severities = new ArrayList<CompilerMessageSeverity>();
    
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
    
    public CompilerOutputData getCompilerOutput() {
        return compilerOutput;
    }
    
    public List<CompilerMessageSeverity> getSeverities() {
        return severities;
    }
    
}
