import {Context} from '@rundeck/testdeck/context'
import {CreateContext} from '@rundeck/testdeck/test/selenium'
import {LoginPage} from 'pages/login.page'
import {until, By, Key} from 'selenium-webdriver'
import '@rundeck/testdeck/test/rundeck'
import {KeyStoragePage} from "../../pages/keyStorage.page";

let ctx = CreateContext({projects: ['SeleniumBasic']})
let loginPage: LoginPage
let keyStoragePage: KeyStoragePage

beforeAll( async () => {
    loginPage = new LoginPage(ctx)
    keyStoragePage = new KeyStoragePage(ctx)
})

beforeAll(async () => {
    await loginPage.login('admin', 'admin')
})

describe('create key', () => {
    it('create succeeds', async () => {
        await keyStoragePage.get();
        const createButton = await keyStoragePage.getCreateButton();
        await createButton.click();

        const keyTypeSelector = await keyStoragePage.getUploadTypeDropdown()
        await keyTypeSelector.click()
        const passwordSelection = await keyStoragePage.selectKeyType()
        await passwordSelection.click()

        const passwordValueField = await keyStoragePage.getUploadPasswordField()
        await passwordValueField.click()
        await passwordValueField.sendKeys("test")

        const resourceNameField = await keyStoragePage.getResourceNameField()
        await resourceNameField.click()
        await resourceNameField.sendKeys("newKey")

        const saveButton = await keyStoragePage.getSaveButton()
        await saveButton.click()
    })
})

describe('overwrite key', () => {
    it('overwrite succeeds', async () => {
        await keyStoragePage.get();
        const selectKey = await keyStoragePage.selectKey("newKey");
        await selectKey.click();

        const overWriteButton = await keyStoragePage.getOverwriteButton()
        await overWriteButton.click()

        const uploadPassword = await keyStoragePage.getUploadPasswordField()
        await uploadPassword.click()
        await uploadPassword.sendKeys("test")

        const saveButton = await keyStoragePage.getSaveButton()
        await saveButton.click()
    })
})

describe('delete key', () => {
    it('delete succeeds', async () => {
        await keyStoragePage.get();
        const selectKey = await keyStoragePage.selectKey("newKey");
        await selectKey.click();

        const deleteButton = await keyStoragePage.getDeleteButton()
        await deleteButton.click()

        const deleteConfirmationButton = await keyStoragePage.getDeleteConfirmButton()
        await deleteConfirmationButton.click()
    })
})