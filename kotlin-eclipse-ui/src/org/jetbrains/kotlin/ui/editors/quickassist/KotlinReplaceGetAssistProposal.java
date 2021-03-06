package org.jetbrains.kotlin.ui.editors.quickassist;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer;
import org.jetbrains.kotlin.eclipse.ui.utils.IndenterUtil;
import org.jetbrains.kotlin.psi.KtCallExpression;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSuperExpression;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinReplaceGetAssistProposal extends KotlinQuickAssistProposal {
    
    public KotlinReplaceGetAssistProposal(KotlinEditor editor) {
        super(editor);
    }

    @Override
    public void apply(@NotNull IDocument document, @NotNull PsiElement psiElement) {
        KtCallExpression callElement = PsiTreeUtil.getParentOfType(psiElement, KtCallExpression.class);
        if (callElement == null) {
            return;
        }
        
        KtQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(psiElement, KtQualifiedExpression.class);
        if (qualifiedExpression == null) {
            return; 
        }
        
        IFile file = getEditor().getEclipseFile();
        if (file == null) {
            KotlinLogger.logError("Failed to retrieve IFile from editor " + getEditor(), null);
            return;
        }

        String arguments = getArguments(qualifiedExpression, file);
        if (arguments == null) {
            return;
        }
        
        replaceGetForElement(qualifiedExpression, arguments);
    }
    
    private void replaceGetForElement(@NotNull KtQualifiedExpression element, @NotNull String arguments) {
        IDocument document = getEditor().getDocument();
        
        try {
            int textLength = element.getTextLength();
            if (TextUtilities.getDefaultLineDelimiter(document).length() > 1) {
                textLength += IndenterUtil.getLineSeparatorsOccurences(element.getText());
            }
            
            int startOffset = KotlinQuickAssistProposalKt.getStartOffset(element, getEditor());
            String receiverExpressionText = element.getReceiverExpression().getText();
            
            document.replace(
                    startOffset, 
                    textLength, 
                    receiverExpressionText + "[" + arguments + "]");
            
            getEditor().getJavaEditor().getViewer().getTextWidget().setCaretOffset(startOffset + receiverExpressionText.length());
        } catch (BadLocationException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Nullable
    private String getArguments(@NotNull KtQualifiedExpression element, @NotNull IFile file) {
        StringBuilder buffer = new StringBuilder();
        List<ValueArgument> valueArguments = getPositionalArguments(element, file);
        
        if (valueArguments == null) return null;
        
        boolean firstArgument = true;
        for (ValueArgument valueArgument : valueArguments) {
            if (!firstArgument) buffer.append(", ");
            firstArgument = false;
            
            KtExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression == null) continue;
            buffer.append(argumentExpression.getText());
        }
        
        return buffer.toString();
    }
    
    @Nullable
    public static List<ValueArgument> getPositionalArguments(@NotNull KtQualifiedExpression element, @NotNull IFile file) {
        ResolvedCall<?> resolvedCall = getResolvedCall(element, file);
        if (resolvedCall == null) return null;
        
        List<ResolvedValueArgument> resolvedValueArguments = resolvedCall.getValueArgumentsByIndex();
        if (resolvedValueArguments == null) return null;
        
        int indexOfFirstDefaultArgument = resolvedValueArguments.indexOf(DefaultValueArgument.DEFAULT);
        
        List<ResolvedValueArgument> valueArgumentGroups = new ArrayList<>();
        if (indexOfFirstDefaultArgument >= 0) {
            ListIterator<ResolvedValueArgument> iter = resolvedValueArguments.listIterator(indexOfFirstDefaultArgument);
            while (iter.hasNext()) {
                ResolvedValueArgument valueArgument = iter.next();
                if (valueArgument != DefaultValueArgument.DEFAULT) {
                    return null; 
                }
            }
            
            valueArgumentGroups = resolvedValueArguments.subList(0, indexOfFirstDefaultArgument);
        } else {
            valueArgumentGroups = resolvedValueArguments;
        }
        
        List<ValueArgument> valueArguments = new ArrayList<>();
        for (ResolvedValueArgument valueArgumentGroup : valueArgumentGroups) {
            for (ValueArgument va : valueArgumentGroup.getArguments()) {
                valueArguments.add(va);
            }
        }
        
        return valueArguments;
    }
    
    @Nullable
    private static ResolvedCall<?> getResolvedCall(@NotNull KtQualifiedExpression element, @NotNull IFile file) {
        KtExpression call = element.getSelectorExpression();
        if (!(call instanceof KtCallExpression)) return null;
        
        KtCallExpression jetCallExpression = (KtCallExpression) call; 
        
        BindingContext bindingContext = getBindingContext(element, file);
        
        return bindingContext != null ? 
                CallUtilKt.getResolvedCall(jetCallExpression.getCalleeExpression(), bindingContext) : null;
    }
    
    @Nullable
    private static BindingContext getBindingContext(@NotNull KtQualifiedExpression element, @NotNull IFile file) {
        KtExpression call = element.getSelectorExpression();
        if (!(call instanceof KtCallExpression)) return null;
        
        BindingContext bindingContext = KotlinAnalyzer.INSTANCE
                .analyzeFile(KotlinPsiManager.INSTANCE.getParsedFile(file))
                .getAnalysisResult()
                .getBindingContext();
        
        return bindingContext;
    }
    
    @Override
    public boolean isApplicable(@NotNull PsiElement psiElement) {
        KtCallExpression callExpression = PsiTreeUtil.getParentOfType(psiElement, KtCallExpression.class);
        if (callExpression == null) {
            return false;
        }
        
        KtExpression calleeExpression = callExpression.getCalleeExpression();

        if (calleeExpression == null) {
            return false;
        }
        
        List<KtValueArgument> valueArguments = callExpression.getValueArguments();
        if (valueArguments.isEmpty()) return false;
        
        for (KtValueArgument argument : valueArguments) {
            if (argument.isNamed()) {
                return false;
            }
        }
        
        KtQualifiedExpression qualifiedExpression = PsiTreeUtil.getParentOfType(psiElement, KtQualifiedExpression.class);
        if (qualifiedExpression != null && !isReceiverExpressionWithValue(qualifiedExpression)) {
            return false;
        }
        
        return "get".equals(calleeExpression.getText()) && 
                callExpression.getTypeArgumentList() == null &&
                valueArguments.size() > 0;
    }
    
    private boolean isReceiverExpressionWithValue(@NotNull KtQualifiedExpression expression) {
        KtExpression receiver = expression.getReceiverExpression();
        if (receiver instanceof KtSuperExpression) return false;
        
        IFile file = getEditor().getEclipseFile();
        if (file == null) return false;
        
        BindingContext bindingContext = getBindingContext(expression, file);
        if (bindingContext != null) {
            return bindingContext.getType(receiver) != null;
        }
        
        return false;
    }

    @NotNull
    @Override
    public String getDisplayString() {
        return "Replace 'get' with index operator";
    }
}