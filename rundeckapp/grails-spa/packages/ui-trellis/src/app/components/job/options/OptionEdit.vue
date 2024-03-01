<template>
  <div class="optEditForm">
    <div class="alert alert-danger" v-if="error">
      {{ error }}
    </div>
    <div class="row" v-if="newOption">
      <div class="col-sm-12">
        <span class="h4">{{ $t("add.new.option") }}</span>
      </div>
    </div>

    <!-- TODO restructure the following into subcomponents -->

    <div class="form-group">
      <label for="opttype_" class="col-sm-2 control-label">
        {{ $t("form.option.type.label") }}
      </label>
      <div class="col-sm-10">
        <select
          v-model="option.optionType"
          name="optionType"
          class="form-control"
          id="opttype_"
        >
          <option value="text">
            {{ $t("form.option.optionType.text.label") }}
          </option>
          <option
            value="file"
            v-if="fileUploadPluginEnabled && fileUploadPluginType"
          >
            {{ $t("form.option.optionType.file.label") }}
          </option>
        </select>
      </div>
    </div>

    <!-- file input (file) -->
    <div class="form-group" v-if="option.optionType === 'file'">
      <div class="col-sm-10 col-sm-offset-2">
        <div v-if="fileUploadPluginEnabled && fileUploadPluginType">
          <plugin-config
            :mode="'edit'"
            :serviceName="'FileUpload'"
            v-model="option.configMap"
            :provider="fileUploadPluginType"
            :show-title="false"
            :show-description="false"
            scope="Instance"
          />
        </div>
      </div>
    </div>

    <!-- name (all)-->
    <div
      class="form-group"
      data-test="option.name"
      :class="{
        'has-error': validationErrors['name'],
        'has-warning': validationWarnings['name'],
      }"
    >
      <label
        for="optname_"
        class="col-sm-2 control-label"
        :class="{ 'has-error': hasError('name') }"
      >
        {{ $t("form.option.name.label") }}
      </label>

      <div class="col-sm-10">
        <input
          type="text"
          v-model="option.name"
          @input="validateOptionName"
          @blur="validateOptionName"
          name="name"
          class="form-control"
          size="40"
          :placeholder="$t('form.option.name.label')"
          id="optname_"
          maxlength="255"
        />
        <div class="help-block" v-if="validationErrors['name']">
          <ErrorsList :errors="validationErrors['name']" />
        </div>
        <div class="help-block" v-if="validationWarnings['name']">
          <ErrorsList :errors="validationWarnings['name']" />
        </div>
      </div>
    </div>

    <!-- label (all)-->
    <div
      class="form-group"
      data-test="option.label"
      :class="{ 'has-error': hasError('label') }"
    >
      <label for="opt_label" class="col-sm-2 control-label">
        {{ $t("form.option.label.label") }}
      </label>

      <div class="col-sm-10">
        <input
          type="text"
          class="form-control"
          name="label"
          id="opt_label"
          v-model="option.label"
          @input="validateOptionLabel"
          @blur="validateOptionLabel"
          size="40"
          :placeholder="$t('form.option.label.label')"
          maxlength="255"
        />
        <div class="help-block" v-if="validationErrors['label']">
          <ErrorsList :errors="validationErrors['label']" />
        </div>
      </div>
    </div>

    <!-- description (all) -->
    <div
      class="form-group"
      data-test="option.description"
      :class="{ 'has-error': hasError('description') }"
    >
      <label class="col-sm-2 control-label" for="optdesc_">{{
        $t("form.option.description.label")
      }}</label>
      <div class="col-sm-10">
        <ace-editor
          v-model="option.description"
          :soft-wrap-control="false"
          height="120px"
          width="100%"
          lang="markdown"
          :readOnly="false"
        />
        <div class="help-block" v-if="validationErrors['description']">
          <ErrorsList :errors="validationErrors['description']" />
        </div>
        <div class="help-block">
          {{ $t("Option.property.description.description") }}
          <a
            href="http://en.wikipedia.org/wiki/Markdown"
            target="_blank"
            class="text-info"
          >
            <i class="glyphicon glyphicon-question-sign"></i>
          </a>
        </div>
      </div>
    </div>

    <!-- main section (text) -->
    <div v-if="option.optionType !== 'file'">
      <!-- default -->
      <div
        class="form-group"
        data-test="option.value"
        :class="{ 'has-error': hasError('value') }"
        v-if="showDefaultValue"
      >
        <label class="col-sm-2 control-label">{{
          $t("form.option.defaultValue.label")
        }}</label>
        <div class="col-sm-10">
          <input
            type="text"
            class="form-control"
            name="defaultValue"
            size="40"
            :placeholder="$t('form.option.defaultValue.label')"
            v-model="option.value"
          />
          <div class="help-block" v-if="validationErrors['value']">
            <ErrorsList :errors="validationErrors['value']" />
          </div>
        </div>
      </div>

      <!-- default key -->
      <div
        class="opt_sec_enabled form-group"
        data-test="option.defaultStoragePath"
        :class="{ 'has-error': hasError('defaultStoragePath') }"
        v-if="shouldShowDefaultStorage"
      >
        <label class="col-sm-2 control-label">
          {{ $t("form.option.defaultStoragePath.label") }}
        </label>

        <div class="col-sm-10">
          <div class="input-group">
            <span
              class="input-group-addon has_tooltip"
              :title="$t('form.option.defaultStoragePath.description')"
            >
              <i class="glyphicon glyphicon-lock"></i>
            </span>

            <input
              type="text"
              class="form-control"
              id="defaultStoragePath_"
              name="defaultStoragePath"
              v-model="option.defaultStoragePath"
              size="40"
              :placeholder="$t('form.option.defaultStoragePath.description')"
            />

            <span class="input-group-btn">
              <key-storage-selector
                v-model="option.defaultStoragePath"
                :storage-filter="'Rundeck-data-type=password'"
                :allow-upload="true"
                :value="'keys'"
                :read-only="false"
              />
            </span>
          </div>
          <div class="help-block" v-if="validationErrors['defaultStoragePath']">
            <ErrorsList :errors="validationErrors['defaultStoragePath']" />
          </div>
        </div>
      </div>

      <!-- input type -->
      <div class="form-group">
        <label class="col-sm-2 control-label">{{
          $t("form.option.inputType.label")
        }}</label>
        <div class="col-sm-10">
          <div class="radio">
            <input
              type="radio"
              name="inputType"
              value="plain"
              v-model="option.inputType"
              id="inputplain_"
            />
            <label for="inputplain_">
              {{ $t("form.option.secureInput.false.label") }}
            </label>
          </div>

          <div class="radio">
            <input
              type="radio"
              name="inputType"
              value="date"
              v-model="option.inputType"
              id="inputdate_"
            />
            <label for="inputdate_">
              {{ $t("form.option.date.label") }}
              <span class="text-strong">
                {{ $t("form.option.date.description") }}
              </span>
            </label>
          </div>
          <div v-if="option.isDate">
            <label>
              {{ $t("form.option.dateFormat.title") }}
              <input
                type="text"
                name="dateFormat"
                class="form-control"
                v-model="option.dateFormat"
                size="60"
                placeholder="MM/DD/YYYY hh:mm a"
              />
            </label>
            <span class="help-block">
              <VMarkdownView
                class="markdown-body"
                mode=""
                :content="$t(`form.option.dateFormat.description.md`)"
              />
            </span>
          </div>

          <div class="radio">
            <input
              type="radio"
              name="inputType"
              value="secureExposed"
              v-model="option.inputType"
              id="sectrue_"
            />
            <label for="sectrue_">
              {{ $t("form.option.secureExposed.true.label") }}
              <span class="text-danger small"> &dagger; </span>
              <span class="text-strong">
                {{ $t("form.option.secureExposed.true.description") }}
              </span>
            </label>
          </div>

          <div class="radio">
            <input
              type="radio"
              name="inputType"
              value="secure"
              v-model="option.inputType"
              id="secexpfalse_"
            />
            <label for="secexpfalse_">
              {{ $t("form.option.secureExposed.false.label") }}
              <span class="text-danger small"> &dagger; </span>
              <span class="text-strong">
                {{ $t("form.option.secureExposed.false.description") }}
              </span>
            </label>
          </div>
          <div class="help-block">
            <span class="text-danger small">&dagger;</span>
            {{ $t("form.option.secureInput.description") }}
          </div>
        </div>
      </div>
      <div class="form-group" v-if="!isSecureInput">
        <label class="col-sm-2 control-label">{{
          $t("form.option.values.label")
        }}</label>
        <div class="col-sm-3">
          <div>
            <div class="radio">
              <input
                type="radio"
                name="valuesType"
                value="list"
                v-model="option.valuesType"
                id="vtrlist_"
              />
              <label
                for="vtrlist_"
                :class="{ 'has-error': hasError('values') }"
              >
                {{ $t("form.label.valuesType.list.label") }}
              </label>
            </div>

            <div class="radio">
              <input
                type="radio"
                name="valuesType"
                value="url"
                v-model="option.valuesType"
                id="vtrurl_"
              />
              <label
                for="vtrurl_"
                class="left"
                :class="{ 'has-error': hasError('valuesUrl') }"
              >
                {{ $t("form.option.valuesType.url.label") }}
              </label>
            </div>
            <template v-if="features['optionValuesPlugin']">
              <template v-for="optionValPlugin in optionValuesPlugins">
                <div class="radio">
                  <input
                    type="radio"
                    name="valuesType"
                    v-model="option.valuesType"
                    :value="optionValPlugin.name"
                    :id="'optvalplugin_' + optionValPlugin.name"
                  />
                  <label
                    :for="'optvalplugin_' + optionValPlugin.name"
                    :class="{ 'has-error': hasError('valuesFromPlugin') }"
                    :title="optionValPlugin.description"
                  >
                    <img
                      :src="optionValPlugin.iconUrl"
                      v-if="optionValPlugin.iconUrl"
                      style="width: 16px; height: 16px; margin-right: 5px"
                    />
                    {{ optionValPlugin.title || optionValPlugin.name }}
                  </label>
                </div>
              </template>
            </template>
          </div>
        </div>
        <div class="col-sm-7">
          <div
            id="vlist_section"
            data-test="option.values"
            v-if="option.valuesType === 'list'"
            :class="{ 'has-error': hasError('values') }"
          >
            <input
              type="text"
              name="valuesList"
              class="form-control"
              v-model="valuesList"
              size="60"
              :placeholder="$t('form.option.valuesList.placeholder')"
            />

            <div class="help-block" v-if="validationErrors['values']">
              <ErrorsList :errors="validationErrors['values']" />
            </div>
          </div>

          <template v-else-if="option.valuesType === 'url'">
            <option-remote-url-config
              v-model:config-remote-url="option.configRemoteUrl"
              v-model:remote-url-authentication-type="
                option.remoteUrlAuthenticationType
              "
              v-model:values-url="option.valuesUrl"
              :validation-errors="validationErrors"
            />
          </template>
          <div v-else-if="option.valuesType && optionValuesPlugins">
            <plugin-info
              :detail="getProviderFor(option.valuesType)"
              :show-icon="false"
              :show-title="false"
              :show-description="true"
              :show-extended="true"
              description-css="help-block"
            >
            </plugin-info>
          </div>
        </div>
      </div>
      <!-- sort values -->
      <div class="form-group">
        <label class="col-sm-2 control-label">{{
          $t("form.option.sort.label")
        }}</label>

        <div class="col-sm-3">
          <div class="radio radio-inline">
            <input
              type="radio"
              name="sortValues"
              :value="false"
              v-model="option.sortValues"
            />
            <label for="option-sort-values-no">
              {{ $t("no") }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              type="radio"
              name="sortValues"
              :value="true"
              v-model="option.sortValues"
            />
            <label for="option-sort-values-yes">
              {{ $t("yes") }}
            </label>
          </div>
          <div class="help-block">
            {{ $t("form.option.sort.description") }}
          </div>
        </div>

        <div
          class="input-group col-sm-3"
          data-test="option.valuesListDelimiter"
          :class="{ 'has-error': hasError('valuesListDelimiter') }"
        >
          <div class="input-group-addon" style="background-color: #e0e0e0">
            {{ $t("form.option.valuesDelimiter.label") }}
          </div>
          <input
            type="text"
            name="valuesListDelimiter"
            v-model="option.valuesListDelimiter"
            size="5"
            class="form-control"
          />
          <div
            class="help-block"
            v-if="validationErrors['valuesListDelimiter']"
          >
            <ErrorsList :errors="validationErrors['valuesListDelimiter']" />
          </div>
        </div>
        <span class="help-block">
          {{ $t("form.option.valuesDelimiter.description") }}
        </span>
      </div>
      <!-- enforced -->
      <div
        class="form-group opt_keystorage_disabled"
        data-test="option.regex"
        :class="{ 'has-error': hasError('regex') }"
        v-if="!isSecureInput"
      >
        <label class="col-sm-2 control-label">{{
          $t("form.option.enforcedType.label")
        }}</label>
        <div class="col-sm-10">
          <div class="radio">
            <input
              type="radio"
              v-model="enforcedType"
              value="none"
              id="enforcedType_none"
            />
            <label for="enforcedType_none">
              {{ $t("none") }}
              <span class="text-strong">{{
                $t("form.option.enforcedType.none.label")
              }}</span>
            </label>
          </div>
          <div class="radio">
            <input
              type="radio"
              v-model="enforcedType"
              value="enforced"
              id="enforcedType_enforced"
            />
            <label
              for="enforcedType_enforced"
              :class="{ 'has-error': hasError('enforced') }"
            >
              {{ $t("form.option.enforced.label") }}
            </label>
          </div>
          <div class="radio">
            <input
              type="radio"
              v-model="enforcedType"
              value="regex"
              id="etregex_"
            />
            <label for="etregex_">
              {{ $t("form.option.regex.label") }}
            </label>
          </div>
        </div>
        <div class="col-sm-10 col-sm-offset-2" v-if="enforcedType === 'regex'">
          <input
            type="text"
            name="regex"
            class="form-control"
            v-model="option.regex"
            size="40"
            :placeholder="$t('form.option.regex.placeholder')"
            id="vregex_"
          />
          <div class="help-block" v-if="validationErrors['regex']">
            <ErrorsList :errors="validationErrors['regex']" />
          </div>
          <template v-if="validationErrors['regexError']">
            <pre class="text-danger">{{
              validationErrors["regexError"][0]
            }}</pre>
          </template>
        </div>
      </div>

      <!-- end MAIN section -->
    </div>
    <!-- required (all) -->
    <div class="form-group">
      <label class="col-sm-2 control-label">{{
        $t("Option.required.label")
      }}</label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            type="radio"
            id="option-required-no"
            name="required"
            :value="false"
            v-model="option.required"
          />
          <label for="option-required-no">
            {{ $t("no") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            type="radio"
            id="option-required-yes"
            name="required"
            :value="true"
            v-model="option.required"
          />
          <label for="option-required-yes">
            {{ $t("yes") }}
          </label>
        </div>
        <div class="help-block">
          {{ $t("Option.required.description") }}
        </div>
      </div>
    </div>

    <!-- hidden (text) -->
    <div
      class="form-group"
      v-if="option.optionType !== 'file'"
      data-test="option.hidden"
      :class="{ 'has-error': hasError('hidden') }"
    >
      <label class="col-sm-2 control-label">{{
        $t("Option.hidden.label")
      }}</label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            type="radio"
            id="option-hidden-no"
            name="hidden"
            :value="false"
            v-model="option.hidden"
          />
          <label for="option-hidden-no">
            {{ $t("no") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            type="radio"
            id="option-hidden-yes"
            name="hidden"
            :value="true"
            v-model="option.hidden"
          />
          <label for="option-hidden-yes">
            {{ $t("yes") }}
          </label>
        </div>
        <div class="help-block">
          {{ $t("Option.hidden.description") }}
        </div>

        <div class="help-block" v-if="validationErrors['hidden']">
          <ErrorsList :errors="validationErrors['hidden']" />
        </div>
      </div>
    </div>

    <!-- multivalue (text) -->
    <div
      class="form-group"
      v-if="option.optionType !== 'file'"
      data-test="option.delimiter"
      :class="{ 'has-error': hasError('multivalued') || hasError('delimiter') }"
    >
      <label class="col-sm-2 control-label">
        {{ $t("form.option.multivalued.label") }}
      </label>
      <div class="col-sm-10">
        <div class="opt_sec_disabled" v-if="!isSecureInput">
          <div class="radio radio-inline">
            <input
              type="radio"
              name="multivalued"
              :value="false"
              v-model="option.multivalued"
              id="mvfalse_"
            />
            <label for="mvfalse_">
              {{ $t("no") }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              type="radio"
              name="multivalued"
              :value="true"
              v-model="option.multivalued"
              id="cdelimiter_"
            />
            <label for="cdelimiter_">
              {{ $t("yes") }}
            </label>
          </div>

          <div class="help-block" v-if="!option.multivalued">
            {{ $t("form.option.multivalued.description") }}
          </div>
          <div v-else>
            <div
              class="input-group col-sm-3"
              :class="{ 'has-error': hasError('delimiter') }"
            >
              <div class="input-group-addon">
                {{ $t("form.option.delimiter.label") }}
              </div>
              <input
                type="text"
                name="delimiter"
                v-model="option.delimiter"
                size="5"
                class="form-control"
                id="vdelimiter_"
              />
            </div>
            <div class="help-block" v-if="validationErrors['delimiter']">
              <ErrorsList :errors="validationErrors['delimiter']" />
            </div>
            <span class="help-block">
              {{ $t("form.option.delimiter.description") }}
            </span>
          </div>
          <div v-if="option.multivalued">
            <div :class="{ 'has-error': hasError('multivalueAllSelected') }">
              <div class="checkbox">
                <input
                  type="checkbox"
                  name="multivalueAllSelected"
                  :value="true"
                  v-model="option.multivalueAllSelected"
                  id="mvalltrue_"
                />
                <label
                  for="mvalltrue_"
                  :class="{ 'has-error': hasError('multivalued') }"
                >
                  {{ $t("form.option.multivalueAllSelected.label") }}
                </label>
              </div>
            </div>
          </div>
        </div>
        <div class="presentation" id="mvsecnote" v-if="isSecureInput">
          <span class="warn note">
            {{ $t("form.option.multivalued.secure-conflict.message") }}
          </span>
        </div>
      </div>
    </div>

    <!-- preview (text)  -->
    <option-usage-preview
      :option="option"
      :validation-errors="validationErrors"
    />
    <div class="flow-h" style="margin: 10px 0">
      <template v-if="newOption">
        <btn
          size="sm"
          @click="$emit('cancel')"
          :title="$t('form.option.cancel.title')"
          >{{ $t("cancel") }}
        </btn>

        <btn
          size="sm"
          type="cta"
          @click="doSave"
          :title="$t('form.option.create.title')"
          >{{ $t("save") }}
        </btn>
      </template>
      <template v-else>
        <btn
          size="sm"
          @click="$emit('cancel')"
          :title="$t('form.option.discard.title')"
          >{{ $t("discard") }}
        </btn>
        <btn
          size="sm"
          type="cta"
          @click="doSave"
          :title="$t('form.option.save.title')"
          >{{ $t("save") }}
        </btn>
      </template>
      <span class="text-warning cancelsavemsg" style="display: none">
        {{ $t("scheduledExecution.option.unsaved.warning") }}
      </span>
      <span class="text-danger" v-if="hasFormErrors">
        {{ $t("form.option.validation.errors.message") }}
      </span>
    </div>
  </div>
</template>
<script lang="ts">
import ErrorsList from "./ErrorsList.vue";
import OptionUsagePreview from "./OptionUsagePreview.vue";
import OptionRemoteUrlConfig from "./OptionRemoteUrlConfig.vue";
import { validateJobOption } from "@/library/services/jobEdit";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";

import { VMarkdownView } from "vue3-markdown";
import { getRundeckContext } from "../../../../library";
import KeyStorageSelector from "../../../../library/components/plugins/KeyStorageSelector.vue";
import PluginConfig from "../../../../library/components/plugins/pluginConfig.vue";
import PluginInfo from "../../../../library/components/plugins/PluginInfo.vue";

import AceEditor from "../../../../library/components/utils/AceEditor.vue";
import {
  JobOption,
  JobOptionEdit,
  OptionPrototype,
} from "../../../../library/types/jobs/JobEdit";

export default defineComponent({
  name: "OptionEdit",
  components: {
    ErrorsList,
    KeyStorageSelector,
    PluginConfig,
    AceEditor,
    VMarkdownView,
    PluginInfo,
    OptionUsagePreview,
    OptionRemoteUrlConfig,
  },
  emits: ["update:modelValue", "cancel", "save"],
  props: {
    error: String,
    newOption: { type: Boolean, default: false },
    modelValue: { type: Object, default: () => ({}) as JobOption },
    features: { type: Object, default: () => ({}) },
    fileUploadPluginType: { type: String, default: "" },
    errors: { type: Object, default: () => ({}) },
    optionValuesPlugins: { type: Array, default: () => [] },
    uiFeatures: { type: Object, default: () => ({}) },
  },
  data() {
    return {
      option: Object.assign(
        {},
        OptionPrototype,
        {
          valuesType: this.modelValue.optionValuesPluginType
            ? this.modelValue.optionValuesPluginType
            : this.modelValue.valuesUrl
              ? "url"
              : "list",
        },
        cloneDeep(this.modelValue),
      ) as JobOptionEdit,
      regexChoice: false,
      validationErrors: {},
      validationWarnings: {},
    };
  },
  watch: {
    "option.inputType"(val: string) {
      this.option.isDate = val === "date";
      this.option.secure = val === "secure" || val === "secureExposed";
      this.option.valueExposed = val === "secureExposed";
      if (this.option.secure) {
        this.option.multivalued = false;
      } else {
        delete this.option.defaultStoragePath;
      }
    },
    "option.valuesType"(val: string) {
      if (val === "url") {
        delete this.option.optionValuesPluginType;
        this.option.valuesUrl = "";
        this.option.remoteUrlAuthenticationType = "";
        this.option.configRemoteUrl = {};
      } else if (val === "list") {
        delete this.option.optionValuesPluginType;
        delete this.option.valuesUrl;
        delete this.option.remoteUrlAuthenticationType;
        delete this.option.configRemoteUrl;
      } else {
        this.option.optionValuesPluginType = val;
        delete this.option.valuesUrl;
        delete this.option.remoteUrlAuthenticationType;
        delete this.option.configRemoteUrl;
      }
    },
  },
  computed: {
    fileUploadPluginEnabled() {
      return this.features["fileUploadPlugin"];
    },
    isDate() {
      return this.option.isDate;
    },
    isSecureInput() {
      return this.option.secure;
    },
    showDefaultValue() {
      return !this.isSecureInput;
    },
    shouldShowDefaultStorage() {
      return !this.showDefaultValue;
    },
    valuesList: {
      get() {
        if (this.option.values && this.option.values.length > 0) {
          return this.option.values.join(this.option.delimiter || ",");
        } else {
          return "";
        }
      },
      set(val: string) {
        if (val) {
          this.option.values = val.split(this.option.delimiter || ",");
        } else {
          this.option.values = null;
        }
      },
    },
    enforcedType: {
      get() {
        if (this.option.enforced) {
          return "enforced";
        }
        if (this.option.regex || this.regexChoice) {
          return "regex";
        }
        return "none";
      },
      set(val: string) {
        this.regexChoice = val === "regex";
        if (val === "enforced") {
          this.option.enforced = true;
          delete this.option.regex;
        } else if (val === "regex") {
          delete this.option.enforced;
          this.option.regex = "";
        } else {
          delete this.option.enforced;
          delete this.option.regex;
        }
      },
    },
    hasFormErrors(): boolean {
      return (
        Object.keys(this.validationErrors).length > 0 ||
        Object.keys(this.validationWarnings).length > 0
      );
    },
  },
  methods: {
    async doSave() {
      this.validationErrors = {};
      this.validationWarnings = {};
      this.validateOptionLabel();
      this.validateOptionName();
      if (this.hasFormErrors) {
        return;
      }
      await this.validateOption();
      if (this.hasFormErrors) {
        return;
      }
      this.$emit("update:modelValue", this.option);
    },
    hasError(field: string) {
      return (
        (this.validationErrors[field] &&
          this.validationErrors[field].length > 0) ||
        Object.keys(this.validationErrors).find((k) =>
          k.startsWith(field + "."),
        )
      );
    },
    addError(field: string, error: string) {
      if (!this.validationErrors[field]) {
        this.validationErrors[field] = [];
      }
      this.validationErrors[field].push(error);
    },
    addWarning(field: string, error: string) {
      if (!this.validationWarnings[field]) {
        this.validationWarnings[field] = [];
      }
      this.validationWarnings[field].push(error);
    },
    clearValidation(field: string) {
      delete this.validationWarnings[field];
      delete this.validationErrors[field];
    },
    getProviderFor(name) {
      return this.optionValuesPlugins.find((p) => p.name === name);
    },
    validateLen(field: string, max: number): boolean {
      if (this.option[field] && this.option[field].length > max) {
        return false;
      } else {
        return true;
      }
    },
    validateOptionName() {
      this.clearValidation("name");
      if (!this.option.name) {
        this.addWarning("name", this.$t("form.field.required.message"));
        return;
      }
      if (!this.validateLen("name", 255)) {
        this.addError(
          "name",
          this.$t("form.field.too.long.message", {
            max: 255,
          }),
        );
      }
      let validOptionNameRegex = /^[a-zA-Z_0-9.-]+$/;
      let inputResult = validOptionNameRegex.test(this.option.name);
      if (!inputResult) {
        this.addError(
          "name",
          this.$t("form.option.name.validation.error", [
            validOptionNameRegex.toString(),
          ]),
        );
      }
    },
    validateOptionLabel() {
      this.clearValidation("label");
      if (!this.validateLen("label", 255)) {
        this.addError(
          "label",
          this.$t("form.field.too.long.message", {
            max: 255,
          }),
        );
      }
    },
    async validateOption() {
      let res = await validateJobOption(getRundeckContext().projectName, {
        ...this.option,
        newoption: this.newOption,
      });
      if (res.messages) {
        this.validationErrors = res.messages;
      }
    },
  },
});
</script>

<style scoped lang="scss">
.flow > * + * {
  margin-top: var(--spacing-2);
}
.flow-h > * + * {
  margin-left: var(--spacing-2);
}
</style>
