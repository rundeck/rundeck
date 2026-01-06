<template>
  <modal v-model="modalShown" :title="title || $t('plugin.choose.title')" size="lg">
    <div class="modal-content-wrapper">
      <p class="text-heading--sm">{{ $t('searchForStep') }}</p>
      // todo: fix margin here
      <p class="text-body--sm text-body--muted">
        <a :href="searchPatternsLearnMoreUrl" target="_blank" rel="noopener noreferrer">{{ $t('learnMore') }}</a> {{ $t('learnMoreSearchPatterns') }}
      </p>
      <plugin-search
          v-if="showSearch"
          :ea="true"
          @search="filterLoadedServices"
      ></plugin-search>
      <select-button v-model="selectedService" :options="serviceOptions" optionLabel="name" optionValue="value"/>
      <p class="text-heading--lg section-heading">{{ sectionHeading }}</p>
      <p class="text-body--lg">
        {{ sectionDescription }}
        <a :href="sectionLearnMoreUrl" target="_blank" rel="noopener noreferrer">{{ $t('learnMore') }}</a>
      </p>
      <p class="text-heading--md subsection-heading">{{ commonStepsHeading }}</p>
      <div class="placeholder">
        <skeleton height="1.25rem" width="1.25rem" shape="rectangle" />
        <skeleton />
      </div>
    </div>
    <template v-if="Object.keys(groupedProviders).length > 0">
      <template v-for="(group, key) in groupedProviders" :key="key">
        <div v-if="group.isGroup" class="list-group-header text-body">
          <div v-if="group.iconUrl" class="img-icon">
            <img :src="group.iconUrl" />
          </div>
          {{key}} ({{ group.providers.length }} {{ $t('plugins') }})
        </div>
        <template v-else>
          <button
            v-for="prov in group.providers"
            :key="prov.name"
            class="list-group-item"
            data-test="provider-button"
            v-bind="dataStepType(selectedService, prov.name)"
            @click.prevent="chooseProviderAdd(selectedService, prov.name)"
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
      </template>
    </template>
    <template #footer>
      <btn data-testid="cancel-button" class="text-button" @click="$emit('cancel')">
        {{ $t("Cancel") }}
      </btn>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import { defineComponent } from "vue";
import PluginSearch from "@/library/components/plugins/PluginSearch.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { ServiceType } from "@/library/stores/Plugins";
import SelectButton from 'primevue/selectbutton';
import Skeleton from "primevue/skeleton";

const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginsEAModal",
  components: { PluginSearch, PluginInfo, SelectButton, Skeleton },
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
      loading: false,
      modalShown: false,
      searchQuery: "",
      selectedService: ServiceType.WorkflowNodeStep,
    };
  },
  computed: {
    serviceOptions() {
      return [
        {
          name: this.$t('nodeSteps'),
          value: ServiceType.WorkflowNodeStep,
        },
        {
          name: this.$t('workflowSteps'),
          value: ServiceType.WorkflowStep,
        }
      ];
    },
    sectionHeading() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t('workflowSteps')
        : this.$t('nodeSteps');
    },
    sectionDescription() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t('workflowStepsDescription')
        : this.$t('nodeStepsDescription');
    },
    sectionLearnMoreUrl() {
      // URLs will be populated later
      return this.selectedService === ServiceType.WorkflowStep
        ? ''
        : '';
    },
    searchPatternsLearnMoreUrl() {
      // URL will be populated later
      return '';
    },
    commonStepsHeading() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t('commonWorkflowSteps')
        : this.$t('commonNodeSteps');
    },
    filteredServices() {
      return this.loadedServices.map((service) => {
        const filteredProviders =
          service.providers?.filter((provider) =>
            this.matchesSearchQuery(provider),
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
    groupedProviders(): Object {
      const result = {};
      const activeService = this.filteredServices?.find(
        service => service.service === this.selectedService
      );

      if (!activeService) {
        return result;
      }

      // First, add highlighted providers (rendered individually)
      const highlighted = activeService.providers.filter(p => p.isHighlighted);
      const nonHighlighted = activeService.providers.filter(p => !p.isHighlighted);

      highlighted.forEach(provider => {
        result[provider.title] = {
          isGroup: false,
          iconUrl: provider.iconUrl,
          providers: [provider],
        };
      });

      // Then, process non-highlighted providers in order
      nonHighlighted.forEach(provider => {
        const groupBy = provider.providerMetadata?.groupBy;

        if (groupBy) {
          // Provider belongs to a group
          if (!result[groupBy]) {
            result[groupBy] = {
              isGroup: true,
              iconUrl: provider.providerMetadata?.groupIconUrl,
              providers: [],
            };
          }
          result[groupBy].providers.push(provider);
        } else {
          // Ungrouped provider - render individually
          result[provider.title] = {
            isGroup: false,
            iconUrl: provider.iconUrl,
            providers: [provider],
          };
        }
      });

      return result;
    }
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
    chooseProviderAdd(service: string, provider: string) {
      this.$emit("selected", { service, provider });
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
  },
});
</script>

<style lang="scss">
.modal-dialog {
  font-family: Inter, var(--fonts-body) !important;
}
.modal-body {
  padding-top: 0 !important;
}
</style>

<style scoped lang="scss">

.modal-content-wrapper {
  .text-heading--sm {
    margin-bottom: 4px;
  }

  .text-body--sm {
    margin-bottom: 10px;
  }

  .plugin-search {
    margin-bottom: 18px;
  }

  .p-selectbutton {
    margin-bottom: 24px;
  }

  .section-heading {
    margin-bottom: 8px;
  }

  .text-body--lg {
    margin-bottom: 14px;
  }

  .subsection-heading {
    margin-bottom: 12px;
  }
}

.placeholder {
  align-items: center;
  display: flex;
  gap: 0.5rem;
}

.img-icon {
  align-items: center;
  display: inline-flex;
  justify-content: center;
  height: 20px;
  width: 20px;

  img {
    height: auto;
    max-width: 80%;
  }
}
</style>
