<template>
  <span>
    <btn v-if="hasQuery && (!query || !query.ftilerName)" @click="saveFilterPrompt" size="xs" type="default">
      {{$t('filter.save.button')}}
    </btn>
    <span v-if="query && query.filterName">{{query.filterName}}</span>

    <dropdown v-if="filters && filters.length > 0">
      <span class="dropdown-toggle btn btn-secondary btn-sm" :class="query && query.filterName?'text-info':'text-secondary'">

        {{$t('Filters')}}
        <span class="caret"></span>
      </span>
      <template slot="dropdown">

        <li v-if="query && query.filterName">
          <a role="button"  @click="deleteFilter" >
            <i class="glyphicon glyphicon-trash"></i>
            {{$t('filter.delete.named.text',[query.filterName])}}
          </a>
        </li>
        <li role="separator" class="divider"  v-if="query && query.filterName"></li>
        <li class="dropdown-header">
          <i class="glyphicon glyphicon-filter"></i>
          {{$t('saved.filters')}}
        </li>
        <li v-for="filter in filters" :key="filter.name">
          <a role="button" @click="selectFilter(filter)">
            {{filter.name}}
            <span v-if="query && filter.name===query.filterName">√</span>
          </a>
        </li>
      </template>
    </dropdown>

  </span>
</template>
<script>
import Vue from "vue";
import { getRundeckContext } from "@/library/rundeckService"
import RundeckContext from '@/library/centralService'

export default {
  props: ["query", "hasQuery","eventBus"],
  data() {
    return {
      rdBase: "",
      projectName: "",
      filterListUrl: "",
      filterSaveUrl: "",
      filterDeleteUrl: "",
      loadError: "",
      filters: []
    };
  },
  methods: {
    async loadFilters() {
      const client = getRundeckContext().rundeckClient;
      try {
        const response = await client.sendRequest({
          method: "GET",
          url: this.filterListUrl,
          queryParameters: { project: this.projectName },
          withCredentials: true
        });
        if (response.parsedBody && response.parsedBody.filters) {
          this.filters = response.parsedBody.filters;
        }
      } catch (error) {
        this.loadError = error.message;
      }
    },
    selectFilter(filter) {
      this.$emit("select_filter", filter);
    },
    deleteFilter() {
      if (!this.query || !this.query.filterName) {
        return;
      }
      this.$confirm({
        title: this.$t("Delete Saved Filter"),
        content: this.$t('filter.delete.confirm.text',[this.query.filterName])
      })
        .then(() => {
          this.doDeleteFilter(this.query.filterName);
        })
        .catch(() => {
          //this.$notify("Delete canceled.");
        });
    },
    async doDeleteFilter(name) {
      const client = getRundeckContext().rundeckClient;
      try {
        const response = await client.sendRequest({
          method: "POST",
          url: this.filterDeleteUrl,
          queryParameters: { project: this.projectName, delFilterName: name }
        });
        if (response.parsedBody && response.parsedBody.success) {
          const index = this.filters.findIndex(q => q.name === name);
          if (index >= 0) {
            this.filters.splice(index, 1);
          }
          this.$emit("select_filter", {});
        }
      } catch (error) {
        this.loadError = error.message;
      }
    },
    async doSaveFilter(name) {
      const client = getRundeckContext().rundeckClient;
      try {
        const response = await client.sendRequest({
          method: "POST",
          url: this.filterSaveUrl,
          queryParameters: { project: this.projectName },
          body: Object.assign(
            { projFilter: this.projectName, newFilterName: name },
            this.query
          )
        });
        if(response.parsedBody.error){
          this.$notify.error(response.parsedBody.message);
        }
        if (response.parsedBody && response.parsedBody.success) {
          const newfilter=Object.assign({ name: name }, this.query)
          this.filters.push(newfilter);
          this.$emit("select_filter", newfilter);
        }
      } catch (error) {
        this.loadError = error.message;
      }
    },
    saveFilterPrompt() {
      this.$prompt({
        title: this.$t("Save Filter"),
        content: this.$t("filter.save.name.prompt"),
        // A simple input validator
        // returns the err msg (not valid) or null (valid)
        validator(value) {
          return /.+/.test(value) ? null : this.$t('filter.save.validation.name.blank');
        }
      })
        .then(value => {
          console.log("save value", value);
          this.doSaveFilter(value);
        })
        .catch(() => {
          //this.$notify("Save canceled.");
        });
    }
  },
  mounted() {
    this.rdBase = window._rundeck.rdBase;
    this.projectName = window._rundeck.projectName;
    if (window._rundeck && window._rundeck.data) {
      this.filterListUrl = window._rundeck.data["filterListUrl"];
      this.filterSaveUrl = window._rundeck.data["filterSaveUrl"];
      this.filterDeleteUrl = window._rundeck.data["filterDeleteUrl"];
      this.loadFilters();
    }
    this.eventBus && this.eventBus.$on('invoke-save-filter',this.saveFilterPrompt)
  }
};
</script>
<style lang="scss">
.modal-footer .btn-primary{
    color: var(--font-fill-color);
    background-color: var(--cta-color);
    &:hover {
    color: var(--font-fill-color);
    background-color: var(--cta-states-color);
  }
}
</style>
