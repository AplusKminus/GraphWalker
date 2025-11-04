package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.pmsoft.graphwalker.data.entity.Clique
import app.pmsoft.graphwalker.data.entity.CliqueNodeCrossRef
import app.pmsoft.graphwalker.data.entity.Graph
import app.pmsoft.graphwalker.data.model.CliqueWithNodes
import app.pmsoft.graphwalker.repository.GraphRepository

class CliqueViewModel(
    private val repository: GraphRepository,
    private val graphId: Long
) : ViewModel() {
    
    val cliques = repository.getCliquesWithNodesByGraphId(graphId)
    val graph = repository.getGraphById(graphId)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun createClique(name: String, edgeWeight: Double = 1.0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val clique = Clique(
                    graphId = graphId,
                    name = name,
                    edgeWeight = edgeWeight
                )
                repository.insertClique(clique)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteClique(clique: Clique) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteClique(clique)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateClique(clique: Clique) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateClique(clique)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addNodeToClique(cliqueId: Long, nodeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val crossRef = CliqueNodeCrossRef(cliqueId, nodeId)
                repository.insertCliqueNodeCrossRef(crossRef)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun removeNodeFromClique(cliqueId: Long, nodeId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.removeNodeFromClique(cliqueId, nodeId)
            } finally {
                _isLoading.value = false
            }
        }
    }
}