package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "cliques",
    foreignKeys = [
        ForeignKey(
            entity = Graph::class,
            parentColumns = ["id"],
            childColumns = ["graphId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Clique(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val graphId: Long,
    val name: String,
    val edgeWeight: Double = 1.0
)