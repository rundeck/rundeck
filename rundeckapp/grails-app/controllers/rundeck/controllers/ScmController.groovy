package rundeck.controllers

import rundeck.ScheduledExecution

class ScmController {
    def scmService
    def index(String project) {
        if(scmService.isAlreadySetup(project)) {
            def pluginConfig = scmService.loadScmConfig(project,'export')
            def describedPlugin = scmService.getPluginDescriptor(pluginConfig.type)
            return [plugin:describedPlugin,config:pluginConfig.config,type:pluginConfig.type]
        }
        def plugins=scmService.listPlugins('export')

        [plugins: plugins]
    }
    def setup(String project, String type){
        if(scmService.isAlreadySetup(project)){
            return redirect(action:'index',params:[project:project])
        }
        [properties:scmService.getSetupProperties(project,type),type:type]
    }

    def saveSetup(String integration, String project, String type) {
        def config = params.config
        //require type param
        scmService.savePlugin(integration, project, type, config)

        flash.message = "setup complete"
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
        def scmStatus=scmService.exportStatusForJobs(jobs)
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
        def commitid=scmService.exportCommit(project,params.commit,jobs)
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
        def diff=scmService.exportDiff(project,job)
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
        def diffResult=scmService.exportDiff(project,job)
        if(diffResult){
            render(contentType: diffResult.contentType?:'text/plain', text: diffResult.content)
        }
    }
}
