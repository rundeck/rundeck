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
      <div id="activity_section" class="card">
        <div class="card-content">
          <div class="_history_content vue-project-activity">
            <activity-list
              :event-bus="eventBus"
              :next-ui-columns="nextUiColumns"
            ></activity-list>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ActivityList from "../activity/activityList.vue";
import type { ActivityColumn } from "../activity/ActivityListEaTableTypes";

/** Ad hoc commands have no job options, so the Commands page has no Options column. */
const COMMANDS_COLUMNS: ActivityColumn[] = [
  "status",
  "id",
  "dateEnded",
  "nodes",
  "duration",
  "user",
  "title",
];

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
  data() {
    return {
      nextUiColumns: COMMANDS_COLUMNS,
    };
  },
});
</script>

<style scoped>
/* Styles inherited from existing CSS */
</style>
