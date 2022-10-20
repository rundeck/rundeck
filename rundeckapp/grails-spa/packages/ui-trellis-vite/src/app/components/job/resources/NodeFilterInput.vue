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
              <span slot="suffix" v-if="selectedFilterName===filter.name">
                <i class="fa fa-check"></i>
              </span>
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
        <template slot="popover">
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
      <div slot="footer">
        <btn @click="saveFilterModal=false">{{$t('button.action.Cancel')}}</btn>
        <btn type="primary" @click="saveFilter">{{ $t('save.filter.ellipsis') }}</btn>
      </div>
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
      <div slot="footer">
        <btn @click="deleteFilterModal=false">{{$t('no')}}</btn>
        <btn type="danger" @click="deleteFilter">{{$t('yes')}}</btn>
      </div>
    </modal>
  </div>
</template>
<script lang="ts">
import {_genUrl} from '@/app/utilities/genUrl'
import {RundeckBrowser} from '@rundeck/client'
import axios from 'axios'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'
import NodeFilterLink from './NodeFilterLink.vue'
import {
  getAppLinks,
  getRundeckContext
} from '@/library/rundeckService'
import Trellis, { getRundeckContext } from '@/library/centralService';

const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase

@Component({components: {NodeFilterLink}})
export default class NodeFilterInput extends Vue {
  @Prop({required: true})
  value!: string
  @Prop({required: false, default: false})
  showTitle!: boolean
  @Prop({required: false, default: false})
  autofocus!: boolean
  @Prop({required: false, default: true})
  helpButton!: boolean
  @Prop({required: false, default: 'default'})
  searchBtnType!: string
  @Prop({required: false, default: ''})
  filterName!: string
  @Prop({required: false, default: 'filter'})
  filterFieldName!: string
  @Prop({required: false, default: ''})
  queryFieldPlaceholderText!: string
  @Prop({required: false, default: 'schedJobNodeFilter'})
  filterFieldId!: string
  /**
   * if true, allow setting/removing default filter
   */
  @Prop({required: false, default: false})
  allowFilterDefault!: boolean
  @Prop({
    required: false, default: () => {
    }
  })
  nodeSummary!: any

  outputValue: string = ''
  selectedFilterName: string = ''
  hideAll: boolean = false
  saveFilterModal: boolean = false
  saveFilterModalError: string = ''
  newFilterName: string = ''
  deleteFilterModal: boolean = false

  get filterNameDisplay() {
    return this.selectedFilterName === '.*' ? 'All Nodes' : this.selectedFilterName
  }

  selectedFilterLink(link: any) {

  }

  filterWithoutAll() {
    if (this.outputValue === '.*' && this.hideAll) {
      return ''
    }
    return this.outputValue
  }

  get canSaveFilter() {
    return !this.selectedFilterName && this.filterWithoutAll()
  }

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
  }

  get canDeleteFilter() {
    return this.selectedFilterName && this.selectedFilterName !== '.*'
  }

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
  }

  get canSetDefaultFilter() {
    return this.nodeSummary && this.allowFilterDefault && this.selectedFilterName && this.selectedFilterName !== this.nodeSummary.defaultFilter
  }

  async setDefaultFilter() {
    let result = await Trellis.FilterPrefs.setFilterPref('nodes', this.selectedFilterName)

    this.nodeSummary.defaultFilter = this.selectedFilterName
  }

  get canRemoveDefaultFilter() {
    return this.nodeSummary && this.allowFilterDefault && !this.selectedFilterName && this.selectedFilterName === this.nodeSummary.defaultFilter
  }

  async removeDefaultFilter() {

    let result = await Trellis.FilterPrefs.unsetFilterPref('nodes')

    this.nodeSummary.defaultFilter = null
  }

  get matchedFilter() {
    if (this.outputValue && this.nodeSummary.filters) {
      let found = this.nodeSummary.filters.find((a: any) => a.filter === this.outputValue)
      if (found) {
        return found
      }
    }
    return null
  }

  get selectedSavedFilter() {
    if (this.selectedFilterName && this.nodeSummary.filters) {
      let found = this.nodeSummary.filters.find((a: any) => a.name === this.selectedFilterName)
      if (found) {
        return found
      }
    }
    return null
  }

  doSearch() {
    if (this.selectedFilterName && this.selectedFilterName !== this.matchedFilter) {
      this.selectedFilterName = ''
    }
    this.$emit('input', this.outputValue)
  }

  handleNodefilter(val: any) {
    if (val.filterName) {
      this.selectedFilterName = val.filterName
    } else {
      this.selectedFilterName = ''
    }
    this.$emit('filter', val)
  }

  @Watch('value')
  updateValue() {
    this.outputValue = this.value
    if (this.selectedFilterName && this.selectedFilterName !== this.matchedFilter) {
      this.selectedFilterName = ''
    }
  }

  @Watch('filterName')
  updateFilterName() {
    this.selectedFilterName = this.filterName
  }

  @Watch('nodeSummary')
  updatedNodeSummary() {
  }

  async mounted() {
    this.updateValue()
    this.updateFilterName()
  }

}
</script>