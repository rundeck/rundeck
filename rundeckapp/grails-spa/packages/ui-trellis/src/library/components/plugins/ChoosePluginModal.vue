<template>
  <modal v-model="modalShown" :title="title || $t('plugin.choose.title')" size="lg">
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
          <slot name="listHeader" :service="service.service" />
          <template
            v-for="(prov, index) in service.providers"
            :key="`providerItem${index}`"
          >
            <p
              v-if="service.dividerIndex > 0 && index === service.dividerIndex"
              data-testid="divider"
              class="list-group-item text-info text-strong"
            >
              {{ dividerTitle(service) }}
            </p>
            <button
              class="list-group-item"
              data-test="provider-button"
              v-bind="dataStepType(service.service, prov.name)"
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
    <div
      v-else-if="filteredServices.length === 1"
      class="list-group"
      data-testid="list-view"
    >
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
      <btn data-testid="cancel-button" @click="$emit('cancel')">
        {{ $t("Cancel") }}
      </btn>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import pluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { defineComponent } from "vue";
import PluginSearch from "@/library/components/plugins/PluginSearch.vue";
import { ServiceType } from "@/library/stores/Plugins";

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
    excludeProviders: {
      type: Array as () => string[],
      default: () => [],
    },
  },
  emits: ["cancel", "selected", "update:modelValue"],
  data() {
    return {
      loadedServices: [],
      loading: false,
      modalShown: false,
      searchQuery: "",
    };
  },
  computed: {
    filteredServices() {
      return this.loadedServices.map((service) => {
        const filteredProviders =
          service.providers?.filter((provider) =>
            this.matchesSearchQuery(provider) &&
            !this.excludeProviders.includes(provider.name),
          ) || [];
        return {
          ...service,
          providers: filteredProviders,
          dividerIndex: this.showDivider
            ? this.calculateDividerIndex(filteredProviders)
            : undefined,
        };
      });
    },
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
      };
    });
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
    filterLoadedServices(searchQuery: string) {
      this.searchQuery = searchQuery.toLowerCase();
    },
    matchesSearchQuery(provider) {
      if (!this.searchQuery) return true;
      const filterValue = this.searchQuery.split("=");
      const prop = filterValue.length > 1 ? filterValue[0] : "title";
      const value = filterValue.length > 1 ? filterValue[1] : filterValue[0];
      const propertyFilterValue = prop.split(":") || undefined;

      const filterByProps =
        propertyFilterValue && propertyFilterValue.length === 2;

      return filterByProps
        ? this.checkMatch(provider, propertyFilterValue[1], value)
        : this.checkMatch(provider, "title", value) ||
            this.checkMatch(provider, "name", value) ||
            this.checkMatch(provider, "description", value);
    },
    checkMatch(obj, field: string, val: string) {
      return obj[field] && val && obj[field].toLowerCase().indexOf(val) >= 0;
    },
    calculateDividerIndex(providers: any) {
      return providers.findIndex(
        (provider) => provider.isHighlighted === false,
      );
    },
    dividerTitle(service: any): string {
      if (service.dividerIndex && service.dividerIndex > 0) {
        const numberOfPluginsNotHighlighted: number =
          service.providers.length - service.dividerIndex;
        let titleString: string = "node.step.plugin.plural";
        if (service.service === ServiceType.WorkflowStep) {
          titleString = "workflow.step.plugin.plural";
        }
        return this.$t(titleString, [numberOfPluginsNotHighlighted]);
      }
      return "";
    },
    dataStepType(service: string, name: string) {
      const servicesWithDataStep = {
        [ServiceType.WorkflowStep]: "data-step-type",
        [ServiceType.WorkflowNodeStep]: "data-node-step-type",
      };

      if (!Object.keys(servicesWithDataStep).includes(service)) {
        return {};
      } else {
        return {
          [servicesWithDataStep[service]]: name,
        };
      }
    },
  },
});
</script>
