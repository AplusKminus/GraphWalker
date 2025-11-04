package app.pmsoft.graphwalker.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.pmsoft.graphwalker.data.entity.Clique
import app.pmsoft.graphwalker.data.entity.CliqueNodeCrossRef
import app.pmsoft.graphwalker.data.entity.Node

data class CliqueWithNodes(
    @Embedded val clique: Clique,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(CliqueNodeCrossRef::class, parentColumn = "cliqueId", entityColumn = "nodeId")
    )
    val nodes: List<Node>
)