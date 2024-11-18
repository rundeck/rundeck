<template>
  <div class="step-container">
    <p>
      {{ $t("migwiz.welcome") }}
    </p>
    <Form
      v-slot="$form"
      :resolver="resolver"
      :validate-on-mount="['acceptAgreement']"
      :validate-on-value-update="false"
      :validate-on-blur="true"
      class="flex-form"
      @submit="next"
    >
      <FormField
        v-for="(field, key) in fields"
        v-slot="$field"
        :key="`initialForm${key}`"
        :name="field.name"
        :validate-on-value-update="field.type === 'checkbox'"
        as-child
      >
        <div
          class="form-group"
          :class="{ 'has-error': $field?.invalid && field.type !== 'checkbox' }"
        >
          <div class="form-row">
            <label v-if="field.type !== 'checkbox'" :for="field.name">
              {{ $t(field.label) }}
            </label>
            <div class="col-xs-12 col-sm-9">
              <div v-if="field.type === 'checkbox'" class="checkbox">
                <input
                  :id="field.name"
                  v-bind="$field.props"
                  type="checkbox"
                  class="form-control"
                />
                <label :for="field.name">
                  {{ $t(field.label) }}
                </label>
              </div>
              <input
                v-else
                v-bind="$field.props"
                v-model="$field.value"
                class="form-control"
              />
            </div>
          </div>
          <div
            v-if="$field?.invalid && field.type !== 'checkbox'"
            class="col-xs-12 col-sm-9"
          >
            <p class="text-danger">{{ $field.error?.message }}</p>
          </div>
        </div>
      </FormField>
      <button type="submit" class="btn btn-submit" :disabled="!$form.valid">
        {{ $t("migwiz.nextStep") }}
      </button>
    </Form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { Form, FormField } from "@primevue/forms";
// @ts-ignore
import { yupResolver } from "@primevue/forms/resolvers/yup";
import * as yup from "yup";
import {
  getCredentials,
  postStartInstance,
} from "@/app/components/migwiz/services/migWizServices";
import { Notification } from "uiv";
import { cloneDeep } from "lodash";

export default defineComponent({
  name: "MigrationFirstStep",
  components: {
    // eslint-disable-next-line vue/no-reserved-component-names
    Form,
    FormField,
  },
  emits: ["nextStep"],
  data() {
    return {
      fields: {
        instanceName: {
          name: "instanceName",
          label: "migwiz.instanceName",
          type: "text",
        },
        firstName: {
          name: "firstName",
          label: "migwiz.firstName",
          type: "text",
        },
        lastName: { name: "lastName", label: "migwiz.lastName", type: "text" },
        email: { name: "email", label: "migwiz.email", type: "email" },
        company: { name: "company", label: "migwiz.company", type: "text" },
        acceptAgreement: {
          name: "acceptAgreement",
          label: "migwiz.confirmAgreement",
          type: "checkbox",
        },
      },
      resolver: yupResolver(
        yup.object().shape({
          instanceName: yup
            .string()
            .required(this.getErrorMessage("instanceName")),
          firstName: yup.string().required(this.getErrorMessage("firstName")),
          lastName: yup.string().required(this.getErrorMessage("lastName")),
          email: yup
            .string()
            .matches(/@[^.]*\./, this.getErrorMessage("email"))
            .required(),
          acceptAgreement: yup.boolean().required().oneOf([true]),
        }),
      ),
    };
  },
  methods: {
    getErrorMessage(fieldName: string) {
      if (fieldName === "email") {
        return this.$t("migwiz.validEmail");
      }
      return this.$t("migwiz.requiredField", {
        field: this.$t(`migwiz.${fieldName}`),
      });
    },
    async next({ values, valid }) {
      const finalValues = cloneDeep(values || {});
      delete finalValues.acceptAgreement;
      if (valid && Object.keys(finalValues).length > 0) {
        try {
          const { access_token: accessToken } =
            await getCredentials(finalValues);
          const resp = await postStartInstance(accessToken, {
            email: finalValues.email,
            instanceName: finalValues.instanceName,
          });

          if (resp) {
            this.$emit("nextStep", {
              data: {
                ...finalValues,
                instanceUrl: resp.instance_url,
                instanceStatus: resp.status,
                trialEndDate: resp.end_date,
              },
              isValid: valid,
            });
          }
        } catch (e) {
          Notification.notify({
            type: "error",
            html: true,
            content: e.message,
          });
        }
      }
    },
  },
});
</script>

<style scoped lang="scss">
.step-container {
  display: flex;
  flex-direction: column;
}

.flex-form {
  display: flex;
  flex-direction: column;
  align-items: flex-end;

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

  .text-danger {
    margin: 5px 0 0;
  }
}
</style>
