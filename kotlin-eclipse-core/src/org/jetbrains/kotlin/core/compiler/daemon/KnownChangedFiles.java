package org.jetbrains.kotlin.core.compiler.daemon;

import java.io.File;
import java.util.List;

public class KnownChangedFiles {
    
    private List<File> modifiedFiles;
    private List<File> deletedFiles;
    
    public List<File> getModifiedFiles() {
        return modifiedFiles;
    }
    public void setModifiedFiles(List<File> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }
    public List<File> getDeletedFiles() {
        return deletedFiles;
    }
    public void setDeletedFiles(List<File> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }
    
}
