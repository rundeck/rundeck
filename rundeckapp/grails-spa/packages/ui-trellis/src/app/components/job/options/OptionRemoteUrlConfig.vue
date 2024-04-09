<template>
  <div
    data-test="option.valuesUrl"
    id="vurl_section"
    :class="{ 'has-error': hasError('valuesUrl') }"
  >
    <input
      type="url"
      class="form-control"
      name="valuesUrl"
      v-model="internal.valuesUrl"
      size="60"
      :placeholder="$t('form.option.valuesURL.placeholder')"
    />
    <div class="help-block" v-if="validationErrors['valuesUrl']">
      <ErrorsList :errors="validationErrors['valuesUrl']" />
    </div>
    <div class="help-block">
      {{ $t("form.option.valuesUrl.description") }}
      <a
        href="https://docs.rundeck.com/docs/manual/job-options.html#option-model-provider"
        target="_blank"
      >
        <i class="glyphicon glyphicon-question-sign"></i>
        {{ $t("rundeck.user.guide.option.model.provider") }}
      </a>
    </div>

    <div
      class="row"
      data-test="option.configRemoteUrl"
      :class="{ 'has-error': hasError('configRemoteUrl') }"
    >
      <div class="col-md-12">
        <label class="control-label">{{
          $t("form.option.valuesType.url.filter.label")
        }}</label>
      </div>

      <div class="col-md-4">
        <div class="">
          <input
            type="text"
            class="form-control"
            name="remoteUrlJsonFilter"
            v-model="internal.configRemoteUrl.jsonFilter"
            size="30"
          />
        </div>
        <div
          class="help-block"
          v-if="validationErrors['configRemoteUrl.jsonFilter']"
        >
          <ErrorsList
            :errors="validationErrors['configRemoteUrl.jsonFilter']"
          />
        </div>
      </div>
      <div class="col-md-12">
        <div class="">
          <div class="help-block">
            {{ $t("form.option.valuesType.url.filter.description") }}
          </div>
        </div>
      </div>
    </div>

    <div class="row">
      <div class="col-md-12">
        <label class="control-label">{{
          $t("form.option.valuesType.url.authType.label")
        }}</label>
      </div>

      <div class="col-md-4">
        <select
          class="form-control"
          v-model="internal.remoteUrlAuthenticationType"
        >
          <option value="" disabled>
            {{ $t("form.option.valuesType.url.authType.empty.label") }}
          </option>
          <option
            v-for="option in remoteUrlAuthenticationList"
            :value="option.value"
            :key="option.value"
          >
            {{ option.label }}
          </option>
        </select>
      </div>

      <div class="col-md-8">
        <!--USER/PASSSWORD AUTH-->
        <div
          id="remoteUrlUserAuth"
          v-if="remoteUrlAuthenticationType === 'BASIC'"
        >
          <div>
            <div class="col-md-3">
              <label class="control-label">{{
                $t("form.option.valuesType.url.authentication.username.label")
              }}</label>
            </div>
            <div class="col-md-8 input-group">
              <input
                type="text"
                class="form-control"
                name="remoteUrlUsername"
                v-model="internal.configRemoteUrl.username"
                size="30"
              />
            </div>
          </div>
          <div>
            <div class="col-md-3">
              <label class="control-label">{{
                $t("form.option.valuesType.url.authentication.password.label")
              }}</label>
            </div>
            <div class="col-md-8 input-group">
              <span
                class="input-group-addon has_tooltip"
                :title="$t('form.option.defaultStoragePath.description')"
              >
                <i class="glyphicon glyphicon-lock"></i>
              </span>

              <input
                type="text"
                class="form-control"
                v-model="internal.configRemoteUrl.passwordStoragePath"
                size="20"
              />

              <span class="input-group-btn">
                <key-storage-selector
                  v-model="internal.configRemoteUrl.passwordStoragePath"
                  :storage-filter="'Rundeck-data-type=password'"
                  :allow-upload="true"
                  :read-only="false"
                />
              </span>
            </div>
          </div>
        </div>
        <!--USER/PASSSWORD AUTH-->

        <!--TOKEN AUTH-->
        <div
          id="remoteUrlTokenAuth"
          v-if="remoteUrlAuthenticationType === 'API_KEY'"
        >
          <div>
            <div class="col-md-3">
              <label class="control-label">{{
                $t("form.option.valuesType.url.authentication.key.label")
              }}</label>
            </div>
            <div class="col-md-8 input-group">
              <input
                type="text"
                class="form-control"
                name="remoteUrlKey"
                v-model="internal.configRemoteUrl.keyName"
                size="30"
              />
            </div>
          </div>
          <div>
            <div class="col-md-3">
              <label class="control-label">{{
                $t("form.option.valuesType.url.authentication.token.label")
              }}</label>
            </div>
            <div class="col-md-8 input-group">
              <span
                class="input-group-addon has_tooltip"
                :title="$t('form.option.defaultStoragePath.description')"
              >
                <i class="glyphicon glyphicon-lock"></i>
              </span>

              <input
                type="text"
                class="form-control"
                name="remoteUrlToken"
                v-model="internal.configRemoteUrl.tokenStoragePath"
                size="20"
                placeholder=""
              />

              <span class="input-group-btn">
                <key-storage-selector
                  v-model="internal.configRemoteUrl.tokenStoragePath"
                  :storage-filter="'Rundeck-data-type=password'"
                  :allow-upload="true"
                  :read-only="false"
                />
              </span>
            </div>
          </div>
          <div>
            <div class="col-md-3">
              <label class="control-label">{{
                $t(
                  "form.option.valuesType.url.authentication.tokenInformer.label",
                )
              }}</label>
            </div>
            <div class="col-md-8 input-group">
              <select
                name="remoteUrlApiTokenReporter"
                class="form-control"
                v-model="internal.configRemoteUrl.apiTokenReporter"
              >
                <option value="HEADER">
                  {{
                    $t(
                      "form.option.valuesType.url.authentication.tokenInformer.header.label",
                    )
                  }}
                </option>
                <option value="QUERY_PARAM">
                  {{
                    $t(
                      "form.option.valuesType.url.authentication.tokenInformer.query.label",
                    )
                  }}
                </option>
              </select>
            </div>
          </div>
        </div>
        <!--TOKEN AUTH-->

        <!--bearerToken AUTH-->
        <div
          id="remoteUrlBearerTokenAuth"
          v-if="remoteUrlAuthenticationType === 'BEARER_TOKEN'"
        >
          <div class="col-md-3">
            <label class="control-label">{{
              $t("form.option.valuesType.url.authentication.token.label")
            }}</label>
          </div>
          <div class="col-md-8 input-group">
            <span
              class="input-group-addon has_tooltip"
              :title="$t('form.option.defaultStoragePath.description')"
            >
              <i class="glyphicon glyphicon-lock"></i>
            </span>

            <input
              type="text"
              class="form-control"
              name="remoteUrlBearerToken"
              v-model="internal.configRemoteUrl.tokenStoragePath"
              size="20"
              placeholder=""
            />

            <span class="input-group-btn">
              <key-storage-selector
                v-model="internal.configRemoteUrl.tokenStoragePath"
                :storage-filter="'Rundeck-data-type=password'"
                :allow-upload="true"
                :read-only="false"
              />
            </span>
          </div>
        </div>
        <!--bearerToken AUTH-->
      </div>
    </div>
  </div>
</template>
<script lang="ts">
import ErrorsList from "@/app/components/job/options/ErrorsList.vue";
import KeyStorageSelector from "@/library/components/plugins/KeyStorageSelector.vue";
import PluginInfo from "@/library/components/plugins/PluginInfo.vue";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";

export default defineComponent({
  name: "OptionRemoteUrlConfig",
  components: { ErrorsList, PluginInfo, KeyStorageSelector },
  emits: [
    "update:configRemoteUrl",
    "update:remoteUrlAuthenticationType",
    "update:valuesUrl",
  ],
  props: {
    validationErrors: {
      type: Object,
      default: () => {},
    },
    configRemoteUrl: {
      type: Object,
      default: () => {},
    },
    remoteUrlAuthenticationType: {
      type: String,
      required: true,
    },
    valuesUrl: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      remoteUrlAuthenticationList: [
        {
          value: "BASIC",
          label: this.$t("form.option.valuesType.url.authType.basic.label"),
        },
        {
          value: "API_KEY",
          label: this.$t("form.option.valuesType.url.authType.apiKey.label"),
        },
        {
          value: "BEARER_TOKEN",
          label: this.$t(
            "form.option.valuesType.url.authType.bearerToken.label",
          ),
        },
      ],
      internal: {
        valuesUrl: this.valuesUrl,
        configRemoteUrl: this.configRemoteUrl
          ? cloneDeep(this.configRemoteUrl)
          : {},
        remoteUrlAuthenticationType: this.remoteUrlAuthenticationType,
      },
    };
  },
  watch: {
    internal: {
      deep: true,
      handler(newVal) {
        this.$emit("update:configRemoteUrl", newVal.configRemoteUrl);
        this.$emit(
          "update:remoteUrlAuthenticationType",
          newVal.remoteUrlAuthenticationType,
        );
        this.$emit("update:valuesUrl", newVal.valuesUrl);
      },
    },
  },
  methods: {
    hasError(field: string) {
      return (
        (this.validationErrors[field] &&
          this.validationErrors[field].length > 0) ||
        Object.keys(this.validationErrors).find((k) =>
          k.startsWith(field + "."),
        )
      );
    },
  },
});
</script>
<style scoped lang="scss"></style>
