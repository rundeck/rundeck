<template>
  <section id="notificationCenter" class="content mainWrapper menu-item">
    <NotificationCenterHeader :notification-count="1" />
    <div id="notificationWidgetsListWrapper" class="notificationWidgetsListWrapper">
      <ul>
        <NotificationCenterEntry v-for="(entry, index) in entries"
          :key="index"
          :icon-string="entry.entry_type.id"
          :icon-label="entry.entry_type.value"
          :notification-entry-title="entry.title"
          :notification-entry-started-at="entry.started_at"
          :notification-entry-status="entry.status"
          :notification-entry-completed-proportion="entry.completed_proportion"
          :notification-entry-progress-proportion="entry.progress_proportion"
        />
      </ul>
    </div>
  </section>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";
    import NotificationCenterHeader from "@/app/components/notification-center/NotificationCenterHeader.vue";
    import NotificationCenterEntry from "@/app/components/notification-center/NotificationCenterEntry.vue";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "ProjectNotificationCenter",
      components: {
        NotificationCenterHeader,
        NotificationCenterEntry
      },
      data(){
        return{
          message: 'Lets begin!',
          entries: []
        }
      },
      methods: {
        async getEntries(){
          const entriesUri = "/api/40/project/test-s3/notifications/entries";
          let result = await rundeckClient.sendRequest({
            method: 'GET',
            url: entriesUri
          })
          if (result.status === 200) {
            this.entries = result.parsedBody.entries
          }
        }
      },
      mounted() {
        // Interval call
        this.getEntries()
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
      }
    }
</style>