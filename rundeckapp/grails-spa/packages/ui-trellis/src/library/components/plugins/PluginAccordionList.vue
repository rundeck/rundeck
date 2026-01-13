<template>
  <div>
    <!-- Loading placeholders -->
    <div v-if="loading">
      <p class="text-heading--md subsection-heading">{{ commonStepsHeading }}</p>
      <div class="placeholder-group">
        <div v-for="n in 4" :key="'common-' + n" class="placeholder">
          <skeleton height="20px" width="20px" shape="rectangle" />
          <skeleton height="20px" width="435px" shape="rectangle" />
        </div>
      </div>
      <p class="text-heading--md subsection-heading divider-title">
        {{ commonStepsHeading }}
      </p>
      <div class="placeholder-group">
        <div v-for="n in 3" :key="'other-' + n" class="placeholder">
          <skeleton height="20px" width="20px" shape="rectangle" />
          <skeleton height="20px" width="435px" shape="rectangle" />
        </div>
      </div>
    </div>

    <div v-if="!loading" :key="providersKey">
        <!-- No results message when searching -->
        <div
          v-if="hasSearchQuery && hasNoResults"
          class="no-results"
        >
          <p>{{ $t("noResultsFound") }}</p>
        </div>

        <!-- Highlighted providers accordion -->
        <p
          v-if="Object.keys(groupedProviders.highlighted).length > 0"
          class="text-heading--md subsection-heading"
        >
          {{ commonStepsHeading }}
        </p>
        <Accordion
          v-if="Object.keys(groupedProviders.highlighted).length > 0"
          :value="[]"
          multiple
          expandIcon="pi pi-chevron-right"
          collapseIcon="pi pi-chevron-right"
        >
          <AccordionPanel
            v-for="(group, key) in groupedProviders.highlighted"
            :key="key"
            :value="key"
          >
            <AccordionHeader @click="handleAccordionClick(group, key)">
              <div class="accordion-header-content">
                <PluginIcon :detail="group.iconDetail" icon-class="img-icon" />
                <div v-if="group.isGroup" class="accordion-title-text">
                  <span class="accordion-title">{{ key }}</span>
                  <span class="provider-count">
                    ({{ group.providers.length }} {{ $t("plugins") }})
                  </span>
                </div>
                <PluginInfo
                  v-else-if="group.providers && group.providers.length > 0"
                  :detail="group.providers[0]"
                  :show-icon="false"
                  :show-description="true"
                  :show-extended="false"
                  description-css="accordion-description"
                  title-css="accordion-title"
                  class="accordion-title-text"
                >
                  <template #descriptionprefix>
                    <span class="accordion-description-separator"> - </span>
                  </template>
                </PluginInfo>
              </div>
            </AccordionHeader>
          </AccordionPanel>
        </Accordion>

        <!-- Divider title -->
        <p
          v-if="dividerTitle"
          class="text-heading--md subsection-heading divider-title"
        >
          {{ dividerTitle }}
        </p>

        <!-- Non-highlighted providers accordion -->
        <Accordion
          v-if="Object.keys(groupedProviders.nonHighlighted).length > 0"
          :value="[]"
          multiple
          expandIcon="pi pi-chevron-right"
          collapseIcon="pi pi-chevron-right"
        >
          <AccordionPanel
            v-for="(group, key) in groupedProviders.nonHighlighted"
            :key="key"
            :value="key"
          >
            <AccordionHeader @click="handleAccordionClick(group, key)">
              <div class="accordion-header-content">
                <PluginIcon :detail="group.iconDetail" icon-class="img-icon" />
                <div v-if="group.isGroup" class="accordion-title-text">
                  <span class="accordion-title">{{ key }}</span>
                  <span class="provider-count">
                    ({{ group.providers.length }} {{ $t("plugins") }})
                  </span>
                </div>
                <PluginInfo
                  v-else-if="group.providers && group.providers.length > 0"
                  :detail="group.providers[0]"
                  :show-icon="false"
                  :show-description="true"
                  :show-extended="false"
                  description-css="accordion-description"
                  title-css="accordion-title"
                  class="accordion-title-text"
                >
                  <template #descriptionprefix>
                    <span class="accordion-description-separator"> - </span>
                  </template>
                </PluginInfo>
              </div>
            </AccordionHeader>
            <AccordionContent v-if="group.isGroup">
              <p class="text-body--sm">
                Grouped provider layout (to be implemented)
              </p>
            </AccordionContent>
          </AccordionPanel>
        </Accordion>
      </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import Skeleton from "primevue/skeleton";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";

export default defineComponent({
  name: "PluginAccordionList",
  components: {
    PluginIcon,
    PluginInfo,
    Skeleton,
    Accordion,
    AccordionPanel,
    AccordionHeader,
    AccordionContent,
  },
  props: {
    groupedProviders: {
      type: Object,
      required: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    commonStepsHeading: {
      type: String,
      required: true,
    },
    dividerTitle: {
      type: String,
      default: "",
    },
    searchQuery: {
      type: String,
      default: "",
    },
  },
  emits: ["select"],
  computed: {
    providersKey() {
      // Create a unique key based on the groupedProviders to trigger transition
      const highlightedKeys = Object.keys(this.groupedProviders.highlighted || {}).sort().join(",");
      const nonHighlightedKeys = Object.keys(this.groupedProviders.nonHighlighted || {}).sort().join(",");
      return `${highlightedKeys}|${nonHighlightedKeys}`;
    },
    hasSearchQuery(): boolean {
      return !!this.searchQuery?.trim();
    },
    hasNoResults(): boolean {
      const highlightedCount = Object.keys(this.groupedProviders.highlighted || {}).length;
      const nonHighlightedCount = Object.keys(this.groupedProviders.nonHighlighted || {}).length;
      return highlightedCount === 0 && nonHighlightedCount === 0;
    },
  },
  methods: {
    handleAccordionClick(group: any, key: string) {
      this.$emit("select", { group, key });
    },
  },
});
</script>

<style lang="scss">
.placeholder-group {
  .placeholder {
    align-items: center;
    display: flex;
    gap: 0.5rem;
    border-bottom: 1px solid var(--colors-gray-300);
    padding: 16px 0;

    &:last-child {
      border-bottom: none;
    }
  }
}

.img-icon {
  align-items: center;
  display: inline-flex;
  justify-content: center;
  height: 20px;
  width: 20px;
  flex-shrink: 0;
}

.accordion-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.accordion-title-text {
  font-family: Inter, var(--fonts-body);
  font-size: 14px;
  font-weight: 400;
  line-height: 14px;
  color: #27272a;
  margin: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.accordion-title {
  font-weight: var(--fontWeights-medium);
  color: #27272a;
}

.accordion-description {
  color: #71717a;
  font-weight: 400;
}

.accordion-description-separator {
  color: #71717a;
}

.provider-count {
  color: var(--colors-gray-600);
  margin-left: 0.25rem;
}

.no-results {
  padding: 32px;
  text-align: center;
  color: var(--colors-gray-600);
}
</style>
