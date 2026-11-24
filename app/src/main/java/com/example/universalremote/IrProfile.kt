package com.example.universalremote

/** A profile must come from a verified protocol/model mapping, never a guessed brand code. */
data class IrProfile(
    val brand: String,
    val model: String,
    val frequencyHz: Int,
    val commands: Map<String, IntArray>
) {
    fun isValid(): Boolean =
        brand.isNotBlank() && model.isNotBlank() &&
            frequencyHz in 30_000..60_000 && commands.isNotEmpty() &&
            commands.keys.all { it.matches(Regex("[A-Z][A-Z0-9_]{1,47}")) } &&
            commands.values.all { pattern ->
                pattern.isNotEmpty() && pattern.size % 2 == 0 &&
                    pattern.all { it in 1..100_000 }
            }
}

/** Registry contains only validated, exact brand+model profiles. */
object VerifiedIrProfiles {
    private val profiles = mutableListOf<IrProfile>()

    @Synchronized
    fun find(brand: String, model: String): IrProfile? =
        profiles.firstOrNull {
            it.brand.equals(brand.trim(), true) &&
                it.model.equals(model.trim(), true) && it.isValid()
        }?.let { profile ->
            // IntArray is mutable: never expose the registry's internal command buffers.
            profile.copy(commands = profile.commands.mapValues { (_, values) -> values.copyOf() })
        }

    @Synchronized
    fun register(profile: IrProfile): Boolean = registerBatch(listOf(profile))

    /** Adds a complete batch or nothing; duplicate keys never partially modify the registry. */
    @Synchronized
    fun registerBatch(batch: List<IrProfile>): Boolean {
        if (batch.isEmpty() || batch.any { !it.isValid() }) return false
        val keys = mutableSetOf<String>()
        for (profile in batch) {
            val key = profile.brand.trim().lowercase() + "\u0000" + profile.model.trim().lowercase()
            if (!keys.add(key)) return false
            if (profiles.any {
                it.brand.equals(profile.brand.trim(), true) &&
                    it.model.equals(profile.model.trim(), true)
            }) return false
        }
        batch.forEach { profile ->
            profiles.add(profile.copy(
                brand = profile.brand.trim(),
                model = profile.model.trim(),
                commands = profile.commands.mapValues { (_, values) -> values.copyOf() }
            ))
        }
        return true
    }

    @Synchronized
    fun brands(): List<String> = profiles.map { it.brand }.distinct().sorted()

    @Synchronized
    fun count(): Int = profiles.size
}
