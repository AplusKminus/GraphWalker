package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.pmsoft.graphwalker.repository.GraphRepository

class CliqueViewModelFactory(
    private val repository: GraphRepository,
    private val graphId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CliqueViewModel::class.java)) {
            return CliqueViewModel(repository, graphId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}