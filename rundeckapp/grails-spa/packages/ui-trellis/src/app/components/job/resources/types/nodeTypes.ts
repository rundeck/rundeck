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
  attributes: { [key: string]: string };
  isLocal?: boolean;
  authrun?: boolean;
  nodename: string;
  tags: Tag[];
}

export { Node, NodeSummary, Tag };
