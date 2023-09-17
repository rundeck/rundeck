<template>
  <div class="well well-sm matchednodes node_filter_results__matched_nodes">
    <div class="row">
      <div class="col-sm-6">
        <span class="text-info node_filter_results__matched_nodes_count" v-if="loaded && !loading">
          {{$t('count.nodes.matched',[total,$tc('Node.count.vue',total)])}}
        </span>
        <span v-if="loading" class="text-muted">
            <i class="glyphicon glyphicon-time"></i>
            {{ $t('loading.matched.nodes') }}
        </span>

        <span v-if="total>maxShown">
            <span class="text-strong">
                  {{$t('count.nodes.shown',[maxShown,$tc('Node.count.vue',maxShown)])}}
            </span>
        </span>
      </div>

      <div class="col-sm-6">

        <btn type="default btn-sm refresh_nodes pull-right"
             data-loading-text="${g.message(code: 'loading')}"
             @click="update"
             :disabled="loading"
             :title="$t('click.to.refresh')">
          {{ $t('refresh') }}
          <i class="glyphicon glyphicon-refresh"></i>
        </btn>

      </div>
    </div>
    <div id='matchednodes' class="clearfix row">
      <div class="col-sm-12 container-fluid">

        <node-list-embed :nodes="nodeSet.nodes"
                         :tagsummary="nodeSet.tagsummary"
                         :show-exclude-filter-links="true"
                         @filter="handleFilter">

        </node-list-embed>
      </div>
    </div>
  </div>
</template>
<script lang="ts">


import NodeListEmbed from '../../job/resources/NodeListEmbed.vue'
import {_genUrl} from '../../../utilities/genUrl'
import axios from 'axios'
import { defineComponent, ref } from 'vue'

import {
  getRundeckContext,
  getAppLinks
} from '../../../../library'

const rdBase = getRundeckContext().rdBase
const project = getRundeckContext().projectName

export default defineComponent({
  name: 'NodeFilterResults',
  components: {
    NodeListEmbed,
  },
  props: {
    nodeFilter: {
      type: String,
      required: true,
    },
    nodeExcludeFilter: {
      type: String,
      required: false,
    },
    filterName: {
      type: String,
      required: false,
      default: '',
    },
    excludeFilterName: {
      type: String,
      required: false,
      default: '',
    },
    excludeFilterUncheck: {
      type: Boolean,
      required: false,
      default: false,
    },
  },
  emits: ['filter'],
  setup() {
    const paging = ref(true)
    const loaded = ref(false)
    const loading = ref(false)
    const total = ref(0)
    const allcount = ref(0)
    const pagingMax = ref(30)
    const maxShown = ref(30)
    const page = ref(0)
    const emptyMode = ref('all')
    const view = ref('')
    const error = ref('')
    const truncated = ref(false)
    const colkeys = ref([])
    const nodeSet= ref({nodes: [], tagsummary: {}})
    return {
      paging,
      loaded,
      loading,
      total,
      allcount,
      pagingMax,
      maxShown,
      page,
      emptyMode,
      view,
      error,
      truncated,
      colkeys,
      nodeSet,
    }
  },
  methods: {
    clear() {
      this.page = 0
      this.total = 0
      this.error = ''
      this.allcount = 0
      this.truncated = false
      this.colkeys = []
      this.nodeSet = {nodeset: [], tagsummary: {}}
    },
    handleFilter(val: any) {
      this.$emit('filter', val)
    },
    async update() {
      if (this.nodeFilter && this.emptyMode == 'blank') {
        return
      }
      var filterdata = this.filterName ? {filterName: this.filterName} : this.nodeFilter ? {filter: this.nodeFilter} : {}
      var filterExcludedata = this.excludeFilterName ? {filterExcludeName: this.excludeFilterName} : this.nodeExcludeFilter ? {filterExclude: this.nodeExcludeFilter} : {}
      var excludeFilterUncheck = this.excludeFilterUncheck
      var page = this.page
      var view = this.view ? this.view : 'table'
      var basedata: any = {
        view: view,
        declarenone: true,
        fullresults: true,
        expanddetail: true,
        inlinepaging: false,
        // nodefilterLinkId: this.nodefilterLinkId,
        excludeFilterUncheck: this.excludeFilterUncheck
      }
      if (this.paging) {
        basedata.page = page
        basedata.max = this.pagingMax
        basedata.inlinepaging = true
        if (page != 0) {
          basedata.view = 'tableContent'
        }
      }
      if (this.maxShown) {
        basedata.maxShown = this.maxShown
      }
      let params = Object.assign({}, basedata, filterdata, filterExcludedata)
      if (this.emptyMode == 'localnode' && !this.nodeFilter) {
        params.localNodeOnly = 'true'
      } else if (this.emptyMode == 'blank' && !this.nodeFilter) {
        this.clear()
        return
      }
      params.nodeExcludePrecedence = 'true'
      this.loading = true

      axios.request({
        method: 'GET',
        headers: {
          'x-rundeck-ajax': 'true',
        },
        url: _genUrl(getAppLinks().frameworkNodesQueryAjax, params),
      }).then(result => {
        this.loading = false
        if (result.status === 403) {
          this.error = ('Not authorized')
        } else if (result.status >= 300) {
          if (result.data.message) {

            this.error = result.data.message
          } else {
            this.error = 'Error: ' + result.status
          }
        } else {
          this.loaded = true
          let data = result.data
          this.nodeSet = {
            nodes: data.allnodes,
            tagsummary: data.tagsummary
          }
          this.allcount = data.allcount
          this.total = data.total
          this.truncated = data.truncated
          this.colkeys = data.colkeys
          this.maxShown = data.max
        }
      }).catch((err) => {
        this.loading = false
        console.log('Nodes Query: request failed: ' + err)
        this.error = ('Nodes Query: request failed: ' + err)
      })
    }
  },
  watch: {
    nodeFilter() {
      this.update();
    },
    nodeExcludeFilter() {
      this.update();
    },
    excludeFilterUncheck() {
      this.update();
    },
  },
  mounted() {
    if (this.nodeFilter) {
      this.update()
    }
  },
})
</script>