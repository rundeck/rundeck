<template>
  <modal v-model="modalShown" :title="title || $t('plugin.choose.title')">
    <slot></slot>
    <plugin-search
      v-if="showSearch"
      @search="filterLoadedServices"
    ></plugin-search>

    <div v-if="loading">
      <i class="fas fa-spinner fa-spin" data-testid="loading-text"></i>
      {{ $t("loading.text") }}
    </div>
    <tabs v-else-if="filteredServices.length > 1" class="vue-tabs">
      <tab
        v-for="(service, i) in filteredServices"
        :key="service.service"
        :title="tabTitle(service.service, i)"
      >
        <div class="list-group">
          <template
            v-for="(prov, index) in service.providers"
            :key="`providerItem${index}`"
          >
            <p
              v-if="service.dividerIndex > 0 && index === service.dividerIndex"
              class="list-group-item text-info text-strong"
            >
              {{ dividerTitle(service) }}
            </p>
            <button
              class="list-group-item"
              data-testid="provider-button"
              @click.prevent="chooseProviderAdd(service.service, prov.name)"
            >
              <plugin-info
                :detail="prov"
                :show-description="true"
                :show-extended="false"
              >
                <template #descriptionprefix> - </template>
              </plugin-info>
            </button>
          </template>
        </div>
      </tab>
    </tabs>
    <div v-else-if="filteredServices.length === 1" class="list-group">
      <button
        v-for="prov in filteredServices[0].providers"
        class="list-group-item"
        @click.prevent="
          chooseProviderAdd(filteredServices[0].service, prov.name)
        "
      >
        <plugin-info
          :detail="prov"
          :show-description="true"
          :show-extended="false"
          ><template #descriptionprefix> - </template>
        </plugin-info>
      </button>
    </div>
    <template #footer>
      <btn data-testid="cancel-button" @click="$emit('cancel')">{{
        $t("Cancel")
      }}</btn>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { defineComponent } from "vue";
import PluginSearch from "@/library/components/plugins/PluginSearch.vue";
const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginModal",
  components: { PluginSearch, pluginInfo },
  props: {
    title: {
      type: String,
      required: true,
    },
    services: {
      type: Array,
      required: true,
    },
    tabNames: {
      type: Array,
      required: false,
      default: () => [],
    },
    modelValue: {
      type: Boolean,
      required: false,
      default: false,
    },
    showSearch: {
      type: Boolean,
      default: false,
    },
    showDivider: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["cancel", "selected", "update:modelValue"],
  data() {
    return {
      loadedServices: [],
      filteredServices: [],

      loading: false,
      modalShown: false,
      stepFilterValue: "",
    };
  },
  watch: {
    modelValue(val) {
      this.modalShown = val;
    },
    modalShown(val) {
      this.$emit("update:modelValue", val);
    },
  },
  async mounted() {
    this.loading = true;
    for (const service of this.services) {
      await context.rootStore.plugins.load(service);
    }
    this.loadedServices = this.services.map((service: string) => {
      const providers = context.rootStore.plugins.getServicePlugins(service);
      return {
        service,
        providers,
        dividerIndex: this.showDivider
          ? this.getDividerPosition(providers)
          : undefined,
      };
    });

    this.filteredServices = this.loadedServices;
    this.loading = false;
    this.modalShown = this.modelValue;
  },
  methods: {
    tabTitle(service: string, i: number) {
      const name =
        this.tabNames && this.tabNames.length > i
          ? this.tabNames[i]
          : $t("plugin.type." + service + ".title.plural") || service;
      const count =
        this.filteredServices.find((s) => s.service === service)?.providers
          .length || 0;
      return name + " (" + count + ")";
    },
    chooseProviderAdd(service: string, provider: string) {
      this.$emit("selected", { service, provider });
      this.active = false;
    },
    findProvider(name: string) {
      return this.services.find((s) => s.name === name);
    },
    filterLoadedServices(searchQuery: string) {
      const filterValue = searchQuery.toLowerCase().split("=");
      const prop = filterValue.length > 1 ? filterValue[0] : "title";
      const value = filterValue.length > 1 ? filterValue[1] : filterValue[0];
      const propertyFilterValue = prop.split(":") || undefined;

      const filterByProps =
        propertyFilterValue && propertyFilterValue.length === 2;

      if (!value) {
        this.filteredServices = this.loadedServices;
      } else {
        if (!filterByProps) {
          this.filteredServices = this.loadedServices.map((service) => {
            const filteredProviders = service.providers.filter(
              (provider) =>
                this.checkMatch(provider, "title", value) ||
                this.checkMatch(provider, "name", value) ||
                this.checkMatch(provider, "description", value),
            );
            return {
              ...service,
              providers: filteredProviders,
              dividerIndex: this.showDivider
                ? this.getDividerPosition(filteredProviders)
                : undefined,
            };
          });
        } else if (filterByProps) {
          this.filteredServices = this.loadedServices.map((service) => {
            const filteredProviders = service.providers.filter((provider) =>
              this.checkMatch(provider, propertyFilterValue[1], value),
            );
            return {
              ...service,
              providers: filteredProviders,
              dividerIndex: this.showDivider
                ? this.getDividerPosition(filteredProviders)
                : undefined,
            };
          });
        }
      }
    },
    checkMatch(obj, field: string, val: string) {
      return obj[field] && val && obj[field].toLowerCase().indexOf(val) >= 0;
    },
    getDividerPosition(providers: any) {
      return providers.findIndex(
        (provider) => provider.isHighlighted === false,
      );
    },
    dividerTitle(service: any): string {
      if (service.dividerIndex && service.dividerIndex > 0) {
        const numberOfPluginsNotHighlighted: number =
          service.providers.length - service.dividerIndex;
        let titleString: string = "node.step.plugin.plural";
        if (service === "WorkflowStep") {
          titleString = "workflow.step.plugin.plural";
        }
        return this.$t(titleString, [numberOfPluginsNotHighlighted]);
      }
      return "";
    },
  },
});
</script>

<style scoped lang="scss"></style>
