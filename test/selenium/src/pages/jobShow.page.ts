import {By, WebElement, WebElementPromise} from 'selenium-webdriver'

import {Page} from '../page'
import { Context } from '../context';

export const Elems= {
  jobTitleLink: By.css('#jobInfo_ > span > a.text-primary'),
  jobUuidText: By.css('#subtitlebar.job-page > div > div > section > small.uuid'),
  jobDescription: By.css('#subtitlebar.job-page > div > div > div.jobInfoSection > section > span.h5'),
  optionInput: By.css('#8f95c8d5_seleniumOption1')
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
  async optionInputText(name: string){
      return await this.ctx.driver.findElement(By.css(`#optionSelect #_commandOptions input[type=text][name='extra.option.${name}']`))
  }
}
