package app.pmsoft.graphwalker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "connectors",
    foreignKeys = [
        ForeignKey(
            entity = Node::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["nodeId"])]
)
data class Connector(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nodeId: Long,
    val name: String
)