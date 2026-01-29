<template>
  <div class="group-detail-view">
    <Breadcrumb :model="breadcrumbItems" class="group-breadcrumb">
      <template #item="{ item }">
        <a v-if="item.command" @click="item.command" class="breadcrumb-link">
          {{ item.label }}
        </a>
        <span v-else class="breadcrumb-current">{{ item.label }}</span>
      </template>
    </Breadcrumb>

    <div class="group-header">
      <PluginIcon
        v-if="group"
        :detail="group.iconDetail"
        icon-class="group-icon"
      />
      <h3 class="group-title text-heading--lg">{{ groupName }}</h3>
      <Badge :value="filteredProviders.length" severity="secondary"/>
    </div>

    <div v-if="filteredProviders.length === 0" class="no-results">
      <p>{{ emptyMessage || $t("noResultsFound") }}</p>
    </div>

    <Accordion
      v-else
      :value="[]"
      multiple
      expandIcon="pi pi-chevron-right"
      collapseIcon="pi pi-chevron-right"
    >
      <AccordionPanel
        v-for="provider in filteredProviders"
        :key="provider.name"
        :value="provider.name"
      >
        <AccordionHeader @click="selectProvider(provider)">
          <div class="accordion-header-content">
            <PluginInfo
              :detail="provider"
              :show-icon="false"
              :show-description="true"
              :show-extended="false"
              description-css="accordion-description text-body--secondary"
              title-css="accordion-title text-body"
              class="accordion-title-text text-body"
            >
              <template #descriptionprefix>
                <span class="accordion-description-separator text-body--secondary"> - </span>
              </template>
            </PluginInfo>
          </div>
        </AccordionHeader>
      </AccordionPanel>
    </Accordion>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import Breadcrumb from "primevue/breadcrumb";
import Accordion from "primevue/accordion";
import AccordionPanel from "primevue/accordionpanel";
import AccordionHeader from "primevue/accordionheader";
import Badge from "primevue/badge";
import "@/library/components/primeVue/Badge/badge.scss";

export default defineComponent({
  name: "GroupedProviderDetail",
  components: {
    PluginIcon,
    PluginInfo,
    Breadcrumb,
    Accordion,
    AccordionPanel,
    AccordionHeader,
    Badge
  },
  props: {
    group: {
      type: Object,
      required: true,
    },
    groupName: {
      type: String,
      required: true,
    },
    serviceTypeLabel: {
      type: String,
      required: true,
    },
    searchQuery: {
      type: String,
      default: "",
    },
    emptyMessage: {
      type: String,
      default: "",
    },
  },
  emits: ["select", "back"],
  computed: {
    breadcrumbItems() {
      return [
        {
          label: `${this.$t("all")} ${this.serviceTypeLabel} ${this.$t("plugins")}`,
          command: () => this.$emit("back"),
        },
        {
          label: this.groupName,
        },
      ];
    },
    filteredProviders() {
      if (!this.group || !this.group.providers) {
        return [];
      }

      if (!this.searchQuery) {
        return this.group.providers;
      }

      // Use parent's matchesSearchQuery logic
      return this.group.providers.filter((provider) =>
        this.matchesSearchQuery(provider)
      );
    },
  },
  methods: {
    selectProvider(provider: any) {
      this.$emit("select", provider);
    },
    matchesSearchQuery(provider: any) {
      if (!this.searchQuery) return true;

      const filterValue = this.searchQuery.toLowerCase().split("=");
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
    checkMatch(obj: any, field: string, val: string) {
      return obj[field] && val && obj[field].toLowerCase().indexOf(val) >= 0;
    },
  },
});
</script>

<style scoped lang="scss">
.group-breadcrumb {
  margin-bottom: 16px;
  padding: 0;
}

.p-breadcrumb-list {
  margin-bottom: 0;
}

.breadcrumb-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--colors-blue-600);
  cursor: pointer;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

:deep(.p-breadcrumb-separator) {
  color: var(--colors-blue-600);
}
.breadcrumb-current {
  color: var(--colors-gray-800-original);
  text-transform: uppercase;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-icon {
  height: 24px;
  text-align: center;
  width: 24px;
}

.group-title {
  margin: 0;
}

.group-count {
  color: var(--colors-gray-600);
  font-size: 14px;
}

.no-results {
  padding: 32px;
  text-align: center;
  color: var(--colors-gray-600);
}

.p-badge {
  width: 21px;
  height: 21px;
  font-size: 10.5px !important;
  line-height: var(--line-height-sm);
}

// Ensure description text truncates with ellipsis when it exceeds one line
:deep(.accordion-description) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 1;
  min-width: 0; // Allows flex item to shrink below content size
}

// Ensure the accordion title text container allows description to shrink
:deep(.accordion-title-text) {
  min-width: 0; // Allows flex item to shrink below content size
  overflow: hidden; // Ensures child elements respect overflow
}
</style>
