<template>
  <div class="rdtabs" :class="`rdtabs--${type}`">
    <div class="rdtabs__tabheader" :class="`rdtabs__tabheader--${type}`">
      <div class="rdtabs__tabheader-inner">
        <div :class="`rdtabs__tablist rdtabs__tablist--${type}`" role="tablist" :aria-label="label">
          <div :class="{
                  'rdtabs__leftendcap': true,
                  'rdtabs__tab-component': true,
                  'rdtabs__tab-previous': activeTab === 0
                  }"
          >
            <div class="rdtabs__tab-inner"/></div>
            <div v-for="(tab, i) in $slots.default()"
                 :key="tab.title"
                 :class="{
                  'rdtabs__tab': true,
                  'rdtabs__tab-component': true,
                  'rdtabs__tab--active': i === activeTab,
                  'rdtabs__tab-previous': i === (activeTab - 1)
                  }"
                 role="tab"
                 :aria-selected="i === activeTab"
                 tabindex="0"
                 data-test-id="tabs-title-button"
                 @click="selectTab(i)"
                 @keydown="(ev) => handleKeypress(ev, tab)"
            >
                {{ tab.props?.title }}
            </div>
            <div :class="{'rdtabs__rightendcap': true, 'rdtabs__tab-component': true}"><div class="rdtabs__tab-inner"/></div>
          </div>
      </div>
    </div>
    <div class="rdtabs__pane" role="tabpanel">
      <slot></slot>
    </div>
  </div>
</template>

<script>
import {defineComponent, computed} from 'vue'

export default defineComponent({
    name: 'Tabs',
    data(){
        return {
            activeTab: this.modelValue || 0,
        }
    },
    props:{
        type: {
            type: String,
            default: 'standard'
        },
        label: {type: String, default: 'Tabs'},
        modelValue: {
            type: Number,
            default: 0,
        }
    },
    emits: ['update:modelValue'],
    provide() {
      return {
        selectedIndex: computed(() => this.activeTab),
      }
    },
    methods: {
        selectTab (i) {
            this.activeTab = i
            this.$emit('update:modelValue', i)
        },
        handleKeypress(ev, tab) {
          if (ev.code === 'Space')
            this.selectTab(tab.index)
        },
    },
    mounted () {
        this.selectTab(0)
    },
})
</script>

<style lang="scss">
@import 'tabs.scss';
@import "tabs-standard.scss";
@import "tabs-rounded.scss";
</style>