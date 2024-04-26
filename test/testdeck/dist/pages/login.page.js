"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.LoginPage = exports.Elems = void 0;
const page_1 = require("../page");
const selenium_webdriver_1 = require("selenium-webdriver");
var Elems;
(function (Elems) {
    Elems["username"] = "//*[@id=\"login\"]";
    Elems["password"] = "//*[@id=\"password\"]";
    /** @todo This button could use an id */
    Elems["loginBtn"] = "//*[@id=\"btn-login\"]";
})(Elems || (exports.Elems = Elems = {}));
class LoginPage extends page_1.Page {
    constructor(ctx) {
        super(ctx);
        this.ctx = ctx;
        this.path = '/';
    }
    async get() {
        const { driver } = this.ctx;
        await driver.get(this.ctx.urlFor('/'));
    }
    async sendLogin(username, password) {
        await this.get();
        const { driver } = this.ctx;
        // Fetches the elements concurrently
        const [usernameFld, passwordFld, loginBtn] = await Promise.all([
            driver.findElement(selenium_webdriver_1.By.xpath(Elems.username)),
            driver.findElement(selenium_webdriver_1.By.xpath(Elems.password)),
            driver.findElement(selenium_webdriver_1.By.xpath(Elems.loginBtn)),
        ]);
        // Fills in the fields concurrently
        await Promise.all([
            usernameFld.sendKeys(username),
            passwordFld.sendKeys(password),
        ]);
        await loginBtn.click();
    }
    async login(username, password) {
        const { driver } = this.ctx;
        await this.sendLogin(username, password);
        await driver.wait(selenium_webdriver_1.until.titleMatches(/^((?!Login).)*$/i), 5000);
    }
    async badLogin(username, password) {
        const { driver } = this.ctx;
        await this.sendLogin(username, password);
        await driver.wait(selenium_webdriver_1.until.urlContains('/user/error'), 5000);
    }
}
exports.LoginPage = LoginPage;
//# sourceMappingURL=login.page.js.map