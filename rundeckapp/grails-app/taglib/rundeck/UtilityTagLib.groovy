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

import com.dtolabs.rundeck.core.plugins.configuration.Property
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.rundeck.app.gui.AuthMenuItem
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator
import org.rundeck.app.gui.MenuItem
import com.dtolabs.rundeck.core.common.FrameworkResourceException
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import grails.util.Environment
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.rundeck.web.infosec.HMacSynchronizerTokensHolder
import org.rundeck.web.infosec.HMacSynchronizerTokensManager
import rundeck.interceptors.FormTokenInterceptor
import rundeck.services.FrameworkService

import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.regex.Pattern

class UtilityTagLib{
    def static  daysofweekkey = [Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY];
    def public static daysofweekord = ScheduledExecution.daysofweeklist;
    def public static monthsofyearord = ScheduledExecution.monthsofyearlist;
    static returnObjectForTags = [
            'nodeStatusColorStyle',
            'nodeStatusColorCss',
            'logStorageEnabled',
            'executionMode',
            'scheduleMode',
            'appTitle',
            'rkey',
            'w3cDateValue',
            'sortGroupKeys',
            'helpLinkUrl',
            'helpLinkParams',
            'parseOptsFromString',
            'relativeDateString',
            'enc',
            'textFirstLine',
            'textRemainingLines',
            'textBeforeLine',
            'textAfterLine',
            'textHasMarker',
            'humanizeValue',
            'jobComponentSections',
            'jobComponentSectionProperties',
            'jobComponentFieldPrefix',
            'jobComponentMessagesType',
    ]

    private static Random rand=new java.util.Random()
    def HMacSynchronizerTokensManager hMacSynchronizerTokensManager
    def configurationService
    def scheduledExecutionService
    FrameworkService frameworkService
    GrailsConventionGroovyPageLocator groovyPageLocator
    /**
     * Return a new random string every time it is called.  Attrs are:
     * len: number of random bytes to use
     * format: printf format for the byte array, default is a hex string "%x"
     */
    def rkey={attrs,body->
        def len=4;
        if(attrs.len){
            len=attrs.len
        }
        def b = new byte[len]
        rand.nextBytes(b)
        return sprintf(attrs.format?:'%02x'*len,b)
    }

    def parseOptsFromString={attrs,body->
        return FrameworkService.parseOptsFromString(attrs.args)
    }
    /**
     * Return the group map sorted by group path
     */
    def sortGroupKeys={attrs,body->
        def groups=attrs.groups

        return groups.sort {a, b ->
            def aa=a.key.split('/')
            def ba = b.key.split('/')
            def i=0
            def comp=0
            //compare each path component
            while(i<aa.length && i<ba.length && comp==0){
                comp= aa[i] <=> ba[i]
                i++
            }
            comp ?: aa.length - ba.length
        }
    }

    /**
    * Render expander component
     */
    def expander={attrs,body->
        if(body && !attrs.text){
            attrs.text=body()
        }
        out<<render(template:"/common/expander",model:attrs)
    }
    /**
    * Render collapser component
     */
    def collapser={attrs,body->
        if(body && !attrs.text){
            attrs.text=body()
        }
        out<<render(template:"/common/collapser",model:attrs)
    }

    def dayOfWeek = { attrs, body ->

        def java.text.DateFormatSymbols DFS = new java.text.DateFormatSymbols(request.getLocale());
        def monthsArray = DFS.getMonths();
        def daysArray = DFS.getWeekdays();
        def daysofweek = [:];
        (0..<daysofweekord.size()).each{ i ->
            daysofweek[daysofweekord[i]]=daysArray[daysofweekkey[i]];
        }
        if(attrs.name){
            out << daysofweek[attrs.name]
        }else if(attrs.index){
            out << daysofweek[daysofweekord[attrs.index]]
        }else{
            throw new Exception("name or index attribute required")
        }
    }

    def eachDay = { attrs, body ->
        daysofweekord.each{ n ->
            body(n)
        }
    }

    def month = { attrs, body ->

        def java.text.DateFormatSymbols DFS = new java.text.DateFormatSymbols(request.getLocale());
        def monthsArray = DFS.getMonths();
        def monthsofyear = [:];
        (0..<12).each{ i ->
            monthsofyear[monthsofyearord[i]]=monthsArray[i];
        }
        if(attrs.name){
            out << monthsofyear[attrs.name]
        }else if(attrs.index){
            out << monthsofyear[attrs.index]
        }else{
            throw new Exception("name or index attribute required")
        }
    }

    def eachMonth = { attrs,body ->
        monthsofyearord.each{ n ->
            body(n)
        }
    }

    def daysOfWeekList = {
        return daysofweekord
    }

    def monthsOfYearList = {
        return monthsofyearord
    }

    def timeDuration = { attrs, body ->

        if (!attrs.start && !attrs.end && !attrs.time){
            return;
        }

        def long dms
        if(attrs.time){
            dms= (long)Math.floor(attrs.time / 1000.0d)
        }else{
            def Date start = attrs.start
            def Date end = attrs.end
            if(null==start && null==end && null==time){
                return;
            }else if(null!=start && null==end){
                end = new Date()
            }
    //        def dms = (end.getTime() - start.getTime()).intdiv(1000)
            dms = (long)Math.floor((end.getTime()-start.getTime()) / 1000.0)
        }

            def s = dms % 60
            def md = (dms - s ).intdiv( 60 )
            def hd = md.intdiv(60)
            def m = md % 60
            def d =  hd.intdiv( 24 )
            def h = hd % 24
        def duration
        if (dms < 60)  {
            duration = "${s}s"
        } else if (dms < (5 * 60) ) {
//            duration = String.valueOf( dms.intdiv( 60) ) + "m"
            def val =  "${m}m"
            if(s > 0){
                val += "${s}s"
            }
            duration=val
        }else if(dms < (60 * 60) ){
            duration=  "${m}m"

        }else if (dms < (24 * 60 * 60)){
            def val = "${h}h"
            if(m > 0 ){
                val += "${m}m"
            }
            duration=val
        }else{
            def val = "${d}d"
            if(h > 0 ){
                val += "${h}h"
            }
            duration=val
        }

        out << duration
    }

    def relativeDate = { attrs, body ->
        out << relativeDateString(attrs + [html: attrs.html != null ? attrs.html : true], body)
    }

    def relativeDateString = { attrs, body ->
        if(attrs.atDate){
            def Date date = attrs.atDate
            def Date nowdate = new Date()
            def Calendar cal = Calendar.getInstance()
            cal.setTime(date)
            def Calendar now = Calendar.getInstance()
            now.setTime(nowdate)
            def StringBuffer val = new StringBuffer()
            if(nowdate.getTime()-date.getTime() < 1000 * 60 * 60 * 24){
                if(cal.get(Calendar.DAY_OF_MONTH) != now.get(Calendar.DAY_OF_MONTH)){
                    val << new SimpleDateFormat("MMM d ha").format(date)
                }else{
                    //same date
                    val << new SimpleDateFormat("h:mm a").format(date)
                }
            }else if(nowdate.getTime()-date.getTime() < 1000 * 60 * 60 * 24 * 7){
                //within a week
                    val << new SimpleDateFormat("E ha").format(date)
            }else if(cal.get(Calendar.YEAR)!=now.get(Calendar.YEAR)){
                    val << new SimpleDateFormat("MMM d yyyy").format(date)
            }else{
                val << new SimpleDateFormat("M/d ha").format(date)
            }
            return val.toString()
        }else if(attrs.elapsed || attrs.start && attrs.end){
            def Date date = (attrs.elapsed instanceof Date) ? attrs.elapsed : null
            def Date enddate = new Date()
            if(attrs.start && attrs.end){
                date = attrs.start
                enddate = attrs.end
            }
            def Calendar cal = Calendar.getInstance()
            cal.setTime(date)
            def Calendar now = Calendar.getInstance()
            now.setTime(enddate)
            def StringBuffer val = new StringBuffer()
            def long diff = (long)Math.floor((date.getTime()-enddate.getTime()) / 1000.0)
            def long test = Math.abs(diff)
//            val << " (diff:${date.getTime()-enddate.getTime()}) "


            def s = test % 60
            def md = (test - s ).intdiv( 60 )
            def hd = md.intdiv(60)
            def m = md % 60
            def d =  hd.intdiv( 24 )
            def h = hd % 24


            if(test < 60 ){
                val << g.message(code: 'format.time.sec.abbrev', args: [s].toArray())
            }else if(test <  (5 * 60) ){
                val << g.message(code: 'format.time.min.abbrev', args: [m].toArray())
                if(s > 0){
                    val << g.message(code: 'format.time.sec.abbrev', args: [s].toArray())
                }
            }else if(test < (60 * 60) ){
                val << g.message(code: 'format.time.min.abbrev', args: [m].toArray())

            }else if (test < (24 * 60 * 60)){
                val << g.message(code: 'format.time.hour.abbrev', args: [h].toArray())
                if(m > 0 ){
                    val << g.message(code: 'format.time.min.abbrev', args: [m].toArray())
                }
            }else{
                val << g.message(code: 'format.time.day.abbrev', args: [d].toArray())
                if(h > 0 ){
                    val << g.message(code: 'format.time.hour.abbrev', args: [h].toArray())
                }
            }
            def StringBuffer val2 = new StringBuffer()

            if (diff > 0 && (!attrs.end || attrs.elapsed)) {
                val2 << g.message(code: 'in') + " "
            }
            if (attrs.html) {
                val2 << """<span class="${
                    enc(attr: diff > 0 ? (attrs.untilClass ?: 'until') : (attrs.agoClass ?: 'ago'))
                }">"""
            }
            val2 << val.toString()
            if (attrs.html) {
                val2 << "</span>"
            }
            if (diff < 0 && (!attrs.end || attrs.elapsed)) {
                val2 << " " + g.message(code: 'ago')
            }
            return val2.toString()
        } else {
            //do nothing.
            return "?"
        }
    }

    def recentDescription = { attrs, body ->
        if(attrs.value){
            def matcher = attrs.value =~ /^(\d+)([hdwmy])$/
            if(matcher.matches()){
                def i = matcher.group(1).toInteger()
                def period
                switch(matcher.group(2)){
                    case 'h':
                        period="hour"
                        break
                    case 'd':
                        period="day"
                        break
                    case 'w':
                        period="week"
                    break
                    case 'm':
                        period="month"
                    break
                    case 'y':
                        period="year"
                    break
                }
                out << "${i} "
                if(i>1){
                    out << g.message(code:"time.unit.${period}.plural")
                }else{
                    out << g.message(code:"time.unit.${period}")
                }
            }
        }
    }

    /**
     * Render crontab item description.
     * attrs:
     * value: string value
     * unit: name of unit
     */
    def cronItem={attrs,body->
        def unit = attrs.unit
        def val=attrs.value
        def desc=''
        if(null==val){
            throw new Exception("value attribute required")
        }
        if('*'==val){
            out<<"every ${unit}"
        }else if(val=~/^[*0]\/\d+$/ && unit!='month'){
            def x=val.substring(2)
            def ch='th'
            if(x=='2'){
                ch='nd'
            }else if (x == '3') {
                ch='rd'
            }
            out<<"every ${x}${ch} ${unit}"
        }else{
            out<<"${unit} ${enc(html:val)}"
        }
    }

    /**
     * Generate an html tag
     */
    def makeTag(name,map,content=null){
        def StringBuffer out = new StringBuffer()
        out<<"<${name}"
            map.attrs.each{k,v->
                if(v){
                    out<<" ${k}=\"${enc(attr:v)}\""
                }
            }
        if(content){
            out<<">"
            out << content
            out<<"</${name}>"
        }else if(map.nocontent){
            out<<"/>"
        }

    }


    /**
    * Utility tag for declaring an <img> tag
     */
    def img={attrs,body->
        def myattrs=attrs.subMap(['title','class','width','height'])
        myattrs.src=resource(dir:'images',file:attrs.file)
        out<<makeTag("img",[nocontent:true,attrs:myattrs])
    }


    /**
     * renders a java Date as the rfc 822 date used by RSS
     */
    def rfc822Date = {attrs,body ->
        if(attrs.date){
            SimpleDateFormat dateFormater = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",Locale.US);
            dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
            out<< dateFormater.format(attrs.date);
        }
    }

    /**
     * renders a java date as the W3C format used by dc:date in RSS feed
     */
    def w3cDate = {attrs,body ->
        if(attrs.date){
            out<< w3cDateValue(attrs,body)
        }
    }
    /**
     * renders a java date as the W3C format used by dc:date in RSS feed
     */
    def w3cDateValue = {attrs,body ->
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US);
        dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormater.format(attrs.date);
    }

    def truncate={attrs,body ->
        def max=30
        if(attrs.max){
            if(attrs.max instanceof String){
                max=Integer.parseInt(attrs.max)
            }else if(attrs.max instanceof Number){
                max=attrs.max
            }
        }
        def String otext=attrs.text?.trim()
        def bstring=body()
        if(bstring){
            otext=bstring?.trim()
        }
        def String text=otext
        if(text && text.size()>max && max>=0){
            if(attrs.front =='true'){
                text="…"+text.substring(text.length()-max)
            }else{
                text=text.substring(0,max)+"…"
            }
        }else if(text){
            text=text
        }
        if(text){
            if(attrs.showtitle=='true' && text!=otext){
                out<<'<span title="'+enc(attr:otext)+'" class="truncatedtext">'
            }
            out<<text

            if(attrs.showtitle=='true' && text!=otext){
                out<<'</span>'
            }
        }
    }

    def datepickerUI = { attrs ->
        def name = attrs['name']?:'myDateField'
        def id = attrs['id']?:'myDateField'
        def optionName = attrs['optionName']?:null;
        def value = attrs['value']
        def options = attrs['options']?:'{}' //{key1: value1, key2: value2, ...}
        def locale = attrs['locale']?:request.getLocale().toString().substring(0,2)
        def htmlClass = attrs['class']
        def htmlRequired = attrs['required']?"required = 'true'":''
        def placeholder = attrs['placeholder']?:''

        def namePicker = id+"_picker";
        def nameDay = id+"_day";
        def nameMonth = id+"_month";
        def nameYear = id+"_year";
        def nameHour = id+"_hour";
        def nameMinute = id+"_minute";

        def c = null
        def hour;
        def minute;
        def day;
        def month;
        def year;
        if(value!=null){
                if(value instanceof Calendar) {
                        c = value
                } else {
                        c = new GregorianCalendar();
                        c.setTime(value)
                }
                minute = c.get(GregorianCalendar.MINUTE)
                hour = c.get(GregorianCalendar.HOUR_OF_DAY)
                day = c.get(GregorianCalendar.DAY_OF_MONTH)
                month = c.get(GregorianCalendar.MONTH)+1
                year = c.get(GregorianCalendar.YEAR)
        }

        out << """
            <input type='text' id='${namePicker}' name='${optionName ?: namePicker}' placeholder='${
            placeholder
        }' class='${htmlClass}' ${htmlRequired} style='position: relative; z-index:999;'/>
            
           
            <input type='hidden' id='${name}' name='${name}' value='date.struct' />
            
            <input type='hidden' id='${nameMinute}' name='${nameMinute}' value='${minute}' />
            <input type='hidden' id='${nameHour}' name='${nameHour}' value='${hour}' />
            <input type='hidden' id='${nameDay}' name='${nameDay}' value='${day}' />
            <input type='hidden' id='${nameMonth}' name='${nameMonth}' value='${month}' />
            <input type='hidden' id='${nameYear}' name='${nameYear}' value='${year}' />
                    """

        out << """
            <script type='text/javascript'>
            jQuery(document).ready(function(){\n
                 jQuery('#${namePicker}').datetimepicker(${options});\n
                 jQuery('#${namePicker}').datetimepicker('option',jQuery.timepicker.regional['${locale}']);\n
                 jQuery('#${namePicker}').on('change', function(){\n
                         selDate = jQuery('#${namePicker}').datetimepicker('getDate');\n
                         jQuery('#${nameMinute}').val(selDate?selDate.getMinutes():null);\n
                         jQuery('#${nameHour}').val(selDate?selDate.getHours():null);\n
                         jQuery('#${nameDay}').val(selDate?selDate.getDate():null);\n
                         jQuery('#${nameMonth}').val(selDate?selDate.getMonth()+1:null);\n
                         jQuery('#${nameYear}').val(selDate?selDate.getFullYear():null);\n
                 });\n
                 var dateFormat = jQuery('#${namePicker}').datetimepicker( 'option', 'dateFormat');\n
                 var timeFormat = jQuery('#${namePicker}').datetimepicker( 'option', 'timeFormat');\n
                 var controlType = jQuery('#${namePicker}').datetimepicker( 'option', 'select');\n
            """
        // If a value is specified it overrides the default date
        if(attrs['value']){
            out << """
                //Set date from value\n
                jQuery('#${namePicker}').datetimepicker('option', 'defaultDate',new Date(${year},${month - 1},${day},${
                hour
            },${minute}));\n
            """
        }
        out << """
            var defaultDate = jQuery('#${namePicker}').datetimepicker( 'option', 'defaultDate');\n
            //Set default date\n
            jQuery('#${namePicker}').val(jQuery.datepicker.formatDate(dateFormat, defaultDate) + ' ' + (defaultDate
.getHours()<10?'0':'') + defaultDate.getHours() + ':' + (defaultDate.getMinutes()<10?'0':'') + defaultDate.getMinutes())\n
            });\n
            </script>
            
            """
    }

    def autoLink={ attrs,body->
        def outx=body()
        def xparams = params.project?[params:[project:params.project]]:[:]
        def linkopts=[
                execution:[
                        pattern: /\{\{(Execution\s+(\d+))\}\}/ ,
                        linkParams:[action:'show',controller:'execution'] + xparams
                ],
                job:[
                        pattern: /\{\{(Job\s+([0-9a-h]{8}-[0-9a-h]{4}-[0-9a-h]{4}-[0-9a-h]{4}-[0-9a-h]{12}))\}\}/ ,
                        linkParams:[action:'show',controller:'scheduledExecution'] + xparams,
                        textValue:{
                            def job= ScheduledExecution.getByIdOrUUID(it)
                            return job?job.generateFullName():it
                        }
                ],
                profile:[
                        pattern: /\{\{user\/profile(\/.+)?\}\}/,
                        linkParams:[action: 'profile',controller: 'user'],
                        textValue: {
                            return it?"User: ${it}": "Your User Profile"
                        }
                ],
                help:[
                        pattern:/\{\{help\/docs\}\}/,
                        linkText: helpLinkUrl()
                ],
                app:[
                        pattern:/\{\{app\/(version|title|ident)?\}\}/,
                        textValue:{
                            if(it[1]=='title'){
                                appTitle()
                            }else if(it[1]=='version'){
                                grailsApplication.metadata['info.app.version']
                            }else if(it[1]=='ident'){
                                grailsApplication.metadata['build.ident']
                            }
                        }
                ]
        ]
        if(attrs.jobLinkId){
            linkopts.jobhref=[
                    pattern: /\{\{job\.permalink\}\}/,
                    hrefParams:[action:'show',controller:'scheduledExecution',id:attrs.jobLinkId] + xparams,
            ]
        }
        if(attrs.tokens){
            attrs.tokens.each{k,v->
                linkopts[k]=[
                        pattern: Pattern.quote("{{$k}}"),
                        linkText: v
                ]
            }
        }
        linkopts.each{k,opts->
            outx=outx.replaceAll(opts.pattern){
                if(opts.linkParams){
                    def lparams= [:]+opts.linkParams
                    lparams.id=it[2]
                    def text = opts.textValue?opts.textValue(it[2]):it[1]
                    return g.link(lparams,text)
                }else if(opts.hrefParams){
                    def lparams= [:]+opts.hrefParams
                    return g.createLink(lparams)
                }else if(opts.textValue){
                    return opts.textValue(it)?:it[0]
                }else if(opts.linkText){
                    return opts.linkText
                }else{
                    return it[0]
                }
            }
        }
        out << outx
    }

    /**
     * Conditionally renders the body content if a servletContext attribute has a specified value.
     *  Attributes: 'attribute' (name of servletcontext attribute), 'value' (value of attribute)
     */
    def ifServletContextAttribute = {attrs,body ->
        if(attrs.attribute && attrs.value){
            if(servletContext.getAttribute(attrs.attribute)==attrs.value){
                out << body()
            }
        }
    }
    /**
     * Conditionally renders the body content if a servletContext attribute exists at all.
     *  Attributes: 'attribute' (name of servletcontext attribute)
     */
    def ifServletContextAttributeExists = {attrs,body ->
        if(attrs.attribute  && servletContext.getAttribute(attrs.attribute)){
            out << body()
        }
    }

    /**
     * Output the value of a servlet context attribute.
     */
    def servletContextAttribute = {attrs,body ->
        if(attrs.attribute ){
            out << servletContext.getAttribute(attrs.attribute)
        }
    }

    def timerStart = { attrs,body->
        if(attrs.key){

            if(!request.pageTimers){
                request.pageTimers=[:]
            }
            if(!request.pageTimersStack){
                request.pageTimersStack=[attrs.key]
            }else{
                request.pageTimersStack<<attrs.key
            }
            def path=request.pageTimersStack.join("/")
            if(!request.pageTimers[path]){
                request.pageTimers[path]=[start:System.currentTimeMillis()]
            }else{
                request.pageTimers[path].start=System.currentTimeMillis()
            }
        }
    }

    def timerEnd={attrs,body->
        if(attrs.key){

            if(!request.pageTimers){
                request.pageTimers=[:]
            }
            if(!request.pageTimersStack){
                request.pageTimersStack=[]
            }
            def path=request.pageTimersStack.join("/")
            request.pageTimersStack.pop()
            if(request.pageTimers[path]){
                request.pageTimers[path].end=System.currentTimeMillis()
                def tot=request.pageTimers[path].end-request.pageTimers[path].start
                if(!request.pageTimers[path].counts){
                    request.pageTimers[path].counts=[tot]
                }else{
                    request.pageTimers[path].counts<<tot
                }
                if(!request.pageTimers[path].total){
                    request.pageTimers[path].total=tot
                }else{
                    request.pageTimers[path].total+=tot
                }
            }
        }
    }
    def timerSummary={attrs,body->
        def sets;
        if(attrs.key){
            sets=[(attrs.key):request.pageTimers[attrs.key]]
        }else{
            sets=request.pageTimers
        }
        //calculate overhead== total time at level X - sum of level X+1 totals
        sets.each{k,v->
            //sum of lower level totals
            def z = sets.keySet().findAll{it.startsWith(k+'/') && it.lastIndexOf('/')==k.length()}.collect{sets[it].total}.inject(0) { acc, val -> acc + val }
            if(z>0){
                sets[k].overhead=sets[k].total-z
            }
        }
        sets.each{k,v->
            out<<"<div>"
            out<<"<b>${k}:</b>"
            out<<"${v.counts?.size()} calls, ${v.total}ms total. "+(v.counts?.size()>1?"Avg: ${v.total/v.counts.size()}ms. ":'')+(v.overhead?"(overhead: ${v.overhead}ms)":'')
            out<<"</div>"
        }
        request.pageTimers=null
    }
    /**
     * Humanize data value, based on unit attribute
     */
    def humanize={attrs,body->
        def result = humanizeValue(attrs, body)
        if (result) {
            out << result
        }
    }
    /**
     * Humanize data value, based on unit attribute
     * @attrs unit required unit of data, 'byte','hbyte','ms','%' will be formatted specially
     * @attrs value required value convert
     */
    def humanizeValue = { attrs, body ->
        if (attrs.unit && null != attrs.value) {
            if (attrs.unit == 'byte' || attrs.unit == 'hbyte') {
                long val = attrs.value instanceof String ? Long.parseLong(attrs.value) : attrs.value
                def testmap = [
                        byte: [
                                [value: 0, name: 'B'],
                                [value: 1024L, name: 'KiB'],
                                [value: 1024L * 1024, name: 'MiB'],
                                [value: 1024L * 1024 * 1024, name: 'GiB'],
                                [value: 1024L * 1024 * 1024 * 1024, name: 'TiB'],
                        ],
                        hbyte: [
                                [value: 0, name: 'B'],
                                [value: 1000L, name: 'KB'],
                                [value: 1000L * 1000, name: 'MB'],
                                [value: 1000L * 1000 * 1000, name: 'GB'],
                                [value: 1000L * 1000 * 1000 * 1000, name: 'TB'],
                        ]
                ]
                def testset = testmap[attrs.unit]
                def found
                testset.eachWithIndex { lvl, x ->
                    if (!found && val < lvl.value) {
                        found = testset[x - 1]
                    }
                }
                if (!found) {
                    found = testset[-1]
                }
                def outputNumber = found.value > 0 ? (val / found.value) : val
                return g.formatNumber([number: outputNumber, type: "number", maxFractionDigits: "2"], body) + ' ' +
                        found.name
            } else if (attrs.unit == 'ms') {
                attrs.time = attrs.value
                return timeDuration(attrs, body)
            } else if (attrs.unit == '%') {
                out << g.formatNumber([number: attrs.value, type: "number", maxFractionDigits: "2"], body) + '%'
            } else {
                return attrs.value + ' (' + attrs.unit + ')'
            }
        }
    }
    def helpLinkParams={attrs,body->
        def medium = "${grailsApplication.metadata['info.app.version']} ${System.getProperty('os.name')} java ${System.getProperty('java.version')}"
        def campaign = attrs.campaign?:'helplink'
        def sourceName = g.message(code:'main.app.id',default: 'rundeckapp')
        def helpParams = [utm_source: sourceName, utm_medium: medium, utm_campaign: campaign, utm_content: (controllerName + '/' + actionName)]
        return genUrlParam(helpParams)
    }

    def String genUrlParam(Map<String, Serializable> params) {
        params.collect { k, v -> k.encodeAsURIComponent() + '=' + v.encodeAsURIComponent() }.join('&')
    }
    def helpLinkUrl={attrs,body->
        def path=''
        def fragment=''
        if(attrs.path){
            path=attrs.path
            if(!path.startsWith('/')){
                path='/'+path
            }
            if(path.contains('#')){
                def split=path.split('#',2)
                path=split[0]
                fragment='#'+split[1]
            }
        }
        def rdversion = grailsApplication.metadata.getProperty('info.app.version', String)
        def rdversionShort = rdversion.split('-')[0]
        def helpBase='http://rundeck.org/' +( rdversion?.contains('SNAPSHOT')?'docs':rdversionShort )
        def helpUrl
        if(grailsApplication.config.rundeck?.gui?.helpLink){
            helpBase= grailsApplication.config.rundeck?.gui?.helpLink
            helpUrl=helpBase + path + fragment
        }else{
            def helpParams = helpLinkParams(attrs,body)
            helpUrl= helpBase + path + '?' + helpParams + fragment
        }
        helpUrl
    }

    def pluginPropertyProjectScopeKey={attrs,body->
        out<< PropertyResolverFactory.projectPropertyPrefix(PropertyResolverFactory.pluginPropertyPrefix(attrs.service, attrs.provider)) + (attrs.property ?: '')
    }
    def pluginPropertyFrameworkScopeKey={attrs,body->
        out << PropertyResolverFactory.frameworkPropertyPrefix(PropertyResolverFactory.pluginPropertyPrefix(attrs.service, attrs.provider))+(attrs.property?:'')
    }

    def markdown={ attrs, body ->
        if(attrs.safe){
            withCodec('raw'){
                out << body().toString().encodeAsHTMLContent().decodeMarkdown()
            }
        }else{
            withCodec('raw') {
                out << body().toString().decodeMarkdown()
            }
        }
    }

    /**
     * Outputs the attribute "user", or "you" if it matches the current user,optionally wrap in span with given class if it
     * is "you", with attribute "youclass"
     */
    def username={attrs,body->
        //mail rendering uses a fake request and getSession() will throw exception, so bypass if needed
        //attribute IS_MAIL_RENDERING_REQUEST is set in the /execution/mailNotification/status.gsp file
        if(request.getAttribute('IS_MAIL_RENDERING_REQUEST')!=Boolean.TRUE && attrs.user==session.user){
            if(attrs.youclass){
                out<<"<span class=\"${enc(attr:attrs.youclass)}\">"
            }
            out << g.message(code:'you',default:'you')
            if (attrs.youclass) {
                out << "</span>"
            }
        }else{
            out<<attrs.user
        }
    }

    /**
     * Render plural text for a count of items
     * attributes: count,
     */
    def plural={attrs,body->
        def singular=attrs.code?g.message(code:attrs.code):attrs.singular?:body()
        def plural=(attrs.code?g.message(code:(attrs.code+'.plural'),default:''):'')?:attrs.plural?:(singular+'s')
        def count=null!=attrs.count?attrs.count:null!=attrs.for?attrs.for.size():0
        def text= count == 1 ? singular.encodeAsHTML() : plural.encodeAsHTML()
        def parts = [count,text]
        def code=attrs.verb&&!attrs.textOnly?'plural.count.verb.format':attrs.verb?'plural.verb.format':!attrs.textOnly?'plural.count.format':'plural.format'
        parts << (count == 1 ? g.message(code: attrs.verb, default: attrs.verb).encodeAsHTML() : g.message(code: attrs.verb + '.plural', default: attrs.verbPlural).encodeAsHTML())
        out << (new MessageFormat(g.message(code: code, default: '{0} {1} {2}'))).format(parts as Object[], new StringBuffer(), null).toString()
    }

    def helpTooltip={attrs,body->
        def code=attrs.code
        def css=attrs.css ?: 'input-group-addon text-info'
        def glyph=attrs.glyphicon?:'question-sign'
        def placement=attrs.placement?:'bottom'
        out<<'<span ' +
                ' class="has_tooltip '+ enc(attr:css)+'" ' +
                ' data-placement="'+enc(attr:placement)+'" ' +
                ' data-container="body" ' +
                ' title="'+enc(attr:g.message(code: code))+ '">\n' +
                (attrs.text?:'') +
                body() +
                ' <i class="glyphicon glyphicon-'+enc(attr:glyph)+'"></i>\n' +
                '</span>'
    }
    /**
     * Encode a string, can be specified via attribute, or HTML encode the body if no attributes are specified. Only a single attribute can be specified.
     * @attr html HTML encode the string
     * @attr attr encode the string for placing inside an HTML attribute value
     * @attr xml encode the string for placing inside XML
     * @attr js javascript encode the string
     * @attr json json encode the string
     * @attr url url encode the string
     * @attr code HTML encode a message from the given code
     * @attr rawtext contents will be rendered without encoding
     * @attr raw if set to true, tag body will be rendered without encoding
     */
    def enc={attrs,body->
        if(attrs.html){
            out << attrs.html.toString().encodeAsHTML()
        }else if(attrs.stripHtml){
            out << raw(attrs.stripHtml.toString().encodeAsStripHTML())
        }else if(attrs.sanitize){
            out << raw(attrs.sanitize.toString().encodeAsSanitizedHTML())
        }else if(null!=attrs.attr){
            out << raw(attrs.attr.toString().encodeAsHTMLAttribute())
        }else if(attrs.xml){
            out << attrs.xml.toString().encodeAsXMLContent()
        }else if(null !=attrs.js){
            out << raw(attrs.js.toString().encodeAsJavaScript2())
        }else if(attrs.json!=null){
            out << attrs.json.encodeAsJSON().toString().replaceAll('<', '\\\\u003c') //nb: replace < to allow embedding in page
        }else if(attrs.url){
            out << attrs.url.toString().encodeAsURL()
        }else if(attrs.code){
            out << g.message(code:attrs.code,encodeAs:attrs.codec?:'HTML')
        }else if(attrs.rawtext) {
            //explicitly not encoded
            out << raw(attrs.rawtext)
        }else if(attrs.raw=='true') {
            withCodec('raw'){
                //explicitly not encoded
                out << body()
            }
        }else {
            out << body().encodeAsHTML()
        }
    }

    /**
     * Strip tags out of an HTML string and then encode the remaining text
     */
    def strip={attrs,body->
        if(attrs.html){
            out<<attrs.html.encodeAsStripHTML().encodeAsHTML()
        }else{
            out<<body().encodeAsStripHTML().encodeAsHTML()
        }
    }
    def textFirstLine={attrs,body->
        if(attrs.text){
            def split=attrs.text.toString().split(/(\r\n?|\n)/,2)
            return  (split.length>0?split[0]:attrs.text)
        }else{
            return attrs.text
        }
    }
    def textRemainingLines={attrs,body->
        if(attrs.text){
            def split=attrs.text.toString().split(/(\r\n?|\n)/,2)
            if(split.length==2){
                return split[1]
            }
        }
    }
    def textBeforeLine={attrs,body->
        if(attrs.text && attrs.marker){
            def split=attrs.text.toString().split("(^|\n|\r\n)"+Pattern.quote(attrs.marker)+"(\n|\r\n)", 2)
            return (split.length>0?split[0]:attrs.text)
        }else{
            return attrs.text
        }
    }
    def textHasMarker = { attrs, body ->
        if(attrs.text && attrs.marker){
            def split=attrs.text.toString().split("(\n|\r\n)"+Pattern.quote(attrs.marker)+"(\n|\r\n)",2)
            if(split.length==2){
                return true
            }
        }
        return false
    }
    def textAfterLine={attrs,body->
        if(attrs.text && attrs.marker){
            def split=attrs.text.toString().split("(^|\n|\r\n)"+Pattern.quote(attrs.marker)+"(\n|\r\n)",2)
            if(split.length==2){
                if(attrs.include){
                    return attrs.marker +"\n"+ split[1]
                }
                return split[1]
            }
        }
    }

    /**
     * Embed JSON in a page within a script tag
     * @attr data the objects to encode as json
     * @attr id element id to use
     */
    def embedJSON={attrs,body->
        def obj=attrs.data
        def id=attrs.id
        out << '<script id="'+enc(attr:id)+'" type="text/json">'
        out << enc(json: obj)
        out << '</script>'
    }
    /**
     * Embed i18n messages available to javascript in the Messages object
     * @attr code a i18n message code, or comma or whitespace separated list
     * @attr codes a list of message codes
     * @attr id element id to use (optional)
     */
    def jsMessages = { attrs, body ->
        def id = attrs.id ?: ('i18nmessages_'+rkey())
        def msgs = [:]
        if (attrs.code) {
            attrs.code.split(/(?s)([,\s\r\n]+)/).each {
                msgs[it] = g.message(code: it.trim(), default: it.trim())
            }
        }
        if(attrs.codes){
            attrs.codes.each{
                msgs[it] = g.message(code: it.trim(), default: it.trim())
            }
        }
        embedJSON.call([id: id, data: msgs],null)
        out << '<script>_loadMessages(\''+enc(js: id)+'\');</script>'
    }
    def refreshFormTokensHeader = { attrs, body ->
        SynchronizerTokensHolder tokensHolder = tokensHolder()
        def uri = attrs.uri ?: params[SynchronizerTokensHolder.TOKEN_URI]
        response.addHeader(FormTokenInterceptor.TOKEN_KEY_HEADER, tokensHolder.generateToken(uri))
        response.addHeader(FormTokenInterceptor.TOKEN_URI_HEADER, uri)
    }


    def generateToken(long duration) {
        SynchronizerTokensHolder tokensHolder = tokensHolder()
        long timestamp = System.currentTimeMillis() + duration
        return [TOKEN:tokensHolder.generateToken(timestamp),TIMESTAMP:timestamp]
    }
    def generateToken(String url) {
        SynchronizerTokensHolder tokensHolder = tokensHolder()
        return tokensHolder.generateToken(url)
    }

    protected SynchronizerTokensHolder tokensHolder() {
        SynchronizerTokensHolder tokensHolder
        if (grailsApplication.config.rundeck?.security?.useHMacRequestTokens in [true, 'true']) {
            //enable hmac request tokens which expire instead of Grails' default UUID based tokens
            tokensHolder = HMacSynchronizerTokensHolder.store(session, hMacSynchronizerTokensManager, [session.user,
                    request.remoteAddr])
        } else {
            tokensHolder = SynchronizerTokensHolder.store(session)
        }
        tokensHolder
    }

    def jsonToken = { attrs, body ->
//        def expiry = 30000L
//        if (attrs.duration) {
//            expiry = Long.parseLong(attrs.duration)
//        }
        def id = attrs.id
        def url =attrs.url?:request.forwardURI
        def token = generateToken(url)
        embedJSON.call([id: id, data: [TOKEN: token, URI: url]],body)
    }

    /**
     * Override the default grails g:form tag, so that we can supply our own Synchronizer Token Holder if necessarry
     */
    def form={attrs,body->
        def useToken = false
        if (attrs.containsKey('useToken')) {
            useToken = attrs.boolean('useToken')
        }
        if(useToken && grailsApplication.config.rundeck?.security?.useHMacRequestTokens in [true,'true']){
            //enable hmac request tokens which expire instead of Grails' default UUID based tokens
            def tokensHolder = HMacSynchronizerTokensHolder.store(session, hMacSynchronizerTokensManager, [session.user, request.remoteAddr])
        }
        //call original form tag
        def applicationTagLib = grailsApplication.mainContext.getBean('org.grails.plugins.web.taglib.FormTagLib')
        applicationTagLib.form.call(attrs,body)
    }

    def appTitle={attrs,body->
        grailsApplication.config.rundeck.gui.title ?:g.message(code:'main.app.name',default:'')?:g.message(code:'main.app.default.name')
    }

    def executionMode={attrs,body->
        def testIsActive = true
        if(null!=attrs.active){
            testIsActive=attrs.active in ['true',true]
        }else if(null!=attrs.passive){
            testIsActive=!(attrs.passive in ['true',true])
        }else if(null!=attrs.is){
            testIsActive=attrs.is =='active'
        }
        if(null!=attrs.project){
            try {
                def projectExec = scheduledExecutionService.isProjectExecutionEnabled(attrs.project)
                if(!projectExec){
                    return testIsActive==projectExec
                }
            }catch (FrameworkResourceException e){
                log.warn(e.message)
            }
        }
        return testIsActive==configurationService.executionModeActive
    }

    def scheduleMode={attrs,body->
        def testIsActive = true
        if(null!=attrs.is){
            testIsActive=attrs.is =='active'
        }
        if(null!=attrs.project){
            try {
                return testIsActive==scheduledExecutionService.isProjectScheduledEnabled(attrs.project)
            }catch (FrameworkResourceException e){
                log.warn(e.message)
            }
        }
    }

    def ifExecutionMode={attrs,body->
        if(executionMode(attrs,body)){
            out<<body()
        }
    }
    def ifScheduleMode={attrs,body->
        if(scheduleMode(attrs,body)){
            out<<body()
        }
    }
    def logStorageEnabled={attrs,body->
        configurationService.getString('execution.logs.fileStoragePlugin',null)!=null
    }
    def ifLogStorageEnabled={attrs,body->
        if(logStorageEnabled(attrs,body)){
            out<<body()
        }
    }
    static final Set<String> glyphiconSet=Collections.unmodifiableSet(
            qw('''asterisk
plus
euro
eur
minus
cloud
envelope
pencil
glass
music
search
heart
star
star-empty
user
film
th-large
th
th-list
ok
remove
zoom-in
zoom-out
off
signal
cog
trash
home
file
time
road
download-alt
download
upload
inbox
play-circle
repeat
refresh
list-alt
lock
flag
headphones
volume-off
volume-down
volume-up
qrcode
barcode
tag
tags
book
bookmark
print
camera
font
bold
italic
text-height
text-width
align-left
align-center
align-right
align-justify
list
indent-left
indent-right
facetime-video
picture
map-marker
adjust
tint
edit
share
check
move
step-backward
fast-backward
backward
play
pause
stop
forward
fast-forward
step-forward
eject
chevron-left
chevron-right
plus-sign
minus-sign
remove-sign
ok-sign
question-sign
info-sign
screenshot
remove-circle
ok-circle
ban-circle
arrow-left
arrow-right
arrow-up
arrow-down
share-alt
resize-full
resize-small
exclamation-sign
gift
leaf
fire
eye-open
eye-close
warning-sign
plane
calendar
random
comment
magnet
chevron-up
chevron-down
retweet
shopping-cart
folder-close
folder-open
resize-vertical
resize-horizontal
hdd
bullhorn
bell
certificate
thumbs-up
thumbs-down
hand-right
hand-left
hand-up
hand-down
circle-arrow-right
circle-arrow-left
circle-arrow-up
circle-arrow-down
globe
wrench
tasks
filter
briefcase
fullscreen
dashboard
paperclip
heart-empty
link
phone
pushpin
usd
gbp
sort
sort-by-alphabet
sort-by-alphabet-alt
sort-by-order
sort-by-order-alt
sort-by-attributes
sort-by-attributes-alt
unchecked
expand
collapse-down
collapse-up
log-in
flash
log-out
new-window
record
save
open
saved
import
export
send
floppy-disk
floppy-saved
floppy-remove
floppy-save
floppy-open
credit-card
transfer
cutlery
header
compressed
earphone
phone-alt
tower
stats
sd-video
hd-video
subtitles
sound-stereo
sound-dolby
sound-5-1
sound-6-1
sound-7-1
copyright-mark
registration-mark
cloud-download
cloud-upload
tree-conifer
tree-deciduous
cd
save-file
open-file
level-up
copy
paste
on-door
on-key
alert
equalizer
king
queen
pawn
bishop
knight
baby-formula
tent
blackboard
bed
apple
erase
hourglass
lamp
duplicate
piggy-bank
scissors
bitcoin
yen
ruble
scale
ice-lolly
ice-lolly-tasted
education
option-horizontal
option-vertical
menu-hamburger
modal-window
oil
grain
sunglasses
text-size
text-color
text-background
object-align-top
object-align-bottom
object-align-horizontal
object-align-left
object-align-vertical
object-align-right
triangle-right
triangle-left
triangle-bottom
triangle-top
console
superscript
subscript
menu-left
menu-right
menu-down
menu-up''')
    )

    static final String UI_COLOR = 'ui:color'
    static final String UI_BGCOLOR = 'ui:bgcolor'
    static final String UI_ICON_COLOR = 'ui:icon:color'
    static final String UI_ICON_BGCOLOR = 'ui:icon:bgcolor'
    static final String UI_ICON_NAME = 'ui:icon:name'
    static final String UI_NODE_STATUS_COLOR = 'ui:status:color'
    static final String UI_NODE_STATUS_BGCOLOR = 'ui:status:bgcolor'
    static final String UI_NODE_STATUS_ICON = 'ui:status:icon'
    static final String UI_NODE_STATUS_TEXT = 'ui:status:text'
    static final String UI_BADGES_GLYPHICON = 'ui:badges'
    static final String glyphiconName(String name){
        if(name && name.startsWith('glyphicon-')) {
            String gname = name.substring('glyphicon-'.length())
            return glyphiconSet.contains(gname)?gname:null
        }
        null
    }
    def nodeBadgeIcons={attrs,body->
        String found = attrs.node?.attributes[UI_BADGES_GLYPHICON]
        if(found){
            found.split(/,\s*/).collect{
                glyphiconName(it)
            }.findAll{it}.each{
                out<<"<i class='glyphicon glyphicon-${it}'></i>"
            }
        }
    }
    def nodeIcon ={ attrs, body->
        if(attrs.node?.attributes[UI_ICON_NAME]){
            out<<icon([name:attrs.node?.attributes[UI_ICON_NAME]],body)
        }else{
            out<<body()
        }
    }
    def nodeStatusIcon ={ attrs, body->
        if(attrs.node?.attributes[UI_NODE_STATUS_ICON]){
            out<<icon([name:attrs.node?.attributes[UI_NODE_STATUS_ICON]],body)
        }else{
            out<<body()
        }
    }
    static final Set<String> cssColors=Collections.unmodifiableSet(
            qw('''aliceblue
antiquewhite
aqua
aquamarine
azure
beige
bisque
black
blanchedalmond
blue
blueviolet
brown
burlywood
cadetblue
chartreuse
chocolate
coral
cornflowerblue
cornsilk
crimson
cyan
darkblue
darkcyan
darkgoldenrod
darkgray
darkgreen
darkkhaki
darkmagenta
darkolivegreen
darkorange
darkorchid
darkred
darksalmon
darkseagreen
darkslateblue
darkslategray
darkturquoise
darkviolet
deeppink
deepskyblue
dimgray
dodgerblue
firebrick
floralwhite
forestgreen
fuchsia
gainsboro
ghostwhite
gold
goldenrod
gray
green
greenyellow
honeydew
hotpink
indianred
indigo
ivory
khaki
lavender
lavenderblush
lawngreen
lemonchiffon
lightblue
lightcoral
lightcyan
lightgoldenrodyellow
lightgreen
lightgrey
lightpink
lightsalmon
lightseagreen
lightskyblue
lightslategray
lightsteelblue
lightyellow
lime
limegreen
linen
magenta
maroon
mediumaquamarine
mediumblue
mediumorchid
mediumpurple
mediumseagreen
mediumslateblue
mediumspringgreen
mediumturquoise
mediumvioletred
midnightblue
mintcream
mistyrose
moccasin
navajowhite
navy
oldlace
olive
olivedrab
orange
orangered
orchid
palegoldenrod
palegreen
paleturquoise
palevioletred
papayawhip
peachpuff
peru
pink
plum
powderblue
purple
red
rosybrown
royalblue
saddlebrown
salmon
sandybrown
seagreen
seashell
sienna
silver
skyblue
slateblue
slategray
snow
springgreen
steelblue
tan
teal
thistle
tomato
turquoise
violet
wheat
white
whitesmoke
yellow
yellowgreen'''))

    private static Set<String> qw(String str) {
        str.split(/\s+/).findAll{it} as Set
    }
    static final Set<String> ansiColors=Collections.unmodifiableSet(
            qw('''ansi-fg-black
ansi-fg-green
ansi-fg-red
ansi-fg-yellow
ansi-fg-blue
ansi-fg-magenta
ansi-fg-cyan
ansi-fg-white
ansi-fg-light-black
ansi-fg-light-green
ansi-fg-light-red
ansi-fg-light-yellow
ansi-fg-light-blue
ansi-fg-light-magenta
ansi-fg-light-cyan'''))
    static final Set<String> ansiBgColors=Collections.unmodifiableSet(
            qw('''ansi-bg-black
ansi-bg-green
ansi-bg-red
ansi-bg-yellow
ansi-bg-blue
ansi-bg-magenta
ansi-bg-cyan
ansi-bg-white
ansi-bg-default'''))

    static final String testAnsiFg(String found) {
        ansiColors.contains(found) ? found : null
    }
    static final String testAnsiBg(String found) {
        ansiBgColors.contains(found) ? found : null
    }

    private static boolean cssColor(String bgcol) {
        cssColors.contains(bgcol?.toLowerCase()) || bgcol =~ /^#[0-9a-fA-F]{3,6}$/
    }
    static final Map nodeColors(def node){
        nodeColors(node, [UI_ICON_COLOR, UI_COLOR, UI_ICON_BGCOLOR, UI_BGCOLOR])
    }

    static final Map nodeStatusColors(def node) {
        nodeColors(node, [UI_NODE_STATUS_COLOR, UI_NODE_STATUS_BGCOLOR])
    }

    static final Map nodeColors(def node, List keys) {
        def map=[:]
        keys.each { attr ->
            def attrVal = node?.attributes[attr]
            if (attrVal) {
                if (testAnsiFg(attrVal)) {
                    map[attr] = [className: attrVal]
                } else if (cssColor(attrVal)) {
                    map[attr] = [style: attrVal]
                }
            }
        }
        map
    }
    def nodeIconStatusColor ={ attrs, body->
        def colors=nodeColors(attrs.node)
        boolean isicon=attrs.icon=='true'
        def found=isicon?(colors[UI_ICON_COLOR]?:colors[UI_COLOR]):colors[UI_COLOR]
        def bgcol=isicon?(colors[UI_ICON_BGCOLOR]?:colors[UI_BGCOLOR]):colors[UI_BGCOLOR]
        def dbg=''
        if(found || bgcol){

            def text=[
                    (found?.style?'color: '+found.style:''),
                    (bgcol?.style?'background-color: '+bgcol.style:''),
            ].findAll{it}.join('; ')
            def classcol=([found,bgcol].findAll{it}*.className).join(' ')
            if(classcol||text) {
                out << "<span class='${classcol}' style='${text}'>"
                out << body()
                out << "</span>"
                return
            }

        }else{
            dbg+=' zilch found: '+found+"/"+bgcol
        }
        out<<body()
    }
    def nodeHealthStatusColor ={ attrs, body->
        def colors=nodeColors(attrs.node, [UI_NODE_STATUS_COLOR, UI_NODE_STATUS_BGCOLOR])
        def found=colors[UI_NODE_STATUS_COLOR]
        def bgcol=colors[UI_NODE_STATUS_BGCOLOR]
        def dbg=''
        if(found || bgcol){
            def text=[
                    (found?.style?'color: '+found.style:''),
                    (bgcol?.style?'background-color: '+bgcol.style:''),
            ].findAll{it}.join('; ')
            def classcol=([found,bgcol].findAll{it}*.className).join(' ')
            if(classcol||text) {
                out << "<span class='${classcol}' style='${text}'"
                if(attrs.title){
                    out<<" title=\"${enc(attr:attrs.title)}\""
                }
                out << ">"
                out << body()
                out << "</span>"
                return
            }

        }else{
            dbg+=' zilch found: '+found+"/"+bgcol + " "+colors
        }
        out<<body()
    }
    def nodeIconStatusColorCss ={ attrs, body->
        def colors=nodeColors(attrs.node)
        boolean isicon=attrs.icon=='true'
        def found=isicon?(colors[UI_ICON_COLOR]?:colors[UI_COLOR]):colors[UI_COLOR]
        def bgcol=isicon?(colors[UI_ICON_BGCOLOR]?:colors[UI_BGCOLOR]):colors[UI_BGCOLOR]
        if(found || bgcol){

            def classcol=([found,bgcol].findAll{it}*.className).join(' ')
            if(classcol){
                return classcol
            }
        }
    }
    def nodeIconStatusColorStyle ={ attrs, body->
        def colors=nodeColors(attrs.node)
        boolean isicon=attrs.icon=='true'
        def found=isicon?(colors[UI_ICON_COLOR]?:colors[UI_COLOR]):colors[UI_COLOR]
        def bgcol=isicon?(colors[UI_ICON_BGCOLOR]?:colors[UI_BGCOLOR]):colors[UI_BGCOLOR]
        if(found || bgcol){

            def text=[
                    (found?.style?'color: '+found.style:''),
                    (bgcol?.style?'background-color: '+bgcol.style:''),
            ].findAll{it}.join('; ')
            if(text){
                return text
            }

        }
    }
    /**
     * @emptyTag
     * @attr name REQUIRED glyphicon name (default) or prefixed with glyphicon-, or font-awesome icon like "fa-name" or "fab-name"
     */
    def icon= { attrs, body ->
        if (attrs.name.startsWith('glyphicon-')) {
            attrs.name=attrs.name.substring('glyphicon-'.length())
        }else if(attrs.name.startsWith('fa-')){
            out << "<i class=\"fas ${attrs.name} ${attrs.css?:''}\"></i>"
            return
        }else if(attrs.name.startsWith('fab-')){
            out << "<i class=\"fab fa-${attrs.name.substring(4)} ${attrs.css?:''}\"></i>"
            return
        }
        if (glyphiconSet.contains(attrs.name)) {
            out << "<i class=\"glyphicon glyphicon-${attrs.name} ${attrs.css?:''}\"></i>"
        }else{
            if(Environment.current==Environment.DEVELOPMENT) {
                throw new Exception("icon name not recognized: ${attrs.name}, suggestions: "+(glyphiconSet.findAll{it.contains(attrs.name)||it=~attrs.name})+"?")
            }
            out<<body()
        }
    }
    /**
     * Output a basic table with headers and rows of data
     *
     * @attr classes additional css classes for the table
     * @attr columTitle map of column name to title
     * @attr columns required list of column names in order
     * @attr data required list of data points in order
     */
    def basicTable = { attrs, body ->
        out << "<table class=\"table ${attrs.classes ?: ''}\">"

        out << "<tr>"
        attrs.columns.each {
            out << "<th>${attrs.columnTitle?.get(it) ?: it}</th>"
        }
        out << "</tr>"

        attrs.data.each { row ->
            out << '<tr>'
            attrs.columns.each {
                out << "<td>${row.hasProperty(it) || row.properties[it] ? row[it] : ''}</td>"
            }
            out << '</tr>'
        }
        out << '</table>'
    }
    /**
     * Output a basic table for a single datapoint, with field names on the left, values on the right
     *
     * @attr classes additional css classes for the table
     * @attr fields list of fields to output in order
     * @attr fieldTitle map of field name to display title (optional)
     * @attr data required single data object with fields
     * @attr dataTitles tooltip titles for data fields
     * @attr recurse if true recurse through collection and map types
     */
    def basicData = { attrs, body ->
        def data = attrs.data
        def recurse = attrs.recurse
        out << "<table class=\"table ${attrs.classes ?: ''}\">"
        def fields=attrs.fields?:(data instanceof Map?data.keySet():[])

        fields.each {
            out << "<tr>"
            out << "<td>${attrs.fieldTitle?.get(it) ?: it}</td>"
            def val = (data.hasProperty(it) || data[it]) ? data[it] : ''
            def title = (attrs.dataTitles?.hasProperty(it) || attrs.dataTitles?.get(it)) ? attrs.dataTitles[it] : ''
            out << "<td title=\"${title}\">"
            if(recurse && val instanceof Map){
                out << g.basicData([classes:attrs.classes, data: val, recurse: true], body)
            }else if(recurse && val instanceof Collection){
                out << g.basicList([ data: val, recurse: true], body)
            }else{
                out<<val.toString()
            }
            out<<"</td>"
            out << "</tr>"
        }

        out << '</table>'

    }
    /**
     * Output a basic list for a list of items
     *
     * @attr classes additional css classes for the list
     * @attr recurse if true recurse through collection and map types
     * @attr ordered if true use ordered list, otherwise unordered list
     * @attr data required single data object with fields
     */
    def basicList = { attrs, body ->
        def data = attrs.data
        def recurse = attrs.recurse
        def ordered = attrs.ordered
        def tag=ordered?'ol':'ul'
        out << "<$tag class=\" ${attrs.classes ?: ''}\">"


        data.each {item->
            out << "<li>"
            if(recurse && item instanceof Map){
                out << g.basicData([classes:attrs.classes, data: item, recurse: true], body)
            }else if(recurse && item instanceof Collection){
                out << g.basicList([classes:attrs.classes, data: item, recurse: true], body)
            }else{
                out<<item.toString()
            }
            out << "</li>"
        }

        out << "</$tag>"

    }

    /**
     * @attr type REQUIRED menu type PROJECT, PROJECT_CONFIG, SYSTEM_CONFIG or USER_MENU
     * @attr var REQUIRED var name
     */
    def forMenuItems = { attrs, body ->
        String type = attrs.type
        String var = attrs.var
        String project = attrs.project
        def menuType = MenuItem.MenuType.valueOf(type.toUpperCase())
        if (menuType.projectType && !project) {
            throw new IllegalArgumentException("project attr is required for PROJECT type menu items")
        }
        String execution = attrs.execution
        if (menuType.executionType && !project && !execution) {
            throw new IllegalArgumentException("[project, execution] attrs is required for EXECUTION type menu items")
        }
        def auth = session.subject ? (
                project ? frameworkService.getAuthContextForSubjectAndProject(session.subject, project) :
                frameworkService.getAuthContextForSubject(session.subject)
        ) : null

        def checkEnabled = AuthMenuItem.getEnabledCheck(menuType, auth, project, execution)
        applicationContext.getBeansOfType(MenuItem).
                findAll { it.value.type == menuType }.
                findAll { checkEnabled.apply(it.value) }.
                each { name, MenuItem item ->
                    out << body((var): item)
                }
    }

    /**
     * Render the contents if menu items of the specified type exist
     * @attr type REQUIRED menu type PROJECT, PROJECT_CONFIG, SYSTEM_CONFIG or USER_MENU
     */
    def ifMenuItems = { attrs, body ->
        String type = attrs.type
        String project = attrs.project
        def menuType = MenuItem.MenuType.valueOf(type.toUpperCase())
        if (menuType.projectType && !project) {
            throw new IllegalArgumentException("project attr is required for PROJECT type menu items")
        }
        String execution = attrs.execution
        if (menuType.executionType && !project && !execution) {
            throw new IllegalArgumentException("[project, execution] attrs is required for EXECUTION type menu items")
        }
        def auth = session.subject ? (
                project ? frameworkService.getAuthContextForSubjectAndProject(session.subject, project) :
                frameworkService.getAuthContextForSubject(session.subject)
        ) : null

        def checkEnabled = AuthMenuItem.getEnabledCheck(menuType, auth, project, execution)

        if (applicationContext.getBeansOfType(MenuItem).
                findAll { it.value.type == menuType }.
                any { checkEnabled.apply(it.value) }
        ) {
            out << body()
        }

    }

    def showLocalLogin = { attrs, body ->
        boolean shouldShowLocalLogin = configurationService.getBoolean("login.localLogin.enabled", true)
        if(configurationService.getBoolean("login.showLocalLoginAfterFirstSSOLogin",false)) {
            shouldShowLocalLogin = frameworkService.getFirstLoginFile().exists()
        }
        if(shouldShowLocalLogin) {
            out << body()
        }
    }

    def templateExists = { attrs, body ->
        if (!attrs.name) {
            throw new IllegalArgumentException("name attr is required for templateExists tag")
        }
        boolean exists = groovyPageLocator.findTemplate(attrs.name) != null
        if(exists) {
            out << body()
        }
    }

    /**
     * @attr section REQUIRED section name
     * @attr jobComponents REQUIRED job components map
     *
     */
    def jobComponentSectionProperties={attrs,body->
        Map<String, JobDefinitionComponent> jobComponents=attrs.jobComponents
        if (!attrs.section) {
            throw new IllegalArgumentException("section attr is required for jobComponentSectionProperties tag")
        }
        String section=attrs.section
        List<Map<String, Object>> compProps = []
        jobComponents.collect { String name, JobDefinitionComponent jobComponent ->
            if(!jobComponent.inputProperties){
                return
            }
            def compSection=jobComponent.inputLocation?.section
            if(compSection!=section){
                return
            }
            compProps<<[name:(jobComponent.name),properties:jobComponent.inputProperties]
        }
        return compProps
    }

    /**
     * @attr defaultSection REQUIRED default section name
     * @attr jobComponents REQUIRED job components map
     * @attr skipSections list of section names to skip from result
     */
    def jobComponentSections = { attrs, body ->
        Map<String, JobDefinitionComponent> jobComponents = attrs.jobComponents
        String defaultSection = attrs.defaultSection
        List<String> skipSections = attrs.skipSections ?: []
        Map<String, Map<String, String>> sections = new HashMap<>()
        jobComponents.each { String name, JobDefinitionComponent jobComponent ->
            if (!jobComponent.inputProperties) {
                return
            }
            if (defaultSection && !jobComponent.inputLocation?.section) {
                sections.put(defaultSection, [:])
            } else if (jobComponent.inputLocation?.section) {
                sections.put(jobComponent.inputLocation?.section, [title: jobComponent.inputLocation?.sectionTitle])
            }
        }
        skipSections.each { sections.remove(it) }

        return sections
    }

    /**
     * Return the input field name prefix text for the component
     * @attr name required name of the job component
     */
    def jobComponentFieldPrefix={attrs,body->
        if (!attrs.name) {
            throw new IllegalArgumentException("name attr is required for jobComponentFieldPrefix tag")
        }
        RundeckJobDefinitionManager.getFormFieldPrefixForJobComponent(attrs.name)
    }

    /**
     * Return the messages type (prefix) for component i18n messages
     * @attr name required name of the job component
     */
    def jobComponentMessagesType={attrs,body->
        if (!attrs.name) {
            throw new IllegalArgumentException("name attr is required for jobComponentMessagesType tag")
        }
        RundeckJobDefinitionManager.getMessagesTypeForJobComponent(attrs.name)
    }
}
