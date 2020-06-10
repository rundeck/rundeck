import {By, until, WebElementPromise} from 'selenium-webdriver'

import {Page} from '@rundeck/testdeck/page'
import {Context} from '@rundeck/testdeck/context'

export const Elems = {
  
}

export class EditNodesPage extends Page {
    path = '/'

    constructor(readonly ctx: Context, readonly project: string) {
        super(ctx)
        this.path = `/project/${project}/nodes/sources`
    }
}