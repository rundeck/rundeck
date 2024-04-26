"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JobsListPage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
exports.Elems = {
    modalOptions: selenium_webdriver_1.By.css('#execDiv #exec_options_form #optionSelect'),
    searchModalFields: selenium_webdriver_1.By.css('#jobs_filters form #jobs_filters_content'),
    runFormButton: selenium_webdriver_1.By.css('#execDiv #exec_options_form #formbuttons #execFormRunButton'),
    jobSearchButton: selenium_webdriver_1.By.css('#subtitlebar  .btn[data-target="#jobs_filters"]'),
    jobSearchNameField: selenium_webdriver_1.By.css('#jobs_filters form input[name="jobFilter"]'),
    jobSearchGroupField: selenium_webdriver_1.By.css('#jobs_filters form input[name="groupPath"]'),
    jobSearchSubmitButton: selenium_webdriver_1.By.css('#jobs_filters form #jobs_filters_footer input[type="submit"][name="_action_jobs"]'),
    jobRowLinks: selenium_webdriver_1.By.css('#job_group_tree .jobname.job_list_row[data-job-id] > a[data-job-id]'),
    optionValidationWarningText: selenium_webdriver_1.By.css('#execDiv #exec_options_form #optionSelect #_commandOptions div.form-group.has-warning p.text-warning')
};
class JobsListPage extends page_1.Page {
    constructor(ctx, project) {
        super(ctx);
        this.ctx = ctx;
        this.project = project;
        this.path = '/';
        this.path = `/project/${project}/jobs`;
    }
    async getRunJobLink(uuid) {
        return this.ctx.driver.findElement(selenium_webdriver_1.By.css(`#job_group_tree a.act_execute_job[data-job-id="${uuid}"]`));
    }
    async searchJobName(name, group) {
        let jobSearchButton = await this.getJobSearchButton();
        await jobSearchButton.click();
        //wait for modal to become available
        await this.ctx.driver.wait(selenium_webdriver_1.until.elementLocated(exports.Elems.searchModalFields), 10000);
        //locate job name search
        if (name) {
            let field = await this.getJobSearchNameField();
            await field.sendKeys(name);
        }
        if (group) {
            let field = await this.getJobSearchGroupField();
            await field.sendKeys(group);
        }
        let jobSearchSubmitButton = await this.getJobSearchSubmitButton();
        await jobSearchSubmitButton.click();
        //wait for job list page reload
        await this.ctx.driver.wait(selenium_webdriver_1.until.urlContains('/jobs'), 10000);
        return this.ctx.driver.wait(selenium_webdriver_1.until.titleMatches(/.*(Jobs).*$/i), 10000);
    }
    async getJobSearchButton() {
        return this.ctx.driver.findElement(exports.Elems.jobSearchButton);
    }
    async getJobSearchSubmitButton() {
        return this.ctx.driver.findElement(exports.Elems.jobSearchSubmitButton);
    }
    async getJobSearchNameField() {
        return this.ctx.driver.findElement(exports.Elems.jobSearchNameField);
    }
    async getJobSearchGroupField() {
        return this.ctx.driver.findElement(exports.Elems.jobSearchGroupField);
    }
    async getJobsRowLinkElements() {
        return this.ctx.driver.findElements(exports.Elems.jobRowLinks);
    }
    async getRunJobNowLink() {
        return this.ctx.driver.findElement(exports.Elems.runFormButton);
    }
    async getOptionWarningText() {
        let elem = await this.ctx.driver.findElement(exports.Elems.optionValidationWarningText);
        return elem.getText();
    }
}
exports.JobsListPage = JobsListPage;
//# sourceMappingURL=jobsList.page.js.map