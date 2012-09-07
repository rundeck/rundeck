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
        BuilderUtil bu = new BuilderUtil()
        xml.joblist() {
            list.each{ ScheduledExecution jobi->
                job{
                    bu.mapToDom(JobsXMLCodec.convertJobMap(jobi.toMap()),delegate)
                }
            }
        }
        return xml
    }
    static decode = {str ->
        def doc
        def reader
        if(str instanceof File || str instanceof InputStream){
            reader = str
        }else if(str instanceof String){
            reader=new StringReader(str)
        }else {
            doc=str
        }
        if(!doc){
            def XmlParser parser = new XmlParser()
            try {
                doc = parser.parse(reader)
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
        return JobsXMLCodec.convertToJobs(doc.job)
    }
    /**
     * Convert set of xml nodes to jobs
     */
    static convertToJobs={ data->
        def list=[]
        data.each{ list<<JobsXMLCodec.convertToJobMap(it) }
        return JobsYAMLCodec.createJobs(list)
    }

    /**
     *  Convert structure produced by XmlParserUtil parsing jobs.xml, and produce map data suitable for passing to
     * {@link ScheduledExecution#fromMap(Map) }.
     * This should be the reverse process from convertJobMap
     *
     */
    static convertToJobMap={ data->
        final Object object = XmlParserUtil.toObject(data)
        if(!(object instanceof Map)){
            throw new JobXMLException("Expected map data")
        }
        Map map = (Map)object

        //perform structure conversions for expected input for populating ScheduledExecution

        if(!map.context){
            throw new JobXMLException("'context' element not found")
        }
        map.project=map.context.remove('project')
        if(!map.project && map.context.depot){
            map.project=map.context.remove('depot')
        }
        if(!map.project){
            throw new JobXMLException("'context/project' element not found")
        }
        if(!map.name){
            throw new JobXMLException("'name' element not found")
        }
        if(null==map.description){
            throw new JobXMLException("'description' element not found")
        }


        if(map.group){
            def group=map.group
            def re = /^\/*(.*?)\/*$/
            def matcher = group =~ re
            if(matcher.matches()){
                map.group=matcher.group(1);
            }
            if(!map.group){
                map.remove('group')
            }
        }
        //convert options:[option:[]] into options:[]
        if(map.context?.options?.option){
            final def opts = map.context.options.remove('option')
            map.remove('context')
            map.options=[:]
            if (opts && opts instanceof Map){
                opts=[opts]
            }
            if(opts && opts instanceof Collection){
                opts.each{optm->
                    map.options[optm.name.toString()]=optm
                    if (optm.values instanceof String) {
                        optm.values = optm.values.split(",") as List
                    } else if (optm.values) {
                        optm.values = [optm.values.toString()]
                    }
                    if(null!=optm.enforcedvalues){
                        optm.enforced=optm.remove('enforcedvalues')
                    }
                }
            }
        }

        if(map.dispatch){
            if(!map.nodefilters){
                map.nodefilters=[:]
            }
            map.nodefilters.dispatch=map.remove('dispatch')
            if(null!=map.nodefilters.excludeprecedence){
                map.nodefilters.dispatch['excludePrecedence']=map.nodefilters.remove('excludeprecedence')
            }
        }
        if(map.schedule){
            if(map.schedule.month?.day){
                map.schedule.dayofmonth=[day:map.schedule.month.remove('day')]
            }
            if(map.schedule.month?.month){
                map.schedule.month=map.schedule.month.remove('month')
            }
            if(map.schedule.year?.year){
                map.schedule.year=map.schedule.year.remove('year')
            }
        }
        if(!map.sequence){
            throw new JobXMLException("'sequence' element not found")
        }
        convertXmlWorkflowToMap(map.sequence)

        if(null!=map.notification){
            if(!map.notification || null==map.notification.onsuccess && null==map.notification.onfailure){
                throw new JobXMLException("notification section had no onsuccess or onfailure element")
            }
            ['onsuccess','onfailure'].each{trigger->
                if(null!=map.notification[trigger]){

                    if(!map.notification[trigger] || null==map.notification[trigger].email && null == map.notification[trigger].webhook){
                        throw new JobXMLException("notification '${trigger}' element had missing 'email' or 'webhook' element")
                    }
                    if(null!=map.notification[trigger].email && (!map.notification[trigger].email || !map.notification[trigger].email.recipients)){
                        throw new JobXMLException("${trigger} email had blank or missing 'recipients' attribute")
                    }
                    if(null !=map.notification[trigger].webhook && (!map.notification[trigger].webhook || !map.notification[trigger].webhook.urls)){
                        throw new JobXMLException("${trigger} webhook had blank or missing 'urls' attribute")
                    }
                    if(map.notification[trigger].email){
                        map.notification[trigger].recipients=map.notification[trigger].email.remove('recipients')
                        map.notification[trigger].remove('email')
                    }
                    if(map.notification[trigger].webhook){
                        map.notification[trigger].urls = map.notification[trigger].webhook.remove('urls')
                        map.notification[trigger].remove('webhook')
                    }
                }
            }
        }
        return map
    }
    static convertXmlWorkflowToMap(Map data){
        if (data?.command) {
            data.commands = data.remove('command')
            if (!(data.commands instanceof Collection)) {
                data.commands = [data.remove('commands')]
            }  //convert script args values to idiosyncratic label
            def fixup = { cmd ->
                if (cmd.scriptfile || cmd.script || cmd.scripturl) {
                    cmd.args = cmd.remove('scriptargs')
                } else if (cmd.jobref?.arg?.line) {
                    cmd.jobref.args = cmd.jobref.arg.remove('line')
                    cmd.jobref.remove('arg')
                }
            }
            data.commands.each(fixup)
            data.commands.each {
                if (it.errorhandler) {
                    fixup(it.errorhandler)
                }
            }
        }
    }
    /**
     * Convert structure returned by job.toMap into correct structure for jobs xml
     */
    static convertJobMap={Map map->
        map.context=[project:map.remove('project')]
        final Map opts = map.remove('options')
        if(null!=opts){
            def optslist=[]
            //xml expects sorted option names
            opts.keySet().sort().each{
                def x = opts[it]
                //add 'name' attribute
                BuilderUtil.addAttribute(x,'name',it)
                //convert to attributes: 'value','regex','valuesUrl'
                BuilderUtil.makeAttribute(x,'value')
                BuilderUtil.makeAttribute(x,'regex')
                BuilderUtil.makeAttribute(x,'valuesUrl')
                //convert 'values' list to comma-separated attribute value @values
                if(x.values){
                    BuilderUtil.addAttribute(x,'values',x.remove('values').join(","))
                }
                if(x.enforced){
                    //convert 'enforced' to @enforcedvalues
                    BuilderUtil.addAttribute(x,'enforcedvalues',x.remove('enforced'))
                }else{
                    x.remove('enforced')
                }
                if(x.secure){
                    //convert 'secure' to @secure
                    BuilderUtil.addAttribute(x,'secure',x.remove('secure'))
                }else{
                    x.remove('secure')
                }
                if(x.valueExposed){
                    //convert 'valueExposed' to @valueExposed
                    BuilderUtil.addAttribute(x,'valueExposed',x.remove('valueExposed'))
                }else{
                    x.remove('valueExposed')
                }
                if(x.required){
                    //convert 'required' to attribute
                    BuilderUtil.makeAttribute(x,'required')
                }else{
                    x.remove('required')
                }
                if(x.multivalued){
                    //convert 'multivalued' and delimiter to attribute
                    BuilderUtil.makeAttribute(x,'multivalued')
                    BuilderUtil.makeAttribute(x,'delimiter')
                }else{
                    x.remove('multivalued')
                    x.remove('delimiter')
                }
                optslist<<x
            }
            map.context[BuilderUtil.pluralize('option')]=optslist
        }
        if(map.nodefilters?.dispatch){
            map.dispatch=map.nodefilters.remove('dispatch')
        }
        if(map.schedule){
            BuilderUtil.makeAttribute(map.schedule.time,'seconds')
            BuilderUtil.makeAttribute(map.schedule.time,'minute')
            BuilderUtil.makeAttribute(map.schedule.time,'hour')
            BuilderUtil.makeAttribute(map.schedule.weekday,'day')
            if(map.schedule.month){
                map.schedule.month=BuilderUtil.toAttrMap('month',map.schedule.remove('month'))
            }

            if(map.schedule.dayofmonth?.day){
                def val=map.schedule.dayofmonth.remove('day')
                if(map.schedule.month){
                    BuilderUtil.addAttribute(map.schedule.month,'day',val)
                }else{
                    map.schedule.month=BuilderUtil.toAttrMap('day',val)
                }
            }

            if(map.schedule.year){
                map.schedule.year=BuilderUtil.toAttrMap('year',map.schedule.remove('year'))
            }
        }

        convertWorkflowMapForBuilder(map.sequence)
        if(map.notification){
            ['onsuccess','onfailure'].each{trigger->
                if(map.notification[trigger]){
                    if(map.notification[trigger]?.recipients){
                        map.notification[trigger].email=BuilderUtil.toAttrMap('recipients',map.notification[trigger].remove('recipients'))
                    }
                    if(map.notification[trigger]?.urls){
                        map.notification[trigger].webhook=BuilderUtil.toAttrMap('urls',map.notification[trigger].remove('urls'))
                    }
                }
            }
        }
        return map
    }

    /**
     * Convert result of Workflow.toMap() to format used by BuilderUtil
     * @param map
     */
    static void convertWorkflowMapForBuilder(Map map) {
        BuilderUtil.makeAttribute(map, 'keepgoing')
        BuilderUtil.makeAttribute(map, 'strategy')
        map.command = map.remove('commands')
        //convert script args values to idiosyncratic label

        def gencmd= { cmd, iseh=false ->
            if (cmd.scriptfile || cmd.script || cmd.scripturl) {
                cmd.scriptargs = cmd.remove('args')
                if (cmd.script) {
                    cmd[BuilderUtil.asCDATAName('script')] = cmd.remove('script')
                }
            } else if (cmd.jobref) {
                BuilderUtil.makeAttribute(cmd.jobref, 'name')
                if (cmd.jobref.group) {
                    BuilderUtil.makeAttribute(cmd.jobref, 'group')
                } else {
                    cmd.jobref.remove('group')
                }
                final def remove = cmd.jobref.remove('args')
                if (null != remove) {
                    cmd.jobref.arg = BuilderUtil.toAttrMap('line', remove)
                }
            }
            if(iseh){
                BuilderUtil.makeAttribute(cmd, 'keepgoingOnSuccess')
            }
        }
        map.command.each(gencmd)
        map.command.each {
            if (it.errorhandler) {
                gencmd(it.errorhandler,true)
            }
        }
    }

}
