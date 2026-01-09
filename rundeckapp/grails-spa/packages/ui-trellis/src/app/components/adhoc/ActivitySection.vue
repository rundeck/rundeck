<template>
  <div class="col-xs-12">
    <div>
      <div class="card card-plain">
        <div class="card-header">
          <h3 class="card-title">
            {{ $t("page.section.Activity.for.adhoc.commands") }}
          </h3>
        </div>
      </div>
      <div class="card" id="activity_section">
        <div class="card-content">
          <div class="_history_content vue-project-activity">
            <activity-list :event-bus="eventBus"></activity-list>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ActivityList from "../activity/activityList.vue";

export default defineComponent({
  name: "ActivitySection",
  components: {
    ActivityList,
  },
  props: {
    eventBus: {
      type: Object,
      required: true,
    },
  },
});
</script>

<style scoped>
/* Styles inherited from existing CSS */
</style>

