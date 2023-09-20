export const STORAGE_ACTIVITY_FILTER_KEY = 'activity-filters'

export interface ActivityFilter {
  filterName: string
  query: { [key: string]: string }
}

export interface ProjectActivityFilters {
  filters?: ActivityFilter[]
  defaultFilter?: string
}

export interface StoredActivityFilterSet {
  [key: string]: ProjectActivityFilters
}

/**
 * Use local storage for managing saved node filters
 */
export class ActivityFilterStore {

  storeFilters(filters: StoredActivityFilterSet) {
    debugger
    localStorage.setItem(STORAGE_ACTIVITY_FILTER_KEY, JSON.stringify(filters))
  }

  loadStoredFilters(): StoredActivityFilterSet {
    const settings = localStorage.getItem(STORAGE_ACTIVITY_FILTER_KEY)

    if (settings) {
      try {
        return JSON.parse(settings)
      } catch (e) {
        localStorage.removeItem(STORAGE_ACTIVITY_FILTER_KEY)
      }
    }
    return {}
  }

  loadForProject(project: string): ProjectActivityFilters {
    let storedNodeFilters = this.loadStoredFilters()
    return storedNodeFilters[project] || {}
  }

  getStoredDefaultFilter(project: string): string | undefined {
    return this.loadForProject(project).defaultFilter
  }

  setStoredDefaultFilter(project: string, name: string) {
    let storedNodeFilters = this.loadStoredFilters()
    if (!storedNodeFilters[project]) {
      storedNodeFilters[project] = {filters: [], defaultFilter: name}
    } else {
      storedNodeFilters[project].defaultFilter = name
    }

    this.storeFilters(storedNodeFilters)
  }

  removeStoredDefaultFilter(project: string) {
    let storedNodeFilters = this.loadStoredFilters()
    if (!storedNodeFilters[project]) {
      storedNodeFilters[project] = {filters: []}
    } else {
      storedNodeFilters[project].defaultFilter = undefined
    }
    this.storeFilters(storedNodeFilters)
  }

  saveFilter(project: string, filter: ActivityFilter) {
    let storedNodeFilters = this.loadStoredFilters()
    if (!storedNodeFilters[project]) {
      storedNodeFilters[project] = {filters: [filter]}
    } else if (!storedNodeFilters[project].filters) {
      storedNodeFilters[project].filters = [filter]
    } else {
      storedNodeFilters[project]?.filters?.push(filter)
    }

    this.storeFilters(storedNodeFilters)
  }

  removeFilter(project: string, filterName: string) {
    let storedNodeFilters = this.loadStoredFilters()
    if (!storedNodeFilters[project]?.filters) {
      return
    }
    storedNodeFilters[project].filters = storedNodeFilters[project]?.filters?.filter(f => f.filterName !== filterName) || []
    this.storeFilters(storedNodeFilters)
  }
}