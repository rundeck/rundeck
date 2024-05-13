<template>
  <span>
    <btn
      v-if="hasQuery && (!query || !query.filterName)"
      size="xs"
      type="default"
      data-test-id="save-filter-button"
      @click="saveFilterPrompt"
    >
      {{ $t("filter.save.button") }}
    </btn>
    <span v-if="query && query.filterName" data-test-id="filter-name">{{
      query.filterName
    }}</span>

    <dropdown v-if="filters && filters.length > 0">
      <span
        class="dropdown-toggle btn btn-secondary btn-sm"
        :class="query && query.filterName ? 'text-info' : 'text-secondary'"
      >
        {{ $t("Filters") }}
        <span class="caret"></span>
      </span>
      <template #dropdown>
        <li v-if="query && query.filterName">
          <a
            role="button"
            data-test-id="delete-filter-btn"
            @click="deleteFilter"
          >
            <i class="glyphicon glyphicon-trash"></i>
            {{ $t("filter.delete.named.text", [query.filterName]) }}
          </a>
        </li>
        <li
          v-if="query && query.filterName"
          role="separator"
          class="divider"
        ></li>
        <li class="dropdown-header">
          <i class="glyphicon glyphicon-filter"></i>
          {{ $t("saved.filters") }}
        </li>
        <li
          v-for="filter in filters"
          :key="filter.filterName"
          data-test-id="filter-item"
        >
          <a role="button" @click="selectFilter(filter)">
            {{ filter.filterName }}
            <span v-if="query && filter.filterName === query.filterName"
            data-testid="checkmark-span"
              >√</span
            >
          </a>
        </li>
      </template>
    </dropdown>
    <span v-else data-test-id="no-filters-message"> No filters available </span>
  </span>
</template>
<script lang="ts">
import { ActivityFilterStore } from "../../../library/stores/ActivityFilterStore";
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../library";
import { MessageBox, Notification } from "uiv";

export default defineComponent({
  props: {
    hasQuery: {
      type: Boolean,
      required: true,
    },
    query: {
      type: Object,
      required: true,
    },
    eventBus: {
      type: Object,
      required: true,
    },
  },
  emits: ["select_filter"],
  data() {
    return {
      projectName: "",
      loadError: "",
      filters: [],
      promptTitle: this.$t("Save Filter"),
      promptContent: this.$t("filter.save.name.prompt"),
      promptError: this.$t("filter.save.validation.name.blank"),
      filterStore: new ActivityFilterStore(),
    };
  },
  mounted() {
    console.log(this.filters);
    console.log(this.$el.querySelectorAll('[data-test-id="filter-item"]'));
    this.projectName = getRundeckContext().projectName;
    this.loadFilters();

    this.eventBus &&
      this.eventBus.on("invoke-save-filter", this.saveFilterPrompt);
  },
  beforeUnmount() {
    this.eventBus && this.eventBus.off("invoke-save-filter");
  },
  methods: {
    notifyError(msg) {
      Notification.notify({
        type: "danger",
        title: "An Error Occurred",
        content: msg,
        duration: 0,
      });
    },
    async loadFilters() {
      console.log("loadFilters is called");
      this.filters =
        this.filterStore.loadForProject(this.projectName).filters || [];
    },
    selectFilter(filter) {
      this.$emit("select_filter", filter);
    },
    deleteFilter() {
      console.log("Before deletion:", this.filters);
      console.log("deleteFilter was called");
      if (!this.query || !this.query.filterName) {
        return;
      }

      this.$confirm({
        title: this.$t("Delete Saved Filter"),
        content: this.$t("filter.delete.confirm.text", [this.query.filterName]),
      })

        .then(() => {
          this.doDeleteFilter(this.query.filterName);
          console.log("After deletion:", this.filters);
        })

        .catch(() => {
          //this.$notify("Delete canceled.");
        });
    },
    async doDeleteFilter(name) {
      this.filterStore.removeFilter(this.projectName, name);
      console.log("Immediately after deletion:", this.filters);
      await this.loadFilters();
    },
    async doSaveFilter(name) {
      if (this.filters.find((f) => f.filterName === name)) {
        this.notifyError(`Filter with name ${name} already exists`);
      } else {
        this.filterStore.saveFilter(this.projectName, {
          filterName: name,
          query: { ...this.query, projFilter: this.projectName },
        });
        await this.loadFilters();
      }
    },
    saveFilterPrompt() {
      console.log("saveFilterPrompt is called");
      console.log("About to call MessageBox.prompt");
      MessageBox.prompt({
        title: this.promptTitle,

        content: this.promptContent,
        validator(value) {
          return /.+/.test(value) ? null : this.promptError;
        },
      })
        .then((value) => {
          console.log("save value", value);
          this.doSaveFilter(value);
          console.log("After save value", value);
        })
        .catch((e) => {
          console.log(e);
          //this.$notify("Save canceled.");
          
        });
    },
  },
});
</script>
<style lang="scss">
.modal-footer .btn-primary {
  color: var(--font-fill-color);
  background-color: var(--cta-color);
  &:hover {
    color: var(--font-fill-color);
    background-color: var(--cta-states-color);
  }
}
</style>
