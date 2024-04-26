"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JobShowPage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
exports.Elems = {
    jobTitleLink: selenium_webdriver_1.By.css('#jobInfo_ > span > a.text-primary'),
    jobUuidText: selenium_webdriver_1.By.css('#subtitlebar.job-page > div > div > section > small.uuid'),
    jobDescription: selenium_webdriver_1.By.css('#subtitlebar.job-page > div > div > div.jobInfoSection > section > span.h5'),
    optionInput: selenium_webdriver_1.By.css('#8f95c8d5_seleniumOption1')
};
class JobShowPage extends page_1.Page {
    constructor(ctx, project, jobid) {
        super(ctx);
        this.ctx = ctx;
        this.project = project;
        this.jobid = jobid;
        this.path = '/placeholder';
        this.path = `/project/${project}/job/show/${jobid}`;
    }
    async jobTitleLink() {
        return await this.ctx.driver.findElement(exports.Elems.jobTitleLink);
    }
    async jobDescription() {
        return await this.ctx.driver.findElement(exports.Elems.jobDescription);
    }
    async jobDescriptionText() {
        let jobDescription = await this.jobDescription();
        return await jobDescription.getText();
    }
    async jobTitleText() {
        let link = await this.jobTitleLink();
        return await link.getText();
    }
    async jobUuidText() {
        let uuidElem = await this.ctx.driver.findElement(exports.Elems.jobUuidText);
        return await uuidElem.getText();
    }
    async optionInputText(name) {
        return await this.ctx.driver.findElement(selenium_webdriver_1.By.css(`#optionSelect #_commandOptions input[type=text][name='extra.option.${name}']`));
    }
}
exports.JobShowPage = JobShowPage;
//# sourceMappingURL=jobShow.page.js.map