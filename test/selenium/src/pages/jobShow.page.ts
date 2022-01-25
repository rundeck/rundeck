import {By, until} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import { Context } from '@rundeck/testdeck/context'

export const Elems = {
  jobTitleLink: By.css('#jobInfo_ > span > a.job-header-link'),
  jobUuidText: By.css('#subtitlebar.job-page > div > div > section > small.uuid'),
  jobDescription: By.css('#subtitlebar.job-page > div > div > div.jobInfoSection > section > span.h5'),
  optionInput: By.css('#8f95c8d5_seleniumOption1'),
  jobDefinition  : By.css('a[href=\'#job-definition-modal\']'),
  jobDefinitionModal  : By.css('#job-definition-modal'),
  jobDefinitionModalCloseBtn  : By.css('#job-definition-modal .modal-footer button[data-dismiss="modal"]'),
  notificationDefinition: By.css('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx'),
  nodeFilterSection: By.css('#detailtable.tab-pane  tr#exec_detail_nodes '),
  nodeFilterSectionMatchednodes: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__matchednodes'),
  nodeFilterSectionThreadcount: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__threadcount'),
  nodeFilterSectionNodeKeepgoing: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeKeepgoing'),
  nodeFilterSectionNodeRankOrderAscending: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeRankOrderAscending'),
  nodeFilterSectionNodeSelectedByDefault: By.css('#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__nodeSelectedByDefault'),
  orchestratorText: By.css('#detailtable.tab-pane  tr#exec_detail_orchestrator  #exec_detail__orchestrator summary'),
  jobEditButton: By.xpath('//*[@id="subtitlebar"]/div/div[2]/div/div/ul/li[1]/a'),
  jobActionDropdown: By.css('.job-action-button > .btn-group > a.dropdown-toggle'),
}

export class JobShowPage extends Page {
  path = '/placeholder'

  constructor(readonly ctx: Context, readonly project: string, readonly jobid: string) {
    super(ctx)
    this.path = `/project/${project}/job/show/${jobid}`
  }


  async jobTitleLink(){
    return await this.ctx.driver.findElement(Elems.jobTitleLink)
  }
  async jobEditButton(){
    return await this.ctx.driver.findElement(Elems.jobEditButton)
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
  async waitJobDefinition(){
    await this.ctx.driver.wait(until.elementLocated(Elems.jobDefinition), 25000)
  }

  async waitDefinitionNotificationText(){
    await this.ctx.driver.wait(until.elementLocated(Elems.notificationDefinition), 25000)
  }
  async jobDefinitionNotificationText(){
    let data= await this.ctx.driver.findElement(Elems.notificationDefinition)
    return await data.getText()
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
}
