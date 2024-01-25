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
          <option value="file" v-if="fileUploadPluginEnabled">
            {{ $t("form.option.optionType.file.label") }}
          </option>
        </select>
      </div>
    </div>

    <!-- file input -->
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

    <!-- name -->
    <div class="form-group">
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
          name="name"
          class="form-control restrictOptName"
          size="40"
          :placeholder="$t('form.option.name.label')"
          id="optname_"
        />
      </div>
    </div>

    <!-- label -->
    <div class="form-group">
      <label
        for="opt_label"
        class="col-sm-2 control-label"
        :class="{ 'has-error': hasError('label') }"
      >
        {{ $t("form.option.label.label") }}
      </label>

      <div class="col-sm-10">
        <input
          type="text"
          class="form-control"
          name="label"
          id="opt_label"
          v-model="option.label"
          size="40"
          :placeholder="$t('form.option.label.label')"
        />
      </div>
    </div>

    <!-- description -->
    <div class="form-group" :class="{ 'has-error': hasError('description') }">
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

    <!-- TODO option MAIN section -->
    <template v-if="option.optionType !== 'file'">
      <div
        class="form-group"
        :class="{ 'has-error': hasError('defaultValue') }"
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
            v-model="option.defaultValue"
          />
        </div>
      </div>

      <div
        class="opt_sec_enabled form-group"
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
        </div>
      </div>

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
                class=" markdown-body"
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
            <label class="col-sm-2 control-label">{{ $t('form.option.values.label') }}</label>
            <div  :class="{'col-sm-10':uiFeatures['next'],'col-sm-3':!uiFeatures['next']}">

              <select v-model="valuesType" class="form-control" v-if="uiFeatures['next']">
                <option value="list">
                  {{ $t('form.label.valuesType.list.label') }}
                </option>
                <option value="url">
                  {{ $t('form.option.valuesType.url.label') }}
                </option>
                <template v-if="features['optionValuesPlugin']">
                  <option v-for="optionValPlugin in optionValuesPlugins"  :value="optionValPlugin.name">
                    {{ optionValPlugin.title||optionValPlugin.name }}
                  </option>
                </template>
              </select>

              <div v-if="!uiFeatures['next']">
                <div class="radio">
                  <input type="radio"
                         name="valuesType"
                           value="list"
                         v-model="valuesType"
                           id="vtrlist_"/>
                  <label for="vtrlist_"
                         :class="{ 'has-error': hasError('valuesList') }"
                  >
                    {{ $t('form.label.valuesType.list.label') }}
                  </label>
                </div>

                <div class="radio">
                  <input type="radio"
                         name="valuesType"
                         value="url"
                         v-model="valuesType"
                           id="vtrurl_"/>
                  <label for="vtrurl_"
                         class="left"
                         :class="{ 'has-error': hasError('valuesUrl') }"
                  >
                    {{ $t('form.option.valuesType.url.label') }}
                  </label>
                </div>
                <template v-if="features['optionValuesPlugin']">
                  <template v-for="optionValPlugin in optionValuesPlugins" >
                    <div class="radio">
                      <input type="radio"
                             name="valuesType"
                             v-model="valuesType"
                             :value="optionValPlugin.name"
                               :id="'optvalplugin_'+optionValPlugin.name"/>
                      <label :for="'optvalplugin_'+optionValPlugin.name"
                             :class="{ 'has-error': hasError('valuesFromPlugin') }"
                             :title="optionValPlugin.description"
                      >
                        <img :src="optionValPlugin.iconUrl" v-if="optionValPlugin.iconUrl" style="width: 16px; height: 16px; margin-right: 5px;"/>
                        {{ optionValPlugin.title||optionValPlugin.name }}
                      </label>
                    </div>

                  </template>
                </template>
              </div>

            </div>
            <div  :class="{'col-sm-10 col-sm-offset-2':uiFeatures['next'],'col-sm-7':!uiFeatures['next']}">

              <div id="vlist_section" v-if="valuesType==='list'"  :class="{ 'has-error': hasError('valuesList') }">

                <input type="text" name="valuesList"
                             class="form-control"
                             v-model="option.valuesList"
                             size="60"
                             :placeholder="$t('form.option.valuesList.placeholder')"
                />

              </div>

              <div id="vurl_section"
                   v-else-if="valuesType==='url'"
                   :class="{ 'has-error': hasError('valuesUrl') }">
                <input type="url"
                             class=" form-control"
                             name="valuesUrl"
                             v-model="option.valuesUrl"
                             size="60"
                             :placeholder="$t('form.option.valuesURL.placeholder')"
                />
                <div class="help-block">
                  {{ $t('form.option.valuesUrl.description') }}
                  <a href="https://docs.rundeck.com/docs/manual/job-options.html#option-model-provider"
                     target="_blank">
                    <i class="glyphicon glyphicon-question-sign"></i>
                    {{ $t('rundeck.user.guide.option.model.provider') }}
                  </a>
                </div>

                <div class="row" :class="{ 'has-error': hasError('configRemoteUrl') }">
                  <div class="col-md-12">
                    <label class="control-label">{{ $t('form.option.valuesType.url.filter.label') }}</label>
                  </div>

                  <div class="col-md-4">
                    <div class="">
                      <input type="text"
                                   class=" form-control"
                                   name="remoteUrlJsonFilter"
                                   v-model="option.configRemoteUrl.jsonFilter"
                                   size="30"
                      />
                    </div>
                  </div>
                  <div class="col-md-12">
                    <div class="">
                      <div class="help-block">
                        {{ $t('form.option.valuesType.url.filter.description') }}
                      </div>
                    </div>
                  </div>
                </div>

                <div class="row">
                  <div class="col-md-12">
                    <label class="control-label">{{ $t('form.option.valuesType.url.authType.label') }}</label>
                  </div>

                  <div class="col-md-4">
                    <select
                      class="form-control"
                      v-model="option.remoteUrlAuthenticationType"
                      >
                      <option value="" disabled>{{$t('form.option.valuesType.url.authType.empty.label')}}</option>
                      <option v-for="option in remoteUrlAuthenticationList" :value="option.value" :key="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                  </div>

                  <div class="col-md-8">

                    <!--USER/PASSSWORD AUTH-->
                    <div id="remoteUrlUserAuth" v-if="option.remoteUrlAuthenticationType==='BASIC'">
                      <div>
                        <div class="col-md-3">
                          <label class="control-label">{{ $t('form.option.valuesType.url.authentication.username.label') }}</label>
                        </div>
                        <div class="col-md-8 input-group">
                          <input type="text"
                                       class=" form-control"
                                       name="remoteUrlUsername"
                                       v-model="option.configRemoteUrl.username"
                                       size="30"
                          />
                        </div>

                      </div>
                      <div>
                        <div class="col-md-3">
                          <label class="control-label">{{ $t('form.option.valuesType.url.authentication.password.label') }}</label>
                        </div>
                        <div class="col-md-8 input-group">
                          <span class="input-group-addon has_tooltip" :title="$t('form.option.defaultStoragePath.description')">
                              <i class="glyphicon glyphicon-lock"></i>
                          </span>

                          <input type="text"
                                 class="form-control"
                                 v-model="option.configRemoteUrl.passwordStoragePath"
                                 size="20"
                          />

                          <span class="input-group-btn">
                            <key-storage-selector v-model="option.configRemoteUrl.passwordStoragePath"
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
                    <div id="remoteUrlTokenAuth" v-if="option.remoteUrlAuthenticationType==='API_KEY'">

                      <div>
                        <div class="col-md-3">
                          <label class="control-label">{{ $t('form.option.valuesType.url.authentication.key.label') }}</label>
                        </div>
                        <div class="col-md-8 input-group">
                          <input type="text"
                                       class=" form-control"
                                       name="remoteUrlKey"
                                       v-model="option.configRemoteUrl.keyName"
                                       size="30"
                          />
                        </div>

                      </div>
                      <div>
                        <div class="col-md-3">
                          <label class="control-label">{{ $t('form.option.valuesType.url.authentication.token.label') }}</label>
                        </div>
                        <div class="col-md-8 input-group">
                          <span class="input-group-addon has_tooltip" :title="$t('form.option.defaultStoragePath.description')">
                          <i class="glyphicon glyphicon-lock"></i>
                          </span>

                          <input type="text"
                                 class="form-control"
                                 name="remoteUrlToken"
                                 v-model="option.configRemoteUrl.tokenStoragePath"
                                 size="20"
                                 placeholder=""
                          />

                          <span class="input-group-btn">
                              <key-storage-selector v-model="option.configRemoteUrl.tokenStoragePath"
                                                    :storage-filter="'Rundeck-data-type=password'"
                                                    :allow-upload="true"
                                                    :read-only="false"
                              />
                          </span>

                        </div>
                      </div>
                      <div>
                        <div class="col-md-3">
                          <label class="control-label">{{ $t('form.option.valuesType.url.authentication.tokenInformer.label') }}</label>
                        </div>
                        <div class="col-md-8 input-group">
                          <select
                            name="remoteUrlApiTokenReporter"
                            class="form-control"
                            v-model="option.configRemoteUrl.apiTokenReporter"
                            >
                            <option value="HEADER">
                              {{$t('form.option.valuesType.url.authentication.tokenInformer.header.label')}}
                            </option>
                            <option value="QUERY_PARAM">
                              {{$t('form.option.valuesType.url.authentication.tokenInformer.query.label')}}
                            </option>
                          </select>

                        </div>
                      </div>
                    </div>
                    <!--TOKEN AUTH-->

                    <!--bearerToken AUTH-->
                    <div id="remoteUrlBearerTokenAuth" v-if="option.remoteUrlAuthenticationType==='BEARER_TOKEN'">
                      <div class="col-md-3">
                        <label class="control-label">{{ $t('form.option.valuesType.url.authentication.token.label') }}</label>
                      </div>
                      <div class="col-md-8 input-group">
                        <span class="input-group-addon has_tooltip" :title="$t('form.option.defaultStoragePath.description')">
                        <i class="glyphicon glyphicon-lock"></i>
                        </span>

                        <input type="text"
                               class="form-control"
                               name="remoteUrlBearerToken"
                               v-model="option.configRemoteUrl.tokenStoragePath"
                               size="20"
                               placeholder=""
                        />

                        <span class="input-group-btn">
                            <key-storage-selector v-model="option.configRemoteUrl.tokenStoragePath"
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
              <div v-else-if="valuesType && optionValuesPlugins && uiFeatures['next']">
                <plugin-info
                  :detail="getProviderFor(valuesType)"
                  :show-description="true"
                  :show-extended="true"
                  description-css="help-block"
                >
                </plugin-info>
              </div>
            </div>
          </div>
          <div class="form-group">

            <label class="col-sm-2 control-label">{{ $t('form.option.sort.label') }}</label>

            <div class="col-sm-3">
              <div class="radio radio-inline">
                <input type="radio" name="sortValues" :value="false" v-model="option.sortValues"/>
                <label for="option-sort-values-no">
                  {{ $t('no') }}
                </label>
              </div>
              <div class="radio radio-inline">
                <input type="radio"  name="sortValues" :value="true" v-model="option.sortValues"/>
                <label for="option-sort-values-yes">
                  {{ $t('yes') }}
                </label>
              </div>
              <div class="help-block">
                {{ $t('form.option.sort.description') }}
              </div>
            </div>

            <div class="input-group col-sm-3"
                 :class="{ 'has-error': hasError('delimiter') }"
            >
              <div class="input-group-addon" style="background-color:#e0e0e0;">
                {{ $t('form.option.valuesDelimiter.label') }}
              </div>
              <input type="text"
                     name="valuesListDelimiter"
                     v-model="option.valuesListDelimiter"
                     size="5"
                     class="form-control"
              />

            </div>
            <span class="help-block">
                {{ $t('form.option.valuesDelimiter.description') }}
            </span>
          </div>
          <div class="form-group opt_keystorage_disabled" v-if="!isSecureInput">
            <label class="col-sm-2 control-label">{{ $t('form.option.enforcedType.label') }}</label>
            <div class="col-sm-10">
              <div class="radio">
                <input type="radio" v-model="enforcedType" value="none" id="enforcedType_none"/>
                <label for="enforcedType_none">
                  {{ $t('none') }}
                  <span class="text-strong">{{ $t('form.option.enforcedType.none.label') }}</span>
                </label>
              </div>
              <div class="radio">
                <input type="radio" v-model="enforcedType" value="enforced"
                       id="enforcedType_enforced"/>
                <label for="enforcedType_enforced"
                       :class="{ 'has-error': hasError('enforced') }"
                >
                  {{ $t('form.option.enforced.label') }}
                </label>
              </div>
              <div class="radio">
                <input type="radio" v-model="enforcedType" value="regex" id="etregex_"/>
                <label for="etregex_"
                       :class="{ 'has-error': hasError('regex') }"
                >
                  {{ $t('form.option.regex.label') }}
                </label>
              </div>
            </div>
            <div class="col-sm-10 col-sm-offset-2" v-if="enforcedType==='regex'">

              <input type="text"
                name="regex"
                class="form-control"
                v-model="option.regex"
                size="40"
                :placeholder="$t('form.option.regex.placeholder')"
                id="vregex_"/>
              <template v-if="errors['regex']">
                <pre class="text-danger">{{errors['regex']}}</pre>
              </template>

            </div>
          </div>
          <!-- /ko -->

      <!-- end MAIN section -->
    </template>
    <!-- required -->
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

    <!-- hidden -->
    <div class="form-group">
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
      </div>
    </div>

    <!-- multivalue -->
    <div class="form-group" v-if="option.optionType !== 'file'">
      <label
        class="col-sm-2 control-label"
        :class="{ 'has-error': hasError('multivalued') }"
      >
        {{ $t("form.option.multivalued.label") }}
      </label>
      <div class="col-sm-10">
        <div class="opt_sec_disabled" v-if="!option.secureInput">
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
            <label
              for="cdelimiter_"
              class="
                ${hasErrors(bean:
                option,
                field:
                'multivalued',
                'fieldError')}
              "
            >
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
        <div class="presentation" id="mvsecnote" v-if="option.secureInput">
          <span class="warn note">
            {{ $t("form.option.multivalued.secure-conflict.message") }}
          </span>
        </div>
      </div>
    </div>

    <!-- preview plain -->
    <section
      id="preview_"
      class="section-separator-solo"
      v-if="option.name && option.optionType !== 'file'"
    >
      <div class="row">
        <label class="col-sm-2 control-label">{{ $t("usage") }}</label>
        <div
          class="col-sm-10 opt_sec_nexp_disabled"
          v-if="!option.secureInput || option.secureExposed"
        >
          <span class="text-strong">{{
            $t("the.option.values.will.be.available.to.scripts.in.these.forms")
          }}</span>
          <div>
            {{ $t("bash.prompt") }} <code>${{ bashVarPreview }}</code>
          </div>
          <div>
            {{ $t("commandline.arguments.prompt") }}
            <code>${option.{{ option.name }}}</code>
          </div>
          <div>
            {{ $t("commandline.arguments.prompt.unquoted") }}
            <code>${unquotedoption.{{ option.name }}}</code>
            {{ $t("commandline.arguments.prompt.unquoted.warning") }}
          </div>
          <div>
            {{ $t("script.content.prompt") }}
            <code>@option.{{ option.name }}@</code>
          </div>
        </div>
        <div class="col-sm-10 opt_sec_nexp_enabled" v-else>
          <span class="warn note">{{
            $t("form.option.usage.secureAuth.message")
          }}</span>
        </div>
      </div>
    </section>
    <section
      id="file_preview_"
      v-if="option.name && option.optionType === 'file'"
      class="section-separator-solo"
    >
      <div class="row">
        <label class="col-sm-2 control-label">{{ $t("usage") }}</label>
        <div class="col-sm-10">
          <span class="text-info">{{
            $t("form.option.usage.file.preview.description")
          }}</span>
          <div>
            {{ $t("bash.prompt") }} <code>${{ fileBashVarPreview }}</code>
          </div>
          <div>
            {{ $t("commandline.arguments.prompt") }}
            <code>${file.{{ option.name }}}</code>
          </div>
          <div>
            {{ $t("script.content.prompt") }}
            <code>@file.{{ option.name }}@</code>
          </div>

          <span class="text-info">{{
            $t("form.option.usage.file.fileName.preview.description")
          }}</span>
          <div>
            {{ $t("bash.prompt") }}
            <code>${{ fileFileNameBashVarPreview }}</code>
          </div>
          <div>
            {{ $t("commandline.arguments.prompt") }}
            <code>${file.{{ option.name }}.fileName}</code>
          </div>
          <div>
            {{ $t("script.content.prompt") }}
            <code>@file.{{ option.name }}.fileName@</code>
          </div>
          <span class="text-info">{{
            $t("form.option.usage.file.sha.preview.description")
          }}</span>
          <div>
            {{ $t("bash.prompt") }} <code>${{ fileShaBashVarPreview }}</code>
          </div>
          <div>
            {{ $t("commandline.arguments.prompt") }}
            <code>${file.{{ option.name }}.sha}</code>
          </div>
          <div>
            {{ $t("script.content.prompt") }}
            <code>@file.{{ option.name }}.sha@</code>
          </div>
        </div>
      </div>
    </section>
    <div class="floatr" style="margin: 10px 0">
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
    </div>
  </div>
</template>
<script lang="ts">
import {plugins} from '@/app/pages/repository/stores/pluginConfig.module'
import {cloneDeep} from "lodash"
import KeyStorageSelector from "../../../..//library/components/plugins/KeyStorageSelector.vue";
import PluginConfig from "../../../../library/components/plugins/pluginConfig.vue";
import PluginInfo from "../../../../library/components/plugins/PluginInfo.vue";

import AceEditor from "../../../../library/components/utils/AceEditor.vue";
import { defineComponent } from "vue";

import { VMarkdownView } from "vue3-markdown";

export default defineComponent({
  name: "OptionEdit",
  components: { KeyStorageSelector, PluginConfig, AceEditor, VMarkdownView, PluginInfo },
  emits: ["update:modelValue", "cancel", "save"],
  props: {
    error: String,
    newOption: { type: Boolean, default: false },
    modelValue: { type: Object, default: () => ({}) },
    features: { type: Object, default: () => ({}) },
    fileUploadPluginType: { type: String, default: "" },
    errors: { type: Object, default: () => ({}) },
    optionValuesPlugins: { type: Array, default: () => [] },
    uiFeatures: { type: Object, default: () => ({}) },
  },
  data() {
    return {
      option: Object.assign(
        {
          configRemoteUrl:{},
          defaultValue:'',
          optionType:'text',
          sortValues: false,
          inputType:'plain',
          hidden:false,
          multiValued:false
        },
        cloneDeep(this.modelValue)
      ),
      regexChoice:false,
      urlChoice:false,
      bashVarPrefix: "RD_",
      remoteUrlAuthenticationList:[
        {value:'BASIC', label:this.$t('form.option.valuesType.url.authType.basic.label')},
        {value:'API_KEY', label:this.$t('form.option.valuesType.url.authType.apiKey.label')},
        {value:'BEARER_TOKEN', label:this.$t('form.option.valuesType.url.authType.bearerToken.label')},
      ]
    };
  },
  watch: {
    "option.inputType"(val: string) {
      // this.option.defaultValue = "";
      // this.option.valuesList = "";
      // this.option.valuesUrl = "";
      // this.option.remoteUrlAuthenticationType = "";
      // this.option.enforceType = "none";
      this.option.isDate = val === "date";
      this.option.secureInput = val === "secure" || val === "secureExposed";
      this.option.secureExposed = val === "secureExposed";
    },
  },
  computed: {
    plugins() {
      return plugins
    },
    fileUploadPluginEnabled() {
      return this.features["fileUploadPlugin"];
    },
    bashVarPreview() {
      return this.option.name?this.tobashvar(this.option.name):'';
    },
    fileBashVarPreview() {
      return this.option.name?this.tofilebashvar(this.option.name):'';
    },

    fileFileNameBashVarPreview() {
      return this.option.name?this.tofilebashvar(this.option.name + ".fileName"):'';
    },
    fileShaBashVarPreview() {
      return this.option.name?this.tofilebashvar(this.option.name + ".sha"):'';
    },
    isDate() {
      return this.option.isDate;
    },
    isSecureInput() {
      return this.option.secureInput;
    },
    showDefaultValue() {
      return !this.isSecureInput ;
    },
    shouldShowDefaultStorage() {
      return !this.showDefaultValue;
    },
    enforcedType:{
      get(){
        if(this.option.enforced){
          return "enforced";
        }
        if(this.option.regex || this.regexChoice){
          return "regex";
        }
        return "none";
      },
      set(val: string){
        if(val === "enforced"){
          this.option.enforced = true;
          this.option.regex = null;
          this.regexChoice = false;
        }else if(val === "regex"){
          this.option.enforced = false;
          this.regexChoice = true;
          this.option.regex = "";
        }else{
          this.regexChoice = false;
          this.option.enforced = false;
          this.option.regex = null;
        }
      }
    },
    valuesType:{
      get(){
        if(this.option.optionValuesPluginType){
          return this.option.optionValuesPluginType;
        }
        if(this.option.realValuesUrl || this.urlChoice){
          return "url";
        }
        return "list";
      },
      set(val: string){
        debugger
        if(val === "url"){
          this.option.optionValuesPluginType = "";
          this.option.realValuesUrl = "";
          this.option.remoteUrlAuthenticationType='';
          this.option.configRemoteUrl = {};
          this.urlChoice=true
        }else if(val === "list"){
          this.option.optionValuesPluginType = "";
          this.option.realValuesUrl = null;
          this.option.remoteUrlAuthenticationType='';
          this.option.configRemoteUrl = {};
          this.urlChoice=false
        }else{
          this.option.optionValuesPluginType = val;
          this.option.realValuesUrl = null;
          this.option.remoteUrlAuthenticationType='';
          this.option.configRemoteUrl = {};
          this.urlChoice=false
        }
      }
    }
  },
  methods: {
    doSave(){
      this.$emit('update:modelValue', this.option)
    },
    hasError(field: string) {
      return this.errors[field] && this.errors[field].length > 0;
    },
    getProviderFor(name){
      return this.optionValuesPlugins.find(p => p.name === name)
    },
    tofilebashvar(str: string) {
      return (
        this.bashVarPrefix +
        "FILE_" +
        str
          .toUpperCase()
          .replace(/[^a-zA-Z0-9_]/g, "_")
          .replace(/[{}$]/, "")
      );
    },
    tobashvar(str: string) {
      return (
        this.bashVarPrefix +
        "OPTION_" +
        str
          .toUpperCase()
          .replace(/[^a-zA-Z0-9_]/g, "_")
          .replace(/[{}$]/, "")
      );
    },
  },
});
</script>

<style scoped lang="scss"></style>