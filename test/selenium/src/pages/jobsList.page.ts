import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Page} from 'page'
import {Context} from 'context'

export const Elems = {
  modalOptions: By.css('#execDiv #exec_options_form #optionSelect'),
  searchModalFields: By.css('#jobs_filters form #jobs_filters_content'),
  runFormButton: By.css('#execDiv #exec_options_form #formbuttons #execFormRunButton'),
  jobSearchButton: By.css('#subtitlebar  .btn[data-target="#jobs_filters"]'),
  jobSearchNameField: By.css('#jobs_filters form input[name="jobFilter"]'),
  jobSearchGroupField: By.css('#jobs_filters form input[name="groupPath"]'),
  jobSearchSubmitButton: By.css('#jobs_filters form #jobs_filters_footer input[type="submit"][name="_action_jobs"]'),
  jobRowLinks: By.css('#job_group_tree .jobname.job_list_row[data-job-id] > a[data-job-id]'),
  optionValidationWarningText: By.css('#execDiv #exec_options_form #optionSelect #_commandOptions div.form-group.has-warning p.text-warning')
}

export class JobsListPage extends Page {
  path = '/'

  constructor(readonly ctx: Context, readonly project: string) {
    super(ctx)
    this.path = `/project/${project}/jobs`
  }

  async getRunJobLink(uuid: string) {
    return this.ctx.driver.findElement(By.css(`#job_group_tree a.act_execute_job[data-job-id="${uuid}"]`))
  }
  async searchJobName(name: string, group:string){

    let jobSearchButton = await this.getJobSearchButton()
    await jobSearchButton.click()
    //wait for modal to become available
    await this.ctx.driver.wait(until.elementLocated(Elems.searchModalFields),10000)

    //locate job name search

    if(name){

      let field = await this.getJobSearchNameField()
      await field.sendKeys(name)
    }
    if(group){

      let field = await this.getJobSearchGroupField()
      await field.sendKeys(group)
    }

    let jobSearchSubmitButton = await this.getJobSearchSubmitButton()
    await jobSearchSubmitButton.click()

    //wait for job list page reload
    await this.ctx.driver.wait(until.urlContains('/jobs'), 10000)
    return  this.ctx.driver.wait(until.titleMatches(/.*(Jobs).*$/i), 10000)
  }

  async getJobSearchButton() {
    return this.ctx.driver.findElement(Elems.jobSearchButton)
  }
  async getJobSearchSubmitButton() {
    return this.ctx.driver.findElement(Elems.jobSearchSubmitButton)
  }

  async getJobSearchNameField() {
    return this.ctx.driver.findElement(Elems.jobSearchNameField)
  }
  async getJobSearchGroupField() {
    return this.ctx.driver.findElement(Elems.jobSearchGroupField)
  }

  async getJobsRowLinkElements() {
    return this.ctx.driver.findElements(Elems.jobRowLinks)
  }
  async getRunJobNowLink() {
    return this.ctx.driver.findElement(Elems.runFormButton)
  }

  async getOptionWarningText() {
    let elem = await this.ctx.driver.findElement(Elems.optionValidationWarningText)
    return elem.getText()
  }
}
