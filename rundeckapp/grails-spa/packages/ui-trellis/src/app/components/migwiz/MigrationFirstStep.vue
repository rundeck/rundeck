<template>
  <div>
    <form>
      <div class="form-group">
        <label> Instance name </label>
        <input class="form-control" />
      </div>
      <div class="form-group">
        <label> Name </label>
        <input class="form-control" />
      </div>
      <div class="form-group">
        <label> E-mail </label>
        <input class="form-control" />
      </div>
      <div class="checkbox">
        <input type="checkbox" class="form-control" />
        <label>I agree to terms and conditions</label>
      </div>
    </form>
    <btn @click="next">Go to next step</btn>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "MigrationFirstStep",
  emits: ["nextStep"],
  data() {
    return {};
  },
  methods: {
    next() {
      this.$emit("nextStep");
    },
  },
});
</script>

<style scoped lang="scss"></style>
