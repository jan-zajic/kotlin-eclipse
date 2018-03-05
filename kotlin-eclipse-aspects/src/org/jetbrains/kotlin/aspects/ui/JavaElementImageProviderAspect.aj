package org.jetbrains.kotlin.aspects.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;

@SuppressWarnings("restriction")
public aspect JavaElementImageProviderAspect {
    
    pointcut getBaseImageDescriptor(IJavaElement element, int renderFlags) : target(JavaElementImageProvider) && args(element, renderFlags) && call(ImageDescriptor getBaseImageDescriptor(IJavaElement, int));
    
    ImageDescriptor around(IJavaElement element, int renderFlags) : getBaseImageDescriptor(element, renderFlags) {        
        switch (element.getElementType()) {
            case IJavaElement.PACKAGE_FRAGMENT:
                IPackageFragment fragment = (IPackageFragment)element;
                boolean containsJavaElements= false;
                try {
                    containsJavaElements = fragment.hasChildren();
                } catch(JavaModelException e) {
                    // assuming no children;
                }
                if(!containsJavaElements) {
                    try {
                        Object[] nonJavaResources = fragment.getNonJavaResources();
                        if(nonJavaResources.length > 0) {
                            for (Object object : nonJavaResources) {
                                if(object instanceof IFile) {
                                    if(KotlinPsiManager.isKotlinFile((IFile)object))
                                        return JavaPluginImages.DESC_OBJS_PACKAGE;
                                }
                            }                        
                            return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE_RESOURCES;
                        } else {
                            return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE;
                        }
                    } catch(JavaModelException e) {
                        // assuming no children;
                        return JavaPluginImages.DESC_OBJS_EMPTY_PACKAGE;
                    }
                } else {
                    return JavaPluginImages.DESC_OBJS_PACKAGE;
                }               
            default:
                return proceed(element, renderFlags);
        }
    }
    
}