import { autorun, observable } from "mobx";

import {RundeckClient} from '@rundeck/client'
import {RootStore} from './RootStore'

enum Theme {
    system = "system",
    dark = "dark",
    light = "light"
}

interface UserPreferences {
    theme?: Theme
}

const THEME_USER_PREFERENCES_KEY = 'theme-user-preferences'

export class ThemeStore {
    @observable userPreferences: UserPreferences = {theme: Theme.system}
    @observable theme!: Theme

    themeMediaQuery: MediaQueryList

    constructor() {
        this.loadConfig()
        this.setTheme(this.userPreferences.theme || Theme.system)

        this.themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
        this.handleSystemChange(this.themeMediaQuery)

        this.themeMediaQuery.addEventListener('change', this.handleSystemChange)
    }

    setUserTheme(theme: Theme) {
        this.userPreferences.theme = theme
        if (theme == Theme.system)
            this.handleSystemChange(this.themeMediaQuery)
        else
            this.setTheme(theme)

        this.saveConfig()
    }

    setTheme(theme: Theme) {
        this.theme = theme
        document.documentElement.dataset.colorTheme = theme
    }

    handleSystemChange = (e: MediaQueryListEvent | MediaQueryList) => {
        console.log(e)
        if (this.userPreferences.theme == Theme.system)
            if (e.matches)
                this.setTheme(Theme.dark)
            else
                this.setTheme(Theme.light)
    }

    loadConfig() {
        const settings = localStorage.getItem(THEME_USER_PREFERENCES_KEY)
  
        if (settings) {
            try {
                const config = JSON.parse(settings)
                Object.assign(this.userPreferences, config)
            } catch (e) {
                localStorage.removeItem(THEME_USER_PREFERENCES_KEY)
            }
        }
    }

    saveConfig() {
        localStorage.setItem(THEME_USER_PREFERENCES_KEY, JSON.stringify(this.userPreferences))
      }
}