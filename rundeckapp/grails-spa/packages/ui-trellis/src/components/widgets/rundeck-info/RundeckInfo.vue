<template>
    <div class="rundeck-info-widget">
        <div class="rundeck-info-widget__group">
            <a :href="welcomeUrl()">
                <div class="rundeck-info-widget__header">
                    <RundeckVersion :edition="edition" :number="version.number" :tag="version.tag"/>
                </div>
            </a>
            <div>
                <VersionDisplay :text="`${version.name} ${version.color} ${version.icon}`" :icon="version.icon" :color="version.color" />
            </div>
            <div>
                <ServerDisplay
                    :name="server.name"
                    :glyphicon="server.icon"
                    :uuid="server.uuid"
                />
            </div>
        </div>
        <div class="rundeck-info-widget__group" style="border-top: solid 1px grey;">
            <div class="rundeck-info-widget__heading">Latest Releases</div>
        </div>
        <div class="rundeck-info-widget__group" style="display: flex; flex-direction: column-reverse; flex-grow: 1;">
            <Copyright/>
        </div>
    </div>
</template>

<script lang="ts">
import Vue from 'vue'
import { Observer } from 'mobx-vue'
import {Component, Prop} from 'vue-property-decorator'

import { VersionInfo, ServerInfo } from 'src/stores/System'

import {url} from '../../../rundeckService'

import Copyright from '../../version/Copyright.vue'
import RundeckVersion from '../../version/RundeckVersionDisplay.vue'
import VersionDisplay from '../../version/VersionIconNameDisplay.vue'
import ServerDisplay from '../../version/ServerDisplay.vue'

@Observer
@Component({components: {
    ServerDisplay,
    VersionDisplay,
    RundeckVersion,
    Copyright
}})
export default class RundeckInfoWidget extends Vue {
    @Prop({default: 'Community'})
    edition!: String

    @Prop()
    version!: VersionInfo

    @Prop()
    server!: ServerInfo

    async mounted() {

    }

    welcomeUrl() {
        return url('/menu/welcome').toString()
    }
}
</script>

<style scoped lang="scss">
.rundeck-info-widget {
    display: flex;
    align-content: center;
    flex-direction: column;
    padding: 15px;
    height: 300px;
    width: 400px;
    // font-size: 12px;

    > div:not(:first-child) {
        margin-top: 10px;
        padding-top: 10px;
    }
}

.rundeck-info-widget__group {
    > :not(:first-child) {
        margin-top: 10px;
    }
}

.rundeck-info-widget__heading {
    font-weight: bold;
}

</style>