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

import rundeck.data.constants.WorkflowStepConstants

/*
* CommandExec.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 25, 2010 3:02:01 PM
* $Id$
*/

public class CommandExec extends WorkflowStep  {
    String argString
    String adhocRemoteString
    String adhocLocalString
    String adhocFilepath
    Boolean adhocExecution = false
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

    public Map getConfiguration() { null }

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
        map.enabled = enabled
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

    static CommandExec fromMap(Map data) {
        CommandExec ce = new CommandExec()
        updateFromMap(ce, data)
        return ce
    }

    static void updateFromMap(CommandExec ce, Map data) {
        if(data.exec != null){
            ce.adhocExecution = true
            ce.adhocRemoteString=data.exec.toString()
        }else if(data.script != null){
            ce.adhocExecution = true
            ce.adhocLocalString=data.script.toString()
        }else if(data.scriptfile != null){
            ce.adhocExecution = true
            ce.adhocFilepath=data.scriptfile.toString()
            ce.expandTokenInScriptFile = !!data.expandTokenInScriptFile
        }else if(data.scripturl != null){
            ce.adhocExecution = true
            ce.adhocFilepath=data.scripturl.toString()
            ce.expandTokenInScriptFile = !!data.expandTokenInScriptFile
        }
        if(data.scriptInterpreter != null && !ce.adhocRemoteString){
            ce.scriptInterpreter=data.scriptInterpreter
            ce.interpreterArgsQuoted = !!data.interpreterArgsQuoted
        }
        if(data.fileExtension != null && !ce.adhocRemoteString){
            ce.fileExtension=data.fileExtension
        }
        if(data.args != null && !ce.adhocRemoteString){
            ce.argString=data.args.toString()
        }
        ce.keepgoingOnSuccess=!!data.keepgoingOnSuccess
        ce.description=data.description?.toString()
        //nb: error handler is created inside Workflow.fromMap
        if (data.plugins) {
            ce.pluginConfig = data.plugins
        }
        ce.enabled=data.enabled!=null?data.enabled:true
    }

    public boolean isNodeStep(){
        return true;
    }

    public Boolean getNodeStep(){
        return isNodeStep();
    }
}
