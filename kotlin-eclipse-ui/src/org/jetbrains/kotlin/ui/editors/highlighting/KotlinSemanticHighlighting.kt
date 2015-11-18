/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.highlighting

import org.jetbrains.kotlin.ui.editors.KotlinReconcilingListener
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jface.preference.IPreferenceStore
import org.jetbrains.kotlin.ui.editors.Configuration.KotlinPresentationReconciler
import org.eclipse.jface.text.ITextPresentationListener
import org.eclipse.jface.text.TextPresentation
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.jface.text.IPositionUpdater
import org.eclipse.jface.text.DocumentEvent
import org.eclipse.swt.custom.StyleRange
import org.eclipse.jface.text.Position
import org.eclipse.swt.graphics.TextStyle
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.Region
import org.eclipse.swt.graphics.RGB
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.widgets.Display
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.eclipse.swt.SWT
import org.jetbrains.kotlin.ui.editors.highlighting.HighlightPosition.StyleAttributes
import org.jetbrains.kotlin.ui.editors.highlighting.HighlightPosition.SmartCast
import org.eclipse.jface.text.source.Annotation
import org.jetbrains.kotlin.ui.editors.annotations.AnnotationManager
import org.jetbrains.kotlin.ui.editors.annotations.withLock
import org.eclipse.jface.text.source.IAnnotationModelExtension
import org.jetbrains.kotlin.ui.editors.KotlinReconcilingStrategy
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent

private val SMART_CAST_ANNOTATION_TYPE = "org.jetbrains.kotlin.ui.annotation.smartCast"

public class KotlinSemanticHighlighter(
        val preferenceStore: IPreferenceStore, 
        val colorManager: IColorManager,
        val presentationReconciler: KotlinPresentationReconciler,
        val editor: KotlinFileEditor) : KotlinReconcilingListener, ITextPresentationListener, IPropertyChangeListener {
    
    private val positionUpdater by lazy { KotlinPositionUpdater(category) }
    
    private val category by lazy { toString() }
    
    override fun applyTextPresentation(textPresentation: TextPresentation) {
        val region = textPresentation.getExtent()
        val regionStart = region.getOffset()
        val regionEnd = regionStart + region.getLength()
        
        editor.document.getPositions(category)
            .filter { regionStart <= it.getOffset() && it.getOffset() + it.getLength() <= regionEnd }
            .filterNot { it.isDeleted() }
            .forEach { position ->
                when (position) {
                    is StyleAttributes -> {
                        val styleRange = position.createStyleRange()
                        
                        textPresentation.replaceStyleRange(styleRange)
                    }
                }
            }
    }

    override fun reconcile(file: IFile, editor: KotlinFileEditor) {
        removeAllPositions()
        
        val ktFile = editor.parsedFile
        if (ktFile == null) return
        
        val javaProject = editor.javaProject
        if (javaProject == null) return
        
        val highlightingVisitor = KotlinSemanticHighlightingVisitor(ktFile, editor.document, javaProject)
        val smartCasts = arrayListOf<SmartCast>()
        highlightingVisitor.computeHighlightingRanges().forEach { position -> 
            when (position) {
                is StyleAttributes -> editor.document.addPosition(category, position)
                is SmartCast -> smartCasts.add(position)
            }
            
        }
        
        invalidateTextPresentation()
        setupSmartCastsAsAnnotations(smartCasts)
    }
    
    override fun propertyChange(event: PropertyChangeEvent) {
        if (event.getProperty().startsWith(PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX)) {
            editor.getFile()?.let { reconcile(it, editor) }
        }
    }
    
    fun install() {
        val viewer = editor.getViewer()
        val file = editor.getFile()
        if (file != null && viewer is JavaSourceViewer) {
            viewer.prependTextPresentationListener(this)
            
            with(editor.document) {
                addPositionCategory(category)
                addPositionUpdater(positionUpdater)
            }
            
            preferenceStore.addPropertyChangeListener(this)
            
            reconcile(file, editor)
        } else {
            KotlinLogger.logWarning("Cannot install Kotlin Semantic highlighter for viewer $viewer")
        }
    }
    
    fun uninstall() {
        val viewer = editor.getViewer()
        if (viewer is JavaSourceViewer) {
            viewer.removeTextPresentationListener(this)
            
            with(editor.document) {
                removePositionCategory(category)
                removePositionUpdater(positionUpdater)
            }
            
            preferenceStore.removePropertyChangeListener(this)
        }
    }
    
    private fun setupSmartCastsAsAnnotations(positions: List<SmartCast>) {
        val annotationMap = positions.toMapBy { 
            Annotation(SMART_CAST_ANNOTATION_TYPE, false, "Smart cast to ${it.typeName}")
        }
        
        AnnotationManager.updateAnnotations(editor, annotationMap, SMART_CAST_ANNOTATION_TYPE)
    }
    
    private fun invalidateTextPresentation() {
        val shell = editor.getSite()?.getShell()
        if (shell == null || shell.isDisposed()) return
        
        val display = shell.getDisplay()
        if (display == null || display.isDisposed()) return
        
        display.asyncExec {
            editor.getViewer()?.invalidateTextPresentation()
        }
    }
    
    private fun removeAllPositions() {
        editor.document.getPositions(category).forEach { it.delete() }
    }
    
    private fun StyleAttributes.createStyleRange(): StyleRange {
        val styleKey = styleAttributes.styleKey
        if (!isEnabled(styleKey, preferenceStore)) {
            return createStyleRange(getOffset(), getLength())
        }
        
        val textStyle = findTextStyle(styleAttributes, preferenceStore, colorManager)
        return with(StyleRange(textStyle)) {
            start = getOffset()
            length = getLength()
            
            fontStyle = SWT.NORMAL
            if (isBold(styleKey, preferenceStore)) fontStyle = fontStyle or SWT.BOLD
            if (isItalic(styleAttributes.styleKey, preferenceStore)) fontStyle = fontStyle or SWT.ITALIC
            
            this
        }
    }
    
    private class KotlinPositionUpdater(val category: String): IPositionUpdater {
        override fun update(event: DocumentEvent) {
            val editionOffset = event.getOffset()
            val editionLength = event.getLength()
            val editionEnd = editionOffset + editionLength
            val newText = event.getText()
            
            val newLength = newText?.length ?: 0
            
            for (position in event.getDocument().getPositions(category)) {
                val posOffset = position.getOffset()
                val posEnd = posOffset + position.getLength()
                if (editionOffset <= posEnd) {
                    if (editionEnd < posOffset) {
                        val delta = newLength - editionLength
                        position.setOffset(posOffset + delta)
                    } else {
                        position.delete()
                    }
                }
            }
        }
    }
}

private fun findTextStyle(attributes: KotlinHighlightingAttributes, store: IPreferenceStore, colorManager: IColorManager): TextStyle {
    val style = TextStyle()
    val rgb = getColor(attributes.styleKey, store)
    style.foreground = getColor(rgb, colorManager)
    style.underline = attributes.underline
    
    return style
}

private fun getColor(rgb: RGB?, colorManager: IColorManager): Color? {
    var color: Color? = null
    Display.getDefault().syncExec {
        color = colorManager.getColor(rgb)
    }
    
    return color
}

private fun getColor(key: String, store: IPreferenceStore): RGB {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX
    
    return PreferenceConverter.getColor(store, preferenceKey)
}

private fun isBold(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun isItalic(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun isEnabled(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX
    
    return store.getBoolean(preferenceKey)
}

private fun createStyleRange(s: Int, l: Int) = with(StyleRange()) {
    start = s
    length = l
    
    this
}