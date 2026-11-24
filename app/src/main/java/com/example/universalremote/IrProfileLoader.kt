package com.example.universalremote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Strictly loads explicitly supplied model-specific IR data from assets/ir_profiles.json. */
object IrProfileLoader {
    data class Result(val loaded: Int, val rejected: Int, val error: String? = null)

    fun loadFromAssets(context: Context): Result {
        val source = try {
            context.assets.open("ir_profiles.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return Result(0, 0, "ir_profiles.json bulunamadı; profil yüklenmedi")
        }
        return try {
            val root = JSONObject(source)
            val array = root.optJSONArray("profiles")
                ?: return Result(0, 0, "profiles dizisi eksik")
            var rejected = 0
            val candidates = mutableListOf<IrProfile>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i)
                if (item == null) { rejected++; continue }
                val commandsObject = item.optJSONObject("commands")
                if (commandsObject == null || commandsObject.length() == 0) {
                    rejected++; continue
                }
                val commands = mutableMapOf<String, IntArray>()
                var malformed = false
                val keys = commandsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val values = commandsObject.optJSONArray(key)
                    if (values == null || values.length() == 0 || values.length() % 2 != 0) {
                        malformed = true
                        break
                    }
                    val pattern = parsePositiveIntArray(values)
                    if (pattern == null) {
                        malformed = true
                        break
                    }
                    commands[key] = pattern
                }
                if (malformed) { rejected++; continue }

                val profile = IrProfile(
                    brand = item.optString("brand", "").trim(),
                    model = item.optString("model", "").trim(),
                    frequencyHz = item.optInt("frequencyHz", -1),
                    commands = commands
                )
                if (profile.isValid()) candidates.add(profile) else rejected++
            }
            // Never leave a half-loaded registry if one profile conflicts with an existing key.
            if (rejected > 0 || candidates.size != array.length()) {
                return Result(0, rejected + candidates.size, "Profil dosyasında geçersiz kayıt var; hiçbiri yüklenmedi")
            }
            if (!VerifiedIrProfiles.registerBatch(candidates)) {
                return Result(0, candidates.size, "Profil çakışması veya tekrar eden marka/model; hiçbiri yüklenmedi")
            }
            Result(candidates.size, 0)
        } catch (e: Exception) {
            Result(0, 0, "Profil JSON okunamadı: ${e.message ?: "biçim hatası"}")
        }
    }

    private fun parsePositiveIntArray(values: JSONArray): IntArray? {
        val result = IntArray(values.length())
        for (index in 0 until values.length()) {
            val value = values.opt(index)
            if (value !is Number) return null
            val number = value.toDouble()
            if (!number.isFinite() || number % 1.0 != 0.0 || number < 1.0 || number > 100_000.0) return null
            result[index] = number.toInt()
        }
        return result
    }
}
