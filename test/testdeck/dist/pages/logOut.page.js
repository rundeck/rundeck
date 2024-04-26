"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LogoutPage = exports.Elems = void 0;
const page_1 = require("../page");
const selenium_webdriver_1 = require("selenium-webdriver");
var Elems;
(function (Elems) {
    Elems["logoutMessageCss"] = "#loginpage p.text-center.h4";
})(Elems || (exports.Elems = Elems = {}));
class LogoutPage extends page_1.Page {
    constructor(ctx) {
        super(ctx);
        this.ctx = ctx;
        this.path = '/user/logout';
    }
    async logout() {
        await this.ctx.driver.get(this.ctx.urlFor(this.path));
    }
    async getLogoutMessage() {
        await this.ctx.driver.findElement(selenium_webdriver_1.By.css(Elems.logoutMessageCss));
    }
}
exports.LogoutPage = LogoutPage;
//# sourceMappingURL=logOut.page.js.map