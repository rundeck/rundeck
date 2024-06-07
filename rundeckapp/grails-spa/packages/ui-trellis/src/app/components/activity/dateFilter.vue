<template>
  <div class="form-group">
    <span class="checkbox col-sm-2" data-testid="checkbox-span">
      <input
        :id="uid"
        v-model="enabled"
        type="checkbox"
        class="checkbox"
        @click="toggleEnabled"
      />
      <label :for="uid"><slot>Enabled</slot></label>
    </span>
    <div class="col-sm-10">
      <dropdown v-if="enabled">
        <div class="input-group">
          <div class="input-group-btn">
            <btn class="dropdown-toggle"
              ><i class="glyphicon glyphicon-calendar"
            /></btn>
          </div>

          <input v-model="datetime" type="text" class="form-control" />
        </div>

        <template #dropdown>
          <li style="padding: 10px">
            <date-time-picker
              v-model="datetime"
              date-class="flex-item-1"
              time-class="flex-item-auto"
              class="flex-container"
            />
          </li>
        </template>
      </dropdown>
    </div>
  </div>
</template>
<script lang="ts">
import { defineComponent } from "vue";
import _ from "lodash";
import DateTimePicker from "./dateTimePicker.vue";
import { ModelValue } from "./tests/type";

export default defineComponent({
  components: {
    DateTimePicker,
  },
  props: {
    modelValue: {
      type: Object as () => ModelValue,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      uid: _.uniqueId(),
      enabled: this.modelValue.enabled,
      datetime: this.modelValue.datetime,
      picker: false,
    };
  },
  watch: {
    enabled: {
      handler(newVal, oldVal) {
        this.$emit("update:modelValue", {
          enabled: this.enabled,
          datetime: this.datetime,
        });
      },
    },
    datetime: {
      handler(newVal, oldVal) {
        this.$emit("update:modelValue", {
          enabled: this.enabled,
          datetime: this.datetime,
        });
      },
    },
    modelValue() {
      this.enabled = this.modelValue.enabled;
      this.datetime = this.modelValue.datetime;
    },
  },
  methods: {
    toggleEnabled() {
      this.enabled = !this.enabled;
    },
  },
});
</script>
<style lang="scss" scoped>
.label-holder {
  padding-right: 10px;
}
</style>
