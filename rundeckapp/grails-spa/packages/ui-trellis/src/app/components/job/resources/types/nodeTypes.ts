import { ProjectFilters } from "../../../../../library/stores/NodeFilterLocalstore";

interface Tag {
  count: number;
  tag: string;
}

interface NodeSummary extends ProjectFilters {
  tags?: Tag[];
  totalCount?: number;
}

interface Node {
  attributes?: { [key: string]: string };
  isLocal?: boolean;
  authrun?: boolean;
  nodename: string;
  tags?: Tag[] | string;
  hostname?: string;
  username?: string;
  description?: string;
  osFamily?: string;
  osArch?: string;
  osName?: string;
  editUrl?: string;
  remoteUrl?: string;
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

export { Node, Nodes, NodeSummary, Tag, NodeTag, NodeTags };
