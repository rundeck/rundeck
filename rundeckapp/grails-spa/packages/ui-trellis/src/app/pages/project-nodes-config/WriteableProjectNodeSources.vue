<template>
  <div>
    <slot v-if="writeableSources.length < 1" name="empty"></slot>
    <div
      v-for="source in writeableSources"
      :key="source.index"
      :class="itemCss"
    >
      <div :class="itemContentCss">
        <plugin-config
          :key="source.type + 'title/' + source.index"
          :mode="'title'"
          :service-name="'ResourceModelSource'"
          :provider="source.type"
          :show-description="!source.resources.description"
        >
          <template #titlePrefix>
            <span :title="'Source #' + source.index">{{ source.index }}.</span>
          </template>
          <template v-if="source.resources.description" #titleSuffix>
            <span>
              <code>{{ source.resources.description }}</code>
            </span>
          </template>
        </plugin-config>

        <div v-if="source.resources.syntaxMimeType" class="item-section">
          Format:
          <span class="text-info">{{ source.resources.syntaxMimeType }}</span>
        </div>

        <div v-if="source.resources.writeable" class="item-section">
          <a
            :href="source.resources.editPermalink"
            class="btn btn-sm btn-default"
          >
            <i class="glyphicon glyphicon-pencil"></i>
            {{ $t("Modify") }}
          </a>
        </div>
        <div v-if="source.errors" class="item-section">
          <div class="well well-sm">
            <div class="text-info">
              {{ $t("The Node Source had an error") }}:
            </div>
            <span class="text-danger">{{ source.errors }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent, PropType } from "vue";
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue";
import { getProjectNodeSources, NodeSource } from "./nodeSourcesUtil";
import { EventBus } from "../../../library";

export default defineComponent({
  name: "WriteableProjectNodeSources",
  components: {
    PluginConfig,
  },
  props: {
    itemCss: {
      type: String,
      default: "",
    },
    itemContentCss: {
      type: String,
      default: "",
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: false,
    },
  },
  data() {
    return {
      sourcesData: [] as NodeSource[],
    };
  },
  computed: {
    writeableSources: function (): NodeSource[] {
      return this.sourcesData.filter((e) => e.resources.writeable);
    },
  },

  mounted() {
    this.eventBus &&
      this.eventBus.on("project-node-sources-saved", () => {
        this.loadNodeSourcesData();
      });
    if (
      window._rundeck &&
      window._rundeck.rdBase &&
      window._rundeck.projectName
    ) {
      this.loadNodeSourcesData();
    }
  },
  methods: {
    async loadNodeSourcesData(): Promise<void> {
      try {
        this.sourcesData = await getProjectNodeSources();
      } catch (e) {
        return console.warn("Error getting node sources list", e);
      }
    },
  },
});
</script>
<style lang="scss">
.item-section {
  margin-top: 0.5em;
}
</style>
