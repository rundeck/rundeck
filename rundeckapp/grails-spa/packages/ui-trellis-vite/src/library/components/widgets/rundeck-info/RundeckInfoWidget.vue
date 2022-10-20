<template>
    <InfoDisplay v-if="loaded" 
        :version="system.versionInfo"
        :latest="releases.releases[0]"
        :server="system.serverInfo"
        :app-info="system.appInfo"
    />
</template>


<script lang="ts">
import Vue, {PropType} from 'vue'
import { Observer } from 'mobx-vue'
import {Component, Inject, Prop} from 'vue-property-decorator'

import { RootStore } from '../../../stores/RootStore'
import { Releases } from '../../../stores/Releases'
import { SystemStore } from '../../../stores/System'

import InfoDisplay from './RundeckInfo.vue'


@Observer
@Component({components: {
    InfoDisplay
}})
export default class RundeckInfoWidget extends Vue {
    @Inject()
    private readonly rootStore!: RootStore

    system!: SystemStore

    releases!: Releases

    loaded = false

    created() {
        this.system = this.rootStore.system
        this.releases = this.rootStore.releases
    }

    async mounted() {
        this.rootStore.releases.load()
        try {
            await Promise.all([
                this.rootStore.system.load(),
            ])
        } catch(e) {}
        this.loaded = true
    }
}

</script>