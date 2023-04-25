import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import {Context} from '@rundeck/testdeck/context'

export const Elems = {
    modalOptions: By.css('#execDiv #exec_options_form #optionSelect'),
    searchModalFields: By.css('#jobs_filters form #jobs_filters_content'),
    runFormButton: By.css('#execDiv #exec_options_form #formbuttons #execFormRunButton'),
    jobSearchButton: By.css('#subtitlebar  .btn[data-target="#jobs_filters"]'),
    jobSearchNameField: By.css('#jobs_filters form input[name="jobFilter"]'),
    jobSearchGroupField: By.css('#jobs_filters form input[name="groupPath"]'),
    jobSearchSubmitButton: By.css('#jobs_filters form #jobs_filters_footer input[type="submit"][name="_action_jobs"]'),
    jobRowLinks: By.css('#job_group_tree .jobname.job_list_row[data-job-id] > a[data-job-id]'),
    optionValidationWarningText: By.css('#execDiv #exec_options_form #optionSelect #_commandOptions div.form-group.has-warning p.text-warning')
}

export class KeyStoragePage extends Page {
    path = '/menu/storage'

    constructor(readonly ctx: Context) {
        super(ctx)
    }

    async getCreateButton() {
        return this.ctx.driver.findElement(By.css(".keySelector-button-group > .btn-cta"))
    }

    async selectKey(keyName: string) {
        const xpathExpression = `//tr[contains(@class, 'action') and .//span[contains(text(), '${keyName}')]]/td/*[contains(@class, 'glyphicon')]`;
        await this.ctx.driver.wait(until.elementLocated(By.xpath(xpathExpression)), 10000);
        return this.ctx.driver.findElement(By.xpath(xpathExpression));
    }

    async getDeleteButton(){
        return this.ctx.driver.findElement(By.css(".btn-danger"))
    }

    async getOverwriteButton(){
        return this.ctx.driver.findElement(By.css(".btn-warning"))
    }

    async getDeleteConfirmButton(){
        return this.ctx.driver.findElement(By.css(".obs-storagedelete-select"))
    }

    async getUploadPasswordField() {
        await this.ctx.driver.wait(until.elementLocated(By.id("uploadpasswordfield")), 10000);
        return this.ctx.driver.findElement(By.id("uploadpasswordfield"))
    }

    async getResourceNameField() {
        return this.ctx.driver.findElement(By.id("uploadResourceName2"))
    }

    async selectKeyType() {
        const dropdown = await this.ctx.driver.findElement(By.name("uploadKeyType"))
        return dropdown.findElement(By.css('option[value="password"]'));
    }

    async getSaveButton() {
        return this.ctx.driver.findElement(By.css(".text-right > .btn-cta"))
    }

    async getUploadTypeDropdown() {
        return this.ctx.driver.findElement(By.name("uploadKeyType"))
    }
}