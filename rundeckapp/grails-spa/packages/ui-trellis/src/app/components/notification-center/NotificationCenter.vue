<template>
  <div class="content mainWrapper">
    <div class="aDiv">
      <h2>hey</h2>
    </div>
  </div>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "ProjectNotificationCenter",
      data(){
        return{
          message: 'Lets begin!'
        }
      }
    })
</script>

<style scoped lang="scss">
    .mainWrapper {
      width: 100%;
      height: 100%;
      border: 1px solid grey;
      border-radius: 5px;
      display: flex;
      justify-content: center;
      align-items: center;
      text-align: center;
    }
    .aDiv {
      height: 50px;
      width: 50px;
    }
</style>