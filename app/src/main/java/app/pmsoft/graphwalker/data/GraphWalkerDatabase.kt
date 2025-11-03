package app.pmsoft.graphwalker.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import app.pmsoft.graphwalker.data.dao.*
import app.pmsoft.graphwalker.data.entity.*

@Database(
    entities = [Graph::class, Node::class, Connector::class, Edge::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GraphWalkerDatabase : RoomDatabase() {
    abstract fun graphDao(): GraphDao
    abstract fun nodeDao(): NodeDao
    abstract fun connectorDao(): ConnectorDao
    abstract fun edgeDao(): EdgeDao

    companion object {
        @Volatile
        private var INSTANCE: GraphWalkerDatabase? = null

        fun getDatabase(context: Context): GraphWalkerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GraphWalkerDatabase::class.java,
                    "graph_walker_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}