<template>
  <div style="margin-top:10px">
    <table class="table table-condensed table-embed node-details-simple" style="margin-bottom:0;">
      <tbody>
      <tr v-if=" attributes.description">
        <td class="value text-strong" colspan="4">
          {{ attributes.description }}
        </td>
      </tr>
      <tr v-if=" !authrun">
        <td class="value text-strong" colspan="4">
          <i class="glyphicon glyphicon-ban-circle"></i>
          {{ $t('node.access.not-runnable.message') }}
        </td>
      </tr>
      <tr v-if=" attributes['ui:status:icon'] || attributes['ui:status:text']">

        <td class="key">
          {{ $t('node.metadata.status') }}
        </td>
        <td class="value">
          <node-status :node="{attributes}" :show-text="true"/>
        </td>
      </tr>
      <tr>
        <!-- OS details -->
        <td class="key" v-if="hasOsData()">
          {{ $t('node.metadata.os') }}
        </td>
        <td class="value" v-if="hasOsData()">

          <node-filter-link
              style="margin-right: 0.5em;"
              v-for="attr in ['osName','osFamily','osVersion','osArch']"
              :key="attr"
              v-if="attributes[attr]"
              :filter-key="attr"
              :filter-val="attributes[attr]"
              :class="{'text-parenthetical':attr==='osFamily' || attr==='osArch'}"
              @nodefilterclick="filterClick"
          ></node-filter-link>

        </td>

        <template v-if="useDefaultColumns">
          <td class="key">
            {{ $t('node.metadata.username-at-hostname') }}
          </td>
          <td>
            <node-filter-link filter-key="username"
                              :filter-val="attributes.username"
                              @nodefilterclick="filterClick"
                              v-if=" attributes.username"></node-filter-link>
            <span class="atsign">@</span>
            <node-filter-link filter-key="hostname"
                              :filter-val="attributes.hostname"
                              @nodefilterclick="filterClick"
                              v-if=" attributes.hostname"></node-filter-link>
          </td>
        </template>


      </tr>
      <!--  unless exclude tags  -->
      <tr v-if=" tags && tags.length > 0">
        <td class="key">
          {{ $t('node.metadata.tags') }}
        </td>
        <td class="" colspan="3">
          <span v-if=" tags">
                  <span class="nodetags">

                          <span class="label label-muted" v-for="tag in tags">
                              {{ tag }}

                              <node-filter-link filter-key="tags"
                                                :filter-val="tag"
                                                @nodefilterclick="filterClick"
                                                class="textbtn textbtn-info textbtn-saturated hover-action"
                              >
                                <i class="glyphicon glyphicon-plus text-success"/>
                              </node-filter-link>

                              <node-filter-link
                                  @nodefilterclick="filterClick"
                                  v-if="showExcludeFilterLinks"
                                  :exclude="true"
                                  filter-key="tags"
                                  :filter-val="tag"
                                  class="text-danger textbtn textbtn-info textbtn-saturated hover-action"
                              >
                                <i class="glyphicon glyphicon-minus text-danger"/>
                              </node-filter-link>
                          </span>
                  </span>
              </span>
        </td>
      </tr>
      </tbody>
      <!--  node attributes with no namespaces -->
      <tbody>
      <tr class="hover-action-holder" v-for="(value,attr) in (useNamespace?displayAttributes:attributes)">
        <td class="key setting">
          <node-filter-link :filter-key="attr"
                            filter-val=".*"
                            @nodefilterclick="filterClick"
          >{{ attr }}:
          </node-filter-link>
        </td>
        <td class="setting" colspan="3">
          <div class="value">
            {{ attributes[attr] }}

            <node-filter-link :filter-key="attr"
                              :filter-val="value"
                              class="textbtn textbtn-info textbtn-saturated hover-action"
                              @nodefilterclick="filterClick"
            >
              <i class="glyphicon glyphicon-plus text-success"/>
            </node-filter-link>

            <node-filter-link
                v-if="showExcludeFilterLinks"
                :exclude="true"
                :filter-key="attr"
                :filter-val="value"
                class="text-danger textbtn textbtn-info textbtn-saturated hover-action"
                @nodefilterclick="filterClick"
            >
              <i class="glyphicon glyphicon-minus text-danger"/>
            </node-filter-link>
          </div>

        </td>
      </tr>
      </tbody>

      <template v-if="useNamespace">
        <!-- node attributes with namespaces -->

        <template v-for="(namespace,idx) in attributeNamespaces">
          <tr>
            <td class="key namespace">
              <a
                  href="#"
                  role="button"
                  @click="toggleNs(namespace.ns)"
                  :class="{'active': uiNs[namespace.ns]}"
                  class="textbtn textbtn-muted textbtn-saturated ">
                {{ namespace.ns }}
                <i class="auto-caret "></i>
              </a>
            </td>
            <td colspan="3" class="text-muted">
              {{ namespace.values.length }}
            </td>
          </tr>
          <tbody class="subattrs collapse collapse-expandable" :class="{'in':uiNs[namespace.ns]}">

          <template v-for="nsattr in namespace.values">
            <tr class="hover-action-holder">

              <td class="key setting">

                <node-filter-link :filter-key="nsattr.name"
                                  filter-val=".*"
                                  @nodefilterclick="filterClick"
                >{{ nsattr.shortname }}:
                </node-filter-link>
              </td>
              <td class="setting " colspan="3">
                <div class="value">
                  {{ nsattr.value }}

                  <node-filter-link :filter-key="nsattr.name"
                                    :filter-val="nsattr.value"
                                    class="textbtn textbtn-info textbtn-saturated hover-action"
                                    @nodefilterclick="filterClick"
                  ><i class="glyphicon glyphicon-search"/></node-filter-link>

                  <node-filter-link v-if=showExcludeFilterLinks
                                    :exclude="true"
                                    :filter-key="nsattr.name"
                                    :filter-val="nsattr.value"
                                    class="text-danger textbtn textbtn-info textbtn-saturated hover-action"
                                    @nodefilterclick="filterClick"
                  ><i class="glyphicon glyphicon-zoom-out text-danger"/></node-filter-link>
                </div>
              </td>

            </tr>
          </template>
          </tbody>
        </template>


      </template>

    </table>
  </div>
</template>
<script lang="ts">
import NodeFilterLink from '@/app/components/job/resources/NodeFilterLink.vue'
import NodeIcon from '@/app/components/job/resources/NodeIcon.vue'
import NodeStatus from '@/app/components/job/resources/NodeStatus.vue'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop} from 'vue-property-decorator'

const OsAttributeNames = 'nodename hostname username description tags osFamily osName osVersion osArch'.split(' ')

@Component({
  components: {NodeIcon, NodeStatus, NodeFilterLink}
})
export default class NodeDetailsSimple extends Vue {
  @Prop({required: true})
  attributes!: any
  @Prop({required: false, default: () => []})
  tags!: Array<string>
  @Prop({required: false, default: false})
  showExcludeFilterLinks!: boolean

  @Prop({required: false, default: false})
  useNamespace!: boolean
  @Prop({required: false, default: false})
  authrun!: boolean
  @Prop({required: false, default: () => []})
  filterColumns!: Array<string>
  @Prop({required: false, default: false})
  nodeColumns!: boolean

  uiNs: { [name: string]: boolean } = {}

  OsTestNames = 'osFamily osName osVersion osArch'.split(' ')

  toggleNs(ns: string) {
    let val = this.uiNs[ns]
    console.log(`toggle ${ns} ${val} = ${!val}`)
    Vue.set(this.uiNs, ns, !val)
  }

  hasOsData() {
    return this.OsTestNames.findIndex((val) => this.attributes[val]) >= 0
  }

  get useDefaultColumns() {
    return this.filterColumns.length < 1 && this.nodeColumns
  }

  filterClick(filter: any) {
    this.$emit('filter', filter)
  }

  /**
   * Return an object with only attributes for display, excluding ui: namespace, and osAttrs
   *
   * @param attrs
   */
  get displayAttributes() {
    let result: any = {}
    for (let e in this.attributes) {
      if (e.indexOf(':') < 0 && OsAttributeNames.indexOf(e) < 0) {
        result[e] = this.attributes[e]
      }
    }
    return result
  };

  startsWith(a: string, b: string) {
    return (a.length >= (b.length)) && a.substring(0, b.length) == b
  }

  attributesInNamespace(attrs: any, ns: string) {
    let result = []
    for (let e in attrs) {
      if (this.startsWith(e, ns + ':') && attrs[e]) {
        result.push({name: e, value: attrs[e], shortname: e.substring(ns.length + 1)})
      }
    }
    result.sort(function(a, b) {
      return a.shortname.localeCompare(b.shortname)
    })
    return result
  };

  attributeNamespaceRegex = /^(.+?):.+$/

  attributeNamespaceNames(attrs: any) {
    let namespaces = []
    for (let e in attrs) {
      let found = e.match(this.attributeNamespaceRegex)
      if (found && found.length > 1) {
        if (namespaces.indexOf(found[1]) < 0) {
          namespaces.push(found[1])
        }
      }
    }
    namespaces.sort()
    return namespaces
  };

  get attributeNamespaces() {
    let index: any = {}
    let names = []
    for (let e in this.attributes) {
      let found = e.match(this.attributeNamespaceRegex)
      if (found && found.length > 1) {
        if (!index[found[1]]) {
          index[found[1]] = []
          names.push(found[1])
        }
        index[found[1]].push({
          name: e,
          value: this.attributes[e],
          shortname: e.substring(found[1].length + 1)
        })
      }
    }
    names.sort()

    let results = []
    for (let i = 0; i < names.length; i++) {
      let values = index[names[i]]
      values.sort(function(a: any, b: any) {
        return a.shortname.localeCompare(b.shortname)
      })
      results.push({ns: names[i], values: values})
    }

    return results
  };
}
</script>
<style type="scss">
.text-parenthetical:before {
  content: '('
}

.text-parenthetical:after {
  content: ')'
}
</style>