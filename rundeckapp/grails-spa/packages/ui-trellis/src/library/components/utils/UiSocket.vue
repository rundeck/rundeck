<template>
  <slot v-if="items.length < 1"></slot>
  <template v-for="(i, x) in items" :key="x">
    <template v-if="i.text">{{ i.text }}</template>
    <span v-else-if="i.html" v-html="i.html"></span>
    <component
      :is="i.widget"
      v-else-if="i.widget && eventBus"
      v-model="internalModel"
      :event-bus="eventBus"
      :item-data="itemData"
    >
      <slot></slot>
    </component>
    <component
      :is="i.widget"
      v-else-if="i.widget"
      v-model="internalModel"
      :item-data="itemData"
    >
      <slot></slot>
    </component>
  </template>
</template>
<script lang="ts">
import { defineComponent, ref } from "vue";
import type { PropType } from "vue";

import { getRundeckContext } from "../../rundeckService";
import { UIItem, UIWatcher } from "../../stores/UIStore";
import { EventBus } from "../../utilities/vueEventBus";

export default defineComponent({
  name: "UiSocket",
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
      default: undefined,
    },
    socketData: {
      type: [String, Object] as PropType<string | Record<string, any>>,
      required: false,
      default: undefined,
    },
    modelValue: {
      type: [String, Object] as PropType<string | Record<string, any>>,
      required: false,
      default: undefined,
    },
  },
  emits: ["update:modelValue"],
  setup() {
    const items = ref<UIItem[]>([]);
    const uiwatcher = ref<UIWatcher>();
    const rootStore = getRundeckContext()?.rootStore || null;
    return {
      items,
      uiwatcher,
      rootStore,
    };
  },
  computed: {
    internalModel: {
      get() {
        return this.modelValue;
      },
      set(val: any) {
        this.$emit("update:modelValue", val);
      },
    },
    itemData() {
      if (typeof this.socketData === "string") {
        try {
          return JSON.parse(this.socketData);
        } catch (e) {
          return this.socketData;
        }
      }
      return this.socketData;
    },
  },
  watch: {
    location() {
      this.reload();
    },
    section() {
      this.reload();
    },
  },
  mounted() {
    this.load();
  },
  unmounted() {
    this.unload();
  },
  methods: {
    load() {
      this.loadItems();
      if (this.rootStore) {
        this.uiwatcher = {
          section: this.section,
          location: this.location,
          callback: (uiItems: UIItem[]) => {
            this.items = uiItems.filter((item) => item.visible);
          },
        } as UIWatcher;
        this.rootStore.ui.addWatcher(this.uiwatcher);
      }
    },
    unload() {
      if (this.uiwatcher) {
        this.rootStore.ui.removeWatcher(this.uiwatcher);
        this.uiwatcher = null;
      }
    },
    reload() {
      this.unload();
      this.load();
    },
    loadItems() {
      if (this.rootStore) {
        this.items = this.rootStore.ui
          .itemsForLocation(this.section, this.location)
          .filter((a) => a.visible);
      }
    },
  },
});
</script>
