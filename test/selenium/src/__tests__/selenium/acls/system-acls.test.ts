import { Context } from '@rundeck/testdeck/context'
import { CreateContext } from '@rundeck/testdeck/test/selenium'
import { SystemAclsPage, Elems } from 'pages/systemAcls.page'
import { LoginPage } from 'pages/login.page'
import { mkdtemp, mkdtempSync, writeFile, writeFileSync } from 'fs'

import { until, By } from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'
import * as util from 'util'
import * as Path from 'path'
let Mkdir = util.promisify(mkdtemp)
let Writefile = util.promisify(writeFile)

// We will initialize and cleanup in the before/after methods
let ctx = CreateContext({ projects: ['SeleniumBasic'] })
let loginPage: LoginPage
let systemAclsPage: SystemAclsPage

beforeAll(async () => {
    loginPage = new LoginPage(ctx)
    systemAclsPage = new SystemAclsPage(ctx)
    await loginPage.login('admin', 'admin')
})

describe('systemAcls', () => {
    it('upload requires file input', async () => {
        await systemAclsPage.get()

        let uploadBtn = await systemAclsPage.getUploadBtn()
        await ctx.driver.wait(until.elementIsVisible(uploadBtn))

        //click
        await systemAclsPage.clickUploadBtn()

        //ensure modal is visible
        let uploadModal = await systemAclsPage.getUploadModal()
        await ctx.driver.wait(until.elementIsVisible(uploadModal))

        let uploadSubmitBtn = await systemAclsPage.getUploadSubmitBtn()
        await uploadSubmitBtn.click()

        //expect file and name field have errors

        let uploadFileField = await systemAclsPage.getUploadFileField()
        let uploadFileParent = await uploadFileField.findElement(By.xpath("./.."))
        let fileHelp = await uploadFileParent.findElement(By.css("span.help-block"))
        let filehelpText = await fileHelp.getText()
        expect(filehelpText).toEqual("File is required")

        let uploadNameField = await systemAclsPage.getUploadNameField()
        let uploadNameParent = await uploadNameField.findElement(By.xpath("./.."))
        let nameHelp = await uploadNameParent.findElements(By.css("span.help-block"))
        expect(nameHelp.length).toEqual(2)
        let namehelpText1 = await nameHelp[0].getText()
        expect(namehelpText1).toEqual("The policy name without file extension, can contain the characters: a-zA-Z0-9,.+_-")
        let namehelpText2 = await nameHelp[1].getText()
        expect(namehelpText2).toEqual("Name is required")

        //enter valid name, expect no "required" text
        await uploadNameField.sendKeys('some-file-name')

        await uploadSubmitBtn.click()
        nameHelp = await uploadNameParent.findElements(By.css("span.help-block"))
        expect(nameHelp.length).toEqual(1)
        namehelpText1 = await nameHelp[0].getText()
        expect(namehelpText1).toEqual("The policy name without file extension, can contain the characters: a-zA-Z0-9,.+_-")

        //enter file, expect no required text
        let dir = await Mkdir("temp")
        let tempFile = Path.resolve(Path.join(dir, "temp.yaml"))
        await Writefile(tempFile, 'test data')

        await uploadFileField.sendKeys(tempFile)

        //clear file name to prevent form submission
        await uploadNameField.clear()

        await uploadSubmitBtn.click()
        let fileHelpElements = await uploadFileParent.findElements(By.css("span.help-block"))
        expect(fileHelpElements.length).toEqual(0)




    })
    it('upload invalid acl content', async () => {
        await systemAclsPage.get()

        let uploadBtn = await systemAclsPage.getUploadBtn()
        await ctx.driver.wait(until.elementIsVisible(uploadBtn))

        //click
        await systemAclsPage.clickUploadBtn()

        //ensure modal is visible
        let uploadModal = await systemAclsPage.getUploadModal()
        await ctx.driver.wait(until.elementIsVisible(uploadModal))

        let uploadSubmitBtn = await systemAclsPage.getUploadSubmitBtn()
        await uploadSubmitBtn.click()

        //expect file and name field have errors

        let uploadFileField = await systemAclsPage.getUploadFileField()

        let uploadNameField = await systemAclsPage.getUploadNameField()

        //enter file name that is valid
        await uploadNameField.sendKeys('some-file-name')
        //submit


        //enter valid name, expect no errors
        let dir = await Mkdir("temp")
        let tempFile = Path.resolve(Path.join(dir, "temp.yaml"))
        await Writefile(tempFile, 'invalid acl content test data')

        await uploadFileField.sendKeys(tempFile)
        await uploadSubmitBtn.click()

        //wait for page to load

        await ctx.driver.wait(until.urlContains('/menu/acls'), 15000)

        //should show general error alert
        let alert = await systemAclsPage.getDangerAlert()
        let alertText = await alert.getText()
        expect(alertText).toContain("Validation failed")

        //should show validation content
        let validation = await systemAclsPage.getUploadedPolicyValidationTitle()
        let validationText = await validation.getText()
        expect(validationText).toEqual("Uploaded File failed ACL Policy Validation:")

    })
    it('upload valid acl content succeeds', async () => {
        await systemAclsPage.get()

        let uploadBtn = await systemAclsPage.getUploadBtn()
        await ctx.driver.wait(until.elementIsVisible(uploadBtn))

        //click
        await systemAclsPage.clickUploadBtn()

        //ensure modal is visible
        let uploadModal = await systemAclsPage.getUploadModal()
        await ctx.driver.wait(until.elementIsVisible(uploadModal))

        let uploadSubmitBtn = await systemAclsPage.getUploadSubmitBtn()
        await uploadSubmitBtn.click()

        //expect file and name field have errors

        let uploadFileField = await systemAclsPage.getUploadFileField()
        let uploadNameField = await systemAclsPage.getUploadNameField()



        //enter valid name, expect no errors
        let dir = await Mkdir("temp")
        let tempFile = Path.resolve(Path.join(dir, "temp.yaml"))
        await Writefile(tempFile, '{context: {"application":"rundeck"}, description: "test",for:{resource: [ { deny:["xyz"]}]}, by: {group: "DNE_test"}}')

        await uploadFileField.sendKeys(tempFile)

        //enter file name that is valid
        await uploadNameField.clear()
        await uploadNameField.sendKeys('test-valid-policy-name')
        //submit
        await uploadSubmitBtn.click()

        //wait for page to load

        await ctx.driver.wait(until.urlContains('/menu/acls'), 15000)

        //should not show alert
        let alerts = await ctx.driver.findElements(Elems.alertDanger)
        expect(alerts.length).toEqual(0)

        //should show 1 count in header
        let storedHeader = await systemAclsPage.getStoredPoliciesHeader()
        let countBadge = await storedHeader.findElement(By.css("h3 .badge"))
        let countText = await countBadge.getText()
        expect(countText).toEqual("1")

        //should include file name in acls list.
        let policiesList = await systemAclsPage.getStoredPoliciesCardList()
        let policiesTitles = await policiesList.findElements(By.css('span.h4 > span[data-bind="text: name"]'))
        expect(policiesTitles.length).toEqual(1)
        let text = await policiesTitles[0].getText()
        expect(text).toEqual("test-valid-policy-name")

    })
    it('upload form warns of duplicate name', async () => {
        await systemAclsPage.get()

        let uploadBtn = await systemAclsPage.getUploadBtn()
        await ctx.driver.wait(until.elementIsVisible(uploadBtn))

        //click
        await systemAclsPage.clickUploadBtn()

        //ensure modal is visible
        let uploadModal = await systemAclsPage.getUploadModal()
        await ctx.driver.wait(until.elementIsVisible(uploadModal))

        let uploadSubmitBtn = await systemAclsPage.getUploadSubmitBtn()
        await uploadSubmitBtn.click()

        //expect file and name field have errors

        let uploadFileField = await systemAclsPage.getUploadFileField()
        let uploadNameField = await systemAclsPage.getUploadNameField()
        let overwriteCheckbox = await systemAclsPage.getOverwriteCheckbox()



        //enter valid name, expect no errors
        let dir = await Mkdir("temp")
        let tempFile = Path.resolve(Path.join(dir, "temp.yaml"))
        await Writefile(tempFile, '{context: {"application":"rundeck"}, description: "test",for:{resource: [ { deny:["xyz"]}]}, by: {group: "DNE_test"}}')

        await uploadFileField.sendKeys(tempFile)

        //enter file name that is valid
        await uploadNameField.clear()
        await uploadNameField.sendKeys('test-valid-policy-name')
        //submit
        await uploadSubmitBtn.click()

        //expect warning in duplicate checkbox area

        //checkbox input is within another div
        let overwriteParent = await overwriteCheckbox.findElement(By.xpath("./../.."))
        let overwriteHelp = await overwriteParent.findElements(By.css("span.help-block"))
        expect(overwriteHelp.length).toEqual(1)
        let overwriteHelpText1 = await overwriteHelp[0].getText()
        expect(overwriteHelpText1).toEqual("A Policy already exists with the specified name")
    })
    it('delete acl policy', async () => {
        await systemAclsPage.get()

        //should not show alert
        let alerts = await ctx.driver.findElements(Elems.alertDanger)
        expect(alerts.length).toEqual(0)

        //should show 1 count in header
        let storedHeader = await systemAclsPage.getStoredPoliciesHeader()
        let countBadge = await storedHeader.findElement(By.css("h3 .badge"))
        let countText = await countBadge.getText()
        expect(countText).toEqual("1")

        //should include file name in acls list.
        let policiesList = await systemAclsPage.getStoredPoliciesCardList()
        let policiesTitles = await policiesList.findElements(By.css('span.h4 > span[data-bind="text: name"]'))
        expect(policiesTitles.length).toEqual(1)
        let text = await policiesTitles[0].getText()
        expect(text).toEqual("test-valid-policy-name")

        //get dropdown
        let actionmenu = await systemAclsPage.getActionDropdown(0)
        await actionmenu.click()
        //
        let menuparent = await actionmenu.findElement(By.xpath('./..'))
        let deleteAction = await menuparent.findElement(By.css("a.acl_menu__action_delete"))
        await deleteAction.click()

        //confirm modal exists
        let modal = await systemAclsPage.getDeleteModal()
        let displayed = await modal.isDisplayed()
        expect(displayed).toBeTruthy()

        let deleteBtn = modal.findElement(By.css('#deleteStorageAclPolicy_btn_0'))
        await deleteBtn.click()

        //confirm 0 policies


        await ctx.driver.wait(until.urlContains('/menu/acls'), 15000)

        //should not show alert
        alerts = await ctx.driver.findElements(Elems.alertDanger)
        expect(alerts.length).toEqual(0)

        //should show 1 count in header
        storedHeader = await systemAclsPage.getStoredPoliciesHeader()
        countBadge = await storedHeader.findElement(By.css("h3 .badge"))
        countText = await countBadge.getText()
        expect(countText).toEqual("0")

    })
})
