package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.pmsoft.graphwalker.repository.GraphRepository

class NodeViewModelFactory(
    private val repository: GraphRepository,
    private val graphId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NodeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NodeViewModel(repository, graphId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}