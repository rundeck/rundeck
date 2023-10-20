package org.rundeck.util.rule

import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.OutputType

class ScreenShot implements MethodRule{

    private WebDriver driver

    ScreenShot(WebDriver driver) {
        this.driver = driver
    }

    @Override
    Statement apply(final Statement statement, final FrameworkMethod frameworkMethod, final Object o) {
        return new Statement() {
            @Override
            void evaluate() throws Throwable {
                try {
                    statement.evaluate()
                } catch (Throwable t) {
                    captureScreenshot(frameworkMethod.getName())
                    throw t
                }
            }
            void captureScreenshot(String fileName) {
                try {
                    File testResourcesDir = new File(System.getProperty("user.home") + "/test-results/images")
                    if (!testResourcesDir.exists()) {
                        testResourcesDir.mkdirs()
                    }
                    new FileOutputStream(System.getProperty("user.home") + "/test-results/images" + "/${driver.class.simpleName}-$fileName.png").withStream { out ->
                        out.write(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES) as byte)
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
