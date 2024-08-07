<template>
  <div>
    <div v-if="nodeSet.tagsummary" id="nodes_tags">
      <span v-for="tag in Object.entries(nodeSet.tagsummary)">
        <span class="summary nodetags">
          <node-filter-link
            class="label label-muted link-quiet"
            filter-key="tags"
            :filter-val="tag[0]"
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
                  :data-test-id="'node-entry-' + index"
                  class="node_entry hover-action-holder ansicolor-on"
                  :class="{ server: node.islocal || false }"
                >
                  <td class="nodeident" :title="node.attributes.description">
                    <a
                      class="link-quiet"
                      data-toggle="collapse"
                      :href="`#detail_${index}1`"
                      data-testid="node-collapse-link"
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
                          data-test-id="node-icon"
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
                        v-for="badge in glyphiconBadges(node.attributes)"
                        v-if="node.attributes['ui:badges']"
                        :key="badge"
                        :class="glyphiconForName(badge)"
                        data-testid="node-badge-icon"
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
                    <span v-if="node.attributes[filter]" class="value">
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
                        <span :data-test-id="'node-attribute-' + filter">
                          {{ node.attributes[filter] }}
                        </span>
                        <node-filter-link
                          class="textbtn textbtn-info"
                          :filter-key="filter"
                          :filter-val="node.attributes[filter]"
                          :data-test-id="'node-attribute-link-' + filter"
                          @nodefilterclick="filterClick"
                        >
                          <i
                            class="glyphicon glyphicon-search textbtn-saturated hover-action"
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
                            node.attributes['editUrl'],
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
                  :id="`detail_${index}1`"
                  class="detail_content nodedetail collapse collapse-expandable"
                  :class="{ server: node.isLocal }"
                >
                  <td :colspan="totalColumnsCount">
                    <node-details-simple
                      :key="node.nodename"
                      :attributes="node.attributes"
                      :authrun="node.authrun"
                      :use-namespace="true"
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
          <div id="nodesPaging" class="col-sm-12">
            <span class="paginate">
              <ul class="pagination pagination-sm pagination-embed">
                <li :class="{ disabled: page === 0 || loading }">
                  <a
                    :href="browseNodesPagePrevUrl"
                    class="btn btn-xs btn-default"
                    :class="{ visible: maxPages > 1 }"
                    :title="$t('Previous')"
                    @click.prevent="browseNodesPagePrev"
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
                    {{ calculatePageNumber(num) }}
                  </a>
                </li>
                <li :class="{ disabled: page === maxPages || loading }">
                  <a
                    class="btn btn-xs btn-default"
                    :class="{ visible: maxPages > 1 }"
                    :href="browseNodesPageNextUrl"
                    :title="$t('Next')"
                    @click.prevent="browseNodesPageNext"
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
                    :value="page + 1"
                    :disabled="loading"
                    class="form-control input-sm"
                    type="number"
                    min="1"
                    :max="maxPages"
                    @input="
                      (event) => browseNodesPage(Number(event.target.value) - 1)
                    "
                  />
                </label>
              </div>
              <div class="form-group form-group-sm">
                <label>
                  {{ $t("per.page") }}
                  <input
                    :value="pagingMax"
                    :disabled="loading"
                    class="form-control input-sm"
                    type="number"
                    min="1"
                    max="100"
                    @input="changePaginationMax"
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
    @remote-edit-stop="stopNodeRemoteEdit"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { _genUrl } from "../../../utilities/genUrl";
import { getAppLinks } from "../../../../library";
import NodeDetailsSimple from "./NodeDetailsSimple.vue";
import NodeRemoteEdit from "./NodeRemoteEdit.vue";
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
} from "../../../utilities/nodeUi";
import NodeFilterLink from "./NodeFilterLink.vue";
import { Node } from "./types/nodeTypes";

export default defineComponent({
  name: "NodeTable",
  components: { NodeFilterLink, NodeRemoteEdit, NodeDetailsSimple },
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
  emits: ["filter", "changePage", "changePagingMax"],
  data() {
    return {
      shouldRefresh: false,
      remoteUrl: null as null | string,
      remoteEditNodename: null as null | string,
    };
  },
  computed: {
    filterColumnsLength(): number {
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
      return !!(this.remoteUrl && this.remoteEditNodename) || false;
    },
    pageNumbersSkipped(): string[] {
      const arr = [];
      const cur = this.page;
      const maxPages = this.maxPages;
      const buffer = 3;
      if (maxPages) {
        for (let i = 0; i < maxPages; i++) {
          if (
            (i >= cur - buffer && i <= cur + buffer) ||
            i < buffer ||
            i >= maxPages - buffer
          ) {
            arr.push(`${i}`);
          } else if (i == cur - buffer - 1 || i == cur + buffer + 1) {
            arr.push("..");
          }
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
    nodeCss(attrs: { [key: string]: string }) {
      const classnames = [];
      const uiColor = nodeFgCss(attrs);
      if (uiColor) {
        classnames.push(uiColor);
      }
      const uiBgcolor = nodeBgCss(attrs);
      if (uiBgcolor) {
        classnames.push(uiBgcolor);
      }

      return classnames.join(" ");
    },
    filterClick(filter: any) {
      this.$emit("filter", filter);
    },
    changePaginationMax(event: any) {
      this.$emit("changePagingMax", Number(event.target.value));
    },
    browseNodesPageNext() {
      if (this.maxPages && this.page + 1 < this.maxPages) {
        this.browseNodesPage(this.page + 1);
      }
    },
    browseNodesPagePrev() {
      if (this.page > 0) {
        this.browseNodesPage(this.page - 1);
      }
    },
    browseNodesPage(newPage: number | string) {
      if (this.page === newPage) {
        return;
      } else {
        this.$emit("changePage", newPage);
      }
    },
    browseNodesPageUrl(page: number | string): string {
      const pageParams = {
        filter: this.filter || "",
        filterAll: this.filterAll,
        page: page,
        max: this.pagingMax,
      };
      const castPage = parseInt(page as string);
      if (castPage >= 0 && this.maxPages && castPage < this.maxPages) {
        pageParams.page = castPage;
      } else {
        pageParams.page = 0;
      }
      return _genUrl(getAppLinks().frameworkNodes, pageParams);
    },

    triggerNodeRemoteEdit(node: Node) {
      if (node.attributes["remoteUrl"]) {
        this.remoteUrl = node.attributes["remoteUrl"];
        this.remoteEditNodename = node.nodename;
      }
    },
    stopNodeRemoteEdit() {
      this.remoteUrl = null;
      this.remoteEditNodename = null;
    },
    calculatePageNumber(page: string): number {
      return parseInt(page) + 1;
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
