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

import org.rundeck.app.data.job.converters.WorkflowToRdWorkflowConverter
import org.rundeck.core.execution.BaseCommandExec
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import rundeck.data.constants.WorkflowStepConstants

/*
* CommandExec.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 25, 2010 3:02:01 PM
* $Id$
*/

public class CommandExec extends WorkflowStep implements BaseCommandExec {
    String argString
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    /**
     * UNUSED
     */
    Boolean adhocExecution = true
    String scriptInterpreter
    String fileExtension
    Boolean interpreterArgsQuoted
    Boolean expandTokenInScriptFile
    static transients = ['nodeStep']

    static mapping = {
        adhocLocalString type: 'text'
        adhocRemoteString type: 'text'
        adhocFilepath type: 'text'
        argString type: 'text'
        scriptInterpreter type: 'text'
        fileExtension type: 'text'
        pluginConfigData(type: 'text')
    }
    public String toString() {
        StringBuffer sb = new StringBuffer()
        sb << "command( "
        sb << (adhocRemoteString ? "exec: ${adhocRemoteString}" : '')
        sb << (adhocLocalString ? "script: ${adhocLocalString}" : '')
        sb << (adhocFilepath ? "scriptfile: ${adhocFilepath}" : '')
        sb << (scriptInterpreter ? "interpreter: ${scriptInterpreter} " : '')
        sb << (fileExtension ? "ext: ${fileExtension} " : '')
        sb << (interpreterArgsQuoted ? "quoted?: ${interpreterArgsQuoted} " : '')
        sb << (expandTokenInScriptFile ? "expandTokens?: ${expandTokenInScriptFile} " : '')
        sb << (argString ? "scriptargs: ${argString}" : '')
        sb << (description ? "description: ${description}" : '')
        sb << (errorHandler ? " [handler: ${errorHandler}]" : '')
        sb << (null!= keepgoingOnSuccess ? " keepgoingOnSuccess: ${keepgoingOnSuccess}" : '')
        sb<<")"

        return sb.toString()
    }

    public String summarize(){
        StringBuffer sb = new StringBuffer()
        sb << (scriptInterpreter ? "${scriptInterpreter}" : '')
        sb << (interpreterArgsQuoted ? "'" : '')
        sb << (adhocRemoteString ? "${adhocRemoteString}" : '')
        sb << (adhocLocalString ? "${adhocLocalString}" : '')
        sb << (adhocFilepath ? "${adhocFilepath}" : '')
        sb << (argString ? " -- ${argString}" : '')
        sb << (interpreterArgsQuoted ? "'" : '')
        sb << (description ?( " ('" + description + "')" ) : '')
        sb << (fileExtension ?( " [" + fileExtension + "]" ) : '')
        return sb.toString()
    }

    static constraints = {
        argString(nullable: true)
        adhocRemoteString(nullable:true)
        adhocLocalString(nullable:true)
        adhocFilepath(nullable:true)
        scriptInterpreter(nullable:true)
        interpreterArgsQuoted(nullable:true)
        expandTokenInScriptFile(nullable:true)
        errorHandler(nullable: true)
        keepgoingOnSuccess(nullable: true)
        fileExtension(nullable: true, maxSize: 255)
        pluginConfigData(nullable: true, blank: true)
    }

    public CommandExec createClone(){
        Map properties = new HashMap(this.properties)
        properties.remove('errorHandler')
        CommandExec ce = new CommandExec(properties)
        return ce
    }

    public Map getConfiguration() {
        return WorkflowToRdWorkflowConverter.convertConfiguration(this.toMap())
    }

    public String getPluginType() {
        if(adhocRemoteString) {
            return WorkflowStepConstants.TYPE_COMMAND
        } else if(adhocLocalString) {
            return WorkflowStepConstants.TYPE_SCRIPT
        } else if(adhocFilepath && adhocFilePathIsUrl()) {
            return WorkflowStepConstants.TYPE_SCRIPT_URL
        } else if(adhocFilepath && !adhocFilePathIsUrl()) {
            return WorkflowStepConstants.TYPE_SCRIPT_FILE
        }
        return null
    }

    boolean adhocFilePathIsUrl() {
        return adhocFilepath==~/^(?i:https?|file):.*$/
    }

    /**
    * Return canonical map representation
     */
    public Map toMap(){
        def map=[:]
        if(adhocRemoteString){
            map.exec=adhocRemoteString
        }else if(adhocLocalString){
            map.script=adhocLocalString
        }else {
            if(adhocFilePathIsUrl()){
                map.scripturl = adhocFilepath
            }else{
                map.scriptfile=adhocFilepath
            }
            if(expandTokenInScriptFile) {
                map.expandTokenInScriptFile = !!expandTokenInScriptFile
            }
        }
        if(scriptInterpreter && !adhocRemoteString) {
            map.scriptInterpreter = scriptInterpreter
            map.interpreterArgsQuoted = !!interpreterArgsQuoted
        }
        if(fileExtension && !adhocRemoteString) {
            map.fileExtension = fileExtension
        }
        if(argString && !adhocRemoteString){
            map.args=argString
        }
        if(errorHandler){
            map.errorhandler=errorHandler.toMap()
        }else if(keepgoingOnSuccess){
            map.keepgoingOnSuccess= keepgoingOnSuccess
        }
        if(description){
            map.description=description
        }
        def config = getPluginConfig()
        if (config) {
            map.plugins = config
        }
        return map
    }
    /**
    * Return map representation without content details
     */
    public Map toDescriptionMap(){
        def map=[:]
        if(adhocRemoteString){
            map.exec='exec'
        }else if(adhocLocalString){
            map.script='script'
        }else {
            if(adhocFilePathIsUrl()){
                map.scripturl = 'scripturl'
            }else{
                map.scriptfile='scriptfile'
            }
            map.expandTokenInScriptFile = !!expandTokenInScriptFile
        }
        if(errorHandler){
            map.errorhandler=errorHandler.toDescriptionMap()
        }
        if(description){
            map.description=description
        }
        return map
    }

    /**
     *
     * @param data
     * @return
     * @deprecated
     */
    @Deprecated
    static CommandExec fromMap(Map data) {
        CommandExec ce = new CommandExec()
        updateFromMap(ce, data)
        return ce
    }

    static void updateFromMap(CommandExec ce, Map data) {
        setConfigurationFromMap(ce, data, true)
        ce.keepgoingOnSuccess = !!data.keepgoingOnSuccess
        ce.description = data.description?.toString()
        //nb: error handler is created inside Workflow.fromMap
        if (data.plugins) {
            ce.pluginConfig = data.plugins
        }
    }



    /**
     * Set configuration properties on the object from the
     * job definition data map
     * @param obj new object, intentially not typed to allow for CommandExec or Map
     * @param data job definition map data
     */
    static void setConfigurationFromMap(Object obj, Map data, boolean parseBoolean=false) {
        if (data.exec != null) {
            obj.adhocRemoteString = data.exec.toString()
        } else if (data.script != null) {
            obj.adhocLocalString = data.script.toString()
        } else if (data.scriptfile != null) {
            obj.adhocFilepath = data.scriptfile.toString()
            if(data.expandTokenInScriptFile!=null) {
                obj.expandTokenInScriptFile = parseBoolean ? booleanVal(data.expandTokenInScriptFile) :
                                              data.expandTokenInScriptFile.toString()
            }
        } else if (data.scripturl != null) {
            obj.adhocFilepath = data.scripturl.toString()
            if(data.expandTokenInScriptFile!=null) {
                obj.expandTokenInScriptFile = parseBoolean ? booleanVal(data.expandTokenInScriptFile) :
                                              data.expandTokenInScriptFile.toString()
            }
        }
        if (data.scriptInterpreter != null && !obj.adhocRemoteString) {
            obj.scriptInterpreter = data.scriptInterpreter.toString()
            if(data.interpreterArgsQuoted!=null) {
                obj.interpreterArgsQuoted = parseBoolean ? booleanVal(data.interpreterArgsQuoted) :
                                            data.interpreterArgsQuoted?.toString()
            }
        }
        if (data.fileExtension != null && !obj.adhocRemoteString) {
            obj.fileExtension = data.fileExtension.toString()
        }
        if (data.args != null && !obj.adhocRemoteString) {
            obj.argString = data.args.toString()
        }
    }

    private static boolean booleanVal(Object val) {
        if (val instanceof Boolean) {
            return val
        } else {
            return Boolean.parseBoolean(val.toString())
        }
    }

    public boolean isNodeStep(){
        return true;
    }

    public Boolean getNodeStep(){
        return isNodeStep();
    }
}
