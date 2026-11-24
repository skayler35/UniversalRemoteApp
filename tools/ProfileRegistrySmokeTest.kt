import com.example.universalremote.IrProfile
import com.example.universalremote.VerifiedIrProfiles

fun main() {
    check(VerifiedIrProfiles.count() == 0)
    val source = intArrayOf(9000, 4500, 560, 560)
    val valid = IrProfile("TestBrand", "Model-1", 38000, mapOf("TV_POWER" to source))
    check(valid.isValid())
    check(VerifiedIrProfiles.register(valid))
    source[0] = 1 // Registration must copy caller-owned arrays.
    val first = VerifiedIrProfiles.find("testbrand", " model-1 ") ?: error("profile missing")
    check(first.commands.getValue("TV_POWER")[0] == 9000)
    first.commands.getValue("TV_POWER")[0] = 2 // Lookup must not expose registry storage.
    val second = VerifiedIrProfiles.find("TestBrand", "Model-1") ?: error("profile missing")
    check(second.commands.getValue("TV_POWER")[0] == 9000)
    check(!VerifiedIrProfiles.register(valid)) // Duplicate must be rejected.
    check(!VerifiedIrProfiles.register(IrProfile("Bad", "Model", 38000, mapOf("bad key" to intArrayOf(1, 2)))))

    // Batch registration must be all-or-nothing if any item is invalid.
    val validSecond = IrProfile("OtherBrand", "Model-2", 38000, mapOf("TV_POWER" to intArrayOf(8000, 4000)))
    val invalidSecond = IrProfile("Broken", "Model-3", 38000, mapOf("bad key" to intArrayOf(1, 2)))
    check(!VerifiedIrProfiles.registerBatch(listOf(validSecond, invalidSecond)))
    check(VerifiedIrProfiles.find("OtherBrand", "Model-2") == null)

    // A collision anywhere in a batch must not partially add earlier candidates.
    val anotherValid = IrProfile("AnotherBrand", "Model-4", 38000, mapOf("TV_POWER" to intArrayOf(7000, 3500)))
    check(!VerifiedIrProfiles.registerBatch(listOf(anotherValid, valid)))
    check(VerifiedIrProfiles.find("AnotherBrand", "Model-4") == null)
    check(VerifiedIrProfiles.count() == 1)
    println("PASS: defensive copies, exact lookup, validation, duplicate rejection, atomic batch rollback")
}
