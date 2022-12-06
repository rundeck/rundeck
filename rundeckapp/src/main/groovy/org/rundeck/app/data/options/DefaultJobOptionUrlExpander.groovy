package org.rundeck.app.data.options

import com.dtolabs.rundeck.core.options.JobOptionUrlExpander
import com.dtolabs.rundeck.core.user.BaseUserProfile
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.option.OptionData
import rundeck.services.FrameworkService
import rundeck.services.UserService

class DefaultJobOptionUrlExpander implements JobOptionUrlExpander {

    FrameworkService frameworkService
    UserService userService

    /**
     * Map of descriptive property name to ScheduledExecution domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static jobprops=[
            name:'jobName',
            group:'groupPath',
            description:'description',
            project:'project'
    ]
    /**
     * Map of descriptive property name to Option domain class property names
     * used by expandUrl for embedded property references in remote options URL
     */
    private static optprops=[
            name:'name'
    ]

    /**
     * Expand the URL string's embedded property references of the form
     * ${job.PROPERTY} and ${option.PROPERTY}.  available properties are
     * limited
     */
    @Override
    String expandUrl(String urlToExpand, JobData job, OptionData option, Map selectedOptsMap) {
        def invalid = []
        def rundeckProps=[
                'nodename':frameworkService.getFrameworkNodeName(),
                'serverUUID':frameworkService.serverUUID?:''
        ]
        def realUrl = opt.realValuesUrl.toExternalForm()
        if(!urlToExpand.matches(/(?i)^https?:.*$/)) {
            rundeckProps.basedir= frameworkService.getRundeckBase()
        }

        BaseUserProfile userProfile = userService.getCurrentUserInfo()
        def extraJobProps=[
                'user.name': userProfile.username,
                'user.email': userProfile.email,
                'user.first': userProfile.firstname,
                'user.last': userProfile.lastname
        ]
        extraJobProps.putAll rundeckProps.collectEntries {['rundeck.'+it.key,it.value]}
        Map globals=frameworkService.getProjectGlobals(job.project)

        def replacement= { Object[] group ->
            if (group[2] == 'job' && jobprops[group[3]] && job.properties.containsKey(jobprops[group[3]])) {
                job.properties.get(jobprops[group[3]]).toString()
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
                OptionData expopt = job.options.find { it.name == optname }
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
            log.error("invalid expansion: " + invalid)
        }
        return srcUrl
    }
}
