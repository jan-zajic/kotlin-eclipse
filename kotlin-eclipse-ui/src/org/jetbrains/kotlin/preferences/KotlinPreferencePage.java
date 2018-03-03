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

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jetbrains.kotlin.ui.Activator;

public class KotlinPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String SHOW_COMPILER_CONSOLE = "showConsole";

    public KotlinPreferencePage() {
    }

    public KotlinPreferencePage(int style) {
        super(style);
    }

    public KotlinPreferencePage(String title, ImageDescriptor image, int style) {
        super(title, image, style);
    }
    
    public KotlinPreferencePage(String title, int style) {
        super(title, style);
    }

    @Override
    public void init(IWorkbench workbench) {
     // Set the preference store for the preference page. 
     setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        ComboFieldEditor consoleEditor = new ComboFieldEditor(SHOW_COMPILER_CONSOLE, "Show console", createConsoleOptions(), getFieldEditorParent());
        addField(consoleEditor);
    }

    private String[][] createConsoleOptions() {
        String[][] options = new String[ConsoleMode.values().length][2];
        for (int i = 0; i < ConsoleMode.values().length; i++) {
            ConsoleMode consoleMode = ConsoleMode.values()[i];
            options[i][0] = consoleMode.name();
            options[i][1] = consoleMode.name();
        }
        return options;
    }

    @Override
    protected void performDefaults() {
        //see KotlinPreferenceInitializer
        super.performDefaults();        
    }
    
}
