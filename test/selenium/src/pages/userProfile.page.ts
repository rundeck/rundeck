import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Context} from '@rundeck/testdeck/context'
import {Page} from '@rundeck/testdeck/page'

export const Elems = {
  languageLabel: By.css('#layoutBody div.form-inline label[for=language]'),
}

export class UserProfilePage extends Page {
  path = 'user/profile'
  lang = ''

  constructor(readonly ctx: Context, lang: string = '') {
    super(ctx)
    this.lang = lang
    this.path = `user/profile?lang=${this.lang}`
  }

  async languageLabel() {
    return await this.ctx.driver.findElement(Elems.languageLabel)
  }
}
