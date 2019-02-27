<!--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div>
    <pluginInfo
      :show-title="inputShowTitle"
      :show-icon="inputShowIcon"
      :show-description="inputShowDescription"
      :detail="detail"
      v-if="isTitleMode"

      />

    <div class="row" v-if="!isTitleMode">
      <div v-if="inputShowIcon||inputShowTitle || inputShowDescription" class="col-xs-12 col-sm-12">
        <p>
          <pluginInfo
          :show-title="inputShowTitle"
          :show-icon="inputShowIcon"
          :show-description="inputShowDescription"
          :detail="detail"

          />
        </p>
      </div>
      <div v-if="isShowMode && config" class="col-xs-12 col-sm-12">
        <span class="text-warning" v-if="validation && !validation.valid">
          <i class="fas fa-exclamation-circle"></i>
        </span>
        <span v-for="prop in props" :key="prop.name" class="configprop">
          <template v-if="config[prop.name]">
            <span class="configpair" v-if="prop.type==='Boolean'">
              <template v-if="config[prop.name]==='true' ||config[prop.name]===true">
                <span :title="prop.desc">{{prop.title}}:</span>
                <span :class="prop.options && prop.options['booleanTrueDisplayValueClass']||'text-success'">yes</span>
              </template>
            </span>
            <span class="configpair" v-else-if="prop.type==='Integer'">
              <span :title="prop.desc">{{prop.title}}:</span>
              <span style="font-family:Courier,monospace">{{config[prop.name]}}</span>
            </span>
            <span class="configpair" v-else-if="['Options', 'Select','FreeSelect'].indexOf(prop.type)>=0">
              <span :title="prop.desc">{{prop.title}}:</span>
              <template v-if="prop.type!=='Options'">
                <span class="text-success">{{prop.selectLabels && prop.selectLabels[config[prop.name]] || config[prop.name]}}</span>
              </template>
              <template v-else>
                <span v-if="typeof config[prop.name]==='string'">
                <span v-for="optval in config[prop.name].split(/, */)" :key="optval" class="text-success">
                  <i class="glyphicon glyphicon-ok-circle"></i>
                  {{prop.selectLabels && prop.selectLabels[optval] || optval}}
                </span>
                </span>
                <span v-else-if="config[prop.name].length>0">
                  <span v-for="optval in config[prop.name]" :key="optval" class="text-success">
                  <i class="glyphicon glyphicon-ok-circle"></i>
                  {{prop.selectLabels && prop.selectLabels[optval] || optval}}
                </span>
                </span>
              </template>
            </span>
            <span v-else class="configpair">
              <span :title="prop.desc">{{ prop.title }}:</span>
              <span class="text-success" v-if="prop.options && prop.options['displayType']==='PASSWORD'">&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;</span>
              <template v-else-if="prop.options && prop.options['displayType']==='CODE'">
                <expandable :options="{linkCss:'expanderLink text-muted'}">
                  <template slot="label">{{config[prop.name].split(/\r?\n/).length}} lines</template>
                  <pre class="scriptContent apply_ace"><code>{{config[prop.name]}}</code></pre>
                </expandable>
              </template>
              <template v-else-if="prop.options && prop.options['displayType']==='MULTI_LINE'">
                <expandable :options="{linkCss:'expanderLink text-muted'}">
                  <template slot="label">{{config[prop.name].split(/\r?\n/).length}} lines</template>
                  <pre class="scriptContent apply_ace"><code>{{config[prop.name]}}</code></pre>
                </expandable>
              </template>
              <span class="text-success" v-else>{{ config[prop.name] }}</span>
            </span>
          </template>
        </span>
      </div>
      <div v-else-if="isShowConfigForm" class="col-xs-12 col-sm-12 form-horizontal">

        <div v-for="(prop,pindex) in visibleProps" :key="prop.name" :class="'form-group '+(prop.required?'required':'')+(validation &&validation.errors[prop.name]?' has-error':'')">
          <template v-if="prop.type==='Boolean'">
            <div class="col-xs-10 col-xs-offset-2">
              <div class="checkbox">
                <input type="checkbox"
                      :name="`${rkey}prop_`+pindex"
                      :id="`${rkey}prop_`+pindex"
                      value="true"
                      v-model="inputValues[prop.name]"
                >
                <label :for="`${rkey}prop_`+pindex">
                  {{prop.title}}
                </label>
              </div>
            </div>
          </template>
          <template v-else>
            <label :class="'col-sm-2 control-label input-sm '+(prop.required ? 'required' : '')" :for="`${rkey}prop_`+pindex">{{prop.title}}</label>
            <div class="col-sm-10" v-if="prop.type==='Select'">
              <select :name="`${rkey}prop_`+pindex"
                      v-model="inputValues[prop.name]"
                      :id="`${rkey}prop_`+pindex"
                      class="form-control input-sm">
                <option v-if="!prop.required" value="">--None Selected--</option>
                <option v-for="opt in prop.allowed" v-bind:value="opt" v-bind:key="opt">
                  {{prop.selectLabels && prop.selectLabels[opt] || opt}}
                </option>
              </select>
            </div>
            <template v-else-if="prop.type==='FreeSelect'">
              <div class="col-sm-5">
                <input :name="`${rkey}prop_`+pindex"
                      v-model="inputValues[prop.name]"
                      :id="`${rkey}prop_`+pindex"
                      size="100"
                      type="text"
                      class="form-control input-sm">
              </div>
              <div class="col-sm-5">
                <select @change="setVal(inputValues,prop.name,$event.target.value)" class="form-control input-sm">
                  <option v-for="opt in prop.allowed" v-bind:value="opt" v-bind:key="opt">
                    {{prop.selectLabels && prop.selectLabels[opt] || opt}}
                  </option>
                </select>
              </div>

            </template>
            <template v-else-if="prop.type==='Options'">
              <div class="col-sm-10">

                <div class=" grid">
                  <div class="optionvaluemulti checkbox" v-for="(opt,oindex) in prop.allowed" v-bind:key="opt">
                    <input type="checkbox"
                          v-model="inputValues[prop.name]"
                          :value="opt"
                          :id="`${rkey}opt_`+pindex+'_'+oindex"
                    />
                    <label class="grid-row optionvaluemulti" :for="`${rkey}opt_`+pindex+'_'+oindex">
                      {{prop.selectLabels && prop.selectLabels[opt] || opt}}
                    </label>
                  </div>
                </div>
              </div>
            </template>
            <div :class="inputColSize(prop)" v-else>
              <input :name="`${rkey}prop_`+pindex"
                    v-model.number="inputValues[prop.name]"
                    :id="`${rkey}prop_`+pindex"
                    size="100"
                    type="number"
                    class="form-control input-sm"
                    v-if="['Integer','Long'].indexOf(prop.type)>=0">
              <template v-else-if="prop.options && prop.options['displayType']==='MULTI_LINE'">
                <textarea :name="`${rkey}prop_`+pindex"
                          v-model="inputValues[prop.name]"
                          :id="`${rkey}prop_`+pindex"
                          rows="10"
                          cols="100"
                          class="form-control input-sm"></textarea>
              </template>
              <template v-else-if="prop.options && prop.options['displayType']==='CODE'">

                <textarea :name="`${rkey}prop_`+pindex"
                          v-model="inputValues[prop.name]"
                          :id="`${rkey}prop_`+pindex"
                          rows="10"
                          cols="100"
                          class="form-control input-sm"></textarea>
                <!-- <ace-editor :name="`${rkey}prop_`+pindex"
                            v-model="inputValues[prop.name]"
                            :lang="prop.options['codeSyntaxMode']"
                            :codeSyntaxSelectable="prop.options['codeSyntaxSelectable']"
                            :id="`${rkey}prop_`+pindex"
                            height="200"
                            width="100%"
                            /> -->
              </template>
              <template v-else-if="prop.options && prop.options['displayType']==='PASSWORD'">
                <input :name="`${rkey}prop_`+pindex"
                      v-model="inputValues[prop.name]"
                      :id="`${rkey}prop_`+pindex"
                      size="100"
                      type="password"
                      autocomplete="new-password"
                      class="form-control input-sm">
              </template>
              <template v-else-if="prop.options && prop.options['displayType']==='STATIC_TEXT'">
                <span v-if="prop.options['staticTextContentType']==='text/html'">
                  {{prop.staticTextDefaultValue}}
                </span>
                <span v-if="prop.options['staticTextContentType']==='text/markdown'">
                  {{prop.staticTextDefaultValue}}
                </span>
                <span v-else>
                  {{prop.staticTextDefaultValue}}
                </span>
              </template>
              <input :name="`${rkey}prop_`+pindex"
                    v-model="inputValues[prop.name]"
                    :id="`${rkey}prop_`+pindex"
                    size="100"
                    type="text"
                    class="form-control input-sm"
                    v-else>
            </div>
            <div v-if="prop.options && prop.options['selectionAccessor']==='PLUGIN_TYPE'" class="col-sm-5">
              <select v-model="inputValues[prop.name]" class="form-control input-sm">
                <option disabled value="" >-- Select Plugin Type --</option>
                <option v-for="opt in propsComputedSelectorData[prop.name]" v-bind:value="opt.value"
                        v-bind:key="opt.key">
                  {{opt.key}}
                </option>
              </select>
            </div>
            <!--<div v-if="prop.options && prop.options['selectionAccessor']==='RUNDECK_JOB'" class="col-sm-5">-->
              <!--<job-config-picker v-model="inputValues[prop.name]"></job-config-picker>-->
            <!--</div>-->
            <slot v-else-if="prop.options && prop.options['selectionAccessor'] "
                  name="accessors"
                  :prop="prop"
                  :inputValues="inputValues"
                  :accessor="prop.options['selectionAccessor']"

            >
            </slot>
          </template>

          <div class="col-sm-10 col-sm-offset-2" v-if="prop.desc">
            <div class="help-block">
              {{prop.desc}}
            </div>
          </div>
          <div class="col-sm-10 col-sm-offset-2 text-warning" v-if="validation && !validation.valid && validation.errors[prop.name]">
            {{validation.errors[prop.name]}}
          </div>

        </div>
      </div>
    </div>
    <slot name="extra"></slot>
  </div>
</template>

<style lang="scss">
.expanderLink {
  text-decoration: underline;
  cursor: pointer;
}

.configprop + .configprop:before {
  content: ' ';
}
</style>
<script lang="ts">
import Vue from 'vue'

// import AceEditor from './AceEditor.vue'
import Expandable from '@rundeck/ui-trellis/src/Blah.vue'
// import JobConfigPicker from './JobConfigPicker.vue'
import PluginInfo from './PluginInfo.vue'
import PluginValidation from './PluginValidation'
import {cleanConfigInput,convertArrayInput} from './InputUtils'

import {getPluginProvidersForService,
  getServiceProviderDescription,
  getEventSourceEventTypes,
  validatePluginConfig} from '@/services/pluginService'

export default Vue.extend({
  name: 'PluginConfig',
  components: {
    Expandable,
    // AceEditor,
    // JobConfigPicker,
    PluginInfo
  },
  props: [
    'serviceName',
    'provider',
    'config',
    'mode',
    'showTitle',
    'showIcon',
    'showDescription',
    'value',
    'savedProps',
    'pluginConfig',
    'validation'
  ],
  data () {
    return {
      props: [] as any[],
      detail: {},
      propsComputedSelectorData: {},
      inputShowTitle: this.showTitle !== null ? this.showTitle : true,
      inputShowIcon: this.showIcon !== null ? this.showIcon : true,
      inputShowDescription: this.showDescription !== null ? this.showDescription : true,
      inputValues: {} as any,
      inputSaved: {} as any,
      inputSavedProps: (typeof this.savedProps !== 'undefined') ? this.savedProps : ['type'],
      rkey: 'r_' + Math.floor(Math.random() * Math.floor(1024)).toString(16) + '_'
    }
  },
  methods: {
    setVal(target: any, prop: any, val: any) {
      Vue.set(target, prop, val)
    },
    prepareInputs () {
      if (!this.isShowConfigForm) {
        return
      }

      if (typeof this.inputSavedProps !== 'undefined' && this.inputSavedProps.length > 0) {
        for (const i of this.inputSavedProps) {
          if (typeof this.inputSaved[i] === 'undefined') {
            this.inputSaved[i] = this.value[i]
          }
        }
      }

      const config = this.value.config

      const modeCreate = this.isCreateMode

      // set up defaults and convert Options to array
      this.props.forEach((prop: any) => {
        if (config[prop.name]) {
          Vue.set(this.inputValues, prop.name, config[prop.name])
        }
        if (modeCreate && !this.inputValues[prop.name] && prop.defaultValue) {
          Vue.set(this.inputValues, prop.name, prop.defaultValue)
        }
        if (prop.type === 'Options' && typeof this.inputValues[prop.name] === 'string') {
          // convert to array
          Vue.set(this.inputValues, prop.name, this.inputValues[prop.name].split(/, */))
        } else if (prop.type === 'Options' && typeof this.inputValues[prop.name] === 'undefined') {
          // convert to array
          Vue.set(this.inputValues, prop.name, [])
        } else if (prop.type === 'Select' && typeof this.inputValues[prop.name] === 'undefined') {
          // select box should use blank string to preselect disabled option
          Vue.set(this.inputValues, prop.name, '')
        }
        this.computeSelectionAccessor(prop)
      })
    },

    computeSelectionAccessor(prop: any) {
      if (prop.options && prop.options['selectionAccessor'] === 'PLUGIN_TYPE') {
        const serviceName = prop.options['selectionAdditional']
        getPluginProvidersForService(serviceName).then((data: any) => {
          Vue.set(this.propsComputedSelectorData, prop.name, data.descriptions.map((provider: any) => {
            return {key: provider.title, value: provider.name, description: provider.description}
          }))
        })
      }
    },
    inputColSize(prop: any) {
      if (prop.options && prop.options['selectionAccessor']) {
        return 'col-sm-5'
      }
      return 'col-sm-10'
    },
    loadPluginData(data: any) {
      this.props = data.props
      this.detail = data
      this.prepareInputs()
    },
    async loadProvider(provider: any) {
      const data:any = await getServiceProviderDescription(this.serviceName, provider)
      if (data.props) {
        this.loadPluginData(data)
      }
    },
    isPropVisible(testProp: any): boolean {
      // determine if property is visible based on required prop value
      const requiredProp = testProp.options && testProp.options['requiredProperty']
      const requiredVal = testProp.options && testProp.options['requiredValue']
      if (requiredProp) {
        if (!this.inputValues || !this.inputValues[requiredProp]) {
          return false
        }
        if (requiredVal && (!this.inputValues || this.inputValues[requiredProp] !== requiredVal)) {
          return false
        }
      }
      return true
    },
    loadForMode(){
      if (this.serviceName && this.provider) {
        this.loadProvider(this.provider)
      } else if (this.isShowConfigForm && this.serviceName && this.value && this.value.type) {
        this.loadProvider(this.value.type)
      } else if (this.pluginConfig) {
        this.loadPluginData(this.pluginConfig)
      }
    },
  },
  watch: {
    inputValues: {
      handler (newValue, oldValue) {
        if (this.isShowConfigForm) {
          this.$emit('input', Object.assign({}, this.inputSaved, {config: this.exportedValues}))
        }
      },
      deep: true
    },
    mode:{
      handler(newValue,oldValue){
        this.loadForMode()
      }
    }
  },
  computed: {
    isEditMode(): boolean {
      return this.mode === 'edit'
    },
    isCreateMode(): boolean {
      return this.mode === 'create'
    },
    isShowConfigForm () {
      return this.isEditMode || this.isCreateMode
    },
    isShowMode(): boolean {
      return this.mode === 'show'
    },
    isTitleMode(): boolean {
      return this.mode === 'title'
    },
    visibility (): { [name: string]: boolean } {
      const visibility: { [name: string]: boolean } = {}
      this.props.forEach((prop: any) => {
        visibility[prop.name] = this.isPropVisible(prop)
        if (!visibility[prop.name]) {
          Vue.delete(this.inputValues, prop.name)
        }
      })
      return visibility
    },
    visibleProps(): any[] {
      const visibility=this.visibility
      return this.props.filter((prop)=>{
        return visibility[prop.name]
      })
    },
    exportedValues(): any{
      return convertArrayInput(cleanConfigInput(this.inputValues))
    }
  },

  mounted () {
    this.loadForMode()
  }
})
</script>
