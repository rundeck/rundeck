import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Context} from '@rundeck/testdeck/context'
import {Page} from '@rundeck/testdeck/page'

export const Elems = {
  abortButton: By.css('#runcontent .executionshow .execution-action-links span.btn.btn-danger[data-bind=\'click: killExecAction\']'),
  commandInputText: By.css('input#runFormExec'),
  filterNodesButton: By.css('#searchForm  .btn.btn-cta.btn-fill'),
  nodeFilterText: By.css('#schedJobNodeFilter'),
  runCommandButton: By.css('form > #runbox a.btn-cta.runbutton'),
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

  /**
   * Load adhoc commands page, run a basic command on a node filter, and return the link to the execution
   * @param command
   */
  async runBasicCommand(command: string, nodefilter: string) {
    const {driver} = this.ctx
    await this.get()
    await driver.wait(until.urlContains('/command/run'), 25000)
    const nodeFilter = await this.nodeFilterInput()
    await nodeFilter.sendKeys(nodefilter)

    const nodeFilterBtn = await this.filterNodesButton()
    await nodeFilterBtn.click()

    const commandInput = await this.commandInput()
    await commandInput.sendKeys(command)

    const commandButton = await this.runCommandButton()
    await commandButton.click()

    // find execution id and get link
    await driver.wait(until.elementLocated(Elems.runningExecutionState), 25000)
    return this.runningExecutionLink()
  }
}
