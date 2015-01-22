package rundeck

import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import org.rundeck.web.infosec.HMacSynchronizerTokensHolder
import org.rundeck.web.infosec.HMacSynchronizerTokensManager
import org.codehaus.groovy.grails.web.servlet.mvc.SynchronizerTokensHolder
import rundeck.filters.FormTokenFilters
import rundeck.services.FrameworkService

import java.text.MessageFormat
import java.text.SimpleDateFormat

class UtilityTagLib{
    def static  daysofweekkey = [Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY];
    def public static daysofweekord = ScheduledExecution.daysofweeklist;
    def public static monthsofyearord = ScheduledExecution.monthsofyearlist;
	static returnObjectForTags = ['rkey','w3cDateValue','sortGroupKeys','helpLinkUrl','helpLinkParams','parseOptsFromString','relativeDateString','enc','textFirstLine','textRemainingLines']

    private static Random rand=new java.util.Random()
    def HMacSynchronizerTokensManager hMacSynchronizerTokensManager
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
            dms= (long)Math.floor(attrs.time / 1000.0)
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
        out<<relativeDateString(attrs,body)
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
            def Date date = attrs.elapsed
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
                val << "${s}s"
            }else if(test <  (5 * 60) ){
                val <<  "${m}m"
                if(s > 0){
                    val << "${s}s"
                }
            }else if(test < (60 * 60) ){
                val <<  "${m}m"

            }else if (test < (24 * 60 * 60)){
                val << "${h}h"
                if(m > 0 ){
                    val << "${m}m"
                }
            }else{
                val << "${d}d"
                if(h > 0 ){
                    val << "${h}h"
                }
            }
            def StringBuffer val2 = new StringBuffer()

            if(diff > 0 && !attrs.end){
                val2 << "in "
            }
            val2 << "<span class=\"${enc(attr:diff > 0 ? (attrs.untilClass?:'until') : (attrs.agoClass ?: 'ago'))}\" >"
            val2 << val.toString()
            val2 << "</span>"
            if(diff < 0 && !attrs.end){
                val2 << " ago"
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
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
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


    def autoLink={ attrs,body->
        def outx=body()
        def xparams = params.project?[params:[project:params.project]]:[:]
        def linkopts=[
                execution:[
                        pattern: /\{\{(Execution\s+(\d+))\}\}/ ,
                        linkParams:[action:'show',controller:'execution']
                ],
                job:[
                        pattern: /\{\{(Job\s+([0-9a-h]{8}-[0-9a-h]{4}-[0-9a-h]{4}-[0-9a-h]{4}-[0-9a-h]{12}))\}\}/ ,
                        linkParams:[action:'show',controller:'scheduledExecution'],
                        textValue:{
                            def job= ScheduledExecution.getByIdOrUUID(it)
                            return job?job.generateFullName():it
                        }
                ]
        ]
        linkopts.each{k,opts->
            outx=outx.replaceAll(opts.pattern){
                if(opts.linkParams){
                    def lparams= [:]+opts.linkParams
                    lparams.id=it[2]
                    def text = opts.textValue?opts.textValue(it[2]):it[1]
                    return g.link(lparams + xparams,text)
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
        if(attrs.unit && null!=attrs.value){
            if(attrs.unit=='byte' || attrs.unit=='hbyte'){
                long val=attrs.value instanceof String?Long.parseLong(attrs.value):attrs.value
                def testmap=[
                        byte:[
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
                def testset=testmap[attrs.unit]
                def found
                testset.eachWithIndex  {lvl, x -> if (!found && val < lvl.value) {found = testset[x-1]} }
                if(!found){
                    found=testset[-1]
                }
                def outputNumber = found.value > 0 ? (val / found.value) : val
                out<<g.formatNumber([number : outputNumber, type : "number", maxFractionDigits: "2"],body)+' '+found.name
            }else if(attrs.unit=='ms'){
                attrs.time=attrs.value
                out<<timeDuration(attrs,body)
            }else if(attrs.unit=='%'){
                out << g.formatNumber([number: attrs.value, type: "number", maxFractionDigits: "2"], body) + '%'
            }else{
                out<<attrs.value+' ('+attrs.unit+')'
            }
        }
    }
    def helpLinkParams={attrs,body->
        def medium = "${grailsApplication.metadata['app.version']} ${System.getProperty('os.name')} java ${System.getProperty('java.version')}"
        def campaign = attrs.campaign?:'helplink'
        def sourceName = g.message(code:'main.app.id',default: 'rundeckapp')
        def helpParams = [utm_source: sourceName, utm_medium: medium, utm_campaign: campaign, utm_content: (controllerName + '/' + actionName)]
        return helpParams.collect { k, v -> k + '=' + v }.join('&')
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
        def rdversion = grailsApplication.metadata['app.version']
        def helpBase='http://rundeck.org/' +( rdversion?.contains('SNAPSHOT')?'docs':rdversion)
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
            out << "you"
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
        def plural=attrs.code?g.message(code:(attrs.code+'.plural')):attrs.plural?:(singular+'s')
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
     * Sanitize an HTML string
     */
    def sanitize={attrs,body->
        if(attrs.html){
            out<<attrs.html.encodeAsSanitizedHTML()
        }else{
            out<<body().encodeAsSanitizedHTML()
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
            out<< (split.length>0?split[0]:attrs.text)
        }else{
            out<<attrs.text
        }
    }
    def textRemainingLines={attrs,body->
        if(attrs.text){
            def split=attrs.text.toString().split(/(\r\n?|\n)/,2)
            if(split.length==2){
                out << split[1]
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
     * @attr code a i18n message code, or comma separated list
     * @attr id element id to use (optional)
     */
    def jsMessages = { attrs, body ->
        def id = attrs.id ?: 'i18nmessages'
        def msgs = [:]
        if (attrs.code) {
            attrs.code.split(',').each {
                msgs[it] = g.message(code: it, default: it)
            }
        }
        embedJSON.call([id: id, data: msgs],null)
        out << '<script>'
        out << 'if(typeof(window.Messages)!=\'object\'){'
        out << 'window.Messages={};'
        out << '}'
        out << 'jQuery.extend(window.Messages,loadJsonData(\'' + enc(js: id) + '\'));'
        out << '</script>'
    }
    def refreshFormTokensHeader = { attrs, body ->
        SynchronizerTokensHolder tokensHolder = tokensHolder()
        def uri = attrs.uri ?: params[SynchronizerTokensHolder.TOKEN_URI]
        response.addHeader(FormTokenFilters.TOKEN_KEY_HEADER, tokensHolder.generateToken(uri))
        response.addHeader(FormTokenFilters.TOKEN_URI_HEADER, uri)
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
        def token = generateToken(attrs.url)
        embedJSON.call([id: id, data: [TOKEN: token, URI: attrs.url]],body)
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
        def applicationTagLib = grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.FormTagLib')
        applicationTagLib.form.call(attrs,body)
    }

}
