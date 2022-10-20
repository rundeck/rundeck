// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import ServerDisplay from '@/library/components/version/ServerDisplay.vue'
import { getRundeckContext } from '@/library'
import { ServerInfo }  from '@/library/stores/System'

Vue.config.productionTip = false


/* eslint-disable no-new */

const serverCollection = Array.from(document.body.getElementsByClassName('rundeck-server-uuid'))
serverCollection.forEach( serverElement => {
    new Vue({
        el: serverElement,
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
            this.showId = serverElement.attributes['data-show-id']?serverElement.attributes['data-show-id'].value !== 'false':true
            this.nameClass = serverElement.attributes['data-name-class']?serverElement.attributes['data-name-class'].value:''
            if (serverElement.attributes['data-server-name'] && serverElement.attributes['data-server-uuid']) {
                this.serverInfo =
                    new ServerInfo(serverElement.attributes['data-server-name'].value, serverElement.attributes['data-server-uuid'].value)
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
})
