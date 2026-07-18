package com.tally.app.data.local.converter

import androidx.compose.ui.graphics.Color
import androidx.room.TypeConverter
import com.tally.app.data.MembershipType
import com.tally.app.data.PlayerColorKey
import com.tally.app.data.ScoringType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Room type adapters for the non-primitive columns across all entities. Registered once on the
 * @Database. Enums store as their `name`, Compose [Color] as its raw ULong bits reinterpreted to
 * Long, timestamps as epoch-millis, and collections as kotlinx.serialization JSON.
 */
class Converters {

    private val json = Json

    // --- Enums -----------------------------------------------------------------
    @TypeConverter fun membershipToString(value: MembershipType): String = value.name
    @TypeConverter fun stringToMembership(value: String): MembershipType = MembershipType.valueOf(value)

    @TypeConverter fun colorKeyToString(value: PlayerColorKey): String = value.name
    @TypeConverter fun stringToColorKey(value: String): PlayerColorKey = PlayerColorKey.valueOf(value)

    @TypeConverter fun scoringToString(value: ScoringType): String = value.name
    @TypeConverter fun stringToScoring(value: String): ScoringType = ScoringType.valueOf(value)

    // --- Compose Color (ULong bits <-> Long) -----------------------------------
    @TypeConverter fun colorToLong(color: Color): Long = color.value.toLong()
    @TypeConverter fun longToColor(bits: Long): Color = Color(value = bits.toULong())

    // --- Time (Instant <-> epoch millis) ---------------------------------------
    @TypeConverter fun instantToLong(instant: Instant): Long = instant.toEpochMilli()
    @TypeConverter fun longToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    // --- Collections (JSON) ----------------------------------------------------
    @TypeConverter fun stringListToJson(value: List<String>): String = json.encodeToString(value)
    @TypeConverter fun jsonToStringList(value: String): List<String> = json.decodeFromString(value)

    /** Per-player integer results (name -> points/placement). */
    @TypeConverter fun intMapToJson(value: Map<String, Int>): String = json.encodeToString(value)
    @TypeConverter fun jsonToIntMap(value: String): Map<String, Int> = json.decodeFromString(value)
}
