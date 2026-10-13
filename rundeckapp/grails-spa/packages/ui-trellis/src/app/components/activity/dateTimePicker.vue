<template>
  <div>
    <date-picker
      v-model="dateString"
      :class="dateClass"
      :clear-btn="false"
      class="bs-date-picker"
      role="combobox"
    />
    <time-picker v-model="time" :class="timeClass" role="combobox" />
  </div>
</template>
<script>
import { defineComponent } from "vue";
import moment from "moment";

export default defineComponent({
  props: {
    modelValue: {
      type: [String, Date],
      required: true,
    },
    dateClass: {
      type: String,
      default: "",
    },
    timeClass: {
      type: String,
      default: "",
    },
  },
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
      handler() {
        this.recalcDate();
      },
    },
    time: {
      handler() {
        this.$emit("update:modelValue", this.datetime);
      },
    },
    modelValue: {
      handler() {
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
