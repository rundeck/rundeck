<template>
    <span v-if="items">
        <slot v-if="items.length<1"></slot>
        <template v-for="i in items">
            <template v-if="i.text">{{ i.text }}</template>
            <span v-else-if="i.html" v-html="i.html"></span>
            <component v-else-if="i.widget" :is="i.widget" :event-bus="eventBus" :item-data="itemData">
                <slot></slot>
            </component>
        </template>
    </span>
</template>
<script lang="ts">
import { defineComponent, ref} from 'vue'
import type { PropType } from 'vue'

import {
    getRundeckContext,
} from '../../rundeckService'
import {UIItem, UIWatcher} from '../../stores/UIStore'
import {EventBus} from "../../utilities/vueEventBus"


export default defineComponent({
  name: 'UiSocket',
  props: {
    section: {
      type: String,
      required: true,
    },
    location: {
      type: String,
      required: true,
    },
    eventBus: {
      type: Object as PropType<typeof EventBus>,
      required: false,
    },
    socketData: {
      type: Object,
      required: false,
    },
  },
  setup() {
    const items = ref<UIItem[]>([])
    const uiwatcher = ref<UIWatcher>()
    const rootStore = getRundeckContext().rootStore
    return {
      items,
      uiwatcher,
      rootStore,
    }
  },
  computed: {
    itemData() {
      if (typeof this.socketData === 'string') {
        try {
          return JSON.parse(this.socketData)
        } catch (e) {
          return this.socketData
        }
      }
      return this.socketData
    },
  },
  methods: {
    loadItems() {
      this.items = this.rootStore.ui.itemsForLocation(this.section, this.location).filter((a) => a.visible)
    },
  },
  mounted() {
    this.loadItems()
    this.uiwatcher = {
      section: this.section,
      location: this.location,
      callback: (uiItems: UIItem[]) => {
        this.items = uiItems
      }
    } as UIWatcher
    this.rootStore.ui.addWatcher(this.uiwatcher)
  },
  unmounted() {
    if (this.uiwatcher) {
      this.rootStore.ui.removeWatcher(this.uiwatcher)
    }
  }
})
</script>
