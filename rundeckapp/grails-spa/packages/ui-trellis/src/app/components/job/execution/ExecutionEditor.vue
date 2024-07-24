<template>
  <div class="help-block">
    {{
      $t("scheduledExecution.property.executionLifecyclePluginConfig.help.text")
    }}
  </div>
  <div v-if="executionLifecyclePlugins" class="list-group">
    <template v-for="plugin in executionLifecyclePlugins">
      <div class="list-group-item">
        <plugin-config
          v-model="plugin.extra"
          :config="plugin.extra.config"
          :mode="currentMode"
          service-name="ExecutionLifecycle"
          :provider="plugin.name"
          scope="Unspecified"
          default-scope="Unspecified"
          :show-icon="true"
        >
          <template #header="{ header }">
            <div class="checkbox">
              <input
                :id="`executionLifecyclePluginEnabled_${plugin.name}`"
                v-model="checkedData"
                :value="plugin.name"
                type="checkbox"
                class="form-control"
              />
              <label :for="`executionLifecyclePluginEnabled_${plugin.name}`">
                <pluginInfo
                  v-if="header.detail.desc"
                  :show-title="header.inputShowTitle"
                  :show-icon="header.inputShowIcon || false"
                  :show-description="header.inputShowDescription || false"
                  :show-extended="true"
                  :detail="header.detail"
                >
                  <template #description>
                    <scheduled-execution-details
                      :allow-html="true"
                      :description="header.detail!.desc"
                      mode="collapsed"
                      more-text="More..."
                    />
                  </template>
                </pluginInfo>
              </label>
            </div>
          </template>
        </plugin-config>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType, ref } from "vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import ScheduledExecutionDetails from "@/app/components/common/ScheduleExecutionDetails.vue";
import { isEqual } from "lodash";

interface PluginConf {
  readonly type: string;
  extra: any;
  config: any;
}
interface ProjectPluginConfigEntry {
  entry: PluginConf;
  extra: PluginConf;
  create?: boolean;
  originIndex?: number;
  index?: number;
  modified: boolean;
  readOnly: boolean;
}

export default defineComponent({
  name: "ExecutionEditor",
  components: { ScheduledExecutionDetails, PluginConfig, PluginInfo },
  props: ["modelValue", "initialData"],
  emits: ["update:modelValue"],
  data() {
    return {
      internalData: null,
      checkedData: [],
      executionLifecyclePlugins: [] as ProjectPluginConfigEntry[],
      loaded: false,
    };
  },
  computed: {
    currentMode() {
      return "create";
    },
    formattedData() {
      if (this.initialData) {
        const temp = {
          ExecutionLifecycle: {},
        };

        this.executionLifecyclePlugins.forEach((plugin) => {
          if (this.checkedData.includes(plugin.name)) {
            temp["ExecutionLifecycle"][plugin.name] = plugin.extra.config;
          }
        });
        return temp;
      }
      return {};
    },
  },
  watch: {
    initialData: {
      deep: true,
      handler(newVal) {
        this.checkedData = Object.keys(newVal.ExecutionLifecycle);

        this.executionLifecyclePlugins = newVal?.pluginsInitialData.map((e) =>
          this.massagePluginInfo(e),
        );
      },
    },
    formattedData: {
      deep: true,
      handler(newVal, oldVal) {
        if (!isEqual(newVal, oldVal)) {
          this.$emit("update:modelValue", newVal);
        }
      },
    },
  },
  methods: {
    massagePluginInfo(plugin: object) {
      let config = {};
      const retrievedValue = this.initialData.ExecutionLifecycle[plugin.name];

      if (retrievedValue) {
        config = { ...retrievedValue };
      }

      return {
        ...plugin,
        type: plugin.name,
        providerMetadata: plugin.metadata,
        extra: {
          config,
          type: undefined,
        },
      };
    },
  },
});
</script>
