<template>
  <ul>
    <li v-for="i in items" v-if="items.length>0">
      <template v-if="i.job">
        {{ i.jobName }}
      </template>
      <template v-else>
        <span>
          <btn @click="toggle(i.groupPath)" class="btn-link" size="sm">
              <i class="glyphicon"
                 :class="{'glyphicon-chevron-right':!isExpanded(i.groupPath),'glyphicon-chevron-down':isExpanded(i.groupPath)}"></i>
              {{ lastPathItem(i.groupPath) }}
          </btn>
        </span>
        <Browser :path="i.groupPath" v-if="isExpanded(i.groupPath)"/>
      </template>
    </li>
    <li v-if="items.length===0">
      Nada
    </li>
  </ul>
</template>

<script lang="ts">

import {JobBrowserStore, JobBrowserStoreInjectionKey} from '@/library/stores/JobBrowser'
import {JobBrowseItem} from '@/library/types/jobs/JobBrowse'
import {defineComponent, inject, ref} from 'vue'

export default defineComponent({
  name: 'Browser',
  props: {
    path: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    return {
      jobBrowserStore: inject(JobBrowserStoreInjectionKey) as JobBrowserStore,
      items: ref([] as JobBrowseItem[]),
      expandedItems: ref([]),
      wasExpanded: []
    }
  },
  methods: {
    isExpanded(path: string) {
      return this.expandedItems.indexOf(path) >= 0
    },
    toggle(path: string) {
      if (this.isExpanded(path)) {
        this.expandedItems = this.expandedItems.filter(i => i !== path)
      } else {
        this.expandedItems.push(path)
      }
    },
    lastPathItem(path: string) {
      const parts = path.split('/')
      return parts[parts.length - 1]
    }
  },
  async mounted() {
    this.items = await this.jobBrowserStore.loadItems(this.path)
  }
})
</script>

<style scoped lang="scss">

</style>