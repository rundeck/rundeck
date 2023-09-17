<template>
  <div>
    <div class="input-group nodefilters multiple-control-input-group">
      <span class="input-group-addon input-group-addon-title" v-if="showTitle">{{ $t('nodes') }}</span>
      <div class="input-group-btn">
        <button type="button" class="btn btn-default dropdown-toggle job_edit__node_filter__filter_select_dropdown"
                :class="{'btn-success':selectedFilterName,'btn-default':!selectedFilterName}" data-toggle="dropdown">
          <span>{{ filterNameDisplay }}</span> <span class="caret"></span>
        </button>
        <ul class="dropdown-menu">
          <li>
            <node-filter-link
                node-filter-name=".*"
                node-filter=".*"
                :class="{active: '.*'=== selectedFilterName, 'job_edit__node_filter__filter_select_all':true }"
                @nodefilterclick="handleNodefilter">
              <i class="fas fa-asterisk"></i>
              {{ $t('show.all.nodes') }}
            </node-filter-link>
          </li>

          <li class="divider"></li>

          <li class="dropdown-header" v-if="selectedFilterName">
            {{ $t('filter') }}: <span>{{ filterNameDisplay }}</span>
          </li>
          <li v-if="canSaveFilter">
            <a href="#" @click="saveFilterModal=true">
              <i class="glyphicon glyphicon-plus"></i>
              {{ $t('save.filter.ellipsis') }}
            </a>
          </li>
          <li v-if="canDeleteFilter">
            <a href="#"
               @click="deleteFilterModal=true">
              <i class="glyphicon glyphicon-remove"></i>
              {{ $t('delete.this.filter') }}
            </a>
          </li>
          <li v-if="canSetDefaultFilter">
            <a href="#"
               @click="setDefaultFilter">
              <i class="glyphicon glyphicon-filter"></i>
              {{ $t('set.as.default.filter') }}
            </a>
          </li>
          <li v-if="canRemoveDefaultFilter">
            <a href="#"
               @click="removeDefaultFilter">
              <i class="glyphicon glyphicon-ban-circle"></i>
              {{ $t('remove.default.filter') }}
            </a>
          </li>

          <template v-if="nodeSummary && nodeSummary.filters&& nodeSummary.filters.length>0">
            <li class="divider"></li>
            <li class="dropdown-header"> {{ $t('saved.filters') }}</li>
            <li v-for="filter in nodeSummary.filters">
              <node-filter-link
                  :node-filter-name="filter.name"
                  :node-filter="filter.filter"
                  @nodefilterclick="handleNodefilter"
              >
                <template v-slot:suffix v-if="selectedFilterName===filter.name">
                  <span>
                    <i class="fa fa-check"></i>
                  </span>
                </template>
              </node-filter-link>
            </li>
          </template>
        </ul>
      </div>

      <!--    <g:jsonToken id="filter_select_tokens" url="${request.forwardURI}"/>-->
      <input type='search'
             :name="filterFieldName"
             class="schedJobNodeFilter form-control"
             :autofocus="autofocus"
             :placeholder="queryFieldPlaceholderText||$t('enter.a.node.filter')"
             v-model="outputValue"
             v-on:keydown.enter.prevent="doSearch"
             v-on:blur="doSearch"
             :id="filterFieldId"/>
      <div class="input-group-btn">
        <btn id="filterSearchHelpBtn" tabindex="0" v-if="helpButton">
          <i class="glyphicon glyphicon-question-sign"></i>
        </btn>
        <btn :type="`${searchBtnType} btn-fill`" @click="doSearch" :disabled="!outputValue" class="node_filter__dosearch">
          {{ $t('search') }}
        </btn>
      </div>
      <popover target="#filterSearchHelpBtn" trigger="focus" placement="bottom" v-if="helpButton">
        <template v-slot:popover>
          <div class="help-block">
            <strong>{{ $t('select.nodes.by.name') }}:</strong>
            <p>
              <code>{{ $t('mynode1.mynode2') }}</code>
            </p>
            <p>
              {{ $t('this.will.select.both.nodes') }}
            </p>

            <strong>{{ $t('filter.nodes.by.attribute.value') }}:</strong>
            <ul>
              <li>{{ $t('include') }}: <code>{{ $t('attribute') }}: {{ $t('value') }}</code></li>

              <li>{{ $t('exclude') }}: <code>!{{ $t('attribute') }}: {{ $t('value') }}</code></li>
            </ul>


            <strong>{{ $t('use.regular.expressions') }}</strong>
            <p>
              <code>{{ $t('node.metadata.hostname') }}: dev(\d+).test.com</code>.
            </p>

            <strong>{{ $t('regex.syntax.checking') }}:</strong>
            <p>
              <code>{{ $t('attribute') }}: /regex/</code>
            </p>

          </div>
        </template>
      </popover>
    </div>
    <modal v-model="saveFilterModal" :title="$t('save.node.filter')">

      <div>
        <div class="form-group">
          <label for="newFilterName" class="control-label col-sm-2">{{$t('name.prompt')}}</label>
          <div class="col-sm-10">
            <input type="text"
                   id="newFilterName"
                   class="form-control input-sm"
                   v-model="newFilterName"/>
          </div>
        </div>
        <div class="form-group ">
          <label class="control-label col-sm-2">
            {{$t('filter')}}
          </label>
          <div class="col-sm-10">
            <span class="form-control form-control-static ">{{ outputValue }}</span>
          </div>
        </div>
        <div v-if="saveFilterModalError" class="text-danger">
          {{ saveFilterModalError }}
        </div>

      </div>
      <template v-slot:footer>
        <div>
          <btn @click="saveFilterModal=false">{{$t('button.action.Cancel')}}</btn>
          <btn type="primary" @click="saveFilter">{{ $t('save.filter.ellipsis') }}</btn>
        </div>
      </template>
    </modal>
    <modal v-model="deleteFilterModal" :title="$t('delete.saved.node.filter')">

      <div>
        <div class="modal-body">
          <div class="form-group">
            <label class="control-label col-sm-2">{{$t('name.prompt')}}</label>
            <div class="col-sm-10">
              <span class="form-control form-control-static">{{ selectedFilterName }}</span>
            </div>
          </div>
          <div class="form-group " v-if="selectedSavedFilter">
            <label class="control-label col-sm-2">
              {{$t('filter')}}
            </label>

            <div class="col-sm-10">
              <span class="form-control form-control-static ">{{ selectedSavedFilter.filter }}</span>
            </div>
          </div>
        </div>
        <div class="modal-body">
          <span class="text-danger">{{ $t('delete.this.filter.confirm') }}</span>
        </div>

      </div>
      <template v-slot:footer>
        <div>
          <btn @click="deleteFilterModal=false">{{$t('no')}}</btn>
          <btn type="danger" @click="deleteFilter">{{$t('yes')}}</btn>
        </div>
      </template>
    </modal>
  </div>
</template>
<script lang="ts">
import {_genUrl} from '../../../utilities/genUrl'
import {RundeckBrowser} from '@rundeck/client'
import axios from 'axios'
import {defineComponent, PropType, ref } from 'vue'
import NodeFilterLink from './NodeFilterLink.vue'
import {
  getAppLinks,
  getRundeckContext
} from '../../../../library'
import Trellis from '../../../../library'

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase

interface NodeSummary {
  filters: any[]
  defaultFilter: any
}

export default defineComponent({
  name: 'NodeFilterInput',
  components: {
    NodeFilterLink,
  },
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    showTitle: {
      type: Boolean,
      required: false,
      default: false,
    },
    autofocus: {
      type: Boolean,
      required: false,
      default: false,
    },
    helpButton: {
      type: Boolean,
      required: false,
      default: true,
    },
    searchBtnType: {
      type: String,
      required: false,
      default: 'default',
    },
    filterName: {
      type: String,
      required: false,
      default: '',
    },
    filterFieldName: {
      type: String,
      required: false,
      default: 'filter',
    },
    queryFieldPlaceholderText: {
      type: String,
      required: false,
      default: '',
    },
    filterFieldId: {
      type: String,
      required: false,
      default: 'schedJobNodeFilter',
    },
    /**
     * if true, allow setting/removing default filter
     */
    allowFilterDefault: {
      type: Boolean,
      required: false,
      default: false,
    },
    nodeSummary: {
      type: Object as PropType<NodeSummary>,
      required: false,
      default: () => {}
    }
  },
  emits: ['filters-updated', 'filter', 'update:modelValue'],
  setup() {
    const outputValue = ref('')
    const selectedFilterName = ref('')
    const hideAll = ref(false)
    const saveFilterModal = ref(false)
    const saveFilterModalError = ref('')
    const newFilterName = ref('')
    const deleteFilterModal = ref(false)
    return {
      outputValue,
      selectedFilterName,
      hideAll,
      saveFilterModal,
      saveFilterModalError,
      newFilterName,
      deleteFilterModal,
    }
  },
  computed: {
    filterNameDisplay() {
      return this.selectedFilterName === '.*' ? 'All Nodes' : this.selectedFilterName
    },
    canSaveFilter() {
      return !this.selectedFilterName && this.filterWithoutAll()
    },
    canDeleteFilter() {
      return this.selectedFilterName && this.selectedFilterName !== '.*'
    },
    canSetDefaultFilter() {
      return this.nodeSummary && this.allowFilterDefault && this.selectedFilterName && this.selectedFilterName !== this.nodeSummary.defaultFilter
    },
    canRemoveDefaultFilter() {
      return this.nodeSummary && this.allowFilterDefault && !this.selectedFilterName && this.selectedFilterName === this.nodeSummary.defaultFilter
    },
    matchedFilter() {
      if (this.outputValue && this.nodeSummary.filters) {
        let found = this.nodeSummary.filters.find((a: any) => a.filter === this.outputValue)
        if (found) {
          return found
        }
      }
      return null
    },
    selectedSavedFilter() {
      if (this.selectedFilterName && this.nodeSummary.filters) {
        let found = this.nodeSummary.filters.find((a: any) => a.name === this.selectedFilterName)
        if (found) {
          return found
        }
      }
      return null
    },
  },
  methods: {
    selectedFilterLink(link: any) {
    },
    filterWithoutAll() {
      if (this.outputValue === '.*' && this.hideAll) {
        return ''
      }
      return this.outputValue
    },
    async saveFilter() {
      let result = await client.sendRequest({
        method: 'POST',
        url: _genUrl(
            getAppLinks().frameworkStoreFilterAjax,
            {
              newFilterName: this.newFilterName,
              isJobEdit: true,
              filter: this.outputValue
            })
      })
      if (result.status == 200) {
        this.saveFilterModal = false
        this.selectedFilterName = this.newFilterName
        this.newFilterName = ''
        this.$emit('filters-updated')
      } else {
        this.saveFilterModalError = 'Error saving filter: ' + result.status + ': ' + (result.parsedBody?.msg || 'Unknown error')
      }
    },
    async deleteFilter() {
      let result = await client.sendRequest(
          {
            method: 'POST',
            url: _genUrl(getAppLinks().frameworkDeleteNodeFilterAjax, {filtername: this.selectedFilterName})
          }
      )

      if (result.status == 200) {
        this.deleteFilterModal = false
        this.selectedFilterName = ''
        this.$emit('filters-updated')
      }
    },
    async setDefaultFilter() {
      let result = await Trellis.FilterPrefs.setFilterPref('nodes', this.selectedFilterName)

      this.nodeSummary.defaultFilter = this.selectedFilterName
    },
    async removeDefaultFilter() {
      let result = await Trellis.FilterPrefs.unsetFilterPref('nodes')
      this.nodeSummary.defaultFilter = null
    },
    doSearch() {
      if (this.selectedFilterName && this.selectedFilterName !== this.matchedFilter) {
        this.selectedFilterName = ''
      }
      this.$emit('update:modelValue', this.outputValue)
    },
    handleNodefilter(val: any) {
      if (val.filterName) {
        this.selectedFilterName = val.filterName
      } else {
        this.selectedFilterName = ''
      }
      this.$emit('filter', val)
    },
    async onMount() {
      this.outputValue = this.modelValue
      if (this.selectedFilterName && this.selectedFilterName !== this.matchedFilter) {
        this.selectedFilterName = ''
      }
      this.selectedFilterName = this.filterName

    }
  },
  watch: {
    modelValue() {
      this.outputValue = this.modelValue
      if (this.selectedFilterName && this.selectedFilterName !== this.matchedFilter) {
        this.selectedFilterName = ''
      }
    },
    filterName() {
      this.selectedFilterName = this.filterName
    },
  },
  mounted() {
    this.onMount();
  },
})
</script>