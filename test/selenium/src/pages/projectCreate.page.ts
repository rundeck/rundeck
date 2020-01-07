import {By} from 'selenium-webdriver'

import {Page} from 'page'

export const Elems= {
   projectNameInput  : By.css('#createform form input[name="newproject"]'),
   labelInput  : By.css('#createform form input[name="label"]'),
   descriptionInput  : By.css('#createform form input[name="description"]'),
   

}

export class ProjectCreatePage extends Page {
    path = '/resources/createProject'

    async projectNameInput(){
        await this.ctx.driver.findElement(Elems.projectNameInput)
    }
    async labelInput(){
        await this.ctx.driver.findElement(Elems.labelInput)
    }
    async descriptionInput(){
        await this.ctx.driver.findElement(Elems.descriptionInput)
    }
}