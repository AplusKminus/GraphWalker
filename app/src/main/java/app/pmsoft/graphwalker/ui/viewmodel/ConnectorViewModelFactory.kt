package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.pmsoft.graphwalker.repository.GraphRepository

class ConnectorViewModelFactory(
    private val repository: GraphRepository,
    private val connectorId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConnectorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConnectorViewModel(repository, connectorId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}