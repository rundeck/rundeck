import {Context} from 'context'
import {By, until, WebElement} from 'selenium-webdriver'

export abstract class Page {
    abstract path: string

    constructor(readonly ctx: Context) {}

    async get() {
        const {driver} = this.ctx
        await driver.get(this.ctx.urlFor(this.path))
    }

    async clickBy(by: By) {
        const {driver} = this.ctx
        const elem = await driver.findElement(by)
        await elem.click()
    }

    async screenshot(freeze = true) {
        if (freeze)
            await this.freeze()
        return await this.ctx.screenshot()
    }

    /** Attempt to freeze the page for stable screenshots */
    async freeze() {
        await Promise.all([
            this.hideSpinners(),
            this.hideSidebarBottom(),
            this.disableTransitions(),
            this.hideServerUuid(),
            this.hideTests(),
            this.blur()
        ])
    }

    /** Hides version box for screenshots */
    async hideSidebarBottom() {
        const versionBox = await this.ctx.driver.findElement(By.id('sidebar-bottom'))
        await this.ctx.driver.executeScript((element: HTMLElement) => {
            element.style.setProperty('display', 'none')
        }, versionBox)
    }

    async hideTests() {
        const testElems = await this.ctx.driver.findElements(By.className('test-elem'))
        if (testElems.length > 0) {
            await this.ctx.driver.executeScript((elements: HTMLElement[]) => {
                elements.forEach(e => e.style.setProperty('display', 'none'))
            }, testElems)
        }
    }

    async hideServerUuid() {
        try {
            const serverUuid = await this.ctx.driver.findElement(By.className('rundeck-server-uuid'))
            if (serverUuid)
                await this.ctx.driver.executeScript((element: HTMLElement) => {
                    element.parentElement.parentElement.parentElement.style.setProperty('display', 'none')
                }, serverUuid)
        } catch{}
    }

    /** Hide spinners */
    async hideSpinners() {
        const spinners = await this.ctx.driver.executeScript<WebElement[]>(() => {
            let spinners = document.getElementsByClassName('loading-spinner')
                for (let elem of spinners) {
                    elem.remove()
                }
            return spinners
        })

        if (spinners.length > 0) {
            await this.ctx.driver.wait(until.stalenessOf(spinners[0]))
        }
    }

    /** Attempts to clear transitions, animations, and animated gifs from the page */
    async disableTransitions() {
        await this.ctx.driver.executeScript<WebElement[]>( () => {
            let styles = document.styleSheets
            let style = styles.item(0) as CSSStyleSheet
            if (style) {
                console.log(`Insterting no-transition rule into ${style.rules.item(0)!.cssText}`)
                style.insertRule(`.notransition * { 
                    -webkit-transition: none !important; 
                    -moz-transition: none !important; 
                    -o-transition: none !important; 
                    -ms-transition: none !important; 
                    transition: none !important; 
                }`, 0)
            }
            document.body.classList.add('notransition')
            let x = document.body.offsetHeight
        })
    }

    /** Blur the active element. Useful for hiding blinking cursor before screen cap. */
    async blur() {
        await this.ctx.driver.executeScript(() => {
            const elem = document.activeElement as HTMLElement
            elem.blur()
        })
    }
}