package com.inialpha.executiveai.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.inialpha.executiveai.ExecutiveAIApplication
import com.inialpha.executiveai.di.AppContainer

/**
 * Every screen calls `rememberViewModelFactory { DashboardViewModel(it) }`-style helpers below to
 * get an [AppContainer]-backed ViewModel without a DI framework. Kept in one file so the
 * boilerplate for wiring a new screen's ViewModel is always identical.
 */
@Composable
fun executiveAIContainer(): AppContainer =
    (LocalContext.current.applicationContext as ExecutiveAIApplication).container

inline fun <reified VM : ViewModel> containerViewModelFactory(
    container: AppContainer,
    crossinline create: (AppContainer) -> VM,
) = viewModelFactory {
    initializer { create(container) }
}
