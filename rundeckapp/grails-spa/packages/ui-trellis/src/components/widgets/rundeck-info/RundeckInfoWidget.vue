<template>
    <InfoDisplay v-if="loaded"
        :edition="edition" 
        :version="system.versionInfo"
        :server="system.serverInfo"
    />
</template>


<script lang="ts">
import Vue, {PropType} from 'vue'
import { Observer } from 'mobx-vue'
import {Component, Inject, Prop} from 'vue-property-decorator'

import { RootStore } from '../../../stores/RootStore'
import { VersionInfo, ServerInfo, SystemStore } from '../../../stores/System'

import InfoDisplay from './RundeckInfo.vue'

@Observer
@Component({components: {
    InfoDisplay
}})
export default class RundeckInfoWidget extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    @Prop({default: 'Community'})
    edition!: string

    system!: SystemStore

    loaded = false

    created() {
        this.system = this.rootStore.system
    }

    async mounted() {
        try {
            await Promise.all([
                this.rootStore.system.load(),
                this.rootStore.releases.load()
            ])
        } catch(e) {}
        this.loaded = true
    }
}

</script>