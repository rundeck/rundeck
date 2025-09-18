<template>
  <div>
    <div v-if="error" class="alert alert-danger">
      {{ error }}
    </div>
    <div v-if="newOption" class="row">
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
          id="opttype_"
          v-model="option.type"
          name="optionType"
          class="form-control"
        >
          <option value="text">
            {{ $t("form.option.optionType.text.label") }}
          </option>
          <option
            v-if="fileUploadPluginEnabled && fileUploadPluginType"
            value="file"
          >
            {{ $t("form.option.optionType.file.label") }}
          </option>
          <option v-if="multilineJobOptionsEnabled" value="multiline">
            {{ $t("form.option.optionType.multiline.label") }}
          </option>
        </select>
      </div>
    </div>

    <!-- file input (file) -->
    <div v-if="option.type === 'file'" class="form-group">
      <div class="col-sm-10 col-sm-offset-2">
        <div v-if="fileUploadPluginEnabled && fileUploadPluginType">
          <plugin-config
            v-model="option.configMap"
            :mode="'edit'"
            :service-name="'FileUpload'"
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
          id="optname_"
          v-model="option.name"
          type="text"
          name="name"
          class="form-control"
          size="40"
          :placeholder="$t('form.option.name.label')"
          maxlength="255"
          @input="validateOptionName"
          @blur="validateOptionName"
        />
        <div v-if="validationErrors['name']" class="help-block">
          <ErrorsList :errors="validationErrors['name']" />
        </div>
        <div v-if="validationWarnings['name']" class="help-block">
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
          id="opt_label"
          v-model="option.label"
          type="text"
          class="form-control"
          name="label"
          size="40"
          :placeholder="$t('form.option.label.label')"
          maxlength="255"
          @input="validateOptionLabel"
          @blur="validateOptionLabel"
        />
        <div v-if="validationErrors['label']" class="help-block">
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
          :read-only="false"
        />
        <div v-if="validationErrors['description']" class="help-block">
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
    <div v-if="option.type !== 'file'">
      <!-- default -->
      <div
        v-if="showDefaultValue"
        class="form-group"
        data-test="option.value"
        :class="{ 'has-error': hasError('value') }"
      >
        <label class="col-sm-2 control-label" for="opt_defaultValue">{{
          $t("form.option.defaultValue.label")
        }}</label>
        <div class="col-sm-10">
          <input
            v-if="!isMultilineType"
            id="opt_defaultValue"
            v-model="option.value"
            type="text"
            class="form-control"
            name="defaultValue"
            size="40"
            :placeholder="$t('form.option.defaultValue.label')"
          />

          <textarea
            v-if="isMultilineType"
            id="opt_defaultValue"
            v-model="option.value"
            class="form-control"
            name="defaultValue"
            rows="4"
            cols="40"
            :placeholder="$t('form.option.defaultValue.label')"
          ></textarea>
          <div v-if="validationErrors['value']" class="help-block">
            <ErrorsList :errors="validationErrors['value']" />
          </div>
        </div>
      </div>

      <!-- default key -->
      <div
        v-if="shouldShowDefaultStorage"
        class="opt_sec_enabled form-group"
        data-test="option.storagePath"
        :class="{ 'has-error': hasError('storagePath') }"
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
              id="storagePath_"
              v-model="option.storagePath"
              type="text"
              class="form-control"
              name="storagePath"
              size="40"
              :placeholder="$t('form.option.defaultStoragePath.description')"
            />

            <span class="input-group-btn">
              <key-storage-selector
                v-model="option.storagePath"
                :storage-filter="'Rundeck-data-type=password'"
                :allow-upload="true"
                :read-only="false"
              />
            </span>
          </div>
          <div v-if="validationErrors['storagePath']" class="help-block">
            <ErrorsList :errors="validationErrors['storagePath']" />
          </div>
        </div>
      </div>

      <!-- input type -->
      <div v-if="showInputType" class="form-group" data-test="option.inputType">
        <label class="col-sm-2 control-label">{{
          $t("form.option.inputType.label")
        }}</label>
        <div class="col-sm-10">
          <div class="radio">
            <input
              id="inputplain_"
              v-model="option.inputType"
              type="radio"
              name="inputType"
              value="plain"
            />
            <label for="inputplain_">
              {{ $t("form.option.secureInput.false.label") }}
            </label>
          </div>

          <div class="radio">
            <input
              id="inputdate_"
              v-model="option.inputType"
              type="radio"
              name="inputType"
              value="date"
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
                v-model="option.dateFormat"
                type="text"
                name="dateFormat"
                class="form-control"
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
              id="sectrue_"
              v-model="option.inputType"
              type="radio"
              name="inputType"
              value="secureExposed"
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
              id="secexpfalse_"
              v-model="option.inputType"
              type="radio"
              name="inputType"
              value="secure"
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
      <div
        v-if="showAllowedValues"
        class="form-group"
        data-test="option.valuesType"
      >
        <label class="col-sm-2 control-label">{{
          $t("form.option.values.label")
        }}</label>
        <div class="col-sm-3">
          <div>
            <div class="radio">
              <input
                id="vtrlist_"
                v-model="option.valuesType"
                type="radio"
                name="valuesType"
                value="list"
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
                id="vtrurl_"
                v-model="option.valuesType"
                type="radio"
                name="valuesType"
                value="url"
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
                    :id="'optvalplugin_' + optionValPlugin.name"
                    v-model="option.valuesType"
                    type="radio"
                    name="valuesType"
                    :value="optionValPlugin.name"
                  />
                  <label
                    :for="'optvalplugin_' + optionValPlugin.name"
                    :class="{ 'has-error': hasError('valuesFromPlugin') }"
                    :title="optionValPlugin.description"
                  >
                    <img
                      v-if="optionValPlugin.iconUrl"
                      :src="optionValPlugin.iconUrl"
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
            v-if="option.valuesType === 'list'"
            id="vlist_section"
            data-test="option.values"
            :class="{ 'has-error': hasError('values') }"
          >
            <input
              v-model="valuesList"
              type="text"
              name="valuesList"
              class="form-control"
              size="60"
              :placeholder="$t('form.option.valuesList.placeholder')"
            />

            <div v-if="validationErrors['values']" class="help-block">
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
      <div
        v-if="showAllowedValues"
        class="form-group"
        data-test="option.sortValues"
      >
        <label class="col-sm-2 control-label">{{
          $t("form.option.sort.label")
        }}</label>

        <div class="col-sm-3">
          <div class="radio radio-inline">
            <input
              v-model="option.sortValues"
              type="radio"
              name="sortValues"
              :value="false"
            />
            <label for="option-sort-values-no">
              {{ $t("no") }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              v-model="option.sortValues"
              type="radio"
              name="sortValues"
              :value="true"
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
            v-model="option.valuesListDelimiter"
            type="text"
            name="valuesListDelimiter"
            size="5"
            class="form-control"
          />
          <div
            v-if="validationErrors['valuesListDelimiter']"
            class="help-block"
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
        v-if="!isSecureInput"
        class="form-group opt_keystorage_disabled"
        data-test="option.regex"
        :class="{ 'has-error': hasError('regex') }"
      >
        <label class="col-sm-2 control-label">{{
          $t("form.option.enforcedType.label")
        }}</label>
        <div class="col-sm-10">
          <div class="radio">
            <input
              id="enforcedType_none"
              v-model="enforcedType"
              type="radio"
              value="none"
            />
            <label for="enforcedType_none">
              {{ $t("none") }}
              <span class="text-strong">{{
                $t("form.option.enforcedType.none.label")
              }}</span>
            </label>
          </div>
          <div v-if="!isMultilineType" class="radio">
            <input
              id="enforcedType_enforced"
              v-model="enforcedType"
              type="radio"
              value="enforced"
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
              id="etregex_"
              v-model="enforcedType"
              type="radio"
              value="regex"
            />
            <label for="etregex_">
              {{ $t("form.option.regex.label") }}
            </label>
          </div>
        </div>
        <div v-if="enforcedType === 'regex'" class="col-sm-10 col-sm-offset-2">
          <input
            id="vregex_"
            v-model="option.regex"
            type="text"
            name="regex"
            class="form-control"
            size="40"
            :placeholder="$t('form.option.regex.placeholder')"
          />
          <span class="help-block">
            <VMarkdownView
              class="markdown-body"
              :content="$t(`form.option.regex.description.md`)"
            />
            <VMarkdownView
              v-if="isMultilineType"
              class="markdown-body"
              :content="$t(`form.option.regex.multiline.description.md`)"
            />
          </span>
          <div v-if="validationErrors['regex']" class="help-block">
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
    <div
      class="form-group"
      data-test="option.required"
      :class="{ 'has-error': hasError('required') }"
    >
      <label class="col-sm-2 control-label">{{
        $t("Option.required.label")
      }}</label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="option-required-no"
            v-model="option.required"
            type="radio"
            name="required"
            :value="false"
          />
          <label for="option-required-no">
            {{ $t("no") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="option-required-yes"
            v-model="option.required"
            type="radio"
            name="required"
            :value="true"
          />
          <label for="option-required-yes">
            {{ $t("yes") }}
          </label>
        </div>
        <div class="help-block">
          {{ $t("Option.required.description") }}
        </div>
        <div v-if="validationErrors['required']" class="help-block">
          <ErrorsList :errors="validationErrors['required']" />
        </div>
      </div>
    </div>

    <!-- hidden (text) -->
    <div
      v-if="option.type !== 'file'"
      class="form-group"
      data-test="option.hidden"
      :class="{ 'has-error': hasError('hidden') }"
    >
      <label class="col-sm-2 control-label">{{
        $t("Option.hidden.label")
      }}</label>
      <div class="col-sm-10">
        <div class="radio radio-inline">
          <input
            id="option-hidden-no"
            v-model="option.hidden"
            type="radio"
            name="hidden"
            :value="false"
          />
          <label for="option-hidden-no">
            {{ $t("no") }}
          </label>
        </div>
        <div class="radio radio-inline">
          <input
            id="option-hidden-yes"
            v-model="option.hidden"
            type="radio"
            name="hidden"
            :value="true"
          />
          <label for="option-hidden-yes">
            {{ $t("yes") }}
          </label>
        </div>
        <div class="help-block">
          {{ $t("Option.hidden.description") }}
        </div>

        <div v-if="validationErrors['hidden']" class="help-block">
          <ErrorsList :errors="validationErrors['hidden']" />
        </div>
      </div>
    </div>

    <!-- multivalue (text) -->
    <div
      v-if="!isFileType && !isMultilineType"
      class="form-group"
      data-test="option.delimiter"
      :class="{ 'has-error': hasError('multivalued') || hasError('delimiter') }"
    >
      <label class="col-sm-2 control-label">
        {{ $t("form.option.multivalued.label") }}
      </label>
      <div class="col-sm-10">
        <div v-if="!isSecureInput" class="opt_sec_disabled">
          <div class="radio radio-inline">
            <input
              id="mvfalse_"
              v-model="option.multivalued"
              type="radio"
              name="multivalued"
              :value="false"
            />
            <label for="mvfalse_">
              {{ $t("no") }}
            </label>
          </div>
          <div class="radio radio-inline">
            <input
              id="cdelimiter_"
              v-model="option.multivalued"
              type="radio"
              name="multivalued"
              :value="true"
            />
            <label for="cdelimiter_">
              {{ $t("yes") }}
            </label>
          </div>

          <div v-if="!option.multivalued" class="help-block">
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
                id="vdelimiter_"
                v-model="option.delimiter"
                type="text"
                name="delimiter"
                size="5"
                class="form-control"
              />
            </div>
            <div v-if="validationErrors['delimiter']" class="help-block">
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
                  id="mvalltrue_"
                  v-model="option.multivalueAllSelected"
                  type="checkbox"
                  name="multivalueAllSelected"
                  :value="true"
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
        <div v-if="isSecureInput" id="mvsecnote" class="presentation">
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
          :title="$t('form.option.cancel.title')"
          @click="$emit('cancel')"
          >{{ $t("cancel") }}
        </btn>

        <btn
          size="sm"
          type="cta"
          :title="$t('form.option.create.title')"
          @click="doSave"
          >{{ $t("save") }}
        </btn>
      </template>
      <template v-else>
        <btn
          size="sm"
          :title="$t('form.option.discard.title')"
          @click="$emit('cancel')"
          >{{ $t("discard") }}
        </btn>
        <btn
          size="sm"
          type="cta"
          :title="$t('form.option.save.title')"
          @click="doSave"
          >{{ $t("save") }}
        </btn>
      </template>
      <span class="text-warning cancelsavemsg" style="display: none">
        {{ $t("scheduledExecution.option.unsaved.warning") }}
      </span>
      <span v-if="hasFormErrors" class="text-danger">
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
import { Validations, ValidationConfig } from "./model/Validations";
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
  props: {
    error: String,
    newOption: { type: Boolean, default: false },
    modelValue: { type: Object, default: () => ({}) as JobOption },
    features: { type: Object, default: () => ({}) },
    fileUploadPluginType: { type: String, default: "" },
    errors: { type: Object, default: () => ({}) },
    optionValuesPlugins: { type: Array, default: () => [] },
    uiFeatures: { type: Object, default: () => ({}) },
    jobWasScheduled: { type: Boolean, default: false },
  },
  emits: ["update:modelValue", "cancel", "save"],
  data() {
    return {
      option: Object.assign({}, OptionPrototype, cloneDeep(this.modelValue), {
        valuesType: this.modelValue.optionValuesPluginType
          ? this.modelValue.optionValuesPluginType
          : this.modelValue.valuesUrl
            ? "url"
            : "list",
        // inputType: this.modelValue.isMultiline //use isMultiline
        //   ? "multiline"
        //   : this.modelValue.isDate
        //     ? "date"
        //     : this.modelValue.secure
        //       ? this.modelValue.valueExposed
        //         ? "secureExposed"
        //         : "secure"
        //       : "plain",
        inputType: this.modelValue.isDate
          ? "date"
          : this.modelValue.secure
            ? this.modelValue.valueExposed
              ? "secureExposed"
              : "secure"
            : "plain",
      }) as JobOptionEdit,
      regexChoice: false,
      validationErrors: {},
      validationWarnings: {},
    };
  },
  computed: {
    fileUploadPluginEnabled() {
      return this.features["fileUploadPlugin"];
    },
    multilineJobOptionsEnabled() {
      return this.features["multilineJobOptions"];
    },
    isDate() {
      return this.option.isDate;
    },
    isSecureInput() {
      return this.option.secure;
    },
    isFileType() {
      return this.option.type === "file";
    },
    isMultilineType() {
      return this.option.type === "multiline";
    },
    showDefaultValue() {
      return !this.isSecureInput;
    },
    shouldShowDefaultStorage() {
      return !this.showDefaultValue;
    },
    showInputType() {
      return !this.isMultilineType;
    },
    showAllowedValues() {
      return !this.isSecureInput && !this.isMultilineType;
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
  watch: {
    "option.inputType"(val: string) {
      // this.option.isMultiline = val === "multiline";
      this.option.isDate = val === "date";
      this.option.secure = val === "secure" || val === "secureExposed";
      this.option.valueExposed = val === "secureExposed";
      if (this.option.secure) {
        this.option.multivalued = false;
      } else {
        delete this.option.storagePath;
      }
    },
    "option.valuesType"(val: string) {
      delete this.option.optionValuesPluginType;
      delete this.option.valuesUrl;
      delete this.option.remoteUrlAuthenticationType;
      delete this.option.configRemoteUrl;
      if (val === "url") {
        this.option.valuesUrl = "";
        this.option.remoteUrlAuthenticationType = "";
        this.option.configRemoteUrl = {};
      } else if (val !== "list") {
        this.option.optionValuesPluginType = val;
      }
    },
  },
  methods: {
    async doSave() {
      this.validationErrors = {};
      this.validationWarnings = {};
      this.localValidations();
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
      return !(this.option[field] && this.option[field].length > max);
    },
    validateRegex(field: string, regex: string): boolean {
      const testRegex = new RegExp(regex);
      return testRegex.test(this.option[field]);
    },
    validateFieldName(field: string): boolean {
      if (Validations[field]) {
        this.clearValidation(field);
        return this.validateField(field, Validations[field]);
      }
      return true;
    },
    validateField(field: string, validationConfig: ValidationConfig): boolean {
      let pass = true;
      if (validationConfig.required) {
        if (!this.option[field]) {
          pass = false;
          this.addWarning(field, this.$t("form.field.required.message"));
        }
      }
      if (validationConfig.length) {
        if (!this.validateLen(field, validationConfig.length)) {
          pass = false;
          this.addError(
            field,
            this.$t("form.field.too.long.message", {
              max: validationConfig.length,
            }),
          );
        }
      }
      if (validationConfig.regex && this.option[field]) {
        if (!this.validateRegex(field, validationConfig.regex)) {
          pass = false;
          this.addError(
            field,
            this.$t(`form.option.regex.validation.error`, [
              validationConfig.regex,
            ]),
          );
        }
      }
      return pass;
    },
    validateOptionName() {
      this.validateFieldName("name");
    },
    validateOptionLabel() {
      this.validateFieldName("label");
    },
    localValidations() {
      // validate all fields from Validations
      for (const field in Validations) {
        this.validateFieldName(field);
      }
    },
    async validateOption() {
      const res = await validateJobOption(
        getRundeckContext().projectName,
        this.jobWasScheduled,
        {
          ...this.option,
          newoption: this.newOption,
        },
      );
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
