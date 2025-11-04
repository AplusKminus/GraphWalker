package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "clique_node_cross_ref",
    primaryKeys = ["cliqueId", "nodeId"],
    foreignKeys = [
        ForeignKey(
            entity = Clique::class,
            parentColumns = ["id"],
            childColumns = ["cliqueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Node::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cliqueId"]),
        Index(value = ["nodeId"])
    ]
)
data class CliqueNodeCrossRef(
    val cliqueId: Long,
    val nodeId: Long
)