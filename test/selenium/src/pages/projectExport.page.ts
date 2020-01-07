/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {By, WebElementPromise} from 'selenium-webdriver'

import {Page} from 'page'
import { Context } from 'context';

export enum Elems {

    /** @todo This button could use an id */
    submitBtn = '//button[@type="submit"]',
}
export const Checkboxes=[
    'exportAll',
    'exportJobs',
    'exportExecutions',
    'exportConfigs',
    'exportReadmes',
    'exportAcls',
    'exportScm',
    // 'exportWebhooks',
    // 'whkIncludeAuthTokens'
]

export const Radios=[
    'dontStrip',
    'stripName',
    'stripUuid'
]

export class ProjectExportPage extends Page {
  path = '/'

  constructor(readonly ctx: Context, readonly project: string) {
    super(ctx)
    this.path = `/project/${project}/export`
  }

  async getLabel( name:string){
    return await this.ctx.driver.findElement(By.xpath(`//label[@for="${name}"]`))
  }
  async getCheckbox( name:string){
    return await this.ctx.driver.findElement(By.xpath(`//input[@type="checkbox"][@name="${name}"]`))
  }
  async getRadio( name:string){
    return await this.ctx.driver.findElement(By.xpath(`//input[@type="radio"][@id="${name}"]`))
  }
}
