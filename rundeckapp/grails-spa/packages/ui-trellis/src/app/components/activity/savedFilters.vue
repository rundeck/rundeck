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
      <template v-slot:dropdown>

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
        <li v-for="filter in filters" :key="filter.filterName">
          <a role="button" @click="selectFilter(filter)">
            {{filter.filterName}}
            <span v-if="query && filter.filterName===query.filterName">√</span>
          </a>
        </li>
      </template>
    </dropdown>

  </span>
</template>
<script lang="ts">
import {ActivityFilterStore} from '../../../library/stores/ActivityFilterStore'
import { defineComponent } from "vue";
import { getRundeckContext } from "../../../library";
import {  MessageBox } from 'uiv';

export default defineComponent({
  props: ["query", "hasQuery","eventBus"],
  data() {
    return {
      projectName: "",
      loadError: "",
      filters: [],
      promptTitle: this.$t('Save Filter'),
      promptContent: this.$t("filter.save.name.prompt"),
      promptError: this.$t("filter.save.validation.name.blank" ),
      filterStore: new ActivityFilterStore()
    };
  },
  emits: ['select_filter'],
  methods: {
    async loadFilters() {
      this.filters = this.filterStore.loadForProject(this.projectName).filters||[]
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
      this.filterStore.removeFilter(this.projectName,name)
      await this.loadFilters()
    },
    async doSaveFilter(name) {
      this.filterStore.saveFilter(this.projectName,{
        filterName:name,
        query:{...this.query, projFilter: this.projectName}
      })
      await this.loadFilters()
    },
    saveFilterPrompt() {
      MessageBox.prompt({
        title: this.promptTitle,
        content: this.promptContent,
        validator(value) {
          return /.+/.test(value) ? null : this.promptError;
        }
      })
        .then(value => {
          console.log("save value", value);
          this.doSaveFilter(value);
        })
        .catch((e) => {
          console.log(e);
          //this.$notify("Save canceled.");
        });
    }
  },
  mounted() {
    this.projectName = getRundeckContext().projectName
    this.loadFilters()
    this.eventBus && this.eventBus.on('invoke-save-filter',this.saveFilterPrompt)
  },
  beforeUnmount() {
    this.eventBus && this.eventBus.off('invoke-save-filter')
  }
})
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
