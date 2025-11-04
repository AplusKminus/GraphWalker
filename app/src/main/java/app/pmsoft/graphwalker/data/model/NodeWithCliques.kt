package app.pmsoft.graphwalker.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import app.pmsoft.graphwalker.data.entity.Clique
import app.pmsoft.graphwalker.data.entity.CliqueNodeCrossRef
import app.pmsoft.graphwalker.data.entity.Node

data class NodeWithCliques(
    @Embedded val node: Node,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(CliqueNodeCrossRef::class, parentColumn = "nodeId", entityColumn = "cliqueId")
    )
    val cliques: List<Clique>
)