<template>
  <div class="flex-col">
    <p>
      {{ $t("migwiz.welcome") }}
    </p>
    <form class="flex-form">
      <div class="form-group row">
        <label> {{ $t("migwiz.instanceName") }} </label>
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <input class="form-control" />
        </div>
      </div>
      <div class="form-group row">
        <label> {{ $t("migwiz.firstName") }} </label>
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <input class="form-control" />
        </div>
      </div>
      <div class="form-group row">
        <label> {{ $t("migwiz.lastName") }} </label>
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <input class="form-control" />
        </div>
      </div>
      <div class="form-group row">
        <label> {{ $t("migwiz.email") }} </label>
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <input class="form-control" />
        </div>
      </div>
      <div class="form-group row">
        <label> {{ $t("migwiz.company") }} </label>
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <input class="form-control" />
        </div>
      </div>
      <div class="form-group row">
        <div class="col-xs-12 col-sm-9 col-lg-11">
          <div class="checkbox">
            <input type="checkbox" class="form-control" />
            <label>
              {{ $t("migwiz.confirmAgreement") }}
            </label>
          </div>
        </div>
      </div>
      <btn @click="next">
        {{ $t("migwiz.startMigration") }}
      </btn>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Form } from "@primevue/forms";

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

<style scoped lang="scss">
.flex-col {
  gap: 20px;
}
.flex-form {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;

  .form-group {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    width: 100%;

    label {
      flex-shrink: 0;
    }
  }
}
</style>
