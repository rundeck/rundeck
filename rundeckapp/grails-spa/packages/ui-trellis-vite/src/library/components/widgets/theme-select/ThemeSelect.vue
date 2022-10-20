<template>
    <div style="padding: 20px;">
        <div class="form-group">
          <label>Theme</label>
          <select class="form-control select"
            v-model="theme"
          >
            <option v-for="themeOpt in themes" :key="themeOpt">{{themeOpt}}</option>
          </select>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'

import {Component, Inject, Prop, Watch} from 'vue-property-decorator'
import {Observer} from 'mobx-vue'

import {RootStore} from '../../../stores/RootStore'
import { ThemeStore } from '../../../stores/Theme'


@Observer
@Component({components: {}})
export default class ThemeSelect extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    themes = ['system', 'light', 'dark']

    theme = ''

    themeStore!: ThemeStore

    created() {
        this.themeStore = this.rootStore.theme
        this.theme = this.themeStore.userPreferences.theme!
    }

    @Watch('theme')
    handleThemeChange(newVal: any) {
        this.themeStore.setUserTheme(newVal)
    }
}
</script>