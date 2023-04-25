import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import {Context} from '@rundeck/testdeck/context'

export const Elems = {
    createButton: By.linkText("Add or Upload a Key"),
    deleteButton: By.linkText("Delete Selected Item"),
    deleteDropdown: By.css(".btn-group > .dropdown-toggle"),
    overwriteButton: By.linkText("Overwrite Key"),
    deleteConfirmationButton: By.css(".obs-storagedelete-select"),
    uploadPasswordField: By.id("uploadpasswordfield"),
    resourceNameField: By.id("uploadResourceName2"),
    uploadKeyTypeDropdown: By.name("uploadKeyType"),
    passwordKeyType: By.css('option[value="password"]'),
    saveButton: By.css(".btn-cta")
}

export class KeyStoragePage extends Page {
    path = '/menu/storage'

    constructor(readonly ctx: Context) {
        super(ctx)
    }

    async getCreateButton() {
        return this.ctx.driver.findElement(Elems.createButton)
    }

    async selectKey(keyName: string) {
        const xpathExpression = `//tr[contains(@class, 'action') and .//span[contains(text(), '${keyName}')]]/td/*[contains(@class, 'glyphicon')]`;
        await this.ctx.driver.wait(until.elementLocated(By.xpath(xpathExpression)), 10000);
        return this.ctx.driver.findElement(By.xpath(xpathExpression));
    }

    async getDeleteButton(){
        return this.ctx.driver.findElement(Elems.deleteButton)
    }

    async getDeleteDropdown(){
        return this.ctx.driver.findElement(Elems.deleteDropdown)
    }

    async getOverwriteButton(){
        return this.ctx.driver.findElement(Elems.overwriteButton)
    }

    async getDeleteConfirmButton(){
        return this.ctx.driver.findElement(Elems.deleteConfirmationButton)
    }

    async getUploadPasswordField() {
        await this.ctx.driver.wait(until.elementLocated(Elems.uploadPasswordField), 10000);
        return this.ctx.driver.findElement(Elems.uploadPasswordField)
    }

    async getResourceNameField() {
        return this.ctx.driver.findElement(Elems.resourceNameField)
    }

    async selectKeyType() {
        const dropdown = await this.ctx.driver.findElement(Elems.uploadKeyTypeDropdown)
        return dropdown.findElement(Elems.passwordKeyType);
    }

    async getSaveButton() {
        return this.ctx.driver.findElement(Elems.saveButton)
    }

    async getUploadTypeDropdown() {
        return this.ctx.driver.findElement(Elems.uploadKeyTypeDropdown)
    }
}