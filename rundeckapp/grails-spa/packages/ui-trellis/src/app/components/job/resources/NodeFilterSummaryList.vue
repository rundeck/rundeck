<template>

    <ul class="list-unstyled">
      <li>
        <dropdown>
          <a href="#" class="nodefilterlink btn btn-default btn-xs" data-node-filter=".*" data-node-filter-all="true">
            {{ $t('all.nodes') }}
            <!--         TODO: <span data-bind="text: nodeSummary().totalCount">0</span>-->
          </a>
          <btn size="xs" title="Filter Actions" class="dropdown-toggle">
            <span class="caret"></span>
          </btn>
          <template #dropdown>
            <li v-if="'.*' !== nodeSummary.defaultFilter">
              <a role="button" @click="setDefaultAll()">
                <i class="glyphicon glyphicon-filter"></i>
                {{ $t('set.all.nodes.as.default.filter') }}
              </a>
            </li>
            <li v-else>
              <a role="button" @click="removeDefault()">
                <i class="glyphicon glyphicon-ban-circle"></i>
                {{ $t('remove.all.nodes.as.default.filter') }}
              </a>
            </li>
          </template>
        </dropdown>
      </li>


      <li v-for="i in nodeSummary.filters">
        <dropdown>
          <a class=" nodefilterlink btn btn-default btn-xs"
             :data-node-filter-name="i.filterName"
             :data-node-filter="i.filter"
             :title="i.filter"
             :href="linkForFilterName(i)"
          >
            {{ i.filterName }}
          </a>
          <btn size="xs" title="Filter Actions" class="dropdown-toggle"><span class="caret"></span></btn>
          <template #dropdown>
            <li>
              <a role="button" @click="deleteFilterConfirm(i)">
                <i class="glyphicon glyphicon-remove"></i>
                {{ $t('delete.this.filter.ellipsis') }}
              </a>
            </li>
            <li v-if="i.filterName!==nodeSummary.defaultFilter">
              <a role="button" @click="setDefault(i)">

                <i class="glyphicon glyphicon-filter"></i>
                {{ $t('set.as.default.filter') }}
              </a>
            </li>
            <li v-else>
              <a role="button"
                 @click="removeDefault()">
                <i class="glyphicon glyphicon-ban-circle"></i>
                {{ $t('remove.default.filter') }}
              </a>
            </li>
          </template>
        </dropdown>


      </li>
    </ul>
    <div v-if="!nodeSummary.filters || nodeSummary.filters.length<1">
      {{ $t('none') }}
    </div>
</template>

<script lang="ts">
import {_genUrl} from '@/app/utilities/genUrl'
import {getRundeckContext} from '@/library'
import {NodeFilterStore, ProjectFilters, StoredFilter} from '../../../../library/stores/NodeFilterStore'
import {defineComponent, ref} from 'vue'

export default defineComponent({
  name: 'NodeFilterSummaryList',
  props: {
    project: {
      type: String,
      required: true
    },
    nodesBaseUrl: {
      type: String,
      required: true
    }
  },
  setup() {
    const nodeFilterStore = new NodeFilterStore()
    const nodeSummary = ref({} as ProjectFilters)
    const eventBus = getRundeckContext().eventBus
    return {
      nodeSummary,
      nodeFilterStore,
      eventBus
    }
  },
  methods: {
    setDefaultAll() {
      this.eventBus.emit('nodefilter:action:setDefaultAll')
    },
    setDefault(filter: StoredFilter) {
      this.eventBus.emit('nodefilter:action:setDefault',filter.filterName)
    },
    removeDefault() {
      this.eventBus.emit('nodefilter:action:removeDefault')
    },
    deleteFilterConfirm(filter: StoredFilter) {
      this.eventBus.emit('nodefilter:action:deleteSavedFilter',filter.filterName)
    },
    linkForFilterName(filter: StoredFilter) {
      return _genUrl(this.nodesBaseUrl,{filter: filter.filter});
    },
    loadNodeFilters() {
      this.nodeSummary = this.nodeFilterStore.loadStoredProjectNodeFilters(this.project)
    },
  },
  mounted() {
    this.loadNodeFilters()
    this.eventBus.on('nodefilter:savedFilters:changed',this.loadNodeFilters)
  }
})
</script>

<style scoped lang="scss">

</style>