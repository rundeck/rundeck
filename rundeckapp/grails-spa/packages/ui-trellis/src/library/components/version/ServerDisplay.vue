<template>
    <span :title="title">
        <span :style="{color}">
            <i class="glyphicon" :class="[`glyphicon-${glyphicon}`]" />
            <span v-if="showId">{{ uuidShort }}</span>
        </span>
        <span style="margin-left: 0.5em" :class="css">{{name}}</span>
        <ui-socket section="server-info-display" location="badges" :socket-data="{uuid}" />
    </span>
</template>

<script lang="ts">
import {defineComponent, PropType} from 'vue'

import UiSocket from '../utils/UiSocket.vue'
import { RundeckVersion } from '../../utilities/RundeckVersion'

export default defineComponent({
  components: {UiSocket},
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
            return this.uuid.substring(0, 2)
        },
        title(): string {
            return `${this.glyphicon}-${this.uuidShort} / ${this.uuid}`
        }
    }
})
</script>