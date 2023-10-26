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
import {defineComponent} from 'vue'

export default defineComponent({
    name:"ThemeSelect",
    data() {
        return {
            themes : ['system', 'light', 'dark'],
            theme : '',
            themeStore : window._rundeck.rootStore.theme
        }
    },
    beforeMount() {
        this.theme = this.themeStore.userPreferences.theme!
    },
    watch: {
        theme(newVal: any) {
            this.themeStore.setUserTheme(newVal)
        }
    }
})

</script>