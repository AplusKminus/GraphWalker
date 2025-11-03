package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.pmsoft.graphwalker.repository.GraphRepository

class GraphListViewModelFactory(
    private val repository: GraphRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GraphListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GraphListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}