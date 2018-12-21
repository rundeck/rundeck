import {By} from 'selenium-webdriver'

import {Page} from 'page'

export enum Elems {
    drpProjectSelect = '//*[@id="projectSelect"]',
    btnSideBarExpand = '/html/body/div/div[2]/div[1]/nav/div/div[1]/button',
    lnkDashboard = '//*[@id="nav-dashboard-link"]',
    lnkJobs = '//*[@id="nav-jobs-link"]',
    lnkNodes = '//*[@id="nav-nodes-link"]',
    lnkCommands = '//*[@id="nav-commands-link"]',
    lnkActivity = '//*[@id="nav-activity-link"]',
    drpProjSettings = '//*[@id="projectAdmin"]',
}

export class NavigationPage extends Page {
    path = '/'

    async toggleSidebarExpand() {
        const {ctx} = this
        const btn = ctx.driver.findElement(By.xpath(Elems.btnSideBarExpand))
        await btn.click()
    }

    async gotoProject(project: string) {
        const url = this.ctx.urlFor(`/project/${project}/home`)
        await this.ctx.driver.get(url)
    }

    visitDashBoard() {
        return this.clickBy(By.xpath(Elems.lnkDashboard))
    }

    visitJobs() {
        return this.clickBy(By.xpath(Elems.lnkJobs))
    }

    visitNodes() {
        return this.clickBy(By.xpath(Elems.lnkNodes))
    }

    visitCommands() {
        return this.clickBy(By.xpath(Elems.lnkCommands))
    }

    visitActivity() {
        return this.clickBy(By.xpath(Elems.lnkActivity))
    }

    async visitSystemConfiguration() {
        const url = this.ctx.urlFor(`/menu/systemConfig`)
        await this.ctx.driver.get(url)
    }

}