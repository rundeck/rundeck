<template>
  <div>
    <ui-socket
      section="resources-editor"
      location="top"
      :event-bus="eventBus"
    />
    <resources-editor
      v-if="updatedData"
      v-model="updatedData"
      :event-bus="eventBus"
    />
    <json-embed :output-data="updatedData" field-name="resourcesJsonData" />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import * as _ from "lodash";

import ResourcesEditor from "../../../components/job/resources/ResourcesEditor.vue";
import UiSocket from "../../../../library/components/utils/UiSocket.vue";
import JsonEmbed from "./JsonEmbed.vue";

import { getRundeckContext } from "../../../../library";

export default defineComponent({
  name: "ResourcesEditorSection",
  components: {
    ResourcesEditor,
    JsonEmbed,
    UiSocket,
  },
  props: ["eventBus"],
  data() {
    return {
      resourcesData: {},
      updatedData: null,
      watching: false,
    };
  },
  watch: {
    updatedData: {
      handler() {
        if (this.watching) {
          if (!_.isEqual(this.resourcesData, this.updatedData)) {
            this.eventBus.emit("jobedit.page.confirm", true);
          }
        }
      },
      deep: true,
    },
  },
  async mounted() {
    if (getRundeckContext().data) {
      this.resourcesData = getRundeckContext().data.resourcesData;
      this.updatedData = Object.assign({}, this.resourcesData);
      this.watching = true;
    }
  },
});
</script>

<style></style>
