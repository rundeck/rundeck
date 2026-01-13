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
        >
          {{ $t("learnMore") }}
        </a>
        {{ $t("learnMoreSearchPatterns") }}
      </p>
      <plugin-search
        v-if="showSearch"
        :ea="true"
        class="plugin-search-container"
        @search="filterLoadedServices"
        @searching="handleSearching"
      />
      <pt-select-button
        v-model="selectedService"
        :options="serviceOptions"
        option-label="name"
        option-value="value"
      />

      <p class="text-heading--lg section-heading">{{ sectionHeading }}</p>
      <p class="text-body--lg">
        {{ sectionDescription }}
        <a :href="sectionLearnMoreUrl" target="_blank" rel="noopener noreferrer">
          {{ $t("learnMore") }}
        </a>
      </p>

      <transition name="view-transition" mode="out-in">
        <PluginAccordionList
          v-if="!showGroup"
          key="accordion-list"
          :grouped-providers="groupedProviders"
          :loading="loading"
          :common-steps-heading="commonStepsHeading"
          :divider-title="dividerTitle"
          @select="handleAccordionSelect"
        />

        <GroupedProviderDetail
          v-else
          key="group-detail"
          :group="selectedGroup"
          :group-name="selectedGroupName"
          :service-type-label="sectionHeading"
          :search-query="searchQuery"
          @select="handleGroupProviderSelect"
          @back="backToAllPlugins"
        />
      </transition>
    </div>
    <template #footer>
      <div class="text-right">
        <btn
          data-testid="cancel-button"
          class="text-button"
          @click="$emit('cancel')"
        >
          {{ $t("Cancel") }}
        </btn>
      </div>
    </template>
  </modal>
</template>
<script lang="ts">
import { getRundeckContext } from "@/library";
import { defineComponent } from "vue";
import PluginSearch from "@/library/components/plugins/PluginSearch.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
import PluginAccordionList from "@/library/components/plugins/PluginAccordionList.vue";
import GroupedProviderDetail from "@/library/components/plugins/GroupedProviderDetail.vue";
import { ServiceType } from "@/library/stores/Plugins";
import { PtSelectButton } from "@/library/components/primeVue";

const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginsEAModal",
  components: {
    PluginSearch,
    PluginInfo,
    PluginIcon,
    PtSelectButton,
    PluginAccordionList,
    GroupedProviderDetail,
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
      showGroup: false,
      selectedGroup: null,
      selectedGroupName: '',
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

      // Check if we should disable grouping when there's a search query
      const shouldDisableGrouping = this.shouldDisableGrouping();

      // Then, process non-highlighted providers in order
      nonHighlighted.forEach((provider) => {
        const groupBy = provider.providerMetadata?.groupBy;

        // If grouping should be disabled, don't group providers
        if (shouldDisableGrouping || !groupBy) {
          // Ungrouped provider - render individually
          nonHighlightedResult[provider.title] = {
            isGroup: false,
            iconDetail: provider,
            providers: [provider],
          };
        } else {
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
      if (val === true) {
        this.searchQuery = "";
        this.backToAllPlugins();
      }
    },
    modalShown(val) {
      this.$emit("update:modelValue", val);
    },
    selectedService(newVal, oldVal) {
      if (this.showGroup && newVal !== oldVal) {
        this.backToAllPlugins();
      }
    },
    searchQuery(newVal) {
      // If user is viewing a group and searches, check if search matches the group
      if (this.showGroup && this.selectedGroup && newVal) {
        const hasMatch = this.selectedGroup.providers?.some((provider) =>
          this.matchesSearchQuery(provider),
        );
        
        // If search doesn't match any providers in the current group, fallback to all plugins
        if (!hasMatch) {
          this.backToAllPlugins();
        }
      }
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
      this.handleAccordionSelect({ group, key });
    },
    handleAccordionSelect({ group, key }) {
      if (group.isGroup) {
        this.showGroup = true;
        this.selectedGroup = group;
        this.selectedGroupName = key;
      } else {
        const provider = group.providers[0];
        this.chooseProviderAdd(this.selectedService, provider.name);
      }
    },
    backToAllPlugins() {
      this.showGroup = false;
      this.selectedGroup = null;
      this.selectedGroupName = '';
    },
    handleGroupProviderSelect(provider: any) {
      this.chooseProviderAdd(this.selectedService, provider.name);
    },
    shouldDisableGrouping(): boolean {
      // Disable grouping whenever there's an active search query
      // Show all search results not grouped
      return !!this.searchQuery?.trim();
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

.text-heading--sm,
.form-group {
  margin-bottom: 0px;
}

.text-body--sm {
  margin-bottom: 4px;
}

.plugin-search {
  margin-bottom: 18px;
}

.plugin-search-container,
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

// View transition animation
.view-transition-enter-active,
.view-transition-leave-active {
  transition: all 0.25s ease-out;
}

.view-transition-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.view-transition-leave-to {
  opacity: 0;
  transform: translateX(-20px);
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
