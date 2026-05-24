package com.afgalindob.assistantapp.data.local.room.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.afgalindob.assistantapp.data.local.room.entity.TaskEntity
import com.afgalindob.assistantapp.data.local.room.entity.NoteEntity
import com.afgalindob.assistantapp.data.local.room.dao.TaskDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.afgalindob.assistantapp.data.local.room.dao.AudioRequestDao
import com.afgalindob.assistantapp.data.local.room.dao.NoteDao
import com.afgalindob.assistantapp.data.local.room.entity.AudioRequestEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

@Database(
    entities = [TaskEntity::class, NoteEntity::class, AudioRequestEntity::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao
    abstract fun audioRequestDao(): AudioRequestDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val databaseName = "assistant_database"
            val oldDatabaseName = "todo_database"
            val dbFile = context.getDatabasePath(databaseName)
            val oldDbFile = context.getDatabasePath(oldDatabaseName)

            // Intento de migración de archivo si existiera en el mismo sandbox
            if (!dbFile.exists() && oldDbFile.exists()) {
                oldDbFile.renameTo(dbFile)
                val checkpoints = listOf("-journal", "-shm", "-wal")
                checkpoints.forEach { suffix ->
                    val oldFile = context.getDatabasePath(oldDatabaseName + suffix)
                    val newFile = context.getDatabasePath(databaseName + suffix)
                    if (oldFile.exists()) oldFile.renameTo(newFile)
                }
            }

            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    databaseName
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE tasks_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                date TEXT NOT NULL,
                completed INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """
        )

        val cursor = db.query("SELECT id, `values` FROM tasks")

        while (cursor.moveToNext()) {

            val id = cursor.getLong(0)
            val json = cursor.getString(1)

            val jsonObject = JSONObject(json)

            val title = jsonObject.optString("title", "")
            val description = jsonObject.optString("description", "")
            val date = jsonObject.optString("date", "")

            val completedString = jsonObject.optString("completed", "false")
            val completed = if (completedString == "true") 1 else 0

            val now = System.currentTimeMillis()

            db.execSQL(
                """
                INSERT INTO tasks_new
                (id, title, description, date, completed, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    id,
                    title,
                    description,
                    date,
                    completed,
                    now,
                    now
                )
            )
        }

        cursor.close()

        db.execSQL("DROP TABLE tasks")
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_completed ON tasks(completed)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_date ON tasks(date)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3){

    private fun parseDateToTimestamp(date: String?): Long {
        if (date.isNullOrEmpty()) return 0L

        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .parse(date)?.time ?: 0L
        } catch (e: Exception) {
            Log.e("AppDatabase", "Error al convertir la fecha: $date", e)
            0L
        }
    }

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE tasks_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                date INTEGER NOT NULL,
                completed INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """
        )
        val cursor = db.query("SELECT id, title, description, date, completed, createdAt, updatedAt FROM tasks")

        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val title = cursor.getString(1)
            val description = cursor.getString(2)
            val dateString = cursor.getString(3)
            val completed = cursor.getInt(4)
            val createdAt = cursor.getLong(5)
            val updatedAt = cursor.getLong(6)

            val date = parseDateToTimestamp(dateString)

            db.execSQL(
                """
                INSERT INTO tasks_new
                (id, title, content, date, completed, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                arrayOf(
                    id,
                    title,
                    description, // aquí haces rename → content
                    date,
                    completed,
                    createdAt,
                    updatedAt
                )
            )

        }

        cursor.close()

        db.execSQL("DROP TABLE tasks")
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_date ON tasks(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_completed ON tasks(completed)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `notes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
                )
            """
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Añadir columna a notes
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `deleteAt` INTEGER NOT NULL DEFAULT 0")

        // Añadir columna a tasks
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `deleteAt` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE IF NOT EXISTS `audio_requests` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `fileName` TEXT NOT NULL, 
                `status` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `deleteAt` INTEGER NOT NULL DEFAULT 0,
                `onTrash` INTEGER NOT NULL DEFAULT 0
                )
            """
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_requests_status` ON `audio_requests` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_audio_requests_createdAt` ON `audio_requests` (`createdAt`)")
    }
}