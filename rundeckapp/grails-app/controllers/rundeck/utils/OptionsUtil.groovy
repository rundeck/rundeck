package rundeck.utils

import grails.util.Holders
import org.apache.log4j.Logger
import org.springframework.web.context.request.RequestContextHolder
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService

import javax.servlet.http.HttpSession

class OptionsUtil {
    static Logger logger = Logger.getLogger(OptionsUtil)

    /**
     * Map of descriptive property name to ScheduledExecution domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static jobprops=[
            name:'jobName',
            group:'groupPath',
            description:'description',
            project:'project',
    ]
    /**
     * Map of descriptive property name to Option domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static optprops=[
            name:'name',

    ]

    /**
     * Expand the URL string's embedded property references of the form
     * ${job.PROPERTY} and ${option.PROPERTY}.  available properties are
     * limited
     */
    protected static String expandUrl(Option opt, String url, ScheduledExecution scheduledExecution,selectedoptsmap=[:],boolean isHttp=true) {
        def invalid = []
        def frameworkService = getFrameworkServiceInstance()
        def se = getHttpSessionInstance()
        def rundeckProps=[
                'nodename':frameworkService.getFrameworkNodeName(),
                'serverUUID':frameworkService.serverUUID?:''
        ]
        if(!isHttp) {
            rundeckProps.basedir= frameworkService.getRundeckBase()
        }
        def extraJobProps=[
                'user.name': (se?.user?: "anonymous"),
        ]
        extraJobProps.putAll rundeckProps.collectEntries {['rundeck.'+it.key,it.value]}
        Map globals=frameworkService.getProjectGlobals(scheduledExecution.project)

        def replacement= { Object[] group ->
            if (group[2] == 'job' && jobprops[group[3]] && scheduledExecution.properties.containsKey(jobprops[group[3]])) {
                scheduledExecution.properties.get(jobprops[group[3]]).toString()
            } else if (group[2] == 'job' && null != extraJobProps[group[3]]) {
                def value = extraJobProps[group[3]]
                value.toString()
            }else if (group[2] == 'globals' && null != globals[group[3]]) {
                def value = globals[group[3]]
                value.toString()
            }else if (group[2] == 'rundeck' && null != rundeckProps[group[3]]) {
                def value = rundeckProps[group[3]]
                value.toString()
            } else if (group[2] == 'option' && optprops[group[3]] && opt.properties.containsKey(optprops[group[3]])) {
                opt.properties.get(optprops[group[3]]).toString()
            } else if (group[2] == 'option' && group[4] == '.value') {
                def optname = group[3].substring(0, group[3].length() - '.value'.length())
                def value = selectedoptsmap && selectedoptsmap instanceof Map ? selectedoptsmap[optname] : null
                //find option with name
                def Option expopt = scheduledExecution.options.find { it.name == optname }
                if (value && expopt?.multivalued && (value instanceof Collection || value instanceof String[])) {
                    value = value.join(expopt.delimiter)
                }
                (value ?: '')
            } else {
                null
            }
        }
        //replace variables in the URL, using appropriate encoding before/after the URL parameter '?' separator
        def arr=url.split(/\?/,2)
        def codecs=['URIComponent','URL']
        def result=[]
        arr.eachWithIndex { String entry, int i ->
            result<<entry.replaceAll(/(\$\{(job|option|rundeck|globals)\.([^}]+?(\.value)?)\})/) { Object[] group ->
                def val = replacement(group)
                if (null != val) {
                    if(!isHttp){
                        return val
                    }
                    val."encodeAs${codecs[i]}"()
                } else {
                    invalid << group[0]
                    group[0]
                }
            }
        }
        String srcUrl = result.join('?')
        if (invalid) {
//            LogFactory.getLog(this).error("invalid expansion: " + invalid);
            logger.error("invalid expansion: " + invalid)
        }
        return srcUrl
    }

    protected static FrameworkService getFrameworkServiceInstance(){
        return Holders.applicationContext.getBean('frameworkService') as FrameworkService
    }

    protected static HttpSession getHttpSessionInstance(){
        return RequestContextHolder.currentRequestAttributes().getSession()
    }
}
