import {By} from 'selenium-webdriver'

import {Page} from 'page'
import { Context } from 'context';

export enum Elems {
   
}

export class JobCreatePage extends Page {
    path = '/resources/createProject'

    constructor(readonly ctx: Context, readonly project: string) {
        super(ctx)
        this.path = `/project/${project}/job/create`
    }
}