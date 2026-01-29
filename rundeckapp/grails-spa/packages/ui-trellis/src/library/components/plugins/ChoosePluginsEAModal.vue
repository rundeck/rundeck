<template>
  <modal
    v-model="modalShown"
    :title="title || $t('plugin.choose.title')"
    size="lg"
  >
    <div class="modal-content-wrapper">
      <p class="text-heading--sm">{{ $t("searchForStep") }}</p>
      <p class="text-body--sm">
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
        :key="pluginSearchKey"
        :ea="true"
        class="plugin-search-container"
        @search="filterLoadedServices"
        @searching="handleSearching"
      />
      <pt-select-button
        v-model="selectedService"
        :options="serviceOptions"
        :option-label="getServiceOptionLabel"
        option-value="value"
      />

      <p class="text-heading--lg section-heading">{{ sectionHeading }}</p>
      <p class="text-body--lg">
        {{ sectionDescription }}
        <a
          :href="sectionLearnMoreUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ $t("learnMore") }}
        </a>
      </p>

      <transition name="view-transition" mode="out-in">
        <PluginAccordionList
          v-if="!showGroup && !(hasSearchQuery && hasNoResults)"
          key="accordion-list"
          :grouped-providers="groupedProviders"
          :loading="loading"
          :common-steps-heading="commonStepsHeading"
          :divider-title="dividerTitle"
          :search-query="searchQuery"
          @select="handleAccordionSelect"
        />
        <GroupedProviderDetail
          v-else-if="showGroup && !(hasSearchQuery && hasNoResults)"
          key="group-detail"
          :group="currentGroupForService"
          :group-name="selectedGroupName"
          :service-type-label="sectionHeading"
          :search-query="searchQuery"
          :empty-message="emptyGroupMessage"
          @select="handleGroupProviderSelect"
          @back="backToAllPlugins"
        />
        <div
          v-else-if="hasSearchQuery && hasNoResults"
          key="no-matches"
          class="no-matches-found"
        >
          <svg
            class="no-matches-icon"
            xmlns="http://www.w3.org/2000/svg"
            width="64"
            height="64"
            viewBox="0 0 64 64"
            fill="none"
          >
            <path
              fill-rule="evenodd"
              clip-rule="evenodd"
              d="M42.6667 25.3333C42.6667 15.7604 34.9063 8 25.3333 8C15.7604 8 8 15.7604 8 25.3333C8 34.9063 15.7604 42.6667 25.3333 42.6667C29.6267 42.6667 33.5733 41.0933 36.6133 38.5067L37.3333 39.2267V41.3333L50.6667 54.6667L54.6667 50.6667L41.3333 37.3333H39.2267L38.5067 36.6133C41.0933 33.5733 42.6667 29.6267 42.6667 25.3333ZM13.3333 25.3333C13.3333 18.6667 18.6667 13.3333 25.3333 13.3333C32 13.3333 37.3333 18.6667 37.3333 25.3333C37.3333 32 32 37.3333 25.3333 37.3333C18.6667 37.3333 13.3333 32 13.3333 25.3333Z"
              fill="#A1A1AA"
            />
          </svg>
          <p class="text-body--lg text-body--medium no-matches-text">
            {{ $t("noMatchesFound") }}
          </p>
          <p class="text-body--sm text-body--secondary">
            {{ $t("noMatchesFoundSecondary") }}
          </p>
        </div>
      </transition>
    </div>
    <template #footer>
      <div class="text-right">
        <PtButton
          outlined
          severity="secondary"
          :label="$t('Cancel')"
          data-testid="cancel-button"
          @click="$emit('cancel')"
        />
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
import PtButton from "@/library/components/primeVue/PtButton/PtButton.vue";

const context = getRundeckContext();

export default defineComponent({
  name: "ChoosePluginsEAModal",
  components: {
    PluginSearch,
    PluginInfo,
    PluginIcon,
    PtSelectButton,
    PtButton,
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
      selectedGroupName: "",
      pluginSearchKey: 0,
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
    serviceCounts() {
      const counts: Record<string, number> = {};
      this.filteredServices.forEach((service) => {
        counts[service.service] = service.providers?.length || 0;
      });
      return counts;
    },
    hasSearchQuery(): boolean {
      return !!this.searchQuery?.trim();
    },
    hasNoResults(): boolean {
      const activeService = this.filteredServices?.find(
        (service) => service.service === this.selectedService,
      );
      return !activeService || (activeService.providers?.length || 0) === 0;
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
    currentGroupForService() {
      // When viewing a group, find the group with the same name in the current service
      if (!this.showGroup || !this.selectedGroupName) {
        return this.selectedGroup;
      }

      // Look for the group in both highlighted and non-highlighted providers
      const allGroups = {
        ...this.groupedProviders.highlighted,
        ...this.groupedProviders.nonHighlighted,
      };

      const group = allGroups[this.selectedGroupName];
      
      // If group exists, return it; otherwise return empty group structure
      if (group) {
        return group;
      }

      // Return empty group structure to show empty message
      return {
        isGroup: true,
        iconDetail: this.selectedGroup?.iconDetail || {},
        providers: [],
      };
    },
    emptyGroupMessage(): string {
      // Return localized message for no steps available
      return `No ${this.sectionHeading} available`;
    },
  },
  watch: {
    modelValue(val) {
      this.modalShown = val;
      if (val === true) {
        this.searchQuery = "";
        this.backToAllPlugins();
        // Reset PluginSearch component by incrementing key
        this.pluginSearchKey += 1;
      }
    },
    modalShown(val) {
      this.$emit("update:modelValue", val);
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
        : this.checkMatch(provider, "title", value);
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
      this.selectedGroupName = "";
    },
    handleGroupProviderSelect(provider: any) {
      this.chooseProviderAdd(this.selectedService, provider.name);
    },
    shouldDisableGrouping(): boolean {
      // Disable grouping whenever there's an active search query
      // Show all search results not grouped
      return !!this.searchQuery?.trim();
    },
    getServiceOptionLabel(option: any): string {
      // Only show count when there's an active search query
      if (!this.hasSearchQuery) {
        return option.name;
      }
      const count = this.serviceCounts[option.value] || 0;
      return `${option.name} (${count})`;
    },
  },
});
</script>

<style lang="scss">
.modal-dialog,
.modal-dialog p,
.modal-dialog h4,
.modal-dialog button,
.modal-dialog a,
span:not(.glyphicon, .fa, .pi) {
  font-family: Inter, var(--fonts-body) !important;
}
.modal-dialog a {
  font-weight: var(--fontWeights-normal);
}

.modal-body {
  padding-top: 0 !important;
}

.modal-title {
  font-size: 24px !important;
  font-weight: 500 !important;
  line-height: var(--line-height-sm) !important;
}

.text-heading--sm,
.form-group {
  margin-bottom: var(--space-none);
}

.text-body--sm {
  margin-bottom: 4px;
}

.plugin-search {
  margin-bottom: 18px;
}

.plugin-search-container,
.p-selectbutton,
.text-body--lg {
  margin-bottom: 24px;
}

.section-heading {
  margin-bottom: 12px;
}

.subsection-heading {
  margin-bottom: 0px;
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

.no-matches-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-8);
  text-align: center;
}

.no-matches-icon {
  width: 48px;
  height: 48px;
  margin-bottom: var(--space-4);
}

.no-matches-text {
  color: var(--colors-gray-800);
  margin: 0 0 var(--space-2) 0;
}

.no-matches-found .text-body--sm {
  margin: 0;
}
</style>
<style scoped lang="scss">
.modal-content-wrapper {
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
      border-bottom: 1px solid var(--colors-gray-200);
      padding-left: 0;

      .p-accordionheader-toggle-icon {
        order: 2;
        margin-left: auto;
      }
    }
  }

  // Common accordion content styles shared by child components
  :deep(.accordion-header-content) {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 98%;
    min-height: 24px;
  }

  // Layout-only styles for accordion text containers (typography handled by typography.css classes)
  :deep(.accordion-title-text) {
    margin: 0;
    flex: 1;
    display: flex;
    align-items: center;
    gap: var(--space-1);
    min-width: 0; // Allows flex item to shrink below content size for ellipsis
    overflow: hidden; // Ensures child elements respect overflow
  }

  :deep(.accordion-title) {
    margin-left: 0 !important;
    flex-shrink: 0;
  }

  :deep(.img-icon) {
    align-items: center;
    display: inline-flex;
    justify-content: center;
    height: 20px;
    width: 20px;
    flex-shrink: 0;
  }
}
</style>
