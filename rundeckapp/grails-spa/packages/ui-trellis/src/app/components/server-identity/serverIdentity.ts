// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {defineComponent, markRaw} from 'vue'
import ServerDisplay from '../../../library/components/version/ServerDisplay.vue'
import {getRundeckContext} from '../../../library'
import {ServerInfo} from '../../../library/stores/System'

const ServerIdentityComp = defineComponent({
  name: 'ServerIdentityComp',
  components: {
    ServerDisplay
  },
  data() {
    return {
      serverInfo: null
    }
  },
  props: {
    showId: {type: Boolean, default: true},
    nameClass: {type: String, default: ''},
    serverName: {type: String, default: ''},
    serverUuid: {type: String, default: ''}
  },
 mounted() {
      this.serverInfo = new ServerInfo(this.serverName, this.serverUuid)
  },
  template: `
    <server-display :uuid="serverInfo.uuid"
                    :name="serverInfo.name"
                    :name-class="nameClass"
                    :glyphicon="serverInfo.icon"
                    :show-id="showId"
                    v-if="serverInfo">
    </server-display>`
})

const rundeckContext = getRundeckContext()
if (rundeckContext && rundeckContext.rootStore && rundeckContext.rootStore.ui) {
  rundeckContext.rootStore.ui.addItems([
    {
      section: 'server-info-display',
      location: 'main',
      visible: true,
      widget: markRaw(defineComponent({
        name: 'StatusIndicatorWrapper',
        components: {
          ServerIdentityComp
        },
        props: ['itemData'],
        template: `
          <server-identity-comp v-bind="itemData"/>`
      }))
    }
  ])
}
