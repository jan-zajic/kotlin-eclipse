/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
 *
 *******************************************************************************/
package org.jetbrains.kotlin.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jetbrains.kotlin.core.CorePreferences;

public class CompilerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    
    public CompilerPreferencePage() {
        super();
    }

    public CompilerPreferencePage(int style) {
        super(style);
    }

    public CompilerPreferencePage(String title, ImageDescriptor image, int style) {
        super(title, image, style);
    }

    public CompilerPreferencePage(String title, int style) {
        super(title, style);
    }

    @Override
    public void init(IWorkbench workbench) {
        // Set the preference store for the preference page.
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, org.jetbrains.kotlin.core.Activator.PLUGIN_ID));
    }

    @Override
    protected void createFieldEditors() {
        BooleanFieldEditor incrementalCompilation = new BooleanFieldEditor(CorePreferences.INCREMENTAL_COMPILATION, "Incremental compilation", getFieldEditorParent());
        addField(incrementalCompilation);
        BooleanFieldEditor buildServer = new BooleanFieldEditor(CorePreferences.BUILD_DAEMON, "Use build daemon", getFieldEditorParent());
        addField(buildServer);
    }
    
}
