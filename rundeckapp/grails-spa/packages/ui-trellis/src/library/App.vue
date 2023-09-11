<template>
  <div id="app">

 
    <copybox content=""/>
  </div>
</template>

<script lang="ts">
import {defineComponent} from 'vue';
import HelloWorld from './components/HelloWorld.vue';
import copybox from './components/containers/copybox/CopyBox.vue';


export default defineComponent({
  name: 'App',
  components: {
    HelloWorld,
    copybox,
  }
});
</script>

<style lang="scss">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
