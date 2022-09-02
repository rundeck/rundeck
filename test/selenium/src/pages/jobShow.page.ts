import {By, until} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import { Context } from '@rundeck/testdeck/context'

export const Elems = {
  jobGroup : By.css('div.jobInfoSection a.text-secondary'),
  jobTitleLink: By.css('#jobInfo_ > span > a.job-header-link'),
  jobTags: By.css('ul#tagsList > li.tag-pill-li'),
  jobUuidText: By.css('#subtitlebar.job-page > div > div > section > small.uuid'),
  jobDescription: By.css('#subtitlebar.job-page > div > div > div.jobInfoSection > section > span.h5'),
  optionInput: By.css('#8f95c8d5_seleniumOption1'),
  jobDefinition  : By.css('a[href="#job-definition-modal"]'),
  jobDefinitionModal  : By.css('div#job-definition-modal div.modal-content'),
  jobDefModalScheduledDaysDiv: By.css('div#DayOfWeekDialog'),
  jobDefModalScheduledMonthsDiv: By.css('div#MonthDialog'),
  jobDefModalScheduleEveryDaySelected: By.xpath('//div[contains(@class, \'cronselected\') and text() = \'every day\']'),
  jobDefinitionModalCloseBtn  : By.css('#job-definition-modal .modal-footer button[data-dismiss="modal"]'),
  notificationDefinition: By.css('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx'),
  notificationDefinitionToggle: By.css('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx > span.toggle'),
  notificationDefinitionDetailHttpRemoteUrl: By.xpath('//*[@id="detailtable"]/div/div/table/tbody[3]/tr[1]/td[2]/div/div/div/span[2]/div[2]/div/span/span[1]/span[2]'),
  nodeFilterSection: By.css('#detailtable.tab-pane  tr#exec_detail_nodes '),
  nodeFilterSectionMatchednodes: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__matchednodes'),
  nodeFilterSectionThreadcount: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__threadcount'),
  nodeFilterSectionNodeKeepgoing: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeKeepgoing'),
  nodeFilterSectionNodeRankOrderAscending: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeRankOrderAscending'),
  nodeFilterSectionNodeSelectedByDefault: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeSelectedByDefault'),
  orchestratorText: By.css('#detailtable.tab-pane  tr#exec_detail_orchestrator  #exec_detail__orchestrator summary'),
  jobEditButton: By.xpath('//*[@id="subtitlebar"]/div/div[2]/div/div/ul/li[1]/a'),
  jobActionDropdown: By.css('.job-action-button > .btn-group > a.dropdown-toggle'),
  jobStepDefinition: By.css('#wfitem_0 > span > div > div > span > span > span.text-success')
  
}

export class JobShowPage extends Page {
  path = '/placeholder'

  constructor(readonly ctx: Context, readonly project: string, readonly jobid: string) {
    super(ctx)
    this.path = `/project/${project}/job/show/${jobid}`
  }


  async jobGroup() {
    return await this.ctx.driver.findElement(Elems.jobGroup)
  }
  async jobGroupText() {
    const jobGroup = await this.jobGroup()
    return await jobGroup.getText()
  }
  async jobTitleLink(){
    return await this.ctx.driver.findElement(Elems.jobTitleLink)
  }
  async jobEditButton(){
    return await this.ctx.driver.findElement(Elems.jobEditButton)
  }
  async jobTags(){
    return await this.ctx.driver.findElements(Elems.jobTags)
  }
  async jobActionDropdown(){
    return await this.ctx.driver.findElement(Elems.jobActionDropdown)
  }
  async jobDescription(){
    return await this.ctx.driver.findElement(Elems.jobDescription)
  }
  async jobDescriptionText(){
    let jobDescription=await this.jobDescription()
    return await jobDescription.getText()
  }

  async jobTitleText(){
    let link = await this.jobTitleLink()
    return await link.getText()
  }
  async jobUuidText(){
    let uuidElem= await this.ctx.driver.findElement(Elems.jobUuidText)
    return await uuidElem.getText()
  }
  async jobDefinition(){
    return await this.ctx.driver.findElement(Elems.jobDefinition)
  }
  async jobDefinitionModal() {
    return await this.ctx.driver.findElement(Elems.jobDefinitionModal);
  }
  async waitJobDefinition(){
    await this.ctx.driver.wait(until.elementLocated(Elems.jobDefinition), 25000)
  }
  async jobDefModalScheduledDaysDiv() {
    return await this.ctx.driver.findElement(Elems.jobDefModalScheduledDaysDiv)
  }
  async jobDefModalScheduledMonthsDiv() {
    return await this.ctx.driver.findElement(Elems.jobDefModalScheduledMonthsDiv)
  }
  async jobDefModalScheduleEveryDaySelected() {
    return await this.ctx.driver.findElement(Elems.jobDefModalScheduleEveryDaySelected);
  }
  async waitDefinitionNotificationText(){
    await this.ctx.driver.wait(until.elementLocated(Elems.notificationDefinition), 25000)
  }
  async jobDefinitionNotificationText(){
    let data= await this.ctx.driver.findElement(Elems.notificationDefinition)
    return await data.getText()
  }
  async jobDefinitionNotificationToggle(){
    return await this.ctx.driver.findElement(Elems.notificationDefinitionToggle)
  }
  async waitDefinitionNodefilters(){
    await this.ctx.driver.wait(until.elementLocated(Elems.nodeFilterSection), 25000)
  }
  async jobDefinitionNodeFilterMatchedText(){
    let data= await this.ctx.driver.findElement(Elems.nodeFilterSectionMatchednodes)
    return await data.getText()
  }
  async jobDefinitionNodeThreadcountText(){
    let data= await this.ctx.driver.findElement(Elems.nodeFilterSectionThreadcount)
    return await data.getText()
  }
  async jobDefinitionNodeKeepgoingText(){
    let data= await this.ctx.driver.findElement(Elems.nodeFilterSectionNodeKeepgoing)
    return await data.getText()
  }
  async jobDefinitionNodeRankOrderAscendingText(){
    let data= await this.ctx.driver.findElement(Elems.nodeFilterSectionNodeRankOrderAscending)
    return await data.getText()
  }
  async jobDefinitionNodeSelectedByDefaultText(){
    let data= await this.ctx.driver.findElement(Elems.nodeFilterSectionNodeSelectedByDefault)
    return await data.getText()
  }
  async closeJobDefinitionModal(){
    let data= await this.ctx.driver.findElement(Elems.jobDefinitionModalCloseBtn)
    return await data.click()
  }
  async optionInputText(name: string){
    return await this.ctx.driver.findElement(By.css(`#optionSelect #_commandOptions input[type=text][name='extra.option.${name}']`))
  }
  async jobDefinitionOrchestratorText() {
    return this.ctx.driver.findElement(Elems.orchestratorText)
  }
  async jobDefinitionNotificationHttpRemoteUrlDetail(){
    return this.ctx.driver.findElement(Elems.notificationDefinitionDetailHttpRemoteUrl)

  }

  async findJobStepDefinition(){
    let stepPluginDefinition = await this.ctx.driver.findElement(Elems.jobStepDefinition)
    return stepPluginDefinition
  }
}
