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

    loadStoredProjectNodeFilters(project: string): ProjectFilters {
        let storedNodeFilters = loadStoredNodeFilters()
        return storedNodeFilters[project] || {}
    }

    getStoredDefaultFilter(project): string {
        return loadStoredProjectNodeFilters(project).defaultFilter
    }

    setStoredDefaultFilter(project: string, name: string) {
        let storedNodeFilters = loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: [], defaultFilter: name}
        } else {
            storedNodeFilters[project].defaultFilter = name
        }

        localStorage.setItem(STORAGE_NODE_FILTER_KEY, JSON.stringify(storedNodeFilters))
    }

    removeStoredDefaultFilter(project: string) {
        let storedNodeFilters = loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: []}
        } else {
            storedNodeFilters[project].defaultFilter = null
        }
        localStorage.setItem(STORAGE_NODE_FILTER_KEY, JSON.stringify(storedNodeFilters))
    }

    saveFilter(project: string, filter: StoredFilter) {
        let storedNodeFilters = loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            storedNodeFilters[project] = {filters: []}
        }
        storedNodeFilters[project].filters.push(filter)

        localStorage.setItem(STORAGE_NODE_FILTER_KEY, JSON.stringify(storedNodeFilters))
    }

    removeFilter(project: string, filterName: string) {
        let storedNodeFilters = loadStoredNodeFilters()
        if (!storedNodeFilters[project]) {
            return
        }
        storedNodeFilters[project].filters = storedNodeFilters[project].filters.filter(f => f.filterName !== filterName)
        localStorage.setItem(STORAGE_NODE_FILTER_KEY, JSON.stringify(storedNodeFilters))
    }

}