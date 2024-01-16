<template>
    <div
        id="notificationWidgetCardIcon"
        class="notificationWidgetCardIcon"
    >
      <div
          id="notificationWidgetCardImageWrapper"
          class="notificationWidgetCardImageWrapper"
      >
        <i :class="iconClass"></i>
      </div>
      <p>{{iconLabel}}</p>
    </div>
    <span
        id="notificationCenterDivisionCaret"
        class="notificationCenterDivisionCaret"
    ></span>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";
    import NotificationCenterHeader from "@/app/components/notification-center/NotificationCenterHeader.vue";
    import EntryTypes from "@/app/components/notification-center/EntryTypes";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "NotificationCenterEntryIconSection",
      props: ['iconString', 'iconLabel'],
      data(){
        return {
          iconClass: '',
        }
      },
      methods: {
        translateIconString(){
          switch (this.iconString) {
            case EntryTypes.TASK.id:
              this.iconClass = EntryTypes.TASK.iconString
              break;
          }
        }
      },
      mounted() {
        this.translateIconString()
      }
    })
</script>

<style scoped lang="scss">
.notificationWidgetCardIcon{
  width: 20%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: center;
  margin-top: 1rem;

  p {
    font-size: small;
    font-weight: lighter;
    font-style: italic;
  }

}
.notificationWidgetCardImageWrapper{
  height: 50px;
  width: 50px;
  border-radius: 50%;
  border: 0.5px solid var(--background-color-lvl3);
  display: flex;
  justify-content: center;
  align-items: center;

  .glyphicon {
    font-size: 30px;
  }
}
.notificationCenterDivisionCaret {
  width: 1px;
  height: 90%;
  background-color: var(--background-color-lvl3);
}
</style>