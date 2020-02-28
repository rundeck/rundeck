import {By, WebElementPromise} from 'selenium-webdriver'

import {Page} from 'page'
import {Context} from 'context'

export const Elems = {
  modalOptions: By.css('#execDiv #exec_options_form #optionSelect'),
  runFormButton: By.css('#execDiv #exec_options_form #formbuttons #execFormRunButton'),
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

  async getRunJobNowLink() {
    return this.ctx.driver.findElement(Elems.runFormButton)

  }

  async getOptionWarningText() {
    let elem = await this.ctx.driver.findElement(Elems.optionValidationWarningText)
    return elem.getText()
  }
}
