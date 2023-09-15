<template>
    <InfoDisplay
        :version="system.versionInfo"
        :latest="releases.releases[0]"
        :server="system.serverInfo"
        :app-info="system.appInfo"
    />
</template>


<script lang="ts">
import {RootStore} from '../../../stores/RootStore'
import {defineComponent, inject} from 'vue'
import InfoDisplay from './RundeckInfo.vue'

export default defineComponent({
    name:"RundeckInfoWidget",
    components: {
        InfoDisplay
    },
    setup() {
      let {system, releases} = inject('rootStore') as RootStore
      return {system, releases}
    },
    async mounted() {
        try {
            await Promise.all([
                this.releases.load(),
                this.system.load(),
            ])
        } catch(e) {}
    }
})

</script>