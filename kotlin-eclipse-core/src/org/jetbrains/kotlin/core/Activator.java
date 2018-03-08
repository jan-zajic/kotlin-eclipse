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
package org.jetbrains.kotlin.core;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.jetbrains.kotlin.core.compiler.daemon.EclipseKotlinCompilerDaemon;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.core.model.KotlinRefreshProjectListener;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {
    
	private static Activator plugin;
	
	public static final String PLUGIN_ID = "org.jetbrains.kotlin.core";

    private final IEclipsePreferences corePreferences;

	public Activator() {
	    plugin = this;
	    corePreferences = InstanceScope.INSTANCE.getNode(PLUGIN_ID);
	}
	
	public static Activator getDefault() {
		return plugin;
	}

	@Override
    public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		
		ResourcesPlugin.getWorkspace().addResourceChangeListener(KotlinAnalysisProjectCache.INSTANCE,
		        IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_BUILD);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(KotlinRefreshProjectListener.INSTANCE,
		        IResourceChangeEvent.PRE_REFRESH);
		boolean startBuildDaemon = corePreferences.getBoolean(CorePreferences.BUILD_DAEMON, false);
		boolean extDaemon = corePreferences.getBoolean(CorePreferences.EXTERNAL_DAEMON_PROCESS, false);
		
		if(startBuildDaemon)
		    EclipseKotlinCompilerDaemon.INSTANCE.start(extDaemon);
	}

	@Override
    public void stop(BundleContext bundleContext) throws Exception {
	    ResourcesPlugin.getWorkspace().removeResourceChangeListener(KotlinAnalysisProjectCache.INSTANCE);
	    ResourcesPlugin.getWorkspace().removeResourceChangeListener(KotlinRefreshProjectListener.INSTANCE);
	    EclipseKotlinCompilerDaemon.INSTANCE.stop();
		plugin = null;
	}
}
