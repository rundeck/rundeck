"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProjectListPage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
var Elems;
(function (Elems) {
    Elems["drpProjectSelect"] = "//*[@id=\"projectSelect\"]";
    Elems["projectsCountCss"] = "#layoutBody span.h3.text-primary > span";
})(Elems || (exports.Elems = Elems = {}));
class ProjectListPage extends page_1.Page {
    constructor() {
        super(...arguments);
        this.path = '/menu/home';
    }
    async getProjectsCount() {
        return await this.ctx.driver.findElement(selenium_webdriver_1.By.css(Elems.projectsCountCss));
    }
}
exports.ProjectListPage = ProjectListPage;
//# sourceMappingURL=projectList.page.js.map