import { defineStore } from "pinia";
import { NodeFilterStore } from "./NodeFilterLocalstore";
import { Nodes } from "../../app/components/job/resources/types/nodeTypes";
import {
  getNodes,
  getNodeTags,
} from "../../app/components/job/resources/services/nodeServices";
import { getAppLinks } from "../rundeckService";

const filterStore = new NodeFilterStore();

export const useNodesStore = defineStore("nodes", {
  state: () => ({
    maxShown: 20,
    nodesCollection: {},
    tags: [],
    nodeFilterStore: filterStore,
  }),
  getters: {
    nodes: (state): Nodes[] => Object.values(state.nodesCollection),
    total(): number {
      return this.nodes.length ? this.nodes.length : 0;
    },
  },
  actions: {
    updateCollection(data) {
      data.forEach((node) => {
        this.nodesCollection[node.nodename] = node;
      });
    },
    async fetchTags() {
      this.tags = await getNodeTags();
    },
    async fetchNodes(params = {}) {
      // todo: replace it with the newer service once properties are exposed
      // missing properties: attributes + isLocal (can solve this
      // by checking if nodename is localhost
      const response = await getNodes(
        {
          ...params,
          filter: this.nodeFilterStore.selectedFilter,
        },
        getAppLinks().frameworkNodesQueryAjax,
      );
      this.updateCollection(response.allnodes);
    },
  },
});
