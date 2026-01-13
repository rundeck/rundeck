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
      <h3 class="group-title">{{ groupName }}</h3>
      <Badge :value="filteredProviders.length" />
    </div>

    <div v-if="filteredProviders.length === 0" class="no-results">
      <p>{{ $t("noResultsFound") }}</p>
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
            <PluginIcon :detail="provider" icon-class="img-icon" />
            <span class="accordion-title">{{ provider.title }}</span>
          </div>
        </AccordionHeader>
      </AccordionPanel>
    </Accordion>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import PluginIcon from "@/library/components/plugins/PluginIcon.vue";
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

.breadcrumb-current {
  color: var(--colors-gray-700);
  font-weight: var(--fontWeights-medium);
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
  font-size: 20px;
  font-weight: var(--fontWeights-medium);
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
