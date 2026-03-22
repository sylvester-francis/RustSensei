package com.sylvester.rustsensei.data

import com.sylvester.rustsensei.llm.InferenceConfig

/**
 * Provides the user's saved inference configuration.
 *
 * Extracted from [PreferencesManager] so that ViewModels can depend
 * on a narrow interface instead of the full preferences surface,
 * following the Interface Segregation Principle.
 */
interface InferenceConfigProvider {
    fun loadInferenceConfig(): InferenceConfig
}
