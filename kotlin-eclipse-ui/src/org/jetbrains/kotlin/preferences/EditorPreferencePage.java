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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jetbrains.kotlin.ui.Activator;

public class EditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String MARK_OCCURRENCES = "markOccurrences";
    
    public EditorPreferencePage() {
        super();
    }

    public EditorPreferencePage(int style) {
        super(style);
    }

    public EditorPreferencePage(String title, ImageDescriptor image, int style) {
        super(title, image, style);
    }

    public EditorPreferencePage(String title, int style) {
        super(title, style);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        BooleanFieldEditor consoleEditor = new BooleanFieldEditor(MARK_OCCURRENCES, "Mark Occurrences", getFieldEditorParent());
        addField(consoleEditor);
    }



}
