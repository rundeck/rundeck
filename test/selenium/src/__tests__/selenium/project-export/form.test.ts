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

import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {ProjectExportPage,Checkboxes,Radios} from 'pages/projectExport.page'
import {LoginPage} from 'pages/login.page'

import {until} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let projectExportPage: ProjectExportPage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    projectExportPage = new ProjectExportPage(ctx, 'SeleniumBasic')
    await loginPage.login('admin', 'admin')
})

describe('projectExport', () => {

  it('form radio inputs have proper name', async () => {
    await projectExportPage.get()
    await ctx.driver.wait(until.urlContains('/export'), 5000)

    let radioElems={}
    for(let i=0;i<Radios.length;i++){
        await projectExportPage.getRadio(Radios[i]).then(radio=>radioElems[Radios[i]]=radio)
        let val = await radioElems[Radios[i]].getAttribute('name')
        expect(val).toBe('stripJobRef')
    }
  })
  it('form radio inputs have labels', async () => {
    await projectExportPage.get()
    await ctx.driver.wait(until.urlContains('/export'), 5000)

    let radioLabels=        await Promise.all(Radios.map(radio=>projectExportPage.getLabel(radio)))
    expect(radioLabels.length).toBe(Radios.length)
    expect(radioLabels).not.toContain(null)
  })
  it('form checkboxes are checked by default', async () => {
    await projectExportPage.get()
    await ctx.driver.wait(until.urlContains('/export'), 5000)

    let elems = await Promise.all(Checkboxes.map((name)=>projectExportPage.getCheckbox(name)))
    let checked = await Promise.all(elems.map((elem)=>elem.getAttribute('checked')))
    expect(checked.length).toBe(Checkboxes.length)
    expect(checked).toContain('true')
    expect(checked).not.toContain('false')
    expect(checked).not.toContain(null)

  })
  it('form checkbox labels work', async () => {
    await projectExportPage.get()
    await ctx.driver.wait(until.urlContains('/export'), 5000)

    let elems = await Promise.all(Checkboxes.map((name)=>projectExportPage.getCheckbox(name)))
    expect(elems).not.toContain(null)

    let labels = await Promise.all(Checkboxes.map((name)=>projectExportPage.getLabel(name)))
    expect(labels).not.toContain(null)
    
    let checked = await Promise.all(elems.map((elem)=>elem.getAttribute('checked')))
    expect(checked.length).toBe(Checkboxes.length)
    expect(checked).toContain('true')
    expect(checked).not.toContain('false')
    expect(checked).not.toContain(null)

    await sleep(3000)

    //click each label
    await Promise.all(labels.map(label=>label.click()))
    let checked2 = await Promise.all(elems.map((elem)=>elem.getAttribute('checked')))
    expect(checked2.length).toBe(Checkboxes.length)
    expect(checked2).not.toContain('true')
    expect(checked2).not.toContain('false')
    expect(checked2).toContain(null)


  })
})
