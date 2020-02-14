import {By, WebElement, WebElementPromise} from 'selenium-webdriver'

import {Page} from '../page'
import { Context } from '../context';

export const Elems= {
  jobTitleLink: By.css('#jobInfo_ > span > a.text-primary')
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
}
