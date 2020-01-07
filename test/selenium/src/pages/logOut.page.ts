import {Context} from 'context'
import {Page} from 'page'
import {By} from 'selenium-webdriver'

export enum Elems{
    logoutMessageCss='#loginpage p.text-center.h4'
}
export class LogoutPage extends Page {
    path = '/user/logout'
    constructor(readonly ctx: Context) {
        super(ctx)
    }

    async logout(){
        await this.ctx.driver.get(this.ctx.urlFor(this.path))
    }
    async getLogoutMessage(){
        await this.ctx.driver.findElement(By.css(Elems.logoutMessageCss))
    }
}