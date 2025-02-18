import { defineStore } from "pinia";
import { NodeFilterStore } from "./NodeFilterLocalstore";
import {
  Node,
  NodeTag,
  Tag,
} from "../../app/components/job/resources/types/nodeTypes";
import {
  getNodes,
  getNodeTags,
} from "../../app/components/job/resources/services/nodeServices";
import { getAppLinks } from "../rundeckService";

interface NodesState {
  entities: Record<string, Node>;
  nodenamesToDisplay: string[];
  tags: NodeTag[] | Tag[];
  maxSize: number;
  lastCountFetched: number;
  nodeFilterStore: NodeFilterStore;
}

export const useNodesStore = defineStore("nodes", {
  state: (): NodesState => ({
    entities: {},
    nodenamesToDisplay: [],
    tags: [],
    maxSize: 20,
    lastCountFetched: 0,
    nodeFilterStore: new NodeFilterStore(),
  }),

  getters: {
    currentNodes(state): Node[] {
      return state.nodenamesToDisplay
        .map((id) => this.entities[id])
        .filter(Boolean);
    },

    total(): number {
      return this.currentNodes.length;
    },
    isResultsTruncated(state): boolean {
      return state.lastCountFetched > state.maxSize
    }
  },

  actions: {
    upsertNodes(nodes: Node[]) {
      if(nodes.length > 0) {
        nodes.forEach((node) => {
          this.entities[node.nodename] = {
            ...this.entities[node.nodename],
            ...node,
          };
        });

        this.nodenamesToDisplay = nodes.map((node) => node.nodename);
      } else {
        this.nodenamesToDisplay = []
      }
    },

    async fetchNodes(params: Record<string, any> = {}) {
      try {
        const response = await getNodes(
          {
            ...params,
            filter: this.nodeFilterStore.selectedFilter,
            maxSize: this.maxSize,
          },
          getAppLinks().frameworkNodesQueryAjax,
        );
        this.upsertNodes(response.allnodes);
        this.lastCountFetched = response.total;
      } catch (error: any) {
        throw new Error(`Failed to fetch nodes: ${error.message}`);
      }
    },

    async fetchTags() {
      try {
        const results = await getNodeTags();
        this.tags = results.tags;
      } catch (error: any) {
        throw new Error(`Failed to fetch tags: ${error.message}`);
      }
    },

    clearResults() {
      this.nodenamesToDisplay = [];
    },
  },
});
