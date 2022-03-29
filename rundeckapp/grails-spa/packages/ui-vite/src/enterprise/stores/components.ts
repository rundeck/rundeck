import { acceptHMRUpdate, defineStore } from 'pinia'

export const useEnterpriseComponentsStore = defineStore('enterprise-components', () => {
  /**
   * Current name of the user.
   */
  const something = 'something'

  return {
    something
  }
})

if (import.meta.hot) import.meta.hot.accept(acceptHMRUpdate(useEnterpriseComponentsStore, import.meta.hot))
