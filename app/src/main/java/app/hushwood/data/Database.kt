package app.hushwood.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import app.hushwood.domain.Beast
import app.hushwood.domain.Daypart
import app.hushwood.domain.Kind
import app.hushwood.domain.Place
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Something you practise. Its [kind] decides its place in the clearing. */
@Entity(tableName = "practices")
data class Practice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = Kind.SIT.name,
    val order: Int = 0,
    val archived: Boolean = false,
) {
    val kindOf: Kind get() = Kind.of(kind)
}

/**
 * One sitting (or a walk, a stretch, a page) and who came out during it. [still]: the longest stretch of
 * unbroken quiet, minutes; [startles]: how many times a movement sent them back into the trees.
 * [manual] ones were written down afterwards, with no clearing to watch.
 */
@Entity(tableName = "sessions", indices = [Index("start")])
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val practiceId: Long,
    val kind: String,
    val place: String = Place.FOREST.name,
    val start: Long,
    val end: Long,
    val minutes: Int,
    val still: Int = 0,
    val startles: Int = 0,
    val beasts: String = "",
    val note: String = "",
    val manual: Boolean = false,
    /** What the guardian left by the stump, if anything. */
    val gift: String = "",
) {
    val kindOf: Kind get() = Kind.of(kind)
    val placeOf: Place get() = Place.of(place)
    val seen: List<Beast> get() = Beast.list(beasts)
    fun at(zone: ZoneId = ZoneId.systemDefault()): LocalDateTime = Instant.ofEpochMilli(start).atZone(zone).toLocalDateTime()
    fun day(zone: ZoneId = ZoneId.systemDefault()): LocalDate = at(zone).toLocalDate()
    fun part(zone: ZoneId = ZoneId.systemDefault()): Daypart = Daypart.of(at(zone))
}

@Dao
interface HushDao {
    @Query("SELECT * FROM practices ORDER BY `order`, id")
    fun practices(): Flow<List<Practice>>

    @Query("SELECT * FROM practices ORDER BY `order`, id")
    suspend fun practicesNow(): List<Practice>

    @Insert
    suspend fun insert(p: Practice): Long

    @Insert
    suspend fun insertPractices(p: List<Practice>)

    @Update
    suspend fun update(p: Practice)

    @Query("SELECT * FROM sessions ORDER BY start")
    fun sessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY start")
    suspend fun sessionsNow(): List<Session>

    @Insert
    suspend fun insert(s: Session): Long

    @Insert
    suspend fun insertSessions(s: List<Session>)

    @Update
    suspend fun update(s: Session)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM practices")
    suspend fun clearPractices()
}

@Database(entities = [Practice::class, Session::class], version = 1, exportSchema = true)
abstract class HushDatabase : RoomDatabase() {
    abstract fun dao(): HushDao

    companion object {
        fun create(context: Context): HushDatabase =
            Room.databaseBuilder(context.applicationContext, HushDatabase::class.java, "hushwood.db").build()
    }
}
