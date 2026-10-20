import { ProjectFilters } from "../../../../../library/stores/NodeFilterLocalstore";

interface Tag {
  count: number;
  tag: string;
}

interface NodeSummary extends ProjectFilters {
  active?: number;
  selectedFilter?: string;
  tags?: Tag[];
  total?: number;
  totalCount?: number;
}

type NodeAttributes = Record<string, string>;

interface Node {
  attributes?: NodeAttributes;
  isLocal?: boolean;
  islocal?: boolean;
  authrun?: boolean;
  nodename: string;
  name?: string;
  tags?: Tag[] | string;
  hostname?: string;
  username?: string;
  description?: string;
  osFamily?: string;
  osArch?: string;
  osName?: string;
  editUrl?: string;
  remoteUrl?: string;
  unselected?: boolean;
}

interface Nodes {
  [key: string]: Node;
}

/*
 * The interfaces NodeTag and NodeTags should be used when using the getNodeTags service method
 */
interface NodeTag {
  nodeCount: number;
  name: string;
}

interface NodeTags {
  tags: NodeTag[] | Tag[];
}

interface NodeFilterSelection {
  filter?: string;
  filterExclude?: string;
  filterName?: string;
  filterNameExclude?: string;
}

interface NodeQueryResponse {
  allcount: number;
  allnodes: Node[];
  colkeys: string[];
  max: number;
  message?: string;
  tagsummary: Tag[] | Record<string, unknown>;
  total: number;
  truncated: boolean;
}

interface NodeSet {
  nodes: Node[];
  tagsummary: Tag[] | Record<string, unknown>;
}

export {
  Node,
  NodeAttributes,
  NodeFilterSelection,
  NodeQueryResponse,
  Nodes,
  NodeSet,
  NodeSummary,
  Tag,
  NodeTag,
  NodeTags,
};
