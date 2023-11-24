import {ProjectFilters} from "../../../../../library/stores/NodeFilterLocalstore";

interface Tag {
    count: number,
    tag: string
}

interface NodeSummary extends ProjectFilters {
    tags?: Tag[],
    totalCount?: number,
}

export { NodeSummary, Tag }