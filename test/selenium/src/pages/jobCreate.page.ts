import {By, WebElement, WebElementPromise} from 'selenium-webdriver'

import {Page} from 'page'
import { Context } from 'context';

export const Elems= {
    jobNameInput  : By.css('form input[name="jobName"]'),
    groupPathInput  : By.css('form input[name="groupPath"]'),
    descriptionTextarea  : By.css('form textarea[name="description"]'),
    saveButton  : By.css('#Create'),
    errorAlert  : By.css('#error'),
    formValidationAlert: By.css('#page_job_edit > div.list-group-item > div.alert.alert-danger')
 
 }
 

export class JobCreatePage extends Page {
    path = '/resources/createProject'

    constructor(readonly ctx: Context, readonly project: string) {
        super(ctx)
        this.path = `/project/${project}/job/create`
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
    errorAlert():WebElementPromise{
        return this.ctx.driver.findElement(Elems.errorAlert)
    }
    formValidationAlert():WebElementPromise{
        return this.ctx.driver.findElement(Elems.formValidationAlert)
    }
}