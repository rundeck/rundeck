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

package rundeck.codecs

import com.dtolabs.rundeck.app.support.BuilderUtil
import com.dtolabs.rundeck.util.XmlParserUtil
import groovy.xml.MarkupBuilder
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.ScheduledExecution
import rundeck.controllers.JobXMLException

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
* @deprecated Should not be used directly or via grails `encodeAs/decode`, use the RundeckJobDefinitionManager
*/
@Deprecated
class JobsXMLCodec {

    /**
     * @deprecated do not use this directly, instead use the injected JobDefinitionManager.exportAsXml
     */
    @Deprecated
    static encode = { list ->
        RundeckJobDefinitionManager rundeckJobDefinitionManager = new RundeckJobDefinitionManager()
        rundeckJobDefinitionManager.exportAsXml(list)
    }
    static encodeMaps (list, boolean preserveUuid = true, Map<String, String> replaceIds = [:] ){
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.expandEmptyElements = false
        encodeMapsWithBuilder(list, xml, preserveUuid, replaceIds)
        return writer.toString()
    }

    static encodeMapsWithBuilder(list, xml, boolean preserveUuid = true, Map<String, String> replaceIds = [:], String stripJobRef = null) {
        BuilderUtil bu = new BuilderUtil()
        bu.forceLineEndings=true
        bu.lineEndingChars='\n'
        //todo: set line ending from config?
        bu.canonical=true
        xml.joblist() {
            list.each{ Map jobMap->
                job{
                    bu.mapToDom(
                            JobsXMLCodec.convertJobMap(jobMap, preserveUuid, replaceIds[jobMap.id], stripJobRef),
                            delegate
                    )
                }
            }
        }
        return xml
    }

    /**
     * @deprecated do not use this directly, instead use the injected JobDefinitionManager.decodeXml
     */
    @Deprecated
    static decode = { str ->
        RundeckJobDefinitionManager manager = new RundeckJobDefinitionManager()
        if(str instanceof File){
            return manager.decodeXml(str)*.job
        } else if( str instanceof InputStream ) {
            return manager.decodeXml(new InputStreamReader(str,"UTF-8"))*.job
        }else if(str instanceof Reader){
            return manager.decodeXml(str)*.job
        }else if(str instanceof String){
            return manager.decodeXml(new StringReader(str))*.job
        }

        throw new JobXMLException( "XML Document could not be parsed.")
    }

    /**
     *  Convert structure produced by XmlParserUtil parsing jobs.xml, and produce map data suitable for passing to
     * {@link ScheduledExecution#fromMap(Map) }.
     * This should be the reverse process from convertJobMap
     *
     */
    static convertToJobMap= { data ->
        final Object object = XmlParserUtil.toObject(data, false)
        if (!(object instanceof Map)) {
            throw new JobXMLException("Expected map data")
        }
        convertXMapToJobMap((Map) object)
    }

    /**
     * Convert the XMap (Map produced by XmlParserUtil) to canonical Job definition Map
     */
    static convertXMapToJobMap={ Map map->
        map.scheduleEnabled = XmlParserUtil.stringToBool(map.scheduleEnabled, true)
        map.executionEnabled = XmlParserUtil.stringToBool(map.executionEnabled, true)
        map.nodeFilterEditable = XmlParserUtil.stringToBool(map.nodeFilterEditable, true)
        map.multipleExecutions = XmlParserUtil.stringToBool(map.multipleExecutions, false)

        //perform structure conversions for expected input for populating ScheduledExecution

        if(map.context instanceof Map) {
            map.project = map.context?.remove('project')
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
        if(map.logging){
            map.loglimit = map.logging.remove('limit')
            map.loglimitAction=map.logging.remove('limitAction')?:'halt'
            if(map.logging.status){
                map.loglimitStatus=map.logging.remove('status')
            }
            map.remove('logging')
        }
        //convert options:[option:[]] into options:[]

        if(map.context instanceof Map) {
            if (map.context?.options && !(map.context?.options instanceof Map)) {
                throw new JobXMLException("'context/options' element is not valid")
            }
            if (map.context?.options && map.context?.options?.option) {
                final def opts = map.context.options.remove('option')
                def ndx = XmlParserUtil.stringToBool(map.context.options.preserveOrder, false) ? 0 : -1;
                map.remove('context')
                map.options = [:]
                if (opts && opts instanceof Map) {
                    opts = [opts]
                }
                def convertOptionConfig={Map oconfig->
                    def e=oconfig.remove('entry')
                    def confMap=[:]
                    if(e instanceof Map){
                        confMap[e['key']]=e['value']
                    }else if(e instanceof Collection){
                        e.each{
                            confMap[it['key']] = it['value']
                        }
                    }
                    confMap
                }
                //if preserveOrder is true, include sortIndex information
                if (opts && opts instanceof Collection) {
                    opts.each { optm ->
                        map.options[optm.name.toString()] = optm
                        if (optm.values instanceof String) {
                            optm.values = optm.values.split(",") as List
                        } else if (optm.values) {
                            optm.values = [optm.values.toString()]
                        }
                        if (null != optm.enforcedvalues) {
                            optm.enforced = XmlParserUtil.stringToBool(optm.remove('enforcedvalues'), false)
                        }
                        if (null != optm.required) {
                            optm.required = XmlParserUtil.stringToBool(optm.remove('required'), false)
                        }
                        if (null != optm.multivalued) {
                            optm.multivalued = XmlParserUtil.stringToBool(optm.remove('multivalued'), false)
                        }
                        if(optm.multivalued) {
                            optm.multivaluedAllSelected = XmlParserUtil.stringToBool(
                                    optm.remove('multivaluedAllSelected'),
                                    false
                            )
                        }
                        if(optm.isDate) {
                            optm.isDate = XmlParserUtil.stringToBool(
                                    optm.remove('isDate'),
                                    false
                            )
                        }
                        if (ndx > -1) {
                            optm.sortIndex = ndx++;
                        }
                        if(optm.config && optm.config instanceof Map){
                            optm.config=convertOptionConfig(optm.remove('config'))
                        }
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
            if(null!=map.nodefilters.dispatch.keepgoing){
                //convert to boolean
                def value= map.nodefilters.dispatch.keepgoing
                map.nodefilters.dispatch.keepgoing= XmlParserUtil.stringToBool(value,false)
            }
            if(null!=map.nodefilters.dispatch.excludePrecedence){
                //convert to boolean
                def value= map.nodefilters.dispatch.excludePrecedence
                map.nodefilters.dispatch.excludePrecedence= XmlParserUtil.stringToBool(value,false)
            }
            if(null!=map.nodefilters.dispatch.successOnEmptyNodeFilter){
                //convert to boolean
                def value= map.nodefilters.dispatch.successOnEmptyNodeFilter
                map.nodefilters.dispatch.successOnEmptyNodeFilter= XmlParserUtil.stringToBool(value,false)
            }
            if(map.nodesSelectedByDefault){
                map.nodesSelectedByDefault=XmlParserUtil.stringToBool(map.nodesSelectedByDefault,false)
            }
        }
        if(map.schedule){
            if(map.schedule.month && map.schedule.month instanceof Map && map.schedule.month?.day){
                map.schedule.dayofmonth=[day:map.schedule.month.remove('day')]
            }
            if(map.schedule.month && map.schedule.month instanceof Map && map.schedule.month?.month){
                map.schedule.month=map.schedule.month.remove('month')
            }else{
                map.schedule.month=null
            }
            if(map.schedule.year && map.schedule.year instanceof Map && map.schedule.year?.year){
                map.schedule.year=map.schedule.year.remove('year')
            }else{
                map.schedule.year=null
            }
        }
        if(!map.sequence){
            throw new JobXMLException("'sequence' element not found")
        }
        convertXmlWorkflowToMap(map.sequence)

        if(null!=map.notification){
            if(!map.notification || !(map.notification instanceof Map)){
                throw new JobXMLException("notification section had no trigger elements")
            }
            def triggers = map.notification?.keySet().findAll { it.startsWith('on') }
            if( !triggers){
                throw new JobXMLException("notification section had no trigger elements")
            }
            def convertPluginToMap={Map plugin->
                def e=plugin.configuration?.remove('entry')
                def confMap=[:]
                if(e instanceof Map){
                    confMap[e['key']]=e['value']
                }else if(e instanceof Collection){
                    e.each{
                        confMap[it['key']] = it['value']
                    }
                }
                plugin['configuration']=confMap
            }
            triggers.each{trigger->
                if(null!=map.notification[trigger]){
                    if(!map.notification[trigger] || null==map.notification[trigger].email && null == map.notification[trigger].webhook
                            && null == map.notification[trigger].plugin
                    ){
                        throw new JobXMLException("notification '${trigger}' element had missing 'email' or 'webhook' or 'plugin' element")
                    }
                    if(null!=map.notification[trigger].email && (!map.notification[trigger].email || !map.notification[trigger].email.recipients)){
                        throw new JobXMLException("${trigger} email had blank or missing 'recipients' attribute")
                    }
                    if(null !=map.notification[trigger].webhook && (!map.notification[trigger].webhook || !map.notification[trigger].webhook.urls)){
                        throw new JobXMLException("${trigger} webhook had blank or missing 'urls' attribute")
                    }
                    if(null !=map.notification[trigger].plugin){
                        if(!map.notification[trigger].plugin){
                            throw new JobXMLException("${trigger} plugin element was empty")
                        }
                        if(map.notification[trigger].plugin instanceof Map && !map.notification[trigger].plugin.type){
                            throw new JobXMLException("${trigger} plugin had blank or missing 'type' attribute")
                        }
                        if(map.notification[trigger].plugin instanceof Collection && !map.notification[trigger].plugin.every{it.type}){
                            throw new JobXMLException("${trigger} plugin had blank or missing 'type' attribute")
                        }
                    }
                    if(map.notification[trigger].webhook){
                        map.notification[trigger].urls = map.notification[trigger].webhook.remove('urls')
                        map.notification[trigger].remove('webhook')
                    }
                    if(map.notification[trigger].plugin && map.notification[trigger].plugin instanceof Map){
                        convertPluginToMap(map.notification[trigger].plugin)
                    }else if(map.notification[trigger].plugin && map.notification[trigger].plugin instanceof Collection){
                        map.notification[trigger].plugin.each{
                            convertPluginToMap(it)
                        }
                    }
                }
            }
        }
        if(map.retry instanceof Map){
            if(map.retry.delay){
                map.retryDelay = map.retry.delay
            }
            map.retry = map.retry['<text>']
        }
        if (map.plugins) {
            map.plugins = decodePlugins(map.plugins)
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
                if (cmd.scriptfile!=null || cmd.script!=null || cmd.scripturl!=null) {
                    cmd.args = cmd.remove('scriptargs')?.toString()
                    if(cmd.scriptinterpreter instanceof Map){
                        cmd.interpreterArgsQuoted = XmlParserUtil.stringToBool(cmd.scriptinterpreter.remove
                                ('argsquoted'),false)
                        cmd.scriptInterpreter = cmd.scriptinterpreter.remove('<text>')
                    }else if(cmd.scriptinterpreter instanceof String){
                        cmd.scriptInterpreter = cmd.remove('scriptinterpreter')
                    }else if(cmd.scriptinterpreter !=null){
                        throw new JobXMLException("'command/scriptinterpreter' value incorrect: ${cmd.scriptinterpreter}, expected String or map")
                    }
                } else if (cmd.jobref!=null) {
                    if(!(cmd.jobref instanceof Map)){
                        throw new JobXMLException("'jobref' value incorrect: ${cmd.jobref}, expected elements: arg, group, name")
                    }
                    if(cmd.jobref.arg!=null){
                        if (!(cmd.jobref.arg instanceof Map)) {
                            throw new JobXMLException("'jobref/arg' value incorrect: ${cmd.jobref.arg}, expected attribute: line")
                        }
                        cmd.jobref.args = cmd.jobref.arg.remove('line')?.toString()
                        cmd.jobref.remove('arg')
                    }
                    if (null != cmd.jobref.nodeStep) {
                        cmd.jobref.nodeStep = XmlParserUtil.stringToBool(cmd.jobref.nodeStep, false)
                    }
                    if (null != cmd.jobref.dispatch && (cmd.jobref.nodefilters instanceof Map)) {
                        cmd.jobref.nodefilters.dispatch = cmd.jobref.remove('dispatch')
                    } else if (null != cmd.jobref.dispatch) {
                        cmd.jobref.nodefilters = [dispatch: cmd.jobref.remove('dispatch')]
                    }
                    if (cmd.project) {
                        cmd.jobref.project = cmd.remove('project')
                    }
                }else if(cmd['node-step-plugin'] || cmd['step-plugin']){
                    def parsePluginConfig={ plc->
                        def outconf=[:]
                        if (plc?.entry instanceof Map && plc?.entry['key']) {
                            outconf[plc?.entry['key']] = plc?.entry['value']
                        } else if (plc?.entry instanceof Collection) {
                            plc?.entry.each { o ->
                                if (o instanceof Map && o['key']) {
                                    outconf[o['key']] = o['value']
                                }
                            }
                        }
                        outconf
                    }
                    if(cmd['node-step-plugin']){
                        def plugin= cmd.remove('node-step-plugin')

                        cmd.nodeStep=true
                        cmd.type = plugin.type
                        cmd.configuration= plugin.configuration?parsePluginConfig(plugin.configuration):null
                    }else if(cmd['step-plugin']){
                        def plugin= cmd.remove('step-plugin')

                        cmd.nodeStep = false
                        cmd.type = plugin.type
                        cmd.configuration = plugin.configuration ? parsePluginConfig(plugin.configuration) : null
                    }
                }
                if(null!= cmd.keepgoingOnSuccess){
                    cmd.keepgoingOnSuccess= XmlParserUtil.stringToBool(cmd.keepgoingOnSuccess,false)
                }
                if (cmd.plugins && cmd.plugins instanceof Map && cmd.plugins.LogFilter ) {
                    if(!(cmd.plugins.LogFilter instanceof Collection)){
                        cmd.plugins.LogFilter = [cmd.plugins.remove('LogFilter')]
                    }
                    cmd.plugins.LogFilter.each {
                        if (!it.config) {
                            //remove potential empty string result
                            it.remove('config')
                        }
                    }
                }
            }
            data.commands.each(fixup)
            data.commands.each {
                if (it.errorhandler) {
                    fixup(it.errorhandler)
                }
            }
        }
        if(null!=data.keepgoing && data.keepgoing instanceof String){
            data.keepgoing = XmlParserUtil.stringToBool(data.keepgoing,false)
        }
        if (data.pluginConfig && data.pluginConfig instanceof Map
                && data.pluginConfig?.LogFilter ) {
            if(!(data.pluginConfig?.LogFilter instanceof Collection)){
                data.pluginConfig.LogFilter = [data.pluginConfig.remove('LogFilter')]
            }
            data.pluginConfig.LogFilter.each {
                if (!it.config) {
                    //remove potential empty string result
                    it.remove('config')
                }
            }
        }
    }
    /**
     * Convert structure returned by job.toMap into correct structure for jobs xml
     */
    static convertJobMap = { Map map, boolean preserveUuid = true, String replaceId = null, String stripJobRef = null ->
        map.remove('project')
        if (!preserveUuid) {
            map.remove('id')
            map.remove('uuid')
            if (replaceId) {
                map['id'] = replaceId
                map['uuid'] = replaceId
            }
        }

        def optdata = map.remove('options')
        boolean preserveOrder=false
        if (map.description && map.description.indexOf('\n') >= 0) {
            map[BuilderUtil.asCDATAName('description')] = map.remove('description')
        }
        if(null!=optdata){
            map.context=[:]
            def opts
            if(optdata instanceof Map){
                opts=optdata.values().sort{a,b->
                    if(null != a.sortIndex && null != b.sortIndex){
                        return a.sortIndex<=>b.sortIndex
                    }else if (null == a.sortIndex && null == b.sortIndex) {
                        return a.name <=> b.name
                    }else{
                        return a.sortIndex!=null?-1:1
                    }
                }
            }else if(optdata instanceof Collection){
                preserveOrder=true
                opts=optdata
            }
            def optslist=[]
            //options are sorted by (sortIndex, name)
            opts.each{x->
                x.remove('sortIndex')
                //add 'name' attribute
                BuilderUtil.addAttribute(x,'name',x.remove('name'))
                //convert to attributes: 'value','regex','valuesUrl'
                BuilderUtil.makeAttribute(x,'value')
                BuilderUtil.makeAttribute(x,'regex')
                BuilderUtil.makeAttribute(x,'valuesUrl')
                if(x.description?.indexOf('\n')>=0 || x.description?.indexOf('\r')>=0){
                    x[BuilderUtil.asCDATAName('description')]=x.remove('description')
                }
                //convert 'values' list to comma-separated attribute value @values
                if(x.values){
                    BuilderUtil.addAttribute(x,'values',x.remove('values').join(","))
                }
                if(x.valuesListDelimiter){
                    BuilderUtil.addAttribute(x,'valuesListDelimiter',x.remove('valuesListDelimiter'))
                }
                if(x.enforced){
                    //convert 'enforced' to @enforcedvalues
                    BuilderUtil.addAttribute(x,'enforcedvalues',x.remove('enforced'))
                }else{
                    x.remove('enforced')
                }
                ['secure','valueExposed','storagePath'].each{key->
                    if(x[key]){
                        //convert 'enforced' to @enforcedvalues
                        BuilderUtil.addAttribute(x,key,x.remove(key))
                    }else{
                        x.remove(key)
                    }
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
                    BuilderUtil.makeAttribute(x, 'multivalueAllSelected')
                }else{
                    x.remove('multivalued')
                    x.remove('delimiter')
                    x.remove('multivalueAllSelected')
                }
                if(x.type) {
                    BuilderUtil.addAttribute(x, 'type', x.remove('type'))
                    if(x.config){
                        def config = x.remove('config')
                        config.keySet().sort().each { k ->
                            def v = config[k]
                            if (!x['config']) {
                                x['config'] = [entry: []]
                            }
                            def entryMap= [key: k, value: v]
                            BuilderUtil.makeAttribute(entryMap,'key')
                            BuilderUtil.makeAttribute(entryMap,'value')
                            x['config']['entry'] <<  entryMap
                        }
                    }
                }
                optslist<<x
            }
            if(preserveOrder){
                map.context['options'] = [option:optslist] + BuilderUtil.toAttrMap('preserveOrder',true)
            }else{
                map.context[BuilderUtil.pluralize('option')] = optslist
            }
        }
        if(map.nodefilters?.dispatch){
            map.dispatch=map.nodefilters.remove('dispatch')
        }
        if(map.loglimit){
            map.logging=BuilderUtil.toAttrMap('limit',map.remove('loglimit'))
            BuilderUtil.addAttribute(map.logging,'limitAction',map.remove('loglimitAction')?:'halt')
            if(map.loglimitStatus){
                BuilderUtil.addAttribute(map.logging,'status',map.remove('loglimitStatus'))
            }
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
        if(map.retry instanceof Map && map.retry.delay){
            map.retry = ['<text>':map.retry.retry,delay:map.retry.delay]
            BuilderUtil.makeAttribute(map.retry,'delay')
        }

        convertWorkflowMapForBuilder(map.sequence, stripJobRef)
        if(map.notification){
            def convertNotificationPlugin={Map pluginMap->
                def confMap = pluginMap.remove('configuration')
                BuilderUtil.makeAttribute(pluginMap, 'type')
                confMap.keySet().sort().each { k ->
                    def v = confMap[k]
                    if (!pluginMap['configuration']) {
                        pluginMap['configuration'] = [entry: []]
                    }
                    def entryMap= [key: k, value: v]
                    BuilderUtil.makeAttribute(entryMap,'key')
                    BuilderUtil.makeAttribute(entryMap,'value')
                    pluginMap['configuration']['entry'] <<  entryMap
                }
            }
            map.notification.keySet().sort().findAll { it.startsWith('on') }.each { trigger ->
                if(map.notification[trigger]){
                    if(map.notification[trigger]?.email){
                        def mail= map.notification[trigger].remove('email')
                        map.notification[trigger].email=[:]
                        mail.each{k,v->
                            BuilderUtil.addAttribute(map.notification[trigger].email,k,v)
                        }
                    }
                    if(map.notification[trigger]?.urls){
                        map.notification[trigger].webhook=BuilderUtil.toAttrMap('urls',map.notification[trigger].remove('urls'))
                    }
                    if(map.notification[trigger]?.plugin){
                        if(map.notification[trigger]?.plugin instanceof Map){
                            convertNotificationPlugin(map.notification[trigger]?.plugin)
                        }else if(map.notification[trigger]?.plugin instanceof Collection){
                            //list of plugins,
                            map.notification[trigger].plugin.each{Map plugin->
                                convertNotificationPlugin(plugin)
                            }
                        }
                    }
                }
            }
        }
        if (map.plugins) {
            map.plugins = encodePlugins(map.plugins)
        }
        return map
    }

    static Map cleanupMap(Map map) {
        def configMap = [:]
        map.each { key, val ->
            if (val != '' && val != null && !key.startsWith('_')) {
                configMap[key] = val
            }
        }
        configMap
    }
    static Map decodePlugins(Map plugins) {
        Map pluginsMap = [:]
        plugins.each { mk, mproviders ->
            pluginsMap.putIfAbsent(mk, [:])
            if(mproviders instanceof Map){
                //single map if only one entry
                mproviders=[mproviders]
            }
            mproviders.each { prov ->
                def type = prov.remove('type')
                def conf = cleanupMap(prov.remove('configuration') ?: [:])
                pluginsMap[mk][type] = conf
            }
        }
        pluginsMap
    }


    static Map encodePlugins(Map plugins) {
        def services = [:]
        plugins.each { svck, prov ->
            services[svck] = prov.collect { k, v ->
                def entry = [:]
                BuilderUtil.addAttribute(entry, 'type', k)
                def configMap = cleanupMap(v)
                if (configMap) {
                    entry[BuilderUtil.asDataValueKey('configuration')] = configMap
                }
                entry
            }
        }
        services
    }
    /**
     * Convert result of Workflow.toMap() to format used by BuilderUtil
     * @param map
     */
    static void convertWorkflowMapForBuilder(Map map, String stripJobRef = null) {
        BuilderUtil.makeAttribute(map, 'keepgoing')
        BuilderUtil.makeAttribute(map, 'strategy')
        map.command = map.remove('commands')
        if(map.pluginConfig?.LogFilter) {
            map.pluginConfig.LogFilter.each { Map plugindef ->
                BuilderUtil.makeAttribute(plugindef, 'type')
                if (!plugindef.config) {
                    //remove null or empty config map
                    plugindef.remove('config')
                }
            }
        }

        //convert script args values to idiosyncratic label

        def gencmd= { cmd, iseh=false, strip=stripJobRef ->
            if (cmd.scriptfile || cmd.script || cmd.scripturl) {
                cmd.scriptargs = cmd.remove('args')
                if (cmd.script) {
                    cmd[BuilderUtil.asCDATAName('script')] = cmd.remove('script')
                }
                if (cmd.scriptInterpreter) {
                    cmd.scriptinterpreter = ['<text>': cmd.remove('scriptInterpreter')]
                    if(!!cmd.interpreterArgsQuoted) {
                        BuilderUtil.addAttribute(cmd.scriptinterpreter, "argsquoted", "true")
                    }
                }
                cmd.remove('interpreterArgsQuoted')
            } else if (cmd.jobref) {

                if(strip == 'name') {
                    if (cmd.jobref.uuid) {
                        cmd.jobref.remove('group')
                        cmd.jobref.remove('name')
                        cmd.jobref.remove('project')
                        cmd.jobref.useName = false
                    }
                }
                if(strip == 'uuid') {
                    if (cmd.jobref.name) {
                        cmd.jobref.remove('uuid')
                        cmd.jobref.useName = true
                    }
                }

                BuilderUtil.makeAttribute(cmd.jobref, 'name')
                if (cmd.jobref.group) {
                    BuilderUtil.makeAttribute(cmd.jobref, 'group')
                } else {
                    cmd.jobref.remove('group')
                }
                if (cmd.jobref.project) {
                    BuilderUtil.makeAttribute(cmd.jobref, 'project')
                } else {
                    cmd.jobref.remove('project')
                }
                final def remove = cmd.jobref.remove('args')
                if (null != remove) {
                    cmd.jobref.arg = BuilderUtil.toAttrMap('line', remove)
                }
                if (cmd.jobref.nodeStep) {
                    BuilderUtil.makeAttribute(cmd.jobref, 'nodeStep')
                }
                if(cmd.jobref.nodefilters?.dispatch){
                    cmd.jobref.dispatch= cmd.jobref.nodefilters.remove('dispatch')
                }
            }else if(cmd.exec){
                //no change
            }else{
                def nodestep= cmd.remove('nodeStep')
                def pluginconf= cmd.remove('configuration')
                def entries=[]
                //wrap key/value in 'entry'
                pluginconf?.keySet()?.sort()?.each{k->
                    def entry = [key: k, value: pluginconf[k]]
                    BuilderUtil.makeAttribute(entry, 'key')
                    BuilderUtil.makeAttribute(entry, 'value')
                    entries<<entry
                }

                def cdata= [type: cmd.remove('type')]
                if(entries){
                    cdata.putAll([configuration: [entry: entries]])
                }
                BuilderUtil.makeAttribute(cdata, 'type')

                cmd[(nodestep?'node-':'')+'step-plugin']=cdata
            }
            if(iseh){
                BuilderUtil.makeAttribute(cmd, 'keepgoingOnSuccess')
            }
            if(cmd.plugins?.LogFilter) {
                cmd.plugins.LogFilter.each { Map plugindef ->
                    BuilderUtil.makeAttribute(plugindef, 'type')
                    if (!plugindef.config) {
                        //remove null or empty config map
                        plugindef.remove('config')
                    }
                }
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
