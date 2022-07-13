<template>
  <div id="app">
    <Tabs>
      <Tab title="Tab 1">Hello From Tab 1</Tab>
      <Tab title="Tab 2">Hello From Tab 2</Tab>
      <Tab title="Tab 3">Hello From Tab 3</Tab>
      <Tab title="Tab 4">Hello From Tab 4</Tab>
    </Tabs>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import '../theme/scss/app.scss';
import Tabs from '@/components/containers/pa-tabs/Tabs.vue' 
import Tab from '@/components/containers/pa-tabs/Tab.vue' 

export default {
  components: {Tabs, Tab}
}
</script>

