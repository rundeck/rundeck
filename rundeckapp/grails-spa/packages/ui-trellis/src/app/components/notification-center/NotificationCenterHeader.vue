<template>
    <div class="notificationCenterHeader">
      <p>{{headerTitle}}{{projectName}}</p>
      <p>{{timestamp}}</p>
      <p><strong>{{notificationCount}}</strong>{{notificationCountLabel}}</p>
    </div>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "NotificationCenterHeader",
      props: ['notificationCount'],
      data(){
        return{
          projectName: 'host',
          headerTitle: 'Notifications for ',
          timestamp: "",
          notificationCountLabel: ' Notifications'
        }
      },
      methods: {
        getNow: function() {
          const today = new Date();
          const date = today.getFullYear()+'-'+(today.getMonth()+1)+'-'+today.getDate();
          const time = today.getHours() + ":" + today.getMinutes() + ":" + today.getSeconds();
          const dateTime = date +' '+ time;
          this.timestamp = dateTime;
        }
      },
      created(){
        setInterval( () => {
          this.getNow()
        }, 1000)
      },
      watch: {

      }
    })
</script>

<style scoped lang="scss">
.notificationCenterHeader {
  height: 120px;
  width: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  padding: 2rem;
}
.notificationCenterHeader > p:nth-of-type(1) {
  font-size: x-large;
  font-weight: bolder;
}
.notificationCenterHeader > p:nth-of-type(2) {
  font-weight: lighter;
}
.notificationCenterHeader > p:nth-of-type(3) {
  font-weight: lighter;
}
</style>