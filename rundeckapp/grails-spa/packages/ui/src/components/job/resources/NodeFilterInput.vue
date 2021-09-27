<template>
  <div>
    <span class="input-group-addon input-group-addon-title" v-if="showTitle">{{ $t('nodes') }}</span>
    <div class="input-group-btn">
      <button type="button" class="btn btn-default dropdown-toggle"
              :class="{'btn-success':filterName,'btn-default':!filterName}" data-toggle="dropdown">
        <span>{{ filterNameDisplay() }}</span> <span class="caret"></span>
      </button>
      <ul class="dropdown-menu">
        <li>
          <node-filter-link
              node-filter-name=".*"
              node-filter=".*"
              :class="{active: '.*'=== filterName }"
              @nodefilterclick="handleNodefilter">
            <i class="fas fa-asterisk"></i>
            {{ $t('show.all.nodes') }}
          </node-filter-link>
        </li>

        <li class="divider"></li>

        <li class="dropdown-header" v-if="filterName">
          {{ $t('filter') }}<span>{{ filterNameDisplay }}</span>
        </li>
        <li v-if="canSaveFilter">
          <a href="#"
             data-toggle="modal"
             data-target="#saveFilterModal">
            <i class="glyphicon glyphicon-plus"></i>
            {{ $t('save.filter.ellipsis') }}
          </a>
        </li>
        <li x-data-bind="visible: canDeleteFilter">
          <a href="#"
             class=""
             x-data-bind="click: deleteFilter">
            <i class="glyphicon glyphicon-remove"></i>
            {{ $t('delete.this.filter.ellipsis') }}
          </a>
        </li>
        <li x-data-bind="visible: canSetDefaultFilter">
          <a href="#"
             class=""
             x-data-bind="click: setDefaultFilter">
            <i class="glyphicon glyphicon-filter"></i>
            {{ $t('set.as.default.filter') }}
          </a>
        </li>
        <li x-data-bind="visible: canRemoveDefaultFilter">
          <a href="#"
             class=""
             x-data-bind="click: nodeSummary().removeDefault">
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
                :class="{active:name===filterName}"
            ></node-filter-link>
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
      <btn :type="`${searchBtnType} btn-fill`" @click="doSearch" :disabled="!outputValue">
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
</template>
<script lang="ts">
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'
import NodeFilterLink from './NodeFilterLink.vue'

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
  @Prop({required: false, default: 'primary'})
  searchBtnType!: string
  @Prop({required: false, default: ''})
  filterName!: string
  @Prop({required: false, default: 'filter'})
  filterFieldName!: string
  @Prop({required: false, default: ''})
  queryFieldPlaceholderText!: string
  @Prop({required: false, default: 'schedJobNodeFilter'})
  filterFieldId!: string

  outputValue: string = ''
  nodeSummary: any = {}
  hideAll: boolean = false

  filterNameDisplay() {
    return this.filterName === '.*' ? 'All Nodes' : this.filterName
  }

  selectedFilterLink(link: any) {

  }

  filterWithoutAll() {
    if (this.outputValue === '.*' && this.hideAll) {
      return ''
    }
    return this.outputValue
  }

  canSaveFilter() {
    return !this.filterName && this.filterWithoutAll()
  }

  doSearch() {
    this.$emit('input', this.outputValue)
  }

  handleNodefilter(val: any) {
    this.$emit('filter', val)
  }

  @Watch('value')
  update() {
    this.outputValue = this.value
  }

  async mounted() {
    this.update()
  }

}
</script>