"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProjectCreatePage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
exports.Elems = {
    projectNameInput: selenium_webdriver_1.By.css('#createform form input[name="newproject"]'),
    labelInput: selenium_webdriver_1.By.css('#createform form input[name="label"]'),
    descriptionInput: selenium_webdriver_1.By.css('#createform form input[name="description"]'),
};
class ProjectCreatePage extends page_1.Page {
    constructor() {
        super(...arguments);
        this.path = '/resources/createProject';
    }
    async projectNameInput() {
        return await this.ctx.driver.findElement(exports.Elems.projectNameInput);
    }
    async labelInput() {
        return await this.ctx.driver.findElement(exports.Elems.labelInput);
    }
    async descriptionInput() {
        return await this.ctx.driver.findElement(exports.Elems.descriptionInput);
    }
}
exports.ProjectCreatePage = ProjectCreatePage;
//# sourceMappingURL=projectCreate.page.js.map