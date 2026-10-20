<template>
  <div>
    <div class="form-horizontal">
      <plugin-config
        v-if="isConfigSet"
        v-model="cacheConfig"
        mode="create"
        :show-title="false"
        :show-description="false"
        :use-runner-selector="true"
        :event-bus="rundeckContext.eventBus"
        :plugin-config="pluginConfig"
        :category="category"
      >
        <template #extra>
          <div class="row">
            <div class="col-xs-12 col-sm-12">
              <span>
                <a
                  key="save"
                  class="btn btn-cta reset_page_confirm"
                  @click="saveConfig"
                  >{{ $t("Save") }}</a
                >
              </span>
            </div>
          </div>
        </template>
      </plugin-config>
    </div>
  </div>
</template>

<script lang="ts">
import PluginConfig from "../../../library/components/plugins/pluginConfig.vue";
import { RundeckContext } from "../../../library";
import {
  getProjectConfigurable,
  setProjectConfigurable,
} from "./nodeSourcesUtil";
import { Notification } from "uiv";
import { defineComponent } from "vue";

interface Prop {
  type: string;
  defaultValue: unknown;
  title: string;
  required: boolean;
  options: Record<string, unknown>;
  allowed: unknown;
  name: string;
  desc: string;
  description: string;
  renderingOptions: Record<string, unknown>;
  staticTextDefaultValue: string;
}

interface ConfigurableItem {
  name: string;
  properties: Prop[];
  propertiesMapping: Record<string, unknown>;
  values: Record<string, unknown>;
}

const errorMessage = (error: unknown): string => {
  if (error instanceof Error) {
    return error.message;
  }
  if (
    typeof error === "object" &&
    error !== null &&
    "message" in error &&
    typeof error.message === "string"
  ) {
    return error.message;
  }
  return String(error);
};

export default defineComponent({
  name: "ProjectConfigurableForm",
  components: {
    PluginConfig,
  },
  props: {
    category: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      rundeckContext: {} as RundeckContext,
      pluginConfig: {} as { type?: string; props?: Prop[] },
      apiResponse: [] as ConfigurableItem[],
      isConfigSet: false,
      cacheConfig: { config: {} as Record<string, unknown> },
      massagedConfig: {} as Record<string, unknown>,
    };
  },
  async mounted() {
    await this.loadConfig();
  },
  methods: {
    async loadConfig() {
      try {
        const config = await getProjectConfigurable(
          window._rundeck.projectName,
          this.category,
        );
        this.apiResponse = (config.response["projectConfigurable"] ||
          []) as ConfigurableItem[];
        const properties: Prop[][] = [];
        this.apiResponse.forEach((item: ConfigurableItem) => {
          const itemName = item.name;
          item.properties.forEach((prop: Prop) => {
            const originalPropName = prop.name;
            prop.name = itemName + "." + prop.name;
            prop.title = this.$t(prop.title) as string;
            prop.description = this.$t(prop.description) as string;
            Object.entries(item.values).forEach(([key, value]) => {
              if (originalPropName === key) {
                this.massagedConfig[prop.name] = value;
              }
            });
          });
          properties.push(item.properties);
        });
        const resolvedProps: Prop[] = [];
        properties.forEach((item: Prop[]) => {
          item.forEach((prop: Prop) => {
            prop.desc = prop.description;
            resolvedProps.push({ ...prop, options: prop.renderingOptions });
          });
        });
        this.pluginConfig = {
          type: "ResourceModelSource",
          props: resolvedProps,
        };
        this.cacheConfig.config = this.massagedConfig;
        this.isConfigSet = true;
      } catch (err: unknown) {
        this.notifyError("Error loading config: " + errorMessage(err));
        console.error(err);
      }
    },
    async saveConfig() {
      const configsToSave = this.convertMapNumbersToStrings(
        this.transformConfigToMap(this.cacheConfig.config),
      );
      try {
        const resp = await setProjectConfigurable(
          window._rundeck.projectName,
          this.category,
          configsToSave,
        );
        if (resp.response === true) {
          this.notifySuccess("Success!", "Config saved successfully");
          await this.loadConfig();
        }
      } catch (err: unknown) {
        console.error("Full error object:", err);
        this.notifyError("Error saving config: " + errorMessage(err));
      }
    },
    convertMapNumbersToStrings(
      obj: Record<string, unknown>,
    ): Record<string, unknown> {
      for (const key in obj) {
        if (typeof obj[key] === "number") {
          // Convert number to string
          obj[key] = obj[key].toString();
        } else if (typeof obj[key] === "object" && obj[key] !== null) {
          // Recursively convert numbers to strings in nested objects
          obj[key] = this.convertMapNumbersToStrings(
            obj[key] as Record<string, unknown>,
          );
        }
      }
      return obj;
    },
    transformConfigToMap(config: Record<string, unknown>) {
      const resultMap: Record<string, Record<string, unknown>> = {};

      Object.entries(config).forEach(([key, value]) => {
        const [mainKey, subKey] = key.split(".");
        if (!resultMap[mainKey]) {
          resultMap[mainKey] = {};
        }
        resultMap[mainKey][subKey] = value;
      });

      return resultMap;
    },
    notifyError(msg: string) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0,
      });
    },

    notifySuccess(title: string, msg: string) {
      Notification.notify({
        type: "success",
        title: title,
        content: msg,
        duration: 5000,
      });
    },
  },
});
</script>
