package app.pmsoft.graphwalker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.pmsoft.graphwalker.data.entity.Graph
import app.pmsoft.graphwalker.data.model.FullGraph
import app.pmsoft.graphwalker.repository.GraphRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GraphListViewModel(
    private val repository: GraphRepository
) : ViewModel() {
    
    val fullGraphs: StateFlow<List<FullGraph>> = repository.getAllFullGraphs()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewGraph() {
        viewModelScope.launch {
            val newGraph = Graph(name = "New Graph ${System.currentTimeMillis()}")
            repository.insertGraph(newGraph)
        }
    }

    fun deleteGraph(fullGraph: FullGraph) {
        viewModelScope.launch {
            val graph = Graph(id = fullGraph.id, name = fullGraph.name, startingNodeId = fullGraph.startingNode?.id)
            repository.deleteGraph(graph)
        }
    }
}