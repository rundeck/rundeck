<template>
  <div class="flex-container flex-col step-container">
    <p>
      {{ $t("migwiz.about") }}
    </p>
    <Form
      v-slot="$form"
      :validate-on-value-update="false"
      :validate-on-blur="true"
      class="flex-form"
    >
      <FormField as-child>
        <div class="form-group">
          <div class="form-row">
            <label>Select projects</label>
            <div class="col-xs-12 col-sm-9">
              <ProjectSelectButton
                :show-default-label="false"
                mode="multi"
                style="width: 100%"
              />
            </div>
          </div>
        </div>
      </FormField>
      <hr />
      <FormField as-child>
        <div class="form-group">
          <div class="form-row">
            <div
              class="control-label text-form-label"
              style="text-align: right"
            >
              <span>{{ $t("migwiz.typeOfCredentials") }}</span>
            </div>
            <div class="col-xs-12 col-sm-9">
              <div class="radio radio-inline">
                <input
                  id="scheduleDefinitionsFalse"
                  type="radio"
                  name="scheduleDefinitionsEnabled"
                />
                <label for="scheduleDefinitionsFalse">
                  <span>{{ $t("migwiz.password") }}</span>
                </label>
              </div>
              <div class="radio radio-inline">
                <input
                  id="scheduleDefinitionsTrue"
                  type="radio"
                  name="scheduleDefinitionsEnabled"
                />
                <label for="scheduleDefinitionsTrue">
                  <span>{{ $t("migwiz.apiToken") }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>
      </FormField>
      <FormField as-child>
        <div class="form-group">
          <div class="form-row">
            <label> {{ $t("migwiz.password") }} </label>
            <div class="col-xs-12 col-sm-9">
              <input class="form-control" />
            </div>
          </div>
        </div>
      </FormField>
      <FormField as-child>
        <div class="form-group">
          <div class="form-row">
            <label> {{ $t("migwiz.apiToken") }} </label>
            <div class="col-xs-12 col-sm-9">
              <input class="form-control" />
            </div>
          </div>
        </div>
      </FormField>

      <button type="submit" class="btn btn-submit" @click="next">
        {{ $t("migwiz.nextStep") }}
      </button>
    </Form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Form, FormField } from "@primevue/forms";
import ProjectSelectButton from "@/library/components/widgets/project-select/ProjectSelectButton.vue";

export default defineComponent({
  name: "MigrationDataStep",
  components: {
    ProjectSelectButton,
    // eslint-disable-next-line vue/no-reserved-component-names
    Form,
    FormField,
  },
  methods: {
    next() {
      this.$emit("nextStep", {
        isValid: true,
      });
    },
  },
});
</script>

<style scoped lang="scss">
.flex-form {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  width: 100%;

  .form-group {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  .form-row {
    display: inline-flex;
    align-items: center;
    justify-content: flex-end;
    width: 100%;

    label {
      flex-shrink: 0;
    }
  }
}
</style>
