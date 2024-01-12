<template>
  <section class="content mainWrapper">
    <div class="notificationCenterHeader">
      <p>Project Notifications</p>
      <p>Fri Jan 12 2024 21:00:51</p>
      <p><strong>01</strong> Notifications</p>
    </div>
    <div id="notificationWidgetsListWrapper" class="notificationWidgetsListWrapper">
      <ul>
        <li
          id="notificationWidgetRenderer"
          class="notificationWidgetRenderer"
        >
          <div
              id="notificationWidgetCardIcon"
              class="notificationWidgetCardIcon"
          >
            <div
                id="notificationWidgetCardImageWrapper"
                class="notificationWidgetCardImageWrapper"
            >
              <i class="glyphicon glyphicon-cog"></i>
            </div>
            <p>Task</p>
          </div>
          <span
              id="notificationCenterDivisionCaret"
              class="notificationCenterDivisionCaret"
          ></span>
          <div
            id="projectNotificationContent"
            class="projectNotificationContent"
          >
            <div
              id="projectNotificationContentHeader"
              class="projectNotificationContentHeader"
            >
              <p>Asynchronous Project Import</p>
              <p>Started at: Fri Jan 12 2024 21:00:51</p>
            </div>
            <div
                id="projectNotificationContainer"
                class="projectNotificationContainer"
            >
              <div>
                <p>Status: <strong>In progress...</strong></p>
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
        </li>
      </ul>
    </div>
  </section>
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
      flex-direction: column;
      justify-content: center;
      align-items: center;
      text-align: center;
      overflow: scroll;
    }
    .notificationWidgetsListWrapper {
      width: 95%;
      height: 90%;
      border-radius: 5px;
      overflow: scroll;

      ul {
        width: 100%;
        list-style-type: none;
        padding: 0;
        display: flex;
        flex-direction: column;
        justify-content: space-evenly;
        align-items: center;
        overflow: scroll;

        li {
          width: 100%;
          height: 150px;
          margin-top: 0.5rem;
          margin-bottom: 0.5rem;
          border-radius: 5px;

          &:hover {
            background-color: var(--background-color-accent-lvl2);
          }

        }
      }
    }
    .notificationWidgetCard {
      width: 100%;
      height: 100%;

      background-color: silver;
    }
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
    .notificationWidgetRenderer{
      display: flex;
      align-items: center;
    }
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