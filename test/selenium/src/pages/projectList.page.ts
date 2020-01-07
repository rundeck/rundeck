import {By, WebElementPromise} from 'selenium-webdriver'

import {Page} from 'page'
import { Context } from 'context';
export enum Elems {
    drpProjectSelect = '//*[@id="projectSelect"]',
    projectsCountCss= '#layoutBody span.h3.text-primary > span'
}

export class ProjectListPage extends Page {
    path = '/menu/home'

    async getProjectsCount(){
        return await this.ctx.driver.findElement(By.css(Elems.projectsCountCss))
    }
}