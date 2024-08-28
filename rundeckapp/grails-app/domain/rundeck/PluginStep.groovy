/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand

class PluginStep extends WorkflowStep{
    public static final List<String> LEGACY_BUILTIN_TYPES = Collections.unmodifiableList(
        [
            ExecCommand.EXEC_COMMAND_TYPE,
            ScriptCommand.SCRIPT_COMMAND_TYPE,
            ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
        ]
    )
    Boolean nodeStep = false
    String type
    String jsonData
    static constraints = {
        type nullable: false, blank: false
        jsonData(nullable: true, blank: true)
        pluginConfigData(nullable: true, blank: true)
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['configuration']

    public String getPluginType() {
        return type
    }

    public Map getConfiguration() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    public void setConfiguration(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }

    static mapping = {
        jsonData(type: 'text')
        pluginConfigData(type: 'text')
    }

    static isOldBuiltInType(String type) {
        return LEGACY_BUILTIN_TYPES.contains(type)
    }

    public Map toMap() {
        if(isOldBuiltInType(this.type)){//keep job defs compatibility with old Rundeck versions prior to 5.x
            return toLegacyCommandMap()
        }
        def map=[type: type, nodeStep:nodeStep]

        if(this.configuration){
            map.put('configuration',this.configuration)
        }
        if (description) {
            map.description = description
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toMap()
        } else if (keepgoingOnSuccess) {
            map.keepgoingOnSuccess = keepgoingOnSuccess
        }
        def config = getPluginConfig()
        if (config) {
            map.plugins = config
        }
        map
    }
    
    /**
     * Mapping from Job Definition data keys to CommandExec/step plugin properties keys
     */
    static final Map<String, String> LEGACY_BUILTIN_STEP_KEYS_MAP = Collections.unmodifiableMap([
        args                   : 'argString',
        exec                   : 'adhocRemoteString',
        script                 : 'adhocLocalString',
        scriptfile             : 'adhocFilepath',
        scripturl              : 'adhocFilepath',
        scriptInterpreter      : 'scriptInterpreter',
        fileExtension          : 'fileExtension',
        interpreterArgsQuoted  : 'interpreterArgsQuoted',
        expandTokenInScriptFile: 'expandTokenInScriptFile',
    ])

    /**
     * Legacy job definition canonical map key names for command/script steps
     */
    static final Set<String> LEGACY_BUILTIN_STEP_JOB_DEFINITION_MAP_KEYS = Collections.unmodifiableSet(LEGACY_BUILTIN_STEP_KEYS_MAP.keySet())

    /**
     * Configuration key names for command/script steps
     */
    static final Set<String> LEGACY_BUILTIN_STEP_CONFIGURATION_KEYS = Collections.unmodifiableSet(LEGACY_BUILTIN_STEP_KEYS_MAP.values().toSet())


    /**
     * Create a legacy command map from the current configuration
     * @return
     */
    private Map<String, Object> toLegacyCommandMap() {
        def legacyData = this.configuration.subMap(LEGACY_BUILTIN_STEP_CONFIGURATION_KEYS)
        CommandExec commandExec = new CommandExec(legacyData)
        commandExec.description = this.description
        commandExec.errorHandler = this.errorHandler
        commandExec.keepgoingOnSuccess = this.keepgoingOnSuccess
        commandExec.pluginConfigData = this.pluginConfigData
        def legacyMap = commandExec.toMap()
        //additional data
        def configuration = new HashMap(this.configuration)
        def keySet = new HashSet(configuration.keySet())
        keySet.removeAll(LEGACY_BUILTIN_STEP_CONFIGURATION_KEYS)

        if (keySet.size() > 0) {
            def additionalData = configuration.subMap(keySet)
            legacyMap.putAll(additionalData)
        }
        return legacyMap
    }

    /**
     * Create step configuration map from the job definition data map
     * @param data job definition step data
     * @return
     */
    static Map createLegacyConfigurationFromDefinitionMap(Map data) {
        def ce = [:]
        def legacyDefinitionData = data.subMap(LEGACY_BUILTIN_STEP_JOB_DEFINITION_MAP_KEYS)
        CommandExec.setConfigurationFromMap(ce, legacyDefinitionData)
        def additionalData = new HashMap(data)
        additionalData.keySet().removeAll(LEGACY_BUILTIN_STEP_JOB_DEFINITION_MAP_KEYS)
        ce.putAll(additionalData)
        return ce
    }
    /**
     *
     * @param data
     * @return new plugin type for legacy step configuration
     */
    static String getLegacyBuiltinCommandType(Map data) {
        if (data.exec != null) {
            return ExecCommand.EXEC_COMMAND_TYPE
        } else if (data.script != null) {
            return ScriptCommand.SCRIPT_COMMAND_TYPE
        } else if (data.scriptfile != null || data.scripturl != null) {
            return ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
        } else {
            throw new IllegalArgumentException("Invalid data: ${data}")
        }
    }
    /**
     *
     * @param data
     * @return true if the data represents a legacy imported command/script step
     */
    static boolean isLegacyBuiltinCommandData(Map data) {
        return !data.type && (
            data.exec != null ||
            data.script != null ||
            data.scriptfile != null ||
            data.scripturl != null
        )
    }

    /**
     *
     * @return map representation without details
     */
    public Map toDescriptionMap() {
        def map=[type: type, nodeStep:nodeStep]
        if (description) {
            map.description = description
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toDescriptionMap()
        }
        map
    }

    static PluginStep fromMap(Map data) {
        PluginStep ce = new PluginStep()
        updateFromMap(ce, data)
        return ce
    }
    /**
     * config keys that are separate from legacy step keys
     */
    static final Set<String> LOCAL_CONFIG_KEYS = [
        'nodeStep',
        'type',
        'configuration',
        'keepgoingOnSuccess',
        'description',
        'plugins',
        'errorhandler'
    ]
    static void updateFromMap(PluginStep ce, Map data) {
        if (isLegacyBuiltinCommandData(data)) {
            //keep job def import compatibility with old Rundeck versions prior to 5.x
            //remove local data
            Map newmap = new HashMap(data)
            LOCAL_CONFIG_KEYS.each { newmap.remove(it) }

            ce.nodeStep = true
            ce.type = getLegacyBuiltinCommandType(newmap)
            ce.configuration = createLegacyConfigurationFromDefinitionMap(newmap)
        } else {
            ce.nodeStep = data.nodeStep
            ce.type = data.type
            ce.configuration = data.configuration
        }

        ce.keepgoingOnSuccess = !!data.keepgoingOnSuccess
        ce.description=data.description?.toString()
        if (data.plugins) {
            ce.pluginConfig = data.plugins
        }
    }

    public PluginStep createClone() {
        return new PluginStep(
                type: type,
                nodeStep: nodeStep,
                jsonData: jsonData,
                keepgoingOnSuccess: keepgoingOnSuccess,
                description: description,
                pluginConfig: pluginConfig
        )
    }

    @Override
    public String toString() {
        return "PluginStep{" +
               "nodeStep=" + nodeStep +
               ", type='" + type + '\'' +
               ", jsonData='" + jsonData + '\'' +
               ", pluginConfig='" + pluginConfig + '\'' +
               '}';
    }

    @Override
    public String summarize() {
        return "Plugin["+ type + ', nodeStep: '+nodeStep+']';
    }
}
