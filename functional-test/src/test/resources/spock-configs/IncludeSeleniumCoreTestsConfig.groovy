import org.rundeck.util.annotations.SeleniumCoreTest

runner {
    include SeleniumCoreTest
    shard.enabled = true
    shard.global = true
    shard.indexEnvVar = 'SPOCK_SHARD_INDEX'
    shard.countEnvVar = 'SPOCK_SHARD_COUNT'
}