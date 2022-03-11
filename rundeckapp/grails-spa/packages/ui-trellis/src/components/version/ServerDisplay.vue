<template>
    <span :title="title">
        <span :style="{color}" class="rundeck-server-uuid" :data-server-uuid="uuid">
            <i class="glyphicon" :class="[`glyphicon-${glyphicon}`]" />
            <span v-if="showId">{{ uuidShort }}</span>
        </span>
        <span style="margin-left: 0.5em" :class="css">{{name}}</span>
    </span>
</template>

<script lang="ts">
import Vue, {PropType} from 'vue'

import {ServerInfo} from '../../stores/System'
import { RundeckVersion } from '../../utilities/RundeckVersion'

export default Vue.extend({
    props: {
        glyphicon: String,
        uuid: String,
        name: String,
        nameClass: String,
        showId: {default:true}
    },
    computed: {
        css(): string {
            return this.nameClass||''
        },
        color(): string {
            if(!this.uuid){
              return ''
            }
            const ver = new RundeckVersion({})
            return `#${ver.splitUUID(this.uuid)['sixes'][0]}`
        },
        uuidShort(): string {
            if(!this.uuid){
              return ''
            }
            return this.uuid.substr(0, 2)
        },
        title(): string {
            return `${this.glyphicon}-${this.uuidShort} / ${this.uuid}`
        }
    }
})
</script>