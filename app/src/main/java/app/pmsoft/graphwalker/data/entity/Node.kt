package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nodes",
    foreignKeys = [
        ForeignKey(
            entity = Graph::class,
            parentColumns = ["id"],
            childColumns = ["graphId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["graphId"])]
)
data class Node(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val graphId: Long,
    val name: String,
    val tags: List<String> = emptyList()
)