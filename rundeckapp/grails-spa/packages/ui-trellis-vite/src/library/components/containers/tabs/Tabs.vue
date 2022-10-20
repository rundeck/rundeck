<template>
  <div class="patabs" :class="[type]">
    <div class="patabs-list">
      <div v-for="(tab, index) in tabs" :key="tab.title" @click="selectTab(index)" class="patab-item" 
        :class="[index == selectedIndex ? 'patab-item--active' : '']">
        {{ tab.title }}
      </div>
    </div>
    <slot></slot>
  </div>
</template>

<script>

import { Vue, Prop } from 'vue-property-decorator';
import Component from 'vue-class-component'


export default {
  data(){
    return {
      selectedIndex: 0,
      tabs: []
    }
  },
  props:{
    type: {
      type: String,
      default: 'patabs--standard'
    },
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
  mounted () {
    this.selectTab(0)
  },
  created (){
    this.tabs = this.$children
  }
}
</script>