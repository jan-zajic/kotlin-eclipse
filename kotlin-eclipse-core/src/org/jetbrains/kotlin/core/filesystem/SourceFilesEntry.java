package org.jetbrains.kotlin.core.filesystem;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;

public class SourceFilesEntry implements Iterable<IFile> {
    
    Set<IFile> sourceFiles;
    boolean alias;
    
    public SourceFilesEntry() {
        this(new HashSet<>(), false);
    }
    
    public SourceFilesEntry(SourceFilesEntry wrapped) {
        this(wrapped.getSourceFiles(), true);
    }
    
    private SourceFilesEntry(Set<IFile> sourceFiles, boolean alias) {
        super();
        this.sourceFiles = sourceFiles;
        this.alias = alias;
    }

    public Set<IFile> getSourceFiles() {
        return sourceFiles;
    }

    public boolean isAlias() {
        return alias;
    }

    public void add(IFile sourceFile) {
        this.sourceFiles.add(sourceFile);
    }

    @Override
    public Iterator<IFile> iterator() {
        return sourceFiles.iterator();
    }

    public boolean isEmpty() {
        return sourceFiles.isEmpty();
    }
    
}
