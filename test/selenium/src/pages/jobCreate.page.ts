import {By} from 'selenium-webdriver'

import {Page} from 'page'
import { Context } from 'context';

export const Elems= {
    jobNameInput  : By.css('form input[name="jobName"]'),
    groupPathInput  : By.css('form input[name="groupPath"]'),
    descriptionTextarea  : By.css('form textarea[name="description"]'),
 
 }
 

export class JobCreatePage extends Page {
    path = '/resources/createProject'

    constructor(readonly ctx: Context, readonly project: string) {
        super(ctx)
        this.path = `/project/${project}/job/create`
    }

    async jobNameInput(){
        await this.ctx.driver.findElement(Elems.jobNameInput)
    }
    async groupPathInput(){
        await this.ctx.driver.findElement(Elems.groupPathInput)
    }
    async descriptionTextarea(){
        await this.ctx.driver.findElement(Elems.descriptionTextarea)
    }
}