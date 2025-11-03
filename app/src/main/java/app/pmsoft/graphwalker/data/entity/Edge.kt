package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "edges",
    foreignKeys = [
        ForeignKey(
            entity = Connector::class,
            parentColumns = ["id"],
            childColumns = ["fromConnectorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Connector::class,
            parentColumns = ["id"],
            childColumns = ["toConnectorId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Edge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromConnectorId: Long,
    val toConnectorId: Long,
    val bidirectional: Boolean,
    val name: String,
    val weight: Double = 0.0
)