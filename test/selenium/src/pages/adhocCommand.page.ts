import {By, WebElementPromise} from 'selenium-webdriver'

import {Context} from '@rundeck/testdeck/context'
import {Page} from '@rundeck/testdeck/page'

export const Elems = {
  abortButton: By.css('#runcontent .executionshow .execution-action-links span.btn.btn-danger[data-bind=\'click: killExecAction\']'),
  commandInputText: By.css('input#runFormExec'),
  filterNodesButton: By.css('#searchForm  a.btn.btn-primary.btn-fill'),
  nodeFilterText: By.css('#schedJobNodeFilter'),
  runCommandButton: By.css('#runbox > form a.btn-success.runbutton'),
  runningContent: By.css('#runcontent'),
  runningExecutionLink: By.css('#runcontent .executionshow .execution-action-links a'),
  runningExecutionState: By.css('#runcontent .executionshow .execution-action-links a .execstate[data-execstate]'),
}

export class AdhocCommandPage extends Page {
  path = '/menu/commands'

  constructor(readonly ctx: Context, readonly project: string) {
    super(ctx)
    this.path = `/project/${project}/command/run`
  }

  async nodeFilterInput() {
    return await this.ctx.driver.findElement(Elems.nodeFilterText)
  }

  async filterNodesButton() {
    return await this.ctx.driver.findElement(Elems.filterNodesButton)
  }

  async commandInput() {
    return await this.ctx.driver.findElement(Elems.commandInputText)
  }

  async runCommandButton() {
    return await this.ctx.driver.findElement(Elems.runCommandButton)
  }
  async runningExecutionLink() {
    const state = await this.ctx.driver.findElement(Elems.runningExecutionState)
    return state.findElement(By.xpath('./..'))
  }
  async runningExecutionState() {
    return await this.ctx.driver.findElement(Elems.runningExecutionState)
  }
  async abortButton() {
    return await this.ctx.driver.findElement(Elems.abortButton)
  }
}
