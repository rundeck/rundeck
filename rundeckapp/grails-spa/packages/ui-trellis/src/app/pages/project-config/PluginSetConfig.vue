<template>
  <div>
    <project-plugin-groups
      :config-prefix="configPrefix"
      service-name="PluginGroup"
      :edit-mode="true"
      :help="help"
      project=""
      :edit-button-text="$t('Edit Plugin Groups')"
      :mode-toggle="modeToggle"
      @input="inputReceived"
    >
    </project-plugin-groups>
    <template v-if="pluginConfigs">
      <input
        type="hidden"
        :value="JSON.stringify(pluginConfigs)"
        :name="configPrefix + 'json'"
      />
    </template>
  </div>
</template>
<script lang="ts">
import { PropType, defineComponent } from "vue";
import { EventBus, getRundeckContext } from "../../../library";
import { RundeckBrowser } from "../../../library";
import PluginValidation from "../../../library/interfaces/PluginValidation";
import ProjectPluginGroups from "./ProjectPluginGroups.vue";

const client: RundeckBrowser = getRundeckContext().rundeckClient;
const rdBase = getRundeckContext().rdBase;

interface PluginConf {
  readonly type: string;
  config: any;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  validation: PluginValidation;
  create?: boolean;
  origIndex?: number;
  modified: boolean;
}
export default defineComponent({
  name: "App",
  components: {
    ProjectPluginGroups,
  },
  props: {
    serviceName: String,
    help: {
      type: String,
      required: false,
    },
    modeToggle: {
      type: Boolean,
      default: true,
    },
    eventBus: { type: Object as PropType<typeof EventBus>, required: false },
    configPrefix: String,
    project: String,
  },
  data() {
    return {
      pluginConfigs: [] as ProjectPluginConfigEntry[],
    };
  },
  methods: {
    inputReceived(pluginConfigs: ProjectPluginConfigEntry[]) {
      this.pluginConfigs = pluginConfigs;
    },
  },
});
</script>
