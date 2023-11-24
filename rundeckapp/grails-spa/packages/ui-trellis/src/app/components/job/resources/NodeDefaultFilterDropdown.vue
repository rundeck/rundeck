<template>
  <ul class="list-unstyled">
    <li>
      <dropdown>
        <a
            class="nodefilterlink btn btn-default btn-xs"
            @click.prevent="saveFilter({ filter: '.*' })"
        >
          {{ $t("all.nodes") }} {{ nodeSummary? nodeSummary.totalCount! : 0 }}
        </a>
        <btn size="xs" title="Filter Actions" class="dropdown-toggle">
          <span class="caret"></span>
        </btn>
        <template #dropdown>
          <li>
            <a
                role="button"
                @click.prevent="toggleDefaultFilter"
            >
              <i
                  class="glyphicon"
                  :class="[
                  isDefaultFilter ? 'glyphicon-ban-circle' : 'glyphicon-filter',
                ]"
              />
              {{
                isDefaultFilter
                    ? $t("remove.all.nodes.as.default.filter")
                    : $t("set.all.nodes.as.default.filter")
              }}
            </a>
          </li>
        </template>
      </dropdown>
    </li>
    <li v-for="i in nodeSummary.filters">
      <dropdown>
        <a
          class="nodefilterlink btn btn-default btn-xs"
          :data-node-filter-name="i.filterName"
          :data-node-filter="i.filter"
          :title="i.filter"
          :href="linkForFilterName(i)"
          @click.prevent="saveFilter(i)"
        >
          {{ i.filterName }}
        </a>
        <btn size="xs" title="Filter Actions" class="dropdown-toggle">
          <span class="caret"></span>
        </btn>
        <template #dropdown>
          <li>
            <a role="button" @click="deleteFilterConfirm(i)">
              <i class="glyphicon glyphicon-remove"></i>
              {{ $t("delete.this.filter.ellipsis") }}
            </a>
          </li>
          <li v-if="i.filterName !== nodeSummary.defaultFilter">
            <a role="button" @click="setDefault(i)">
              <i class="glyphicon glyphicon-filter"></i>
              {{ $t("set.as.default.filter") }}
            </a>
          </li>
          <li v-else>
            <a role="button" @click="removeDefault()">
              <i class="glyphicon glyphicon-ban-circle"></i>
              {{ $t("remove.default.filter") }}
            </a>
          </li>
        </template>
      </dropdown>
    </li>
  </ul>
  <div v-if="!nodeSummary.filters || nodeSummary.filters.length < 1">
    {{ $t("none") }}
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { NodeSummary } from "@/app/components/job/resources/types/nodeTypes";
import { StoredFilter } from "@/library/stores/NodeFilterLocalstore";
import { _genUrl } from "@/app/utilities/genUrl";
import { getRundeckContext } from "@/library";

export default defineComponent({
  name: "NodeDefaultFilterDropdown",
  props: {
    isDefaultFilter: {
      type: Boolean,
      required: true,
    },
    nodeSummary: {
      type: Object as PropType<NodeSummary>,
      required: true,
    },
  },
  emits: ["filter"],
  data() {
    return {
      rundeckContext: getRundeckContext(),
    };
  },
  computed: {
    nodesBaseUrl() {
      return (
        this.rundeckContext.rdBase +
        "/project/" +
        this.rundeckContext.projectName +
        "/nodes"
      );
    },
    eventBus() {
      return this.rundeckContext.eventBus;
    },
  },
  methods: {
    saveFilter(selectedFilter: object) {
      this.$emit("filter", selectedFilter);
    },
    linkForFilterName(filter: StoredFilter) {
      return _genUrl(this.nodesBaseUrl, { filter: filter.filter });
    },
    setDefaultAll() {
      this.eventBus.emit("nodefilter:action:setDefaultAll");
    },
    setDefault(filter: StoredFilter) {
      this.eventBus.emit("nodefilter:action:setDefault", filter.filterName);
    },
    removeDefault() {
      this.eventBus.emit("nodefilter:action:removeDefault");
    },
    deleteFilterConfirm(filter: StoredFilter) {
      this.eventBus.emit(
        "nodefilter:action:deleteSavedFilter",
        filter.filterName
      );
    },
    toggleDefaultFilter() {
      if(this.isDefaultFilter) {
        this.removeDefault();
      } else {
        this.setDefaultAll();
      }
    }
  },
});
</script>