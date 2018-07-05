package ftl.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.linkedin.dex.parser.DexParser
import ftl.android.*
import ftl.gc.GcStorage
import kotlinx.coroutines.experimental.runBlocking

class AndroidConfig(
        @field:JsonProperty("app")
        val appApk: String = "",
        @field:JsonProperty("test")
        val testApk: String = "",
        @field:JsonProperty("auto-google-login")
        val autoGoogleLogin: Boolean = true,
        @field:JsonProperty("use-orchestrator")
        val useOrchestrator: Boolean = true,
        @field:JsonProperty("environment-variables")
        val environmentVariables: Map<String, String> = mapOf(),
        @field:JsonProperty("directories-to-pull")
        val directoriesToPull: List<String> = listOf(),
        rootGcsBucket: String,
        disablePerformanceMetrics: Boolean = true,
        disableVideoRecording: Boolean = false,
        testTimeoutMinutes: Long = 60,
        testShards: Int = 1,
        testRuns: Int = 1,
        waitForResults: Boolean = true,
        testMethods: List<String> = listOf(),
        limitBreak: Boolean = false,
        projectId: String = YamlConfig.getDefaultProjectId(),
        devices: List<Device> = listOf(Device("NexusLowRes", "23")),
        testShardChunks: Set<Set<String>> = emptySet()
) : YamlConfig(
        rootGcsBucket,
        disablePerformanceMetrics,
        disableVideoRecording,
        testTimeoutMinutes,
        testShards,
        testRuns,
        waitForResults,
        testMethods,
        limitBreak,
        projectId,
        devices,
        testShardChunks
) {

    init {
        if (appApk.startsWith(FtlConstants.GCS_PREFIX)) {
            assertGcsFileExists(appApk)
        } else {
            assertFileExists(appApk, "appApk")
        }

        if (testApk.startsWith(FtlConstants.GCS_PREFIX)) {
            assertGcsFileExists(testApk)
        } else {
            assertFileExists(testApk, "testApk")
        }

        // Download test APK if necessary so it can be used to validate test methods
        var testLocalApk = testApk
        if (testApk.startsWith(FtlConstants.GCS_PREFIX)) {
            runBlocking {
                testLocalApk = GcStorage.downloadTestApk(this@AndroidConfig)
            }
        }

        devices.forEach { device -> assertDeviceSupported(device) }

        val dexValidTestNames = DexParser.findTestMethods(testLocalApk).map { "class ${it.testName}" }
        validateTestMethods(dexValidTestNames, "Test APK")
    }

    private fun assertDeviceSupported(device: Device) {
        val deviceConfigTest = AndroidCatalog.supportedDeviceConfig(device.model, device.version)
        when (deviceConfigTest) {
            SupportedDeviceConfig -> {
            }
            UnsupportedModelId -> throw RuntimeException("Unsupported model id, '${device.model}'\nSupported model ids: ${AndroidCatalog.androidModelIds}")
            UnsupportedVersionId -> throw RuntimeException("Unsupported version id, '${device.version}'\nSupported Version ids: ${AndroidCatalog.androidVersionIds}")
            is IncompatibleModelVersion -> throw RuntimeException("Incompatible model, '${device.model}', and version, '${device.version}'\nSupported version ids for '${device.model}': $deviceConfigTest")
        }
    }

    companion object {
        fun load(yamlPath: String): AndroidConfig {
            val typeRef = object : TypeReference<HashMap<String, AndroidConfig>>() {}
            return YamlConfig.load(yamlPath, typeRef).map { it.value }[0]
        }
    }

    override fun toString(): String {
        return """AndroidConfig
  project: '$projectId'
  app: '$appApk',
  test: '$testApk',
  rootGcsBucket: '$rootGcsBucket',
  autoGoogleLogin: '$autoGoogleLogin',
  useOrchestrator: $useOrchestrator,
  disablePerformanceMetrics: $disablePerformanceMetrics,
  recordVideo: $recordVideo,
  testTimeoutMinutes: $testTimeoutMinutes,
  testShards: $testShards,
  testRuns: $testRuns,
  async: $waitForResults,
  testMethods: $testMethods,
  limitBreak: $limitBreak,
  devices: $devices,
  environmentVariables: $environmentVariables,
  directoriesToPull: $directoriesToPull
            """
    }
}
