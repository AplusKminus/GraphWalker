package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graphs")
data class Graph(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startingNodeId: Long? = null,
    val isDirected: Boolean = true,
    val hasEdgeWeights: Boolean = false,
    val hasEdgeLabels: Boolean = false,
    val hasConnectors: Boolean = false,
)