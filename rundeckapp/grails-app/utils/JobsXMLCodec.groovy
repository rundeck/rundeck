/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Matcher
import groovy.xml.MarkupBuilder


/*
* JobsXMLCodec encapsulates encoding and decoding of the Jobs XML format.
*
* the encode method takes a list of either ScheduledExecution instances, or Map instances with the same
* keys as field names in ScheduledExecution.  the output is the XML output string.  Alternatively, the
* encodeWithBuilder method accepts a groovy Builder instance which will be used to build the document.
* the decode method can accept an XML string, or a parsed groovy.util.Node instance.
*
* User: greg
* Created: Jul 24, 2008 11:17:29 AM
* $Id$
*/

class JobsXMLCodec {

    static encode = {list ->
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        JobsXMLCodec.encodeWithBuilder(list,xml)
        return writer.toString()
    }
    static encodeWithBuilder={ list,xml ->


//        def NodeBuilder xml = new NodeBuilder(writer)
        xml.joblist() {
            list.each{ jobi->
                job{
                    if(jobi.id){
                        id(jobi.id)
                    }
                    name(jobi.jobName)
                    description(jobi.description)
                    loglevel(jobi.loglevel)
                    if(jobi.groupPath){
                        group(jobi.groupPath)
                    }
                    context{
                        project(jobi.project)

                        if(!jobi.workflow){
                            if(jobi.type){
                                type(jobi.type)
                            }
                            if(jobi.name){
                                object(jobi.name)
                            }
                            if(jobi.command){
                                command(jobi.command)
                            }
                        }
                        if(jobi.argString||jobi.workflow){
                            def args=jobi.argString?jobi.argString.split(/ /):[]
                            def buf=''
                            def key=''
                            def opts=[:]
                            def match=/^-(.*)$/
                            def boolean m1=false
                            args.each{ val->
                                def Matcher m = val=~match
                                if(m.matches()){
                                    if(key){
                                       opts[key]=buf?buf:m1?'true':''
                                       buf=''
                                    }
                                    key=m.group(1);
                                    m1=true
                                }else{
                                    m1=false
                                    if(buf){
                                        buf+=' '
                                    }
                                    buf+=val
                                }
                            }
                            if(key){
                               opts[key]=buf?buf:m1?'true':''
                                buf=''
                            }
                            if(opts || jobi.options){
                                def keyset = new TreeSet(opts?opts.keySet():[])
                                def jobopts = [:]
                                if(jobi.options){
                                    jobi.options.each{opt->
                                        jobopts[opt.name]=opt
                                        keyset.add(opt.name)
                                    }
                                }
                                options{
                                    keyset.each{k->
                                        def optv=jobopts[k]
                                        def optp=[name:k]
                                        def defvalue=opts[k]?opts[k]:optv?optv.defaultValue:null
                                        if(defvalue){
                                            optp.value=defvalue
                                        }
                                        if(optv){
                                            optp.enforcedvalues=optv.enforced?'true':'false'
                                            optp.required=optv.required?'true':'false'
                                            if(optv.values){
                                                optp.values=optv.values.join(",")
                                            }else if (optv.valuesUrl) {
                                                optp.valuesUrl=optv.valuesUrl
                                            }
                                            if(optv.regex){
                                                optp.regex=optv.regex
                                            }
                                            if(optv.description){
                                                optp.description=optv.description
                                            }
                                        }
                                        option(optp);
                                    }
                                }
                            }

                        }

                        /*
                        <options>
                        <option name="number" values="one,two,three" value="one" regex="(?one|two|tree)"/>
                        <option name="name" valuesUrl="http://server/action?parm=${job.name}&parm2=${job.context.project}" />
                        </options>

                         */

                    }
                    if(jobi.adhocExecution && jobi.adhocRemoteString){
                        exec(jobi.adhocRemoteString)
                    }else if(jobi.adhocExecution && jobi.adhocLocalString){
                        script{
                            mkp.yieldUnescaped("<![CDATA["+jobi.adhocLocalString+"]]>")
                        }
                        if(jobi.argString){
                            scriptargs(jobi.argString)
                        }
                    }else if(jobi.adhocExecution && jobi.adhocFilepath){
                        scriptfile(jobi.adhocFilepath)
                        if(jobi.argString){
                            scriptargs(jobi.argString)
                        }
                    }
                    if(jobi.workflow){
                        sequence(threadcount:jobi.workflow.threadcount,keepgoing:jobi.workflow.keepgoing?Boolean.toString(true):Boolean.toString(false),strategy:jobi.workflow.strategy?jobi.workflow.strategy:'node-first'){
                            jobi.workflow.commands.each{def cexec->
                                def attrset=[:]
                                if(cexec.returnProperty){
                                    attrset['return']=cexec.returnProperty
                                }
                                if(cexec.ifString){
                                    attrset['if']=cexec.ifString
                                }
                                if(cexec.unlessString){
                                    attrset['unless']=cexec.unlessString
                                }
                                if(cexec.equalsString){
                                    attrset['equals']=cexec.equalsString
                                }
                                boolean isJobExec = (cexec instanceof Map && cexec.jobName || cexec instanceof JobExec)
                                if(!cexec.adhocExecution && !isJobExec){
                                    if(cexec.name){
                                        attrset['resource']=cexec.name
                                    }
                                    if(cexec.command){
                                        attrset['name']=cexec.command
                                    }
                                    if(cexec.type){
                                        attrset['module']=cexec.type
                                    }
                                }
                                command(attrset){
                                    if(cexec.adhocExecution){
                                        if(cexec.adhocRemoteString){
                                            exec(cexec.adhocRemoteString)
                                        }else if(cexec.adhocLocalString){
                                            script{
                                                mkp.yieldUnescaped("<![CDATA["+cexec.adhocLocalString+"]]>")
                                            }
                                            if(cexec.argString){
                                                scriptargs(cexec.argString)
                                            }
                                        }else if(cexec.adhocFilepath){
                                            scriptfile(cexec.adhocFilepath)
                                            if(cexec.argString){
                                                scriptargs(cexec.argString)
                                            }
                                        }
                                    }else if(isJobExec){
                                        def jobrefparams=[name:cexec.jobName]
                                        if(cexec.jobGroup){
                                            jobrefparams.group=cexec.jobGroup
                                        }
                                        jobref(jobrefparams){
                                            if(cexec.argString){
                                                arg(line:cexec.argString)
                                            }
                                        }

                                    }else{
                                        if(cexec.argString){
                                            arg(line:cexec.argString)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(jobi.doNodedispatch ){
                        nodefilters(excludeprecedence:jobi.nodeExcludePrecedence?"true":"false"){
                            def keys = [hostname: '', type: 'Type', tags: 'Tags', 'os-name': 'OsName', 'os-family': 'OsFamily',
                            'os-arch': 'OsArch', 'os-version': 'OsVersion','name':'Name']
                            if(keys.keySet().find{ek-> jobi."nodeExclude${keys[ek]}"}){
                                exclude{
                                    keys.keySet().each{ ek->
                                        if(jobi."nodeExclude${keys[ek]}"){
                                            "${ek}"(jobi."nodeExclude${keys[ek]}")
                                        }
                                    }
                                }
                            }
                            if(keys.keySet().find{ek-> jobi."nodeInclude${keys[ek]}"}){
                                include{
                                    keys.keySet().each{ ek->
                                        if(jobi."nodeInclude${keys[ek]}"){
                                            "${ek}"(jobi."nodeInclude${keys[ek]}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    dispatch{
                        threadcount(jobi.nodeThreadcount)
                        keepgoing(jobi.nodeKeepgoing?"true":"false")
                    }
                    if(jobi.scheduled){
                        schedule{
                            def tprops=[hour:jobi.hour,minute:jobi.minute]
                            if(jobi.seconds && jobi.seconds!='0'){
                                tprops['seconds']=jobi.seconds
                            }
                            time(tprops)
                            if(jobi.dayOfWeek!='?'){
                                weekday(day:jobi.dayOfWeek)
                            }
                            def mprops=[month:jobi.month]
                            if(jobi.dayOfMonth && jobi.dayOfMonth!='?'){
                                mprops['day']=jobi.dayOfMonth
                            }
                            month(mprops)
                            if(jobi.year && jobi.year!='*'){
                                year(year:jobi.year)
                            }
                        }
                    }
                    if(jobi.notifications){
                        notification{
                            jobi.notifications.each{note->
                                if(note.type=='email'){
                                    delegate."${note.eventTrigger}"{
                                        email(recipients:note.content)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return xml
    }
    static decode = {str ->
        def doc
        if(!(str instanceof String)){
            doc=str
        }else if(str instanceof String){
            def XmlSlurper parser = new XmlSlurper()

            try {
                doc = parser.parse(new StringReader(str))
            } catch (Exception e) {
                throw new JobXMLException( "Unable to parse xml: ${e}")
            }
        }
        
        if (!doc) {
            throw new JobXMLException( "XML Document could not be parsed.")
        }
        if(doc.name()!='joblist'){
            throw new JobXMLException( "Document root tag was not 'joblist': '${doc.name()}'")
        }
        def jobset = []
        if (!doc.job || doc.job.size() < 1) {
            throw new JobXMLException("No 'job' element was found")
        }
        def i=1;
            doc.job.each {def job ->
                Map props = [:]


                if (!job.name) {
                    throw new JobXMLException("'name' element not found")
                }
                props['jobName'] = job.name.text()
                if (!job.description) {
                    throw new JobXMLException("'description' element not found")
                }
                props['description'] = job.description.text()
                if(job.loglevel){
                    props['loglevel'] = job.loglevel.text()
                }
                if (job.context.size()<1) {
                    throw new JobXMLException("'context' element not found")
                }
                if (job.context.project.size()<1
                    && job.context.depot.size()<1 /* "depot" allowed for backwards compat */) {
                    throw new JobXMLException("'context/project' element not found")
                }
                if(job.group){
                    props['groupPath'] = job.group.text()
                    def re = /^\/*(.*?)\/*$/
                    def matcher = props.groupPath =~ re
                    if(matcher.matches()){
                        props.groupPath=matcher.group(1);
                    }
                    if(!props.groupPath){
                        props.groupPath=null
                    }
                }
                if(job.context.project.size()>0){
                    props['project'] = job.context.project.text()
                }else if(job.context.depot.size()>0){
                    /* depot allowed for backwards compat */
                    props['project'] = job.context.depot.text()
                }
                if (job.context.size()>0 && job.context.type.size()>0 && job.context.command.size()>0) {
                    props['type'] = job.context.type.text()
                    if (job.context.object) {
                        props['name'] = job.context.object.text()
                    } else {
                        props['name'] = ''
                    }
                    props['command'] = job.context.command.text()
                } else if (job.exec.size()>0) {
                    props['adhocExecution'] = 'true'
                    props['adhocExecutionType'] = 'remote'
                    props['command'] = ''
                    props['type'] = ''
                    props['name'] = ''
                    props['adhocRemoteString'] = job.exec.text()
                } else if (job.script.size()>0) {
                    props['adhocExecution'] = 'true'
                    props['adhocExecutionType'] = 'local'
                    props['command'] = ''
                    props['type'] = ''
                    props['name'] = ''
                    props['adhocLocalString'] = job.script.text()
                    props['argString'] = job.scriptargs?job.scriptargs.text():''
                } else if (job.scriptfile.size()>0) {
                    props['adhocExecution'] = 'true'
                    props['adhocExecutionType'] = 'file'
                    props['command'] = ''
                    props['type'] = ''
                    props['name'] = ''
                    props['adhocFilepath'] = job.scriptfile.text()
                    props['argString'] = job.scriptargs?job.scriptargs.text():''
                }else if(job.sequence.size()>0){
                    //generate workflow properties
                    props['command'] = ''
                    props['type'] = ''
                    props['name'] = ''
                    def wfprops =[:]
                    wfprops['threadcount']=job.sequence['@threadcount'].text()?job.sequence['@threadcount'].text():"1"
                    wfprops['keepgoing']=job.sequence['@keepgoing'].text()?job.sequence['@keepgoing'].text():"false"
                    wfprops['strategy']=job.sequence['@strategy'].text()?job.sequence['@strategy'].text():'node-first'
                    if(job.sequence.command.size()>0){
                        def cmdi=0
                        job.sequence.command.each{def cmddef->
                            def cmdprops=[:]
                            if(cmddef['@return'].text()){
                                cmdprops['returnProperty']=cmddef['@return'].text()
                            }
                            if(cmddef['@if'].text()){
                                cmdprops['ifString']=cmddef['@if'].text()
                            }
                            if(cmddef['@unless'].text()){
                                cmdprops['unlessString']=cmddef['@unless'].text()
                            }
                            if(cmddef['@equals'].text()){
                                cmdprops['equalsString']=cmddef['@equals'].text()
                            }
                            if(cmddef.exec.size()>0 ){
                                cmdprops['adhocExecution'] = 'true'
                                cmdprops['adhocRemoteString']=cmddef.exec.text()
                            }else if(cmddef.script.size()>0 ){
                                cmdprops['adhocExecution'] = 'true'
                                cmdprops['adhocLocalString']=cmddef.script.text()
                                if(cmddef.scriptargs.size()>0){
                                    cmdprops['argString']=cmddef.scriptargs.text()
                                }
                            }else if(cmddef.scriptfile.size()>0 ){
                                cmdprops['adhocExecution'] = 'true'
                                cmdprops['adhocFilepath']=cmddef.scriptfile.text()
                                if(cmddef.scriptargs.size()>0){
                                    cmdprops['argString']=cmddef.scriptargs.text()
                                }
                            }else if(cmddef.jobref.size()>0){
                                //define jobref
                                cmdprops['jobName']=cmddef.jobref['@name'].text()
                                if(cmddef.jobref['@group'].text()){
                                    cmdprops['jobGroup']=cmddef.jobref['@group'].text()
                                }
                                if(cmddef.jobref.arg.size()>0 && cmddef.jobref.arg['@line'].size()>0){
                                    cmdprops['argString']=cmddef.jobref.arg['@line'].text()
                                }
                            }else{
                                if(cmddef['@module'].text()){
                                    cmdprops['type']=cmddef['@module'].text()
                                }
                                if(cmddef['@name'].text()){
                                    cmdprops['command']=cmddef['@name'].text()
                                }
                                if(cmddef['@resource'].text()){
                                    cmdprops['name']=cmddef['@resource'].text()
                                }
                                if(cmddef.arg.size()>0 && cmddef.arg['@line'].size()>0){
                                    cmdprops['argString']=cmddef.arg['@line'].text()
                                }
                            }
                            wfprops["commands[${cmdi}]"]=cmdprops
                            cmdi++
                        }
                    }
                    props['workflow']=wfprops
                    props['_workflow_data']=true
                }else{
                    throw new JobXMLException("No valid job definition content found. context/type AND context/command OR exec OR script OR scriptfile or sequence are required")
                }
                if (job.nodefilters.size()>0 && (job.nodefilters.include.size()>0 || job.nodefilters.exclude.size()>0)) {
                    props['nodeExcludePrecedence'] = 'true'
                    if (job.nodefilters.@excludeprecedence.text()) {
                        props['nodeExcludePrecedence'] = job.nodefilters.@excludeprecedence.text()
                    }
                    def keys = [hostname: '', type: 'Type', tags: 'Tags', 'os-name': 'OsName', 'os-family': 'OsFamily',
                        'os-arch': 'OsArch', 'os-version': 'OsVersion']
                    keys.each {k, v ->
                        if (job.nodefilters.include[k].size()>0) {
                            props['nodeInclude' + v] = job.nodefilters.include[k].text()
                            props['doNodedispatch'] = "true"
                        }
                        if (job.nodefilters.exclude[k].size()>0) {
                            props['nodeExclude' + v] = job.nodefilters.exclude[k].text()
                            props['doNodedispatch'] = "true"
                        }
                    }
                    //'name',
                    if (job.nodefilters.exclude.name.size()>0) {
                        props['nodeExcludeName'] = job.nodefilters.exclude.name.text()
                        props['doNodedispatch'] = "true"
                    }
                    if (job.nodefilters.include.name.size()>0) {
                        props['nodeIncludeName'] = job.nodefilters.include.name.text()
                        props['doNodedispatch'] = "true"
                    }
                }
                if (job.dispatch && job.dispatch.threadcount) {
                    props['nodeThreadcount'] = job.dispatch.threadcount.text()
                }
                if (job.dispatch && job.dispatch.keepgoing) {
                    props['nodeKeepgoing'] = job.dispatch.keepgoing.text()
                }
                /*job.context.options.option.each {opt ->
                    def optname = opt['@name'].text()
                    def optval = opt['@value'].text()
                    props["command.option.${optname}"] = optval
                }*/

                //parse options input

                /*
                <options>
                <option name="number" values="one,two,three" value="one" regex="(?one|two|tree)"/>
                <option name="name" valuesUrl="http://server/action?parm=${job.name}&parm2=${job.context.project}" />
                </options>

                 */
                if(job.context.options.option.size()>0){
                    def optionsprops =[:]
                    def opti=0
                    job.context.options.option.each{def optdef->
                        def optname = optdef['@name'].text()
                        def optval = optdef['@value'].text()
                        def optprops=[:]
                        if(optdef['@name'].text()){
                            optprops['name']=optdef['@name'].text()
                        }
                        if(optdef['@value'].text()){
                            optprops['defaultValue']=optdef['@value'].text()
                        }
                        if(optdef['@regex'].text()){
                            optprops['regex']=optdef['@regex'].text()
                        }
                        if(optdef['@description'].text()){
                            optprops['description']=optdef['@description'].text()
                        }
                        if(optdef['@enforcedvalues'].text()){
                            optprops['enforced']=optdef['@enforcedvalues'].text()=='true'
                        }else{
                            optprops['enforced']=false
                        }
                        if(optdef['@required'].text()){
                            optprops['required']=optdef['@required'].text()=='true'
                        }else{
                            optprops['required']=false
                        }
                        if(optdef['@values'].text()){
                            def values=[]
                            values=optdef['@values'].text().split(',')
                            optprops['values']=values
                        }
                        if(optdef['@valuesUrl'].text()){
                            optprops['valuesUrl']=optdef['@valuesUrl'].text()
                        }
                        optionsprops["options[${opti}]"]=optprops
                        props["command.option.${optname}"] = optval
                        opti++
                    }
                    props['options']=optionsprops
                }else{
                    props['_nooptions']=true
                }
                props["argString"] = props['argString'] ? props['argString'] : ''
                if (job.schedule != null && job.schedule.size() > 0) {
                    props['scheduled'] = true
                    if(job.schedule['@crontab'].text()){
                        props['crontabString']=job.schedule['@crontab'].text()
                        props['useCrontabString']='true'
                    }else{
                    props['hour'] = job.schedule.time['@hour'].text()
                    props['minute'] = job.schedule.time['@minute'].text()
                    props['seconds'] = job.schedule.time['@seconds'].text()

                    def String days = job.schedule.weekday['@day'].text()
                    def String months = job.schedule.month['@month'].text()
                    props.dayOfMonth = job.schedule.month['@day'].text()
                    props.year = job.schedule.year['@year'].text()

                    def rangpat = /^(.+)-(.+)$/

                    def parseRangeForList = {String input, List list, key ->
                        def aprop = [:]
                        def inputl = Arrays.asList(input.split(','))
                        def uplist = list.collect {it.toUpperCase()}
                        inputl.each {String dayv ->
                            def mat = dayv =~ rangpat
                            if (mat.matches()) {
                                String m1 = mat.group(1)
                                String m2 = mat.group(2)

                                def a = uplist.indexOf(m1.toUpperCase())
                                def b = uplist.indexOf(m2.toUpperCase())
                                //increment index if found
                                if(a>=0){
                                    a++
                                }
                                if(b>=0){
                                    b++
                                }

                                if(a<0 || b<0){
                                    try{
                                        a=m1.toInteger()
                                        b=m2.toInteger()
                                    }catch(NumberFormatException){

                                    }
                                }
                                if(a>0 && b>0 && a<=list.size() && b<=list.size()){
                                    def rang = a..b
                                    rang.each {
                                        def name = list[it - 1]
                                        aprop["${key}.${name}"] = "true"
                                    }
                                }
                            } else {
                                def a = uplist.indexOf(dayv.toUpperCase())
                                //increment index if found
                                if(a>=0){
                                    a++
                                }
                                if(a<0){
                                    try{
                                    a = dayv.toInteger()
                                    }catch(NumberFormatException e){

                                    }
                                }
                                if (a > 0 && a <= list.size()) {
                                    def name = list[a - 1]
                                    aprop["${key}.${name}"] = "true"
                                }
                            }
                        }
                        return aprop
                    }

                    if (days == '*') {
                        props['everyDayOfWeek'] = true
                    } else if(days){
                        def newprops = parseRangeForList(days, ScheduledExecution.daysofweeklist, "crontab.dayOfWeek")
                        if('?'==days || !newprops){
                            props['dayOfWeek']=days
                        }else{
                            props.putAll(newprops)
                        }
                    }else{
                        props['dayOfWeek']=props.dayOfMonth && props.dayOfMonth !='?'?'?':'*'
                    }

                    if (months == '*') {
                        props['everyMonth'] = true
                    } else {
                        def newprops = parseRangeForList(months, ScheduledExecution.monthsofyearlist, "crontab.month")
                        if(!newprops){
                            props['month']=months
                        }else{
                            props.putAll(newprops)
                        }
                    }
                    }
                }else{
                    props['scheduled'] = false
                }
                //add notification definitions
                if(job.notification != null && job.notification.size() > 0){
                    def nots=[:]
                    if(job.notification.onsuccess.size()>0 ){
                        if( job.notification.onsuccess.email.size()>0){
                            def emails = job.notification.onsuccess.email['@recipients'].text()
                            if(emails){
                                nots['onsuccess']=[email:emails]
                            }else{
                                throw new JobXMLException("onsuccess handler had blank or missing 'recipients' attribute (job #${i}:${props['jobName']})")
                            }
                        }else{
                            throw new JobXMLException("notification 'onsuccess' element had missing 'email' element (job #${i}:${props['jobName']})")
                        }
                    }
                    if(job.notification.onfailure.size()>0 ){
                        if(job.notification.onfailure.email.size()>0){
                            def emails = job.notification.onfailure.email['@recipients'].text()
                            if(emails){
                                nots['onfailure']=[email:emails]
                            }else{
                                throw new JobXMLException("onfailure handler had blank or missing 'recipients' attribute (job #${i}:${props['jobName']})")
                            }
                        }else{
                            throw new JobXMLException("notification 'onfailure' element had missing 'email' element (job #${i}:${props['jobName']})")
                        }
                    }
                    if(nots){
                        props['notifications']=nots
                    }else{
                        throw new JobXMLException("notification section no onsuccess or onfailure element (job #${i}:${props['jobName']})")
                    }
                }
                //now use props to create ScheduledExecution
                jobset << props
                i++
            }

        return jobset
    }
}
