<template>
    <div
        id="projectNotificationContent"
        class="projectNotificationContent"
    >
      <div
          id="projectNotificationContentHeader"
          class="projectNotificationContentHeader"
      >
        <p>{{notificationEntryTitle}}</p>
        <p>Started at: {{notificationEntryStartedAt}}</p>
      </div>
      <div
          id="projectNotificationContainer"
          class="projectNotificationContainer"
      >
        <div>
          <p>Status: <strong>{{notificationEntryStatus}}</strong></p>
          <p>Progress: </p>
        </div>
        <div>
          <!--  Progress bar -->
          <span>
                  40%
                </span>
          <div></div>
        </div>
      </div>
    </div>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "NotificationCenterContent",
      props: [
          'notificationEntryTitle',
          'notificationEntryStartedAt',
          'notificationEntryStatus',
          'notificationEntryCompletedProportion',
          'notificationEntryProgressProportion'
      ],
      data(){
        return{
        }
      }
    })
</script>

<style scoped lang="scss">
.projectNotificationContent {
  width: 80%;
  height: 90%;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
}
.projectNotificationContentHeader {
  width: 100%;
  height: 35%;
  display: flex;
  flex-direction: column;
  justify-content: inherit;
  align-items: baseline;
  padding-left: 1rem;
}
.projectNotificationContentHeader > p:nth-of-type(1) {
  font-size: medium;
}
.projectNotificationContentHeader > p:nth-of-type(2) {
  font-size: small;
  font-weight: lighter;
}
.projectNotificationContainer {
  width: 100%;
  height: 65%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  padding-left: 1rem;
  padding-right: 1rem;
}
.projectNotificationContainer > div:nth-of-type(1) {
  height: 50%;
  width: 100%;
  text-align: left;

  p {
    width: 100%;
    height: 50%;
    margin-bottom: 0;
    font-weight: lighter;
    font-size: small;
  }
}
.projectNotificationContainer > div:nth-of-type(2) {
  height: 50%;
  width: 100%;
  margin-top: 1rem;
  margin-bottom: 1rem;
  border-radius: 2.5px;
  background-color: var(--background-color-lvl3);
  display: flex;
  justify-content: flex-start;
  align-items: center;
  position: relative;

  span {
    font-size: small;
    font-weight: bolder;
    z-index: 9999;
    position: absolute;
    left: 0;
    right: 0;
    margin-left: auto;
    margin-right: auto;
    width: 100px;
  }
}

.projectNotificationContainer > div:nth-of-type(2) > div:nth-of-type(1) {
  width: 40%;
  height: 100%;
  z-index: 999;
  background-color: var(--card-default-background-color);
  border-radius: 2.5px;
}
</style>