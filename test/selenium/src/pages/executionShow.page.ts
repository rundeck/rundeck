import {By, WebElementPromise} from 'selenium-webdriver'

import {Context} from '@rundeck/testdeck/context'
import {Page} from '@rundeck/testdeck/page'

export const Elems = {
  abortButton: By.css('#subtitlebar section.execution-action-links span.btn-danger[data-bind=\'click: killExecAction\''),
  executionStateDisplay: By.css('#subtitlebar summary > span.execution-summary-status > span.execstate.execstatedisplay.overall'),
}

export class ExecutionShowPage extends Page {
  path = '/project/project/execution/id'

  constructor(readonly ctx: Context, readonly project: string, readonly eid: string) {
    super(ctx)
    this.path = `/project/${project}/execution/${eid}`
  }

  async executionStateDisplay() {
    return await this.ctx.driver.findElement(Elems.executionStateDisplay)
  }

  async abortButton() {
    return await this.ctx.driver.findElement(Elems.abortButton)
  }
}
