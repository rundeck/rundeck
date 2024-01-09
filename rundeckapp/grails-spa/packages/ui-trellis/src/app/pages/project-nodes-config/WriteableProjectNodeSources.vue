<template>
  <div>
    <slot name="empty" v-if="writeableSources.length < 1"></slot>
    <div :class="itemCss" v-for="source in writeableSources" :key="source.index">
      <div :class="itemContentCss">
        <plugin-config
          :mode="'title'"
          :serviceName="'ResourceModelSource'"
          :provider="source.type"
          :key="source.type+'title/'+source.index"
          :show-description="!source.resources.description"
        >
          <template v-slot:titlePrefix>
            <span :title="'Source #'+source.index">{{source.index}}.</span>
          </template>
          <template v-if="source.resources.description" v-slot:titleSuffix>
            <span>
            <code>{{source.resources.description}}</code>
          </span>
          </template>
        </plugin-config>

        <div v-if="source.resources.syntaxMimeType" class="item-section">
          Format:
          <span class="text-info">{{source.resources.syntaxMimeType}}</span>
        </div>

        <div v-if="source.resources.writeable" class="item-section">
          <a :href="source.resources.editPermalink" class="btn btn-sm btn-default">
            <i class="glyphicon glyphicon-pencil"></i>
            {{$t('Modify')}}
          </a>
        </div>
        <div class="item-section" v-if="source.errors">
          <div class="well well-sm">
            <div class="text-info">{{$t('The Node Source had an error')}}:</div>
            <span class="text-danger">{{source.errors}}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent, PropType } from "vue"
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue"
import {
  getProjectNodeSources,
  NodeSource
} from "./nodeSourcesUtil"
import { EventBus } from "../../../library"

export default defineComponent({
  name: 'WriteableProjectNodeSources',
  components: {
    PluginConfig
  },
  data() {
    return {
      sourcesData: [] as NodeSource[]
    }
  },
  methods: {
    async loadNodeSourcesData(): Promise<void> {
      try {
        this.sourcesData = await getProjectNodeSources()
      } catch (e) {
        return console.warn("Error getting node sources list", e)
      }
    }
  },
  computed: {
    writeableSources: function(): NodeSource[] {
      return this.sourcesData.filter(e => e.resources.writeable)
    }
  },
  props: {
    itemCss: {
      type: String,
      default: ""
    },
    itemContentCss: {
      type: String,
      default: ""
    },
    eventBus:{
      type: Object as PropType<typeof EventBus>,
      required:false,
    }
  },

  mounted() {

    this.eventBus&&this.eventBus.on('project-node-sources-saved', () => {
      this.loadNodeSourcesData()
    })
    if (
      window._rundeck &&
      window._rundeck.rdBase &&
      window._rundeck.projectName
    ) {
      this.loadNodeSourcesData()
    }
  }
})
</script>
<style lang="scss">
.item-section{
  margin-top: 0.5em
}
</style>
