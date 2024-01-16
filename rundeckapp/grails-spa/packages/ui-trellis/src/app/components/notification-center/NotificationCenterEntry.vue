<template>
  <li
      id="notificationWidgetRenderer"
      class="notificationWidgetRenderer"
      @click="update"
  >
    <NotificationCenterEntryIconSection
      :icon-string="iconString"
      :icon-label="iconLabel"
    />
    <NotificationCenterEntryContent
        :notification-entry-title="notificationEntryTitle"
        :notification-entry-started-at="notificationEntryStartedAt"
        :notification-entry-status="notificationEntryStatus"
        :notification-entry-completed-proportion="notificationEntryCompletedProportion"
        :notification-entry-progress-proportion="notificationEntryProgressProportion"
    />
  </li>
</template>

<script>
    import {defineComponent} from "vue";
    import {getRundeckContext} from "@/library";
    import NotificationCenterEntryIconSection
      from "@/app/components/notification-center/NotificationCenterEntryIconSection.vue";
    import NotificationCenterEntryContent
      from "@/app/components/notification-center/NotificationCenterEntryContent.vue";

    const rundeckClient = getRundeckContext().rundeckClient

    export default defineComponent({
      name: "NotificationCenterEntry",
      components: {
        NotificationCenterEntryIconSection,
        NotificationCenterEntryContent
      },
      props: [
        'iconString',
        'iconLabel',
        'notificationEntryTitle',
        'notificationEntryStartedAt',
        'notificationEntryStatus',
        'notificationEntryCompletedProportion',
        'notificationEntryProgressProportion'
      ],
      data(){
        return{
          message: 'Lets begin!'
        }
      },
      methods: {
      },
      mounted(){
        console.log(this.iconLabel)
      }
    })
</script>

<style scoped lang="scss">
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
.notificationWidgetRenderer{
  display: flex;
  align-items: center;
}
</style>