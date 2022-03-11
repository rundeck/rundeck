// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import ServerDisplay from '@rundeck/ui-trellis/lib/components/version/ServerDisplay'
import { getRundeckContext } from '@rundeck/ui-trellis'
import {ServerInfo}  from '@rundeck/ui-trellis/src/stores/System'

Vue.config.productionTip = false


/* eslint-disable no-new */

const serveruuid = document.body.getElementsByClassName('rundeck-server-uuid')
for (var i = 0; i < serveruuid.length; i++) {
    const e = serveruuid[i]

    new Vue({
        el: e,
        components: {
            ServerDisplay
        },
        data() {
            return {
                serverInfo:null,
                showId:true,
                nameClass:''
            }
        },
        async mounted () {
            this.showId = e.attributes['data-show-id']?e.attributes['data-show-id'].value !== 'false':true
            this.nameClass = e.attributes['data-name-class']?e.attributes['data-name-class'].value:''
            if (e.attributes['data-server-name'] && e.attributes['data-server-uuid']) {
                this.serverInfo =
                    new ServerInfo(e.attributes['data-server-name'].value, e.attributes['data-server-uuid'].value)
            } else {
                const rootStore=getRundeckContext().rootStore
                try {
                    await rootStore.system.load()
                } catch (e) {
                }
                this.serverInfo = rootStore.system.serverInfo
            }
        },
        template:`<server-display :uuid="serverInfo.uuid" 
                                  :name="serverInfo.name"
                                  :name-class="nameClass"
                                  :glyphicon="serverInfo.icon"
                                  :show-id="showId"
                                  v-if="serverInfo">
        </server-display>`
    })
}
