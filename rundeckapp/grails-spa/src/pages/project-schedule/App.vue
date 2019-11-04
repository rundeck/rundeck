<template>
  <div id="app">
    <schedule-definitions-view v-bind:event-bus="eventBus"/>
  </div>
</template>
<script type="javascript">

    import Vue from "vue";
    import ScheduleDefinitionsView from "@/pages/project-schedule/views/ScheduleDefinitionsView.vue";
    import ScheduleAssign from "@/pages/project-schedule/views/ScheduleAssign.vue";

    export default Vue.extend({
        name: "ProjectScheduleMain",
        props: ['eventBus'],
        components: {ScheduleDefinitionsView, ScheduleAssign}
    });
</script>
<style lang="scss">
</style>
