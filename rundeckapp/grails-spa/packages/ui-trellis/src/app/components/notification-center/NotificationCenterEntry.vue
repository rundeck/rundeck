<template>
  <li
      id="notificationWidgetRenderer"
      class="notificationWidgetRenderer"
  >
    <NotificationCenterEntryIconSection
      :icon-string="iconString"
      :icon-label="iconLabel"
    />
    <NotificationCenterEntryContent />
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
          'iconLabel'
      ],
      data(){
        return{
          message: 'Lets begin!'
        }
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