import {By, WebElement, WebElementPromise, until} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import { Context } from '@rundeck/testdeck/context';

export const Elems= {
    jobNameInput  : By.css('form input[name="jobName"]'),
    groupPathInput  : By.css('form input[name="groupPath"]'),
    descriptionTextarea  : By.css('form textarea[name="description"]'),
    saveButton  : By.css('#Create'),
    editSaveButton: By.css('#editForm div.card-footer input.btn.btn-primary[type=submit][value=Save]'),
    errorAlert  : By.css('#error'),
    formValidationAlert: By.css('#page_job_edit > div.list-group-item > div.alert.alert-danger'),

    tabWorkflow  : By.css('#job_edit_tabs > li > a[href=\'#tab_workflow\']'),
    addNewWfStepButton: By.xpath('//*[@id="wfnewbutton"]/span'),
    addNewWfStepCommand: By.css('#wfnewtypes #addnodestep > div > a.add_node_step_type[data-node-step-type=command]'),
    wfStepCommandRemoteText: By.css('#adhocRemoteStringField'),
    wfStep0SaveButton: By.css('#wfli_0 div.wfitemEditForm div._wfiedit > div.floatr > span.btn.btn-primary.btn-sm'),
    wfstep0vis: By.css('#wfivis_0'),
    optionNewButton: By.css('#optnewbutton > span'),
    option0EditForm: By.css('#optvis_0 > div.optEditForm'),
    option0NameInput: By.css('#optvis_0 > div.optEditForm input[type=text][name=name]'),
    optionFormSaveButton: By.css('#optvis_0 > div.optEditForm  div.floatr > span.btn.btn-primary.btn-sm'),
    option0UsageSession: By.css('#optvis_0 > div.optEditForm > div > section.section-separator-solo'),
    option0Type: By.xpath('//*[starts-with(@id,"sectrue")]'),
    option0KeySelector: By.xpath('//*[starts-with(@id,"defaultStoragePath")]'),
    option0OpenKeyStorage: By.xpath('//*[starts-with(@id,"optedit")]/div[7]/div/div/span[2]/a'),
    option0li: By.css('#optli_0'),
    
    storagebrowse: By.xpath('//*[starts-with(@id,"storagebrowse")]'),
    storagebrowseClose: By.xpath('//*[@id="storagebrowse"]/div/div/div[3]/button[1]'),

    notificationsTab: By.css("#job_edit_tabs > li > a[href=\'#tab_notifications\']"),
    enableNotifications: By.css('#notifiedTrue'),
    notifyOnsuccessEmail: By.css('#notifyOnsuccessEmail'),
    notifySuccessRecipients: By.css('#notifySuccessRecipients'),
    tabNodes  : By.css('#job_edit_tabs > li > a[href=\'#tab_nodes\']'),
    doNodedispatchTrue  : By.xpath('//*[@id="doNodedispatchTrue"]'),
    nodeFilter  : By.xpath('//*[@id="schedJobNodeFilter"]'),
    nodeFilterButton  : By.xpath('//*[@id="nodegroupitem"]/div[3]/div/div/span/div[1]/button'),
    nodeFilterSelectAllLink  : By.xpath('//*[@id="nodegroupitem"]/div[3]/div/div/span/div[1]/ul/li[1]/a'),
    matchedNodesText  : By.xpath('//*[@id="nodegroupitem"]/div[6]/div/div[1]/div/div/div[1]/div[1]/span[1]/span'),
    workflowStrategy  : By.xpath('//*[@id="workflow.strategy"]'),
    strategyPluginparallel: By.xpath('//*[@id="strategyPluginparallel"]'),
    strategyPluginparallelMsg: By.xpath('//*[@id="strategyPluginparallel"]/span/span'),
    strategyPluginsequential: By.xpath('//*[@id="strategyPluginsequential"]'),
    strategyPluginsequentialMsg: By.xpath('//*[@id="strategyPluginsequential"]/span/span'),   

    optionUndoButton: By.xpath('//*[@id="optundoredo"]/div/span[1]'),
    optionRedoButton: By.xpath('//*[@id="optundoredo"]/div/span[2]'),
    revertOptionsButton: By.xpath('//*[@id="optundoredo"]/div/span[3]'),
    revertOptionsConfirm: By.xpath('//*[starts-with(@id,"popover")]/div[2]/span[2]'),

    //wfUndoButton: By.xpath('//*[@id="wfundoredo"]/div/span[1]'),
    wfUndoButton: By.css('#wfundoredo > div > span.btn.btn-xs.btn-default.act_undo.flash_undo'),    
    //wfRedoButton: By.xpath('//*[@id="wfundoredo"]/div/span[2]'),
    wfRedoButton: By.css('#wfundoredo > div > span.btn.btn-xs.btn-default.act_redo.flash_undo'),
    revertWfButton: By.xpath('//*[@id="wfundoredo"]/div/span[3]'),
    revertWfConfirm: By.xpath('//*[starts-with(@id,"popover")]/div[2]/span[2]')

 }
 

export class JobCreatePage extends Page {
    path = '/resources/createProject'
    projectName=''

    constructor(readonly ctx: Context, readonly project: string) {
        super(ctx)
        this.projectName=project
        this.path = `/project/${project}/job/create`
    }
    editPagePath(jobId: string){
        return `/project/${this.projectName}/job/edit/${jobId}`
    }
    async getEditPage(jobId: string) {
        const {driver} = this.ctx
        await driver.get(this.ctx.urlFor(this.editPagePath(jobId)))
    }

    async jobNameInput(){
        return await this.ctx.driver.findElement(Elems.jobNameInput)
    }
    async groupPathInput(){
        return await this.ctx.driver.findElement(Elems.groupPathInput)
    }
    async descriptionTextarea(){
        return await this.ctx.driver.findElement(Elems.descriptionTextarea)
    }
    saveButton():WebElementPromise{
        return this.ctx.driver.findElement(Elems.saveButton)
    }
    async editSaveButton(){
        return this.ctx.driver.findElement(Elems.editSaveButton)
    }
    errorAlert():WebElementPromise{
        return this.ctx.driver.findElement(Elems.errorAlert)
    }
    async tabWorkflow(){
        return await this.ctx.driver.findElement(Elems.tabWorkflow)
    }
    async addNewWfStepCommand(){
        return await this.ctx.driver.findElement(Elems.addNewWfStepCommand)
    }
    async waitWfStepCommandRemoteText(){
        await this.ctx.driver.wait(until.elementLocated(Elems.wfStepCommandRemoteText), 15000)
    }
    async wfStepCommandRemoteText(){
        return await this.ctx.driver.findElement(Elems.wfStepCommandRemoteText)
    }
    async wfStep0SaveButton(){
        return await this.ctx.driver.findElement(Elems.wfStep0SaveButton)
    }
    async wfstep0vis(){
        return await this.ctx.driver.findElement(Elems.wfstep0vis)
    }
    async waitWfstep0vis(){
        await this.ctx.driver.wait(until.elementLocated(Elems.wfstep0vis), 15000)
    }
    async optionNewButton(){
        return await this.ctx.driver.findElement(Elems.optionNewButton)
    }
    async waitoption0EditForm(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0EditForm),15000)
    }
    async option0NameInput(){
        return await this.ctx.driver.findElement(Elems.option0NameInput)
    }
    async optionFormSaveButton(){
        return await this.ctx.driver.findElement(Elems.optionFormSaveButton)
    }
    async waitOptionFormSaveButton(){
        await this.ctx.driver.wait(until.elementLocated(Elems.optionFormSaveButton), 15000)
    }
    async waitOption0li(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0li),15000)
    }

    async enableNotificationInput(){
        return this.ctx.driver.wait(until.elementLocated(Elems.enableNotifications),15000)
    }

    async notifyOnsuccessEmail(){
        return this.ctx.driver.wait(until.elementLocated(Elems.notifyOnsuccessEmail),15000)
    }

    async notifySuccessRecipients(){
        return this.ctx.driver.wait(until.elementLocated(Elems.notifySuccessRecipients),15000)
    }

    async notificationsTab(){
        return await this.ctx.driver.findElement(Elems.notificationsTab)
    }
    async tabNodes(){
        return await this.ctx.driver.findElement(Elems.tabNodes)
    }
    async dispatchNodes(){
        return this.ctx.driver.wait(until.elementLocated(Elems.doNodedispatchTrue),15000)
    }
    async nodeFilter(){
        return this.ctx.driver.wait(until.elementLocated(Elems.nodeFilter),15000)
    }
    async nodeFilterButton(){
        return this.ctx.driver.wait(until.elementLocated(Elems.nodeFilterButton),15000)
    }
    async nodeFilterSelectAllLink(){
        return this.ctx.driver.wait(until.elementLocated(Elems.nodeFilterSelectAllLink),15000)
    }
    async matchedNodes(){
        return this.ctx.driver.wait(until.elementLocated(Elems.matchedNodesText),15000)
    }
    async matchedNodesText(){
        let matchedNodeElem = await this.matchedNodes()
        return await matchedNodeElem.getText()
    }
    async workflowStrategy(){
        return this.ctx.driver.wait(until.elementLocated(Elems.workflowStrategy),15000)
    }
    async strategyPluginparallel(){
        return this.ctx.driver.wait(until.elementLocated(Elems.strategyPluginparallel),15000)
    }

    async strategyPluginparallelMsg(){
        return this.ctx.driver.wait(until.elementLocated(Elems.strategyPluginparallelMsg),15000)
    }
    async strategyPluginparallelText(){
        let matchedNodeElem = await this.strategyPluginparallelMsg()
        return await matchedNodeElem.getText()
    }

    async strategyPluginsequential(){
        return this.ctx.driver.wait(until.elementLocated(Elems.strategyPluginsequential),25000)
    }

    async strategyPluginsequentialMsg(){
        return this.ctx.driver.wait(until.elementLocated(Elems.strategyPluginsequentialMsg),25000)
    }
    async strategyPluginsequentialText(){
        let matchedNodeElem = await this.strategyPluginsequentialMsg()
        return await matchedNodeElem.getText()
    }

    async option0UsageSession(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0UsageSession),25000)
    }

    async option0Type(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0Type),25000)
    }

    async option0KeySelector(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0KeySelector),25000)
    }

    async option0OpenKeyStorage(){
        return this.ctx.driver.wait(until.elementLocated(Elems.option0OpenKeyStorage),25000)
    }

    async storagebrowse(){
        return this.ctx.driver.wait(until.elementLocated(Elems.storagebrowse),25000)
    }

    async storagebrowseClose(){
        return this.ctx.driver.wait(until.elementLocated(Elems.storagebrowseClose),25000)
    }

    async optionNameInput(position: string){
        let  optionNameInput = By.css('#optvis_'+position+' > div.optEditForm input[type=text][name=name]')
        return await this.ctx.driver.findElement(optionNameInput)
    }

    async optionFormSave(position: string){
        let  optionEditForm = By.css('#optvis_'+position+' > div.optEditForm  div.floatr > span.btn.btn-primary.btn-sm')
        return await this.ctx.driver.findElement(optionEditForm)
    }

    async waitoptionEditForm(position: string){
        let  optionEditForm = By.css('#optvis_'+position+' > div.optEditForm')
        return this.ctx.driver.wait(until.elementLocated(optionEditForm),15000)
    }

    async isOptionli(position: string){
        let  optionEditForm = By.css('#optli_'+position)

        try {
            await this.ctx.driver.findElement(optionEditForm);
            return true;
        } catch (e) {
            return false;
        }
    }
  
    async waitOptionli(position: string){
        let  optionEditForm = By.css('#optli_'+position)

        return this.ctx.driver.wait(until.elementLocated(optionEditForm),15000)
    }

    async optionUndoButton(){
        return await this.ctx.driver.findElement(Elems.optionUndoButton)
    }

    async optionRedoButton(){
        return await this.ctx.driver.findElement(Elems.optionRedoButton)
    }

    async waitUndoRedo(wait: any){
        return await this.ctx.driver.sleep(wait)
    }

    async revertOptionsButton(){
        return await this.ctx.driver.findElement(Elems.revertOptionsButton)
    }

    async revertOptionsConfirm(){
        return await this.ctx.driver.findElement(Elems.revertOptionsConfirm)
    }

    async wfStepSaveButton(position: string){
        let wfStep0SaveButton = By.css('#wfli_'+position+' div.wfitemEditForm div._wfiedit > div.floatr > span.btn.btn-primary.btn-sm')
        return await this.ctx.driver.findElement(wfStep0SaveButton)
    }
    async waitWfstepvis(position: string){
        let wfstep0vis = By.css('#wfivis_'+position)
        await this.ctx.driver.wait(until.elementLocated(wfstep0vis), 15000)
    }

    async wfUndoButton(){
        return await this.ctx.driver.findElement(Elems.wfUndoButton)
    }

    async wfRedoButton(){
        return await this.ctx.driver.findElement(Elems.wfRedoButton)
    }

    async waitWfUndoButton(){
        await this.ctx.driver.wait(until.elementLocated(Elems.wfUndoButton), 15000)
    }

    async waitWfRedoButton(){
        await this.ctx.driver.wait(until.elementLocated(Elems.wfRedoButton), 15000)
    }

    async waitRevertWfButton(){
        await this.ctx.driver.wait(until.elementLocated(Elems.revertWfButton), 15000)
    }

    async revertWfButton(){
        return await this.ctx.driver.findElement(Elems.revertWfButton)
    }

    async revertWfConfirm(){
        return await this.ctx.driver.findElement(Elems.revertWfConfirm)
    }

    async isWfli(position: string){
        let  optionEditForm = By.css('#wfivis_'+position)

        try {
            await this.ctx.driver.findElement(optionEditForm);
            return true;
        } catch (e) {
            return false;
        }
    }

    async addNewWfStepButton(){
        return await this.ctx.driver.findElement(Elems.addNewWfStepButton)
    }

    async waitAddNewWfStepButton(){
        await this.ctx.driver.wait(until.elementLocated(Elems.addNewWfStepButton), 15000)
    }
    
    formValidationAlert():WebElementPromise{
        return this.ctx.driver.findElement(Elems.formValidationAlert)
    }

}
