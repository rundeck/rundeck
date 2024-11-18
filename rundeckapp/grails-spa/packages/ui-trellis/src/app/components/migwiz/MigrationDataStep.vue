<template>
  <div class="flex-container flex-col step-container">
    <i18n-t keypath="migwiz.about" tag="p">
      <a
        href="https://docs.rundeck.com/docs/api/api_basics.html#running-the-welcome-project-and-new-user-token-creation"
        target="_blank"
      >
        {{ $t("migwiz.link") }}
      </a>
    </i18n-t>
    <div class="flex-form">
      <div class="form-group mb-0">
        <div class="form-row">
          <div class="control-label text-form-label" style="text-align: right">
            <span>{{ $t("migwiz.typeOfCredentials") }}</span>
          </div>
          <div class="col-xs-12 col-sm-9">
            <div class="radio radio-inline">
              <input
                id="password"
                v-model="selectedCredential"
                name="credentials"
                type="radio"
                value="password"
              />
              <label for="scheduleDefinitionsFalse">
                <span>{{ $t("migwiz.password") }}</span>
              </label>
            </div>
            <div class="radio radio-inline">
              <input
                id="api"
                v-model="selectedCredential"
                name="credentials"
                type="radio"
                value="api"
              />
              <label for="scheduleDefinitionsTrue">
                <span>{{ $t("migwiz.apiToken") }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
    <Form
      :resolver="resolver"
      :validate-on-value-update="false"
      :validate-on-blur="true"
      class="flex-form"
      @submit="next"
    >
      <FormField
        v-if="selectedCredential === 'password'"
        v-slot="$field"
        name="password"
        as-child
      >
        <div class="form-group">
          <div class="form-row">
            <label> {{ $t("migwiz.password") }} </label>
            <div class="col-xs-12 col-sm-9">
              <input
                v-bind="$field.props"
                v-model="$field.value"
                class="form-control"
              />
            </div>
          </div>
        </div>
      </FormField>
      <FormField v-else v-slot="$field" name="token" as-child>
        <div class="form-group">
          <div class="form-row">
            <label> {{ $t("migwiz.apiToken") }} </label>
            <div class="col-xs-12 col-sm-9">
              <input
                v-bind="$field.props"
                v-model="$field.value"
                class="form-control"
              />
            </div>
          </div>
        </div>
      </FormField>
      <FormField
        v-slot="$field"
        :initial-value="instanceUrl"
        name="instanceUrl"
        as-child
      >
        <div class="form-group">
          <div class="form-row">
            <label> {{ $t("migwiz.instanceUrl") }} </label>
            <div class="col-xs-12 col-sm-9">
              <input
                v-bind="$field.props"
                v-model="$field.value"
                class="form-control"
              />
            </div>
          </div>
        </div>
      </FormField>
      <hr class="col-sm-10" style="margin: 20px 15px" />
      <FormField v-slot="$field" name="selectedProject" as-child>
        <div class="form-group">
          <div class="form-row">
            <label>Select projects</label>
            <div class="col-xs-12 col-sm-9">
              <input
                v-bind="$field.props"
                v-model="$field.value"
                class="form-control"
              />
            </div>
          </div>
        </div>
      </FormField>
      <button type="submit" class="btn btn-submit">
        {{ $t("migwiz.nextStep") }}
      </button>
    </Form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Form, FormField } from "@primevue/forms";
import { Notification } from "uiv";
import { postStartMigration } from "@/app/components/migwiz/services/migWizServices";
import { yupResolver } from "@primevue/forms/resolvers/yup";
import * as yup from "yup";

export default defineComponent({
  name: "MigrationDataStep",
  components: {
    // eslint-disable-next-line vue/no-reserved-component-names
    Form,
    FormField,
  },
  props: {
    instanceUrl: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      selectedCredential: "password",
      resolver: yupResolver(
        yup.object().shape({
          instanceUrl: yup.string().required(this.$t("migwiz.requiredField"), {
            field: this.$t(`migwiz.instanceName`),
          }),
          password: yup.string(),
          token: yup.string().required(this.$t("migwiz.requiredField"), {
            field: "Token",
          }),
          selectedProject: yup.string().required(),
        }),
      ),
    };
  },
  methods: {
    async next({ values, valid }) {
      console.log(values);
      try {
        const resp = await postStartMigration(values.selectedProject, {
          url: values.instanceUrl,
          token: values.token,
        });
        if (resp.ok && valid) {
          this.$emit("nextStep", {
            isValid: true,
          });
        }
      } catch (e) {
        Notification.notify({
          type: "error",
          html: true,
          content: e.message,
        });
      }
    },
    handleSelection(selected: string[]) {
      this.selectedProjects = selected;
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

    &.mb-0 {
      margin-bottom: 0;
    }
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
