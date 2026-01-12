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

    <!-- Highlighted providers accordion -->
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
      expandIcon="pi pi-chevron-right"
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
      expandIcon="pi pi-chevron-right"
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
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
import Skeleton from "primevue/skeleton";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import AccordionContent from "primevue/accordioncontent";

export default defineComponent({
  name: "PluginAccordionList",
  components: {
    PluginIcon,
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
  },
  emits: ["select"],
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
