<template>
  <div id="app">
    <a href="/command/">command</a>
    <section>
      <div class="">
        <h3>Tabs Standard</h3>
      </div>
      <Tabs>
        <Tab title="Tab 1">
          <div class="card">
            <div class="card-content">
              <div class="form-group">
                <label data-v-76e51786="">Name</label>
                <input class="form-control">
              </div>
              <div class="form-group">
                <div class="checkbox">
                  <input type="checkbox" id="wh-enabled" class="form-control">
                  <label for="wh-enabled">Enabled</label>
                </div>
              </div>
            </div>
          </div>
        </Tab>
        <Tab title="Tab 2">Hello From Tab 2</Tab>
        <Tab title="Tab 3">Hello From Tab 3</Tab>
        <Tab title="Tab 4">Hello From Tab 4</Tab>
      </Tabs>
    </section>
    <section>
       <div class="">
        <h3>Tabs Card</h3>
      </div>
      <Tabs type="patabs--card">
        <Tab title="Tab 1">
          <div class="card">
            <div class="card-content">
              <div class="form-group">
                <label>Name</label>
                <input class="form-control">
              </div>
              
            </div>
          </div>
        </Tab>
        <Tab title="Tab 2">Hello From Tab 2</Tab>
        <Tab title="Tab 3">Hello From Tab 3</Tab>
        <Tab title="Tab 4">Hello From Tab 4</Tab>
      </Tabs>
    </section>
    <section>
       <div class="">
        <h3>Input Switch</h3>
        <InputSwitch v-model="ivalue"/>
      </div>
    </section>
    <section>
       <div class="">
        <h3>Copy Box</h3>
        <CopyBox content="Copy Me"/>
      </div>
    </section>

    <div class="footerbar">
      <div class="utility-bar__item-icon fas fa-sun fas-xs" @click="setTheme()"></div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
// import '../theme/scss/app.scss';
import Tabs from '@/library/components/containers/tabs/Tabs.vue' 
import Tab from '@/library/components/containers/tabs/Tab.vue' 
import InputSwitch from '@/library/components/inputs/Switch.vue'
import CopyBox from '@/library/components/containers/copybox/CopyBox.vue'

export default {
  components: {
    Tabs, 
    Tab,
    InputSwitch,
    CopyBox
    },
  data() {
    return {
      theme: 'light',
      ivalue: true,

    }
  },
  mounted(){
    let html = document.querySelector('html')
    html.setAttribute('data-color-theme', this.theme);

    console.log(import.meta.env)
  },
  methods: {
    setTheme(){
      if(this.theme == 'light'){
        this.theme = 'dark'
      } else {
        this.theme = 'light'
      }
       let html = document.querySelector('html')
      html.setAttribute('data-color-theme', this.theme);
    }
  }
}
</script>

<style lang="scss">
@import "@/library/scss/app.scss";

#app {
  padding: 20px;
}
.footerbar{
  background-color: var(--utility-bar-background-color);
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 2px 10px;
  z-index: 99999999;
}
</style>
