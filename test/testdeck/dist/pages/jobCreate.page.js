"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JobCreatePage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
exports.Elems = {
    jobNameInput: selenium_webdriver_1.By.css('form input[name="jobName"]'),
    groupPathInput: selenium_webdriver_1.By.css('form input[name="groupPath"]'),
    descriptionTextarea: selenium_webdriver_1.By.css('form textarea[name="description"]'),
    saveButton: selenium_webdriver_1.By.css('#Create'),
    editSaveButton: selenium_webdriver_1.By.css('#editForm div.card-footer input.btn.btn-cta[type=submit][value=Save]'),
    errorAlert: selenium_webdriver_1.By.css('#error'),
    formValidationAlert: selenium_webdriver_1.By.css('#page_job_edit > div.list-group-item > div.alert.alert-danger'),
    tabWorkflow: selenium_webdriver_1.By.css('#job_edit_tabs > li > a[href=\'#tab_workflow\']'),
    addNewWfStepCommand: selenium_webdriver_1.By.css('#wfnewtypes #addnodestep > div > a.add_node_step_type[data-node-step-type=command]'),
    wfStepCommandRemoteText: selenium_webdriver_1.By.css('#adhocRemoteStringField'),
    wfStep0SaveButton: selenium_webdriver_1.By.css('#wfli_0 div.wfitemEditForm div._wfiedit > div.floatr > span.btn.btn-cta.btn-sm'),
    wfstep0vis: selenium_webdriver_1.By.css('#wfivis_0'),
    optionNewButton: selenium_webdriver_1.By.css('#optnewbutton > span'),
    option0EditForm: selenium_webdriver_1.By.css('#optvis_0 > div.optEditForm'),
    option0NameInput: selenium_webdriver_1.By.css('#optvis_0 > div.optEditForm input[type=text][name=name]'),
    optionFormSaveButton: selenium_webdriver_1.By.css('#optvis_0 > div.optEditForm  div.floatr > span.btn.btn-cta.btn-sm'),
    option0li: selenium_webdriver_1.By.css('#optli_0')
};
class JobCreatePage extends page_1.Page {
    constructor(ctx, project) {
        super(ctx);
        this.ctx = ctx;
        this.project = project;
        this.path = '/resources/createProject';
        this.projectName = '';
        this.projectName = project;
        this.path = `/project/${project}/job/create`;
    }
    editPagePath(jobId) {
        return `/project/${this.projectName}/job/edit/${jobId}`;
    }
    async getEditPage(jobId) {
        const { driver } = this.ctx;
        await driver.get(this.ctx.urlFor(this.editPagePath(jobId)));
    }
    async jobNameInput() {
        return await this.ctx.driver.findElement(exports.Elems.jobNameInput);
    }
    async groupPathInput() {
        return await this.ctx.driver.findElement(exports.Elems.groupPathInput);
    }
    async descriptionTextarea() {
        return await this.ctx.driver.findElement(exports.Elems.descriptionTextarea);
    }
    saveButton() {
        return this.ctx.driver.findElement(exports.Elems.saveButton);
    }
    async editSaveButton() {
        return this.ctx.driver.findElement(exports.Elems.editSaveButton);
    }
    errorAlert() {
        return this.ctx.driver.findElement(exports.Elems.errorAlert);
    }
    async tabWorkflow() {
        return await this.ctx.driver.findElement(exports.Elems.tabWorkflow);
    }
    async addNewWfStepCommand() {
        return await this.ctx.driver.findElement(exports.Elems.addNewWfStepCommand);
    }
    async waitWfStepCommandRemoteText() {
        await this.ctx.driver.wait(selenium_webdriver_1.until.elementLocated(exports.Elems.wfStepCommandRemoteText), 15000);
    }
    async wfStepCommandRemoteText() {
        return await this.ctx.driver.findElement(exports.Elems.wfStepCommandRemoteText);
    }
    async wfStep0SaveButton() {
        return await this.ctx.driver.findElement(exports.Elems.wfStep0SaveButton);
    }
    async wfstep0vis() {
        return await this.ctx.driver.findElement(exports.Elems.wfstep0vis);
    }
    async waitWfstep0vis() {
        await this.ctx.driver.wait(selenium_webdriver_1.until.elementLocated(exports.Elems.wfstep0vis), 15000);
    }
    async optionNewButton() {
        return await this.ctx.driver.findElement(exports.Elems.optionNewButton);
    }
    async waitoption0EditForm() {
        return this.ctx.driver.wait(selenium_webdriver_1.until.elementLocated(exports.Elems.option0EditForm), 15000);
    }
    async option0NameInput() {
        return await this.ctx.driver.findElement(exports.Elems.option0NameInput);
    }
    async optionFormSaveButton() {
        return await this.ctx.driver.findElement(exports.Elems.optionFormSaveButton);
    }
    async waitOption0li() {
        return this.ctx.driver.wait(selenium_webdriver_1.until.elementLocated(exports.Elems.option0li), 15000);
    }
    formValidationAlert() {
        return this.ctx.driver.findElement(exports.Elems.formValidationAlert);
    }
}
exports.JobCreatePage = JobCreatePage;
//# sourceMappingURL=jobCreate.page.js.map