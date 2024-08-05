package com.rundeck.tests.spock.ext

import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.spockframework.runtime.InvalidSpecException
import org.spockframework.runtime.model.SpecInfo
import spock.config.ConfigurationObject

/**
 * <p>Configuration object for the Shard extension,
 * used to determine the number of splits and the index of the split.
 * </p>
 * <p>The count and index can be set directly or via environment variables, the names of which must be defined in the
 * config.</p>
 * <p>If the global flag is set, all specs will be sharded, otherwise only those annotated with {@link Shard}.</p>
 *
 * <p>
 * The Sharding mechanism is based on the hash of the spec name, so the same spec name will always be assigned the same
 * shard. (See {@link String#hashCode()})). It may not distribute the specs evenly across the shards.
 * </p>
 *
 * <p>
 * If the builtin spock include/exclude mechanism is also used, the sharding will only affect
 * the included specs, meaning that a spec that is included will possibly be skipped, but no spec that was
 * excluded will be run.
 * </p>
 */
@ConfigurationObject("shard")
@ToString
@CompileStatic
@Slf4j
class ShardConfig {
    /**
     * Whether to enable the shard extension
     */
    boolean enabled = false
    /**
     * Whether to automatically shard all specs
     */
    boolean global = false
    /**
     * Skip all tests, but log which would be skipped or run
     */
    boolean dryRun = false
    /**
     * Environment variable name to determine the number of shards
     */
    String countEnvVar = null
    /**
     * Environment variable name to determine the index of the shard
     */
    String indexEnvVar = null
    /**
     * Optionally define number of shards directly
     */
    int count = 1
    /**
     * Optionally define current index
     */
    int index = 0

    void validate() {
        count = validateConfig(count, 1, -1, "count", countEnvVar)
        index = validateConfig(index, 0, count - 1, "index", indexEnvVar)
    }

    private static int validateConfig(int value, int min, int max, String varName, String envVar) {
        if (!envVar && value < min) {
            throw new InvalidSpecException("Shard extension requires config ${varName} or ${varName}EnvVar to be set")
        }
        if (envVar && System.getenv(envVar)) {
            try {
                value = System.getenv(envVar).toInteger()
            } catch (NumberFormatException e) {
                throw new InvalidSpecException(
                    "Shard extension ${varName}EnvVar=${envVar}, but the env var is not a " +
                    "number"
                )
            }
        }

        //validate value
        if (value < min) {
            throw new InvalidSpecException("Shard extension ${varName}=${value} must be >= ${min}")
        }
        if (max >= 0 && value > max) {
            throw new InvalidSpecException("Shard extension ${varName}=${value} must be <= ${max}")
        }
        return value
    }


    /**
     * Process the spec, determine if it should be skipped, and set the skipped flag if necessary
     * @param spec the spec to process
     * @param annotated whether the spec is annotated with {@link Shard}
     */
    void processSpec(SpecInfo spec, boolean annotated) {
        if (!enabled || !annotated && !global) {
            if(dryRun){
                log.info("Shard extension disabled, skipping spec: ${spec.name}")
            }
            return
        }

        //spec may already be skipped based on another extension
        if (!spec.skipped) {
            if (shouldSkip(spec)) {
                if(dryRun){
                    log.info("Shard: SKIP: ${spec.name}, index: ${getSpecIndex(spec)}")
                }
                spec.setSkipped(true)
            }else{

                if(dryRun){
                    log.info("Shard: RUN: ${spec.name}, index: ${getSpecIndex(spec)}")
                    spec.setSkipped(true)
                }
            }
        }else if(dryRun){
            log.info("(Previously Skipped) spec: ${spec.name}, index: ${getSpecIndex(spec)}")
        }
    }

    /**
     * Determine if the spec should be skipped, based on the shard index and a hash of the spec name
     * @param spec
     * @return
     */
    private boolean shouldSkip(SpecInfo spec) {
        getSpecIndex(spec) != index
    }

    private int getSpecIndex(SpecInfo spec) {
        Math.abs(spec.getName().hashCode()) % count
    }
}
