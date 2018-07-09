package ftl.config

import com.google.common.math.IntMath
import ftl.util.Utils
import java.math.RoundingMode

class FlankConfig(
        testShards: Int = 1,
        testRuns: Int = 1,
        val limitBreak: Boolean = false,
        var testShardChunks: Set<Set<String>> = emptySet()
) {

    var testShards: Int = testShards
        set(value) {
            field = assertVmLimit(value)
        }

    var testRuns: Int = testRuns
        set(value) {
            field = assertVmLimit(value)
        }

    private fun assertVmLimit(value: Int): Int {
        if (value > 100 && !limitBreak) {
            Utils.fatalError("Shard count exceeds 100. Set limitBreak=true to enable large shards")
        }
        return value
    }

    fun calculateShards(allTestMethods: Collection<String>, testMethods: Collection<String>) {
        val testShardMethods = if (testMethods.isNotEmpty()) {
            testMethods
        } else {
            allTestMethods
        }.sorted()

        if (testShards < 1) testShards = 1

        var chunkSize = IntMath.divide(testShardMethods.size, testShards, RoundingMode.UP)
        // 1 method / 40 shard = 0. chunked(0) throws an exception.
        // default to running all tests in a single chunk if method count is less than shard count.
        if (chunkSize < 1) chunkSize = testShardMethods.size

        testShardChunks = testShardMethods.chunked(chunkSize).map { it.toSet() }.toSet()

        // Ensure we don't create more VMs than requested. VM count per run should be <= testShards
        if (testShardChunks.size > testShards) {
            Utils.fatalError("Calculated chunks $testShardChunks is > requested $testShards testShards.")
        }
        if (testShardChunks.isEmpty()) Utils.fatalError("Failed to populate test shard chunks")
    }

}