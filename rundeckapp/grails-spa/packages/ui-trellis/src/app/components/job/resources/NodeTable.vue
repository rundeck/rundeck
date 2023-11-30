<template>
  <div>
    <div id="nodes_tags" v-if="nodeSet.tagsummary">
      <span v-for="tag in Object.entries(nodeSet.tagsummary)">
        <span class="summary nodetags">
          <node-filter-link
              class="label label-muted link-quiet"
              filterKey="tags"
              :filterVal="tag[0]"
              @nodefilterclick="filterClick"
          >
            <template #suffix> ({{ tag[1] }}) </template>
          </node-filter-link>
        </span>
      </span>
    </div>
    <template v-if="!remoteEditStarted">
      <div class="row row-space">
        <div class="col-sm-12">
          <table id="nodesTable" class="nodesTable">
            <thead>
            <tr>
              <th class="table-header">
                {{ $t("Node") }}
              </th>
              <th
                  v-for="filter in filterColumns"
                  class="text-capitalize table-header"
              >
                {{ filter }}
              </th>
              <template v-if="useDefaultColumns">
                <th class="table-header">
                  {{ $t("resource.metadata.entity.tags") }}
                </th>
                <th colspan="3" class="table-header">
                  {{ $t("user.at.host") }}
                </th>
              </template>
              <th></th>
            </tr>
            </thead>
            <tbody>
            <template v-for="(node, index) in nodeSet.nodes">
              <tr
                  class="node_entry hover-action-holder ansicolor-on"
                  :class="{ server: node.islocal || false }"
              >
                <td class="nodeident" :title="node.attributes.description">
                  <a
                      class="link-quiet"
                      data-toggle="collapse"
                      :href="`#detail_${index}1`"
                  >
                    <i class="auto-caret text-muted"></i>
                    <span
                        class="node_ident"
                        :class="[
                          { server: node.isLocal },
                          nodeCss(node.attributes),
                        ]"
                        :style="[
                          styleForNode(node.attributes),
                          'margin-left:4px',
                        ]"
                    >
                        <span
                            :class="cssForIcon(node.attributes)"
                            :style="[
                            styleForIcon(node.attributes),
                            'margin-right: 4px',
                          ]"
                        >
                          <i
                              v-if="node.attributes['ui:icon:name']"
                              :class="
                              glyphiconForName(node.attributes['ui:icon:name'])
                            "
                          ></i>
                          <i v-else class="fas fa-hdd"></i>
                        </span>
                        <span>
                          {{ node.nodename }}
                        </span>
                      </span>
                  </a>

                  <node-filter-link
                      class="link-quiet"
                      style="margin: 0 5px"
                      filter-key="name"
                      :filter-val="node.nodename!"
                      @nodefilterclick="filterClick"
                  >
                    <i class="glyphicon glyphicon-circle-arrow-right"></i>
                  </node-filter-link>

                  <span class="nodedesc"></span>
                  <span class="text-strong">
                      <i
                          v-if="node.attributes['ui:badges']"
                          v-for="badge in glyphiconBadges(node.attributes)"
                          :class="glyphiconForName(badge)"
                          :key="badge"
                      ></i>
                      <span>
                        {{ node.attributes.description }}
                      </span>
                    </span>
                  <span
                      :class="statusIconCss(node.attributes)"
                      :style="statusIconStyle(node.attributes)"
                  >
                      <i
                          v-if="node.attributes['ui:status:icon']"
                          :class="
                          glyphiconForName(node.attributes['ui:status:icon'])
                        "
                      ></i>
                      <span v-if="node.attributes['ui:status:text']">
                        {{ node.attributes["ui:status:text"] }}
                      </span>
                    </span>
                </td>

                <td v-for="filter in filterColumns">
                    <span class="value" v-if="node.attributes[filter]">
                      <span v-if="filter === 'tags'">
                        <span class="nodetags">
                          <span v-for="tag in node.tags" :key="tag">
                            <node-filter-link
                                class="label label-muted link-quiet"
                                filter-key="tags"
                                :filter-val="tag"
                                @nodefilterclick="filterClick"
                            ></node-filter-link>
                          </span>
                        </span>
                      </span>
                      <span v-else>
                        <span>
                          {{ node.attributes[filter] }}
                        </span>
                        <node-filter-link
                            class="textbtn textbtn-info"
                            :filter-key="filter"
                            :filter-val="node.attributes[filter]"
                            @nodefilterclick="filterClick"
                        >
                          <i
                              class="
                              glyphicon glyphicon-search
                              textbtn-saturated
                              hover-action
                            "
                          ></i>
                        </node-filter-link>
                      </span>
                    </span>
                </td>

                <template v-if="useDefaultColumns">
                  <td title="Tags" class="nodetags">
                      <span v-if="node.tags">
                        <span class="nodetags">
                          <span v-for="tag in node.tags">
                            <node-filter-link
                                class="link-quiet"
                                style="margin-right: 2px"
                                filter-key="tags"
                                :filter-val="tag"
                                @nodefilterclick="filterClick"
                            ></node-filter-link>
                          </span>
                        </span>
                      </span>
                  </td>
                  <td class="username" :title="$t('node.metadata.username')">
                      <span v-if="node.attributes.username">
                        <node-filter-link
                            class="link-quiet"
                            filter-key="username"
                            :filter-val="node.attributes.username"
                            @nodefilterclick="filterClick"
                        ></node-filter-link>
                        <span class="atsign">@</span>
                      </span>
                  </td>
                  <td class="hostname" title="Hostname">
                      <span v-if="node.attributes.hostname">
                        <node-filter-link
                            class="link-quiet"
                            filter-key="hostname"
                            :filter-val="node.attributes.hostname"
                            @nodefilterclick="filterClick"
                        ></node-filter-link>
                      </span>
                    <span v-else>
                        <span
                            class="text-warning"
                            :title="$t('node.hostname.unset.description')"
                        >
                          {{ $t("node.hostname.unset.label") }}
                        </span>
                      </span>
                  </td>
                </template>

                <td>
                    <span v-if="node.attributes['remoteUrl']">
                      <span
                          class="textbtn"
                          :title="$t('edit.this.node.via.remote.url')"
                          @click="triggerNodeRemoteEdit(node)"
                      >
                        {{ $t("edit.ellipsis") }}
                      </span>
                    </span>
                  <span v-if="node.attributes['editUrl']">
                      <a
                          target="_blank"
                          :href="
                          expandNodeAttributes(
                            node.attributes,
                            node.attributes['editUrl']
                          )
                        "
                          :title="
                          $t('opens.a.link.to.edit.this.node.at.a.remote.site')
                        "
                      >
                        {{ $t("button.Edit.label") }}
                      </a>
                    </span>
                </td>
              </tr>

              <tr
                  class="detail_content nodedetail collapse collapse-expandable"
                  :class="{ server: node.isLocal }"
                  :id="`detail_${index}1`"
              >
                <td :colspan="totalColumnsCount">
                  <node-details-simple
                      :key="node.nodename"
                      :attributes="node.attributes"
                      :authrun="node.authrun"
                      :useNamespace="true"
                      :tags="node.tags || []"
                      :node-columns="true"
                      :filter-columns="filterColumns"
                      @filter="filterClick"
                  />
                </td>
              </tr>
            </template>
            </tbody>
          </table>
        </div>
      </div>
      <div v-if="hasPaging && !loading">
        <div class="row row-space">
          <div class="col-sm-12" id="nodesPaging">
            <span class="paginate">
              <ul class="pagination pagination-sm pagination-embed">
                <li :class="{ disabled: page === 0 || loading }">
                  <a
                      :href="browseNodesPagePrevUrl"
                      class="btn btn-xs btn-default"
                      :class="{ visible: maxPages > 1 }"
                      @click.prevent="browseNodesPagePrev"
                      :title="$t('Previous')"
                  >
                    {{ $t("default.paginate.prev") }}
                  </a>
                </li>
                <li
                    v-for="num in pageNumbersSkipped"
                    :key="`skipped_${num}`"
                    :class="{
                    active: num === page,
                    disabled: num === '..' || loading,
                  }"
                >
                  <a v-if="num === '..'" href="#"> &hellip; </a>
                  <a
                      v-else
                      :href="browseNodesPageUrl(num)"
                      @click.prevent="browseNodesPage(num)"
                  >
                    {{ num + 1 }}
                  </a>
                </li>
                <li :class="{ disabled: page === maxPages || loading }">
                  <a
                      class="btn btn-xs btn-default"
                      :class="{ visible: maxPages > 1 }"
                      :href="browseNodesPageNextUrl"
                      @click.prevent="browseNodesPageNext"
                      :title="$t('Next')"
                  >
                    {{ $t("default.paginate.next") }}
                  </a>
                </li>
              </ul>
            </span>
            <div class="form-inline">
              <div class="form-group form-group-sm">
                <label>
                  {{ $t("jump.to") }}
                  <input
                      @input="event => browseNodesPage(Number(event.target.value) - 1)"
                      :value="page + 1"
                      :disabled="loading"
                      class="form-control input-sm"
                      type="number"
                      min="1"
                      :max="maxPages"
                  />
                </label>
              </div>
              <div class="form-group form-group-sm">
                <label>
                  {{ $t("per.page") }}
                  <input
                      @input="changePaginationMax"
                      :value="pagingMax"
                      :disabled="loading"
                      class="form-control input-sm"
                      type="number"
                      min="1"
                      max="100"
                  />
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <div class="row">
      <div class="col-sm-12">
        <span v-if="loading" class="text-info">
          <i class="glyphicon glyphicon-time"></i>
          {{ $t("loading.matched.nodes") }}
        </span>
      </div>
    </div>
  </div>
  <NodeRemoteEdit
      v-if="remoteEditStarted"
      :nodename="remoteEditNodename"
      :remote-url="remoteUrl"
      @remoteEditStop="stopNodeRemoteEdit"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { _genUrl } from "@/app/utilities/genUrl";
import { getAppLinks } from "@/library";
import NodeDetailsSimple from "@/app/components/job/resources/NodeDetailsSimple.vue";
import NodeRemoteEdit from "@/app/components/job/resources/NodeRemoteEdit.vue";
import {
  cssForIcon,
  expandNodeAttributes,
  glyphiconBadges,
  glyphiconForName,
  nodeBgCss,
  nodeFgCss,
  statusIconCss,
  statusIconStyle,
  styleForIcon,
  styleForNode,
} from "@/app/utilities/nodeUi";
import NodeFilterLink from "@/app/components/job/resources/NodeFilterLink.vue";

export default defineComponent({
  name: "NodeTable",
  components: { NodeFilterLink, NodeRemoteEdit, NodeDetailsSimple },
  emits: ["filter", "changePage", "changePagingMax"],
  props: {
    nodeSet: {
      type: Object,
    },
    filterColumns: {
      type: Array as PropType<string[]>,
      required: false,
      default: () => [],
    },
    loading: {
      type: Boolean,
      default: false,
    },
    hasPaging: {
      type: Boolean,
      default: false,
    },
    pagingMax: {
      type: Number,
      default: 20,
    },
    page: {
      type: Number,
      default: 0,
    },
    maxPages: {
      type: Number,
      required: false,
    },
    filterAll: {
      type: Boolean,
      default: false,
    },
    filter: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      shouldRefresh: false,
      remoteUrl: null,
      remoteEditNodename: null,
    };
  },
  computed: {
    filterColumnsLength():number {
      return Object.keys(this.filterColumns || {}).length;
    },
    useDefaultColumns(): boolean {
      return this.filterColumnsLength < 1;
    },
    totalColumnsCount(): number {
      const filterColumnsLength = Object.keys(this.filterColumns).length;
      return this.useDefaultColumns ? 5 : filterColumnsLength + 2;
    },
    remoteEditStarted(): boolean {
      return this.remoteUrl && this.remoteEditNodename;
    },
    pageNumbersSkipped(): string[] {
      var arr = [];
      var cur = this.page;
      var maxPages = this.maxPages;
      var buffer = 3;
      for (var i = 0; i < maxPages; i++) {
        if (
            (i >= cur - buffer && i <= cur + buffer) ||
            i < buffer ||
            i >= maxPages - buffer
        ) {
          arr.push(i);
        } else if (i == cur - buffer - 1 || i == cur + buffer + 1) {
          arr.push("..");
        }
      }
      return arr;
    },
    browseNodesPageNextUrl(): string {
      return this.browseNodesPageUrl(this.page + 1);
    },
    browseNodesPagePrevUrl(): string {
      return this.browseNodesPageUrl(this.page - 1);
    },
  },
  methods: {
    statusIconStyle,
    glyphiconBadges,
    glyphiconForName,
    styleForIcon,
    cssForIcon,
    statusIconCss,
    styleForNode,
    expandNodeAttributes,
    nodeCss(attrs) {
      var classnames = [];
      var uiColor = nodeFgCss(attrs);
      if (uiColor) {
        classnames.push(uiColor);
      }
      var uiBgcolor = nodeBgCss(attrs);
      if (uiBgcolor) {
        classnames.push(uiBgcolor);
      }
      const cnames = classnames.join(" ");

      return cnames;
    },
    filterClick(filter: any) {
      this.$emit("filter", filter);
    },
    changePaginationMax(event: any) {
      this.$emit("changePagingMax", Number(event.target.value));
    },
    browseNodesPageNext() {
      if (this.page + 1 < this.maxPages) {
        this.browseNodesPage(this.page + 1);
      }
    },
    browseNodesPagePrev() {
      if (this.page > 0) {
        this.browseNodesPage(this.page - 1);
      }
    },
    browseNodesPage(newPage) {
      if (this.page === newPage) {
        return;
      } else {
        this.$emit("changePage", newPage);
      }
    },
    browseNodesPageUrl(page: number|string):string {
      var pageParams = {
        filter: this.filter || "",
        filterAll: this.filterAll,
        page: page,
        max: this.pagingMax,
      };
      const castPage = parseInt(page as string);
      if (castPage >= 0 && castPage < this.maxPages) {
        pageParams.page = castPage;
      } else {
        pageParams.page = 0;
      }
      return _genUrl(getAppLinks().frameworkNodes, pageParams);
    },

    triggerNodeRemoteEdit(node) {
      if (node.attributes["remoteUrl"]) {
        this.remoteUrl = node.attributes["remoteUrl"];
        this.remoteEditNodename = node.nodename;
      }
    },
    stopNodeRemoteEdit() {
      this.remoteUrl = null;
      this.remoteEditNodename = null;
    },
  },
});
</script>

<style>
.nodesTable {
  border-spacing: 0;
  border-collapse: collapse;
  width: 100%;
}
</style>