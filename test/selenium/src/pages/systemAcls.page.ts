import { By, WebElementPromise } from 'selenium-webdriver'

import { Page } from '@rundeck/testdeck/page'
import { Context } from '@rundeck/testdeck/context'

export const Elems = {
    uploadBtn: By.css('#storage_acl_upload_btn'),
    uploadSubmitBtn: By.css('button#aclStorageUpload_submit_btn'),
    uploadModal: By.css('#aclStorageUpload'),
    uploadFileField: By.css('#aclStorageUploadForm input#aclStorageUpload_input_file'),
    uploadNameField: By.css('#aclStorageUploadForm input#aclStorageUpload_input_name'),
    overwriteCheckbox: By.css('#aclStorageUploadForm input#acl_upload_overwrite'),
    alertDanger: By.css('.alert.alert-danger'),
    uploadedPolicyValidationTitle: By.css('#uploadedPolicyValidation .alert h4'),
    storedPoliciesHeader: By.css('#storedPolicies_header'),
    storedPoliciesCard: By.css('#storedPolicies'),
    storedPoliciesCardList: By.css('#storedPolicies_list'),
    deleteModal: By.css('#deleteStorageAclPolicy'),
}

export class SystemAclsPage extends Page {
    path = '/'

    constructor(readonly ctx: Context) {
        super(ctx)
        this.path = `/menu/acls`
    }


    async getUploadModal() {
        return this.ctx.driver.findElement(Elems.uploadModal)
    }
    async getUploadFileField() {
        return this.ctx.driver.findElement(Elems.uploadFileField)
    }
    async getUploadNameField() {
        return this.ctx.driver.findElement(Elems.uploadNameField)
    }
    async getOverwriteCheckbox() {
        return this.ctx.driver.findElement(Elems.overwriteCheckbox)
    }

    async getUploadBtn() {
        return this.ctx.driver.findElement(Elems.uploadBtn)
    }

    async getUploadSubmitBtn() {
        return this.ctx.driver.findElement(Elems.uploadSubmitBtn)
    }
    async getDangerAlert() {
        return this.ctx.driver.findElement(Elems.alertDanger)
    }
    async getUploadedPolicyValidationTitle() {
        return this.ctx.driver.findElement(Elems.uploadedPolicyValidationTitle)
    }
    async getStoredPoliciesHeader() {
        return this.ctx.driver.findElement(Elems.storedPoliciesHeader)
    }

    async getStoredPoliciesCard() {
        return this.ctx.driver.findElement(Elems.storedPoliciesCard)
    }

    async getStoredPoliciesCardList() {
        return this.ctx.driver.findElement(Elems.storedPoliciesCardList)
    }
    async getDeleteModal() {
        return this.ctx.driver.findElement(Elems.deleteModal)
    }
    async getActionDropdown(index: number) {
        let list = await this.getStoredPoliciesCardList()
        let div = list.findElement(By.css("div:nth-of-type(" + (index + 1) + ")"))
        return div.findElement(By.css(" a[data-toggle='dropdown']"))
    }

    async clickUploadBtn() {
        const btn = await this.getUploadBtn()
        await btn.click()
    }
}
