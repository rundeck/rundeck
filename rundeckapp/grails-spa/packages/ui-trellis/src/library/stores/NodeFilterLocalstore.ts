export interface StoredFilter {
    filterName: string
    isJobEdit: boolean
    filter: string
}

export interface ProjectFilters {
    filters?: StoredFilter[]
    defaultFilter?: string
}

export interface StoredNodeFilters {
    [key: string]: ProjectFilters
}

export const STORAGE_NODE_FILTER_KEY = 'node-filters'

/**
 * Use local storage for managing saved node filters
 */
export class NodeFilterStore {
    private filter: string = '';

    constructor(data: any = {}) {
        this.filter = data.filter || this.filter;
    }

    get selectedFilter(): string {
        return this.filter;
    }

    setSelectedFilter(newSelection: string) {
        this.filter = newSelection;
    }

    loadStoredNodeFilters(): StoredNodeFilters {
        const settings = localStorage.getItem(STORAGE_NODE_FILTER_KEY)

        if (settings) {
            try {
                return JSON.parse(settings)
            } catch (e) {
                localStorage.removeItem(STORAGE_NODE_FILTER_KEY)
            }
        }
        return {}
    }

    storeNodeFilters(nodeFilters: StoredNodeFilters) {
        localStorage.setItem(STORAGE_NODE_FILTER_KEY, JSON.stringify(nodeFilters))
    }

    loadStoredProjectNodeFilters(project: string): ProjectFilters {
        let storedNodeFilters = this.loadStoredNodeFilters()
        return storedNodeFilters[project] || {}
    }

    getStoredDefaultFilter(project: string): string|undefined {
        return this.loadStoredProjectNodeFilters(project).defaultFilter
    }

    setStoredDefaultFilter(project: string, name: string) {
        let storedNodeFilters = this.loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: [], defaultFilter: name}
        } else {
            storedNodeFilters[project].defaultFilter = name
        }

        this.storeNodeFilters(storedNodeFilters)
    }

    removeStoredDefaultFilter(project: string) {
        let storedNodeFilters = this.loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: []}
        } else {
            storedNodeFilters[project].defaultFilter = undefined
        }
        this.storeNodeFilters(storedNodeFilters)
    }

    saveFilter(project: string, filter: StoredFilter) {
        let storedNodeFilters = this.loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: [filter]}
        } else if (!storedNodeFilters[project].filters) {
            storedNodeFilters[project].filters = [filter]
        }else{
            storedNodeFilters[project]?.filters?.push(filter)
        }

        this.storeNodeFilters(storedNodeFilters)
    }

    removeFilter(project: string, filterName: string) {
        let storedNodeFilters = this.loadStoredNodeFilters()
        if (!storedNodeFilters[project]?.filters) {
            return
        }
        storedNodeFilters[project].filters = storedNodeFilters[project]?.filters?.filter(f => f.filterName !== filterName)||[]
        this.storeNodeFilters(storedNodeFilters)
    }

}