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
          :validation="plugin.validation"
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
                <plugin-info
                  v-if="header.detail.desc"
                  :show-title="header.inputShowTitle"
                  :show-icon="header.inputShowIcon || false"
                  :show-description="header.inputShowDescription || false"
                  :show-extended="true"
                  :detail="header.detail"
                >
                  <template #description>
                    <plugin-details
                      :description="header.detail!.desc"
                      extended-css=""
                      allow-html
                      inline-description
                    />
                  </template>
                </plugin-info>
              </label>
            </div>
          </template>
        </plugin-config>
      </div>
    </template>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginConfig from "@/library/components/plugins/pluginConfig.vue";
import { isEqual } from "lodash";
import {
  PluginInitialData,
  Plugin,
  JobPlugins,
  PluginDataFromApi,
} from "./types/executionTypes";
import PluginDetails from "@/library/components/plugins/PluginDetails.vue";

export default defineComponent({
  name: "ExecutionEditor",
  components: {
    PluginDetails,
    PluginConfig,
    PluginInfo,
  },
  props: {
    modelValue: {
      type: Object as PropType<JobPlugins>,
      default: () => {},
    },
    initialData: {
      type: Object as PropType<PluginDataFromApi>,
      default: () => {},
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      internalData: null,
      checkedData: [] as string[],
      executionLifecyclePlugins: [] as Plugin[],
      currentMode: "create",
    };
  },
  computed: {
    formattedData(): JobPlugins {
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
        this.checkedData = Object.keys(newVal?.ExecutionLifecycle);

        this.executionLifecyclePlugins = newVal?.pluginsInitialData.map((e) =>
          this.massagePluginInfo(e),
        );

        if (this.checkedData.length > 0) {
          this.currentMode = "edit";
        }
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
    massagePluginInfo(plugin: PluginInitialData): Plugin {
      let config = {};
      const retrievedValue = this.initialData.ExecutionLifecycle[plugin.name];
      let validationData = {
        errors: {},
        valid: true,
      };

      if (
        this.initialData.validationErrors &&
        Object.keys(this.initialData.validationErrors).includes(plugin.name)
      ) {
        validationData = this.initialData.validationErrors[plugin.name];
      }

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
        validation: validationData,
      };
    },
  },
});
</script>
