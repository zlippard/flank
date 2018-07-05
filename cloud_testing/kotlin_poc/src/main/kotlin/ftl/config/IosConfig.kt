package ftl.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import ftl.ios.IosCatalog
import ftl.ios.Xctestrun
import ftl.util.Utils.fatalError

class IosConfig(
        @field:JsonProperty("test")
        val xctestrunZip: String = "",
        @field:JsonProperty("xctestrun-file")
        val xctestrunFile: String = "",
        rootGcsBucket: String,
        disablePerformanceMetrics: Boolean = true,
        disableVideoRecording: Boolean = false,
        timeout: Long = 60,
        testShards: Int = 1,
        testRuns: Int = 1,
        waitForResults: Boolean = true,
        testMethods: List<String> = listOf(),
        limitBreak: Boolean = false,
        projectId: String = YamlConfig.getDefaultProjectId(),
        devices: List<Device> = listOf(Device("iphone8", "11.2")),
        testShardChunks: Set<Set<String>> = emptySet()
) : YamlConfig(
        rootGcsBucket,
        disablePerformanceMetrics,
        disableVideoRecording,
        timeout,
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
        if (xctestrunZip.startsWith(FtlConstants.GCS_PREFIX)) {
            assertGcsFileExists(xctestrunZip)
        } else {
            assertFileExists(xctestrunZip, "xctestrunZip")
        }
        assertFileExists(xctestrunFile, "xctestrunFile")

        devices.forEach { device -> assertDeviceSupported(device) }

        val xctestValidTestNames = Xctestrun.findTestNames(xctestrunFile)
        validateTestMethods(xctestValidTestNames, "xctest binary")
    }

    private fun assertDeviceSupported(device: Device) {
        if (!IosCatalog.supported(device.model, device.version)) {
            fatalError("iOS ${device.version} on ${device.model} is not a supported device")
        }
    }

    companion object {
        fun load(yamlPath: String): IosConfig {
            val typeRef = object : TypeReference<HashMap<String, IosConfig>>() {}
            return YamlConfig.load(yamlPath, typeRef).map { it.value }[0]
        }
    }

    override fun toString(): String {
        return """IosConfig
  project: '$projectId'
  xctestrunZip: '$xctestrunZip',
  xctestrunFile: '$xctestrunFile',
  rootGcsBucket: '$rootGcsBucket',
  recordVideo: $recordVideo,
  testTimeoutMinutes: $testTimeoutMinutes,
  testRuns: $testRuns,
  async: $waitForResults,
  limitBreak: $limitBreak,
  devices: $devices
            """
    }
}
