package org.jetbrains.kotlin.core

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.core.runtime.preferences.InstanceScope
import org.eclipse.ui.preferences.ScopedPreferenceStore

class KotlinCorePreferenceInitializer : AbstractPreferenceInitializer() {
    override fun initializeDefaultPreferences() {
        val kotlinStore = ScopedPreferenceStore(InstanceScope.INSTANCE, org.jetbrains.kotlin.core.Activator.PLUGIN_ID)
        with(kotlinStore) {
			setDefault(CorePreferences.INCREMENTAL_COMPILATION, true)
			setDefault(CorePreferences.BUILD_DAEMON, false)
			setDefault(CorePreferences.EXTERNAL_DAEMON_PROCESS, false)
        }
    }
}