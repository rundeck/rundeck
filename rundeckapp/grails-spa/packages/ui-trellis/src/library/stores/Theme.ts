import { autorun, observable } from "mobx";

import {RundeckClient} from '@rundeck/client'
import {RootStore} from './RootStore'

export enum Theme {
    system = "system",
    dark = "dark",
    light = "light"
}

export enum PrefersColorScheme {
    dark = 'dark',
    light = 'light'
}

interface UserPreferences {
    theme?: Theme
}

const THEME_USER_PREFERENCES_KEY = 'theme-user-preferences'

export class ThemeStore {
    @observable userPreferences: UserPreferences = {theme: Theme.system}
    @observable theme!: Theme
    @observable prefersColorScheme!: PrefersColorScheme 

    themeMediaQuery: MediaQueryList

    constructor() {
        this.loadConfig()
        this.setTheme()

        this.themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
        this.handleSystemChange(this.themeMediaQuery)

        // Safari <14
        if (typeof this.themeMediaQuery.addListener != 'undefined')
            this.themeMediaQuery.addListener(this.handleSystemChange)
        else
            this.themeMediaQuery.addEventListener('change', this.handleSystemChange)

    }

    setUserTheme(theme: Theme) {
        this.userPreferences.theme = theme
        this.setTheme()
        this.saveConfig()
    }

    setTheme() {
        if (this.userPreferences.theme == Theme.system)
            this.theme = Theme[this.prefersColorScheme]
        else
            this.theme = this.userPreferences.theme!

        document.documentElement.dataset.colorTheme = this.theme
    }

    handleSystemChange = (e: MediaQueryListEvent | MediaQueryList) => {
        if (e.matches)
            this.prefersColorScheme = PrefersColorScheme.dark
        else
            this.prefersColorScheme = PrefersColorScheme.light

        this.setTheme()
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