import {Context} from 'context'
import {Page} from 'page'
import {By, until} from 'selenium-webdriver'

export enum Elems {
    username= '//*[@id="login"]',
    password = '//*[@id="password"]',
    /** @todo This button could use an id */
    loginBtn = '//*[@id="btn-login"]',
}

export class LoginPage extends Page {
    path = '/'

    constructor(readonly ctx: Context) {
        super(ctx)
    }

    async get() {
        const {driver} = this.ctx
        await driver.get(this.ctx.urlFor('/'))
    }

    async sendLogin(username: string, password: string){
        await this.get()
        const {driver} = this.ctx

        // Fetches the elements concurrently
        const [usernameFld, passwordFld, loginBtn] = await Promise.all([
            driver.findElement(By.xpath(Elems.username)),
            driver.findElement(By.xpath(Elems.password)),
            driver.findElement(By.xpath(Elems.loginBtn)),
        ])

        // Fills in the fields concurrently
        await Promise.all([
            usernameFld.sendKeys(username),
            passwordFld.sendKeys(password),
        ])

        await loginBtn.click()
    }
    async login(username: string, password: string) {
        const {driver} = this.ctx
        await this.sendLogin(username, password)

        await driver.wait(until.titleMatches(/^((?!Login).)*$/i), 5000)
    }

    async badLogin(username: string, password: string) {
        const {driver} = this.ctx
        await this.sendLogin(username, password)

        await driver.wait(until.urlContains('/user/error'), 5000)
    }
}