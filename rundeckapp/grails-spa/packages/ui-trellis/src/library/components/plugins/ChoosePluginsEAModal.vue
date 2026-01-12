<template>
  <modal
    v-model="modalShown"
    :title="title || $t('plugin.choose.title')"
    size="lg"
  >
    <div class="modal-content-wrapper">
      <p class="text-heading--sm">{{ $t("searchForStep") }}</p>
      <p class="text-body--sm text-body--muted">
        <a
          :href="searchPatternsLearnMoreUrl"
          target="_blank"
          rel="noopener noreferrer"
          >{{ $t("learnMore") }}</a
        >
        {{ $t("learnMoreSearchPatterns") }}
      </p>
      <plugin-search
        v-if="showSearch"
        :ea="true"
        @search="filterLoadedServices"
        @searching="handleSearching"
      ></plugin-search>
      <pt-select-button
        v-model="selectedService"
        :options="serviceOptions"
        option-label="name"
        option-value="value"
      />
      <p class="text-heading--lg section-heading">{{ sectionHeading }}</p>
      <p class="text-body--lg">
        {{ sectionDescription }}
        <a
          :href="sectionLearnMoreUrl"
          target="_blank"
          rel="noopener noreferrer"
          >{{ $t("learnMore") }}</a
        >
      </p>
      <div v-if="loading" class="placeholder">
        <skeleton height="1.25rem" width="1.25rem" shape="rectangle" />
        <skeleton />
      </div>
    </div>
    <p
      v-if="!loading && Object.keys(groupedProviders.highlighted).length > 0"
      class="text-heading--md subsection-heading"
    >
      {{ commonStepsHeading }}
    </p>
    <Accordion
      v-if="!loading && Object.keys(groupedProviders.highlighted).length > 0"
      :value="[]"
      multiple
      expandIcon="pi pi-chevron-down"
      collapseIcon="pi pi-chevron-up"
    >
      <AccordionPanel
        v-for="(group, key) in groupedProviders.highlighted"
        :key="key"
        :value="key"
      >
        <AccordionHeader @click="handleAccordionClick(group, key)">
          <div class="accordion-header-content">
            <PluginIcon :detail="group.iconDetail" icon-class="img-icon" />
            <span class="accordion-title">{{ key }}</span>
            <span v-if="group.isGroup" class="provider-count">
              ({{ group.providers.length }} {{ $t("plugins") }})
            </span>
          </div>
        </AccordionHeader>
      </AccordionPanel>
    </Accordion>

    <!-- Divider title -->
    <p
      v-if="!loading && dividerTitle"
      class="text-heading--md subsection-heading divider-title"
    >
      {{ dividerTitle }}
    </p>

    <!-- Non-highlighted providers accordion -->
    <Accordion
      v-if="!loading && Object.keys(groupedProviders.nonHighlighted).length > 0"
      :value="[]"
      multiple
      expandIcon="pi pi-chevron-down"
      collapseIcon="pi pi-chevron-up"
    >
      <AccordionPanel
        v-for="(group, key) in groupedProviders.nonHighlighted"
        :key="key"
        :value="key"
      >
        <AccordionHeader @click="handleAccordionClick(group, key)">
          <div class="accordion-header-content">
            <PluginIcon :detail="group.iconDetail" icon-class="img-icon" />
            <span class="accordion-title">{{ key }}</span>
            <span v-if="group.isGroup" class="provider-count">
              ({{ group.providers.length }} {{ $t("plugins") }})
            </span>
          </div>
        </AccordionHeader>
        <AccordionContent v-if="group.isGroup">
          <p class="text-body--sm">
            Grouped provider layout (to be implemented)
          </p>
        </AccordionContent>
      </AccordionPanel>
    </Accordion>
    <template #footer>
      <btn
        data-testid="cancel-button"
        class="text-button"
        @click="$emit('cancel')"
      >
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
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
import { ServiceType } from "@/library/stores/Plugins";
import { PtSelectButton } from "@/library/components/primeVue";
import Skeleton from "primevue/skeleton";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";

const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginsEAModal",
  components: {
    PluginSearch,
    PluginInfo,
    PluginIcon,
    PtSelectButton,
    Skeleton,
    Accordion,
    AccordionPanel,
    AccordionHeader,
    AccordionContent,
  },
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
      groupExpanded: false,
    };
  },
  computed: {
    serviceOptions() {
      return [
        {
          name: this.$t("nodeSteps"),
          value: ServiceType.WorkflowNodeStep,
        },
        {
          name: this.$t("workflowSteps"),
          value: ServiceType.WorkflowStep,
        },
      ];
    },
    sectionHeading() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t("workflowSteps")
        : this.$t("nodeSteps");
    },
    sectionDescription() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t("workflowStepsDescription")
        : this.$t("nodeStepsDescription");
    },
    sectionLearnMoreUrl() {
      // URLs will be populated later
      return this.selectedService === ServiceType.WorkflowStep ? "" : "";
    },
    searchPatternsLearnMoreUrl() {
      // URL will be populated later
      return "";
    },
    commonStepsHeading() {
      return this.selectedService === ServiceType.WorkflowStep
        ? this.$t("commonWorkflowSteps")
        : this.$t("commonNodeSteps");
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
    groupedProviders(): { highlighted: Object; nonHighlighted: Object } {
      const highlightedResult = {};
      const nonHighlightedResult = {};
      const activeService = this.filteredServices?.find(
        (service) => service.service === this.selectedService,
      );

      if (!activeService) {
        return {
          highlighted: highlightedResult,
          nonHighlighted: nonHighlightedResult,
        };
      }

      // First, add highlighted providers (rendered individually)
      const highlighted = activeService.providers.filter(
        (p) => p.isHighlighted,
      );
      const nonHighlighted = activeService.providers.filter(
        (p) => !p.isHighlighted,
      );

      highlighted.forEach((provider) => {
        highlightedResult[provider.title] = {
          isGroup: false,
          iconDetail: provider,
          providers: [provider],
        };
      });

      // Then, process non-highlighted providers in order
      nonHighlighted.forEach((provider) => {
        const groupBy = provider.providerMetadata?.groupBy;

        if (groupBy) {
          // Provider belongs to a group
          if (!nonHighlightedResult[groupBy]) {
            nonHighlightedResult[groupBy] = {
              isGroup: true,
              iconDetail: {
                iconUrl: provider.providerMetadata?.groupIconUrl,
                providerMetadata: {},
              },
              providers: [],
            };
          }
          nonHighlightedResult[groupBy].providers.push(provider);
        } else {
          // Ungrouped provider - render individually
          nonHighlightedResult[provider.title] = {
            isGroup: false,
            iconDetail: provider,
            providers: [provider],
          };
        }
      });

      return {
        highlighted: highlightedResult,
        nonHighlighted: nonHighlightedResult,
      };
    },
    dividerTitle(): string {
      // Count total providers, not accordion items
      let totalProviders = 0;
      Object.values(this.groupedProviders.nonHighlighted).forEach(
        (group: any) => {
          totalProviders += group.providers.length;
        },
      );

      if (totalProviders === 0) {
        return "";
      }

      let titleString = "node.step.plugin.plural";
      if (this.selectedService === ServiceType.WorkflowStep) {
        titleString = "workflow.step.plugin.plural";
      }
      return this.$t(titleString, [totalProviders]);
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
    handleSearching(isSearching: boolean) {
      this.loading = isSearching;
    },
    handleAccordionClick(group: any, key: string) {
      if (group.isGroup) {
        // For grouped providers: set flag for future layout
        this.groupExpanded = true;
        // Future: show different layout for group selection
      } else {
        // For single providers: emit selected event
        const provider = group.providers[0];
        this.chooseProviderAdd(this.selectedService, provider.name);
        // Note: chooseProviderAdd already emits 'selected' which closes the modal
      }
    },
  },
});
</script>

<style lang="scss">
.modal-dialog,
.modal-dialog p,
.modal-dialog h4,
.modal-dialog button,
span:not(.glyphicon, .fa, .pi) {
  font-family: Inter, var(--fonts-body) !important;
}
.modal-body {
  padding-top: 0 !important;
}

.modal-title {
  font-size: 24px !important;
}

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

.divider-title {
  margin-top: 16px;
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
}

.accordion-header-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.accordion-title {
  font-weight: var(--fontWeights-medium);
}

.provider-count {
  color: var(--colors-gray-600);
  margin-left: 0.25rem;
}
</style>
<style scoped lang="scss">
:deep(.p-accordion) {
  .p-accordionpanel {
    border: none;
    box-shadow: none;

    &:last-child .p-accordionheader {
      border-bottom: none !important;
    }
  }

  .p-accordionheader {
    background: var(--colors-white);
    border: none;
    border-bottom: 1px solid var(--colors-gray-300);
    padding-left: 0;

    .p-accordionheader-toggle-icon {
      order: 2;
      margin-left: auto;
    }
  }
}
</style>
