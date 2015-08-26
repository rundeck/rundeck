package rundeck.controllers

import com.dtolabs.rundeck.plugins.scm.ScmPlugin
import rundeck.ScheduledExecution

class ScmController {
    def scmService
    def index(String project) {
        if(scmService.isAlreadySetup(project)) {
            def type = scmService.hasConfig(project)
            def plugin = scmService.loadConfig(type, project)
            def describedPlugin=scmService.getPluginDescriptor(type)
            return [plugin:describedPlugin,config:plugin,type:type]
        }
        def plugins=scmService.listPlugins(project)

        [plugins: plugins]
    }
    def setup(String project, String type){
        if(scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        [properties:scmService.getSetupProperties(project,type),type:type]
    }
    def saveSetup(String project, String type){
        def config=params.config
        //require type param
        scmService.initPlugin(project,type,config)

        flash.message="setup complete"
        redirect(action: 'index', params: [project: project])

    }
    def commit(String project, int ndx){
        if(!scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        List<String> jobIds=[]
        if(params.jobIds){
            jobIds=[params.jobIds].flatten()
        }else if(params.allJobs){
            jobIds=ScheduledExecution.findAllByProject(params.project).collect{
                it.extid
            }
        }
        System.err.println("jobids: ${jobIds}")
        List<ScheduledExecution> jobs = jobIds.collect{
            ScheduledExecution.getByIdOrUUID(it)
        }
        def scmStatus=scmService.statusForJobs(jobs)
        def scmFiles=scmService.filePathsMapForJobRefs(scmService.jobRefsForJobs(jobs))
        [
                properties:scmService.getCommitProperties(project,jobIds),
                jobs:jobs,
                scmStatus:scmStatus,
                selected:params.jobIds?jobIds:[],
                filesMap:scmFiles
        ]
    }
    def saveCommit(String project, int ndx){
        if(!scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        if(!params.jobIds){
            flash.message="No Job Ids Selected"
            return redirect(action:'index',params:[project:project])
        }
        List<String> jobIds=[params.jobIds].flatten()
        System.err.println("jobids: ${jobIds}")
        List<ScheduledExecution> jobs = jobIds.collect{
            ScheduledExecution.getByIdOrUUID(it)
        }
        def commitid=scmService.commit(project,params.commit,jobs)
        flash.message="Committed: ${commitid}"
        redirect(action: 'jobs',controller: 'menu',params: [project:params.project])
    }

    /**
     * Ajax endpoint for job diff
     */
    def diffRemote(String project, String jobId) {
        if(!scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        if(!jobId){
            flash.message="No jobId Selected"
            return redirect(action:'index',params:[project:project])
        }
        def job=ScheduledExecution.getByIdOrUUID(jobId)
        def diff=scmService.diff(project,job)
        render(contentType: 'application/json'){
            contentType=diff.contentType
            content=diff.content
        }
    }
    def diff(String project, String jobId) {
        if(!scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        if(!jobId){
            flash.message="No jobId Selected"
            return redirect(action:'index',params:[project:project])
        }
        def job=ScheduledExecution.getByIdOrUUID(jobId)
        def diffResult=scmService.diff(project,job)
        System.err.println("type: ${diffResult?.contentType}")
        System.err.println("content: ${diffResult?.content}")
        if(diffResult){
            System.err.println("type: ${diffResult.contentType}")
            System.err.println("content: ${diffResult.content}")
            render(contentType: diffResult.contentType?:'text/plain', text: diffResult.content)
        }
    }
}
