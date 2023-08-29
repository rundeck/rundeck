<template>
    <InfoDisplay v-if="loaded" 
        :version="system.versionInfo"
        :latest="releases.releases[0]"
        :server="system.serverInfo"
        :app-info="system.appInfo"
    />
</template>


<script lang="ts">
import {defineComponent} from 'vue'
import InfoDisplay from './RundeckInfo.vue'

export default defineComponent({
    name:"RundeckInfoWidget",
    components: {
        InfoDisplay
    },
    data() {
        return {
            system: window._rundeck.rootStore.system,
            releases: window._rundeck.rootStore.releases,
            loaded: false
        }
    },
    async mounted() {
        await this.releases.load()
        try {
            await Promise.all([
                this.system.load(),
            ])
        } catch(e) {}
        this.loaded = true
    }
})

</script>