package org.rundeck.util.gui.pages.webhooks

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class WebhooksPage extends BasePage{

    String loadPath
    By whEditBy = By.id('wh-edit')
    By whHeaderBy = By.id('wh-header')
    By dropdownMenu = By.className("dropdown-menu")
    By modalContent = By.className("modal-content")
    By alertDangerBy = By.className("alert-danger")
    By createWebhookButtonBy = By.cssSelector(".btn.btn-primary.btn-full")
    By handlerConfigTabBy = By.xpath("//*[@class='rdtabs__tab-inner' and contains(text(),'Handler Configuration')]")
    By chooseWebhookPluginBy = By.xpath("//button//*[contains(text(),'Choose Webhook Plugin')]")
    By trashButtonBy = By.xpath("//div[contains(@id,'rule-')]//i[contains(@class,'fa-trash')]")
    By okButtonBy = By.xpath("//button[contains(.,'OK')]")
    By saveButtonBy = By.xpath("//button[contains(.,'Save')]")
    By deleteButtonBy = By.linkText('Delete')
    By formControl = By.className("form-control")
    By logEventsBy = By.xpath("//li/a//*[contains(text(),'Log Events')]")
    By webhookSelectItem = By.className('webhook-select__item')
    By alertInfoBy = By.cssSelector(".alert.alert-info")

    WebhooksPage(SeleniumContext context) {
        super(context)
    }

    WebElement getCreateWebhookButton(){
        el createWebhookButtonBy
    }

    WebElement getHandlerConfigTab() {
        return el(whEditBy).findElement(handlerConfigTabBy)
    }

    WebElement getChooseWebhookPlugin() {
        return el(whEditBy).findElement(chooseWebhookPluginBy)
    }

    WebElement getTrashButton() {
        return el(whEditBy).findElement(trashButtonBy)
    }

    WebElement getOkButton() {
        return el(modalContent).findElement(okButtonBy)
    }

    WebElement getSaveButton() {
        return el(whHeaderBy).findElement(saveButtonBy)
    }

    WebElement getDeleteButton() {
        return el(whHeaderBy).findElement(deleteButtonBy)
    }

    WebElement getAlertDanger() {
        return el(alertDangerBy)
    }

    WebElement getNameFormField() {
        return els(formControl).get(3)
    }

    WebElement getLogEventsPlugin() {
        return el(whEditBy).findElement(dropdownMenu).findElement(logEventsBy)
    }

    List<WebElement> getWebhookSelectItems() {
        return els(webhookSelectItem)
    }

    WebElement getAlertInfo() {
        return el(alertInfoBy)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on execution show page: " + driver.currentUrl)
        }
    }

    void loadPageForProject(String projectName){
        this.loadPath = "/webhook/admin?project=${projectName}"
    }
}
