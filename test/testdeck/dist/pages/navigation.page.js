"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NavigationPage = exports.Elems = void 0;
const selenium_webdriver_1 = require("selenium-webdriver");
const page_1 = require("../page");
var Elems;
(function (Elems) {
    Elems["drpProjectSelect"] = "//*[@id=\"projectSelect\"]";
    Elems["btnSideBarExpand"] = "/html/body/div/div[2]/div[1]/nav/div/div[1]/button";
    Elems["lnkDashboard"] = "//*[@id=\"nav-dashboard-link\"]";
    Elems["lnkJobs"] = "//*[@id=\"nav-jobs-link\"]";
    Elems["lnkNodes"] = "//*[@id=\"nav-nodes-link\"]";
    Elems["lnkCommands"] = "//*[@id=\"nav-commands-link\"]";
    Elems["lnkActivity"] = "//*[@id=\"nav-activity-link\"]";
    Elems["drpProjSettings"] = "//*[@id=\"projectAdmin\"]";
})(Elems || (exports.Elems = Elems = {}));
class NavigationPage extends page_1.Page {
    constructor() {
        super(...arguments);
        this.path = '/';
    }
    async toggleSidebarExpand() {
        const { ctx } = this;
        const btn = ctx.driver.findElement(selenium_webdriver_1.By.xpath(Elems.btnSideBarExpand));
        await btn.click();
    }
    async gotoProject(project) {
        const url = this.ctx.urlFor(`/project/${project}/home`);
        await this.ctx.driver.get(url);
    }
    visitDashBoard() {
        return this.clickBy(selenium_webdriver_1.By.xpath(Elems.lnkDashboard));
    }
    visitJobs() {
        return this.clickBy(selenium_webdriver_1.By.xpath(Elems.lnkJobs));
    }
    visitNodes() {
        return this.clickBy(selenium_webdriver_1.By.xpath(Elems.lnkNodes));
    }
    visitCommands() {
        return this.clickBy(selenium_webdriver_1.By.xpath(Elems.lnkCommands));
    }
    visitActivity() {
        return this.clickBy(selenium_webdriver_1.By.xpath(Elems.lnkActivity));
    }
    async visitSystemConfiguration() {
        const url = this.ctx.urlFor(`/menu/systemConfig`);
        await this.ctx.driver.get(url);
    }
}
exports.NavigationPage = NavigationPage;
//# sourceMappingURL=navigation.page.js.map