<template>
  <div>
    <date-picker
      v-model="dateString"
      :class="dateClass"
      :clear-btn="false"
      class="bs-date-picker"
    />
    <time-picker v-model="time" :class="timeClass" />
  </div>
</template>
<script>
import { defineComponent } from "vue";
import moment from "moment";

export default defineComponent({
  props: ["modelValue", "dateClass", "timeClass"],
  emits: ["update:modelValue"],
  data() {
    return {
      dateString: "",
      time: new Date(),
    };
  },
  computed: {
    datetime() {
      return moment(this.time).format();
    },
  },
  watch: {
    dateString: {
      handler(newVal, oldVal) {
        this.recalcDate();
      },
    },
    time: {
      handler(newVal, oldVal) {
        this.$emit("update:modelValue", this.datetime);
      },
    },
    modelValue: {
      handler(newVal, oldVal) {
        this.setFromValue();
      },
    },
  },
  mounted() {
    this.setFromValue();
  },
  methods: {
    recalcDate() {
      const mo = moment(this.time);
      const date = moment(this.dateString);
      mo.year(date.year());
      mo.month(date.month());
      mo.date(date.date());
      this.time = mo.toDate();
    },
    setFromValue() {
      if (this.modelValue) {
        const dt = moment(this.modelValue);
        this.time = dt.toDate();
        this.dateString = dt.format("YYYY-MM-DD");
      }
    },
  },
});
</script>
<style lang="scss">
.bs-date-picker {
  .btn-primary {
    background: #4499ff;
    color: white;
  }
}
</style>
