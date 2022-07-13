<template>
  <div>
    <ul class="tabs__header">
      <li v-for="(tab, index) in tabs" :key="tab.title" @click="selectTab(index)" class="rdtabs__tab">
        {{ tab.title }}
      </li>
    </ul>
    <slot></slot>
  </div>
</template>

<script lang="ts">

import { Component, Vue, Prop } from 'vue-property-decorator';

@Component
export default class Tabs extends Vue {
  selectedIndex = 0
  tabs = []

  selectTab (i: number) {
    this.selectedIndex = i
    // loop over all the tabs
    this.tabs.forEach((tab, index) => {
      //@ts-ignore
      tab.isActive = (index === i)
    })
  }
  mounted () {
    this.selectTab(0)
  }
  created () {
    //@ts-ignore
    this.tabs = this.$children
  }
}

/*
  export default {
    data () {
      return {
        selectedIndex: 0, // the index of the selected tab,
        tabs: [],        // all of the tabs
      }
    },
    mounted () {
      this.selectTab(0)
  },
  methods: {
    selectTab (i) {
      this.selectedIndex = i
      // loop over all the tabs
      this.tabs.forEach((tab, index) => {
        tab.isActive = (index === i)
      })
    }
   },
    created () {
      //@ts-ignore
      this.tabs = this.$children
    }
  }
  */
</script>

<style>
.rdtabs__tab:not(.rdtabs__tab--active) {
    cursor: pointer;
}
</style>
