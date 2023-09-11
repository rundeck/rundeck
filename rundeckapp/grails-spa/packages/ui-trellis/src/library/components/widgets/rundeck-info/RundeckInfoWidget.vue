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
import type {PropType} from "vue"
import InfoDisplay from './RundeckInfo.vue'
import {SystemStore} from "../../../stores/System"
import {Releases} from "../../../stores/Releases"

export default defineComponent({
    name:"RundeckInfoWidget",
    props: {
      system: {
        type: Object as PropType<SystemStore>,
        required: true,
      },
      releases: {
        type: Object as PropType<Releases>,
        required: true,
      },
    },
    components: {
        InfoDisplay
    },
    data() {
        return {
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