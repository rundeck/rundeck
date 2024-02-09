<template>
  <div>
    <notifications-editor
      v-if="notificationData"
      :notification-data="notificationData"
      @changed="changed"
    />
    <json-embed
      :output-data="updatedData.notifications"
      field-name="jobNotificationsJson"
    />
  </div>
</template>

<script>
import * as _ from "lodash";
import NotificationsEditor from "../../../components/job/notifications/NotificationsEditor.vue";
import JsonEmbed from "./JsonEmbed.vue";

import { getRundeckContext } from "../../../../library";

export default {
  name: "App",
  components: {
    NotificationsEditor,
    JsonEmbed,
  },
  props: ["eventBus"],
  data() {
    return {
      notificationData: null,
      updatedData: {
        notifications: [],
        notifyAvgDurationThreshold: null,
      },
    };
  },
  async mounted() {
    if (getRundeckContext() && getRundeckContext().data) {
      this.notificationData = getRundeckContext().data.notificationData;
      this.updatedData = this.notificationData;
    }
  },
  methods: {
    changed(data) {
      if (!_.isEqual(data, this.updatedData.notifications)) {
        this.updatedData.notifications = data;
        //nb: hook to indicate job was editted, defined in jobedit.js
        window.jobWasEdited();
      }
    },
  },
};
</script>

<style></style>
