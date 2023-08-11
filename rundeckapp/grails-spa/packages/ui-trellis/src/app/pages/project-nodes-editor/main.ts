// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {defineComponent, markRaw} from 'vue'
import {loadJsonData} from '../../utilities/loadJsonData'
import EditProjectNodeSourcePage from './EditProjectNodeSourcePage.vue'
import {getRundeckContext} from '../../../library'


// include any i18n injected in the page by the app
const context = getRundeckContext()
// Create VueI18n instance with options

context.rootStore.ui.addItems([{
  section: 'edit-project-node-source-file',
  location: 'main',
  visible: true,
  widget: markRaw(defineComponent({
    name: 'ProjectNodesEditorWidget',
    data: function(){
      return {
        EventBus: context.eventBus,
        index:-1,
        nextPageUrl:''
      }
    },
    provide: { nodeSourceFile:context.rootStore.nodeSourceFile },
    components: {
      EditProjectNodeSourcePage
    },
    template:`<edit-project-node-source-page :index="index"  :next-page-url="nextPageUrl" :event-bus="EventBus" v-if="index>=0"/>`,
    mounted(){
      const context = getRundeckContext()

      if (context.data && context.data.editProjectNodeSourceData) {
        this.index = context.data.editProjectNodeSourceData.index
        this.nextPageUrl = context.data.editProjectNodeSourceData.nextPageUrl
      }else {
        const data = loadJsonData('editProjectNodeSourceData')
        this.index = data.index
        this.nextPageUrl = data.nextPageUrl
      }
    },
  }))
}])
