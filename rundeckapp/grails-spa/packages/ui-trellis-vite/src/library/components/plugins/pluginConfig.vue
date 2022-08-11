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
    <span v-if="error && isTitleMode" class="text-warning">
      {{error}}
    </span>
    <pluginInfo
      :show-title="inputShowTitle"
      :show-icon="inputShowIcon"
      :show-description="inputShowDescription"
      :detail="detail"
      v-if="isTitleMode && !error"

      >
      <slot name="titlePrefix"></slot>
      <span slot="suffix"><slot name="titleSuffix"></slot></span>
      </pluginInfo>

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
          <i class="fas fa-exclamation-circle"></i> {{validationWarningText}}
        </span>
        <span v-for="prop in props" :key="prop.name" class="configprop">

            <plugin-prop-view :prop="prop" :value="config[prop.name]"  v-if="(prop.type === 'Boolean' || config[prop.name]) && isPropInScope(prop)"/>

        </span>
      </div>
      <div v-else-if="isShowConfigForm && inputLoaded" class="col-xs-12 col-sm-12 form-horizontal">

        <div v-for="(group,gindex) in groupedProperties" :key="group.name">
            <div v-if="!group.name">
              <div v-for="(prop,pindex) in group.props"
                   :key="'g_'+gindex+'/'+prop.name"
                   :class="'form-group '+(prop.required?'required':'')+(validation &&validation.errors[prop.name]?' has-error':'')"
                   :data-prop-name="prop.name"
              >
                <plugin-prop-edit v-model="inputValues[prop.name]"
                                :prop="prop"
                                :input-values="inputValues"
                                :context-autocomplete="inputContextAutocomplete"
                                @pluginPropsMounted="notifyHandleAutoComplete"
                                :validation="validation"
                                :rkey="'g_'+gindex+'_'+rkey"
                                :pindex="pindex"/>
              </div>
            </div>
            <details :open="!group.secondary" v-else class="more-info details-reset">
              <summary >
                <span class="row">
                  <span class="col-sm-2 control-label h5 header-reset">
                {{group.name!=='-' ? group.name :"More"}}
                  <i class="more-indicator-verbiage more-info-icon glyphicon glyphicon-chevron-right"></i>
                  <i class="less-indicator-verbiage more-info-icon glyphicon glyphicon-chevron-down"></i>
                  </span>
                </span>
              </summary>

              <div v-for="(prop,pindex) in group.props"
                   :key="'g_'+gindex+'/'+prop.name"
                   :class="'form-group '+(prop.required?'required':'')+(validation &&validation.errors[prop.name]?' has-error':'')"
                   :data-prop-name="prop.name"
              >
                <plugin-prop-edit v-model="inputValues[prop.name]"
                                :prop="prop"
                                :input-values="inputValues"
                                :validation="validation"
                                :rkey="'g_'+gindex+'_'+rkey"
                                :pindex="pindex"/>
              </div>
            </details>
        </div>
      </div>
    </div>
    <slot name="extra"></slot>
  </div>
</template>

<style lang="scss">
.configprop + .configprop:before {
  content: ' ';
}
</style>
<script lang="ts">
import Vue from 'vue'

import AceEditor from '../utils/AceEditor.vue'
import Expandable from '../utils/Expandable.vue'
import PluginInfo from './PluginInfo.vue'
import PluginValidation from '../../interfaces/PluginValidation'
import PluginPropView from './pluginPropView.vue'
import PluginPropEdit from './pluginPropEdit.vue'
import {cleanConfigInput,convertArrayInput} from '../../modules/InputUtils'

import {diff} from 'deep-object-diff'

import {getPluginProvidersForService,
  getServiceProviderDescription,
  validatePluginConfig} from '../../modules/pluginService'

interface PropGroup{
  name?:string
  secondary:boolean
  props:any[],
}

export default Vue.extend({
  name: 'PluginConfig',
  components: {
    Expandable,
    AceEditor,
    PluginInfo,
    PluginPropView,
    PluginPropEdit
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
    'validation',
    'validationWarningText',
    'scope',
    'defaultScope',
    'contextAutocomplete',
  ],
  data () {
    return {
      props: [] as any[],
      detail: {},
      error: null as any|null,
      propsComputedSelectorData: {},
      inputShowTitle: this.showTitle !== null ? this.showTitle : true,
      inputShowIcon: this.showIcon !== null ? this.showIcon : true,
      inputShowDescription: this.showDescription !== null ? this.showDescription : true,
      inputValues: {} as any,
      inputSaved: {} as any,
      inputSavedProps: (typeof this.savedProps !== 'undefined') ? this.savedProps : ['type'],
      rkey: 'r_' + Math.floor(Math.random() * Math.floor(1024)).toString(16) + '_',
      groupExpand:{} as {[name:string]:boolean},
      inputLoaded: false,
      inputContextAutocomplete: this.contextAutocomplete !== null ? this.contextAutocomplete : false
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
      this.inputLoaded=false

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
        } else if (prop.type === 'Boolean' && typeof this.inputValues[prop.name] === 'string') {
          // boolean should convert to boolean
          Vue.set(this.inputValues, prop.name, this.inputValues[prop.name]==='true')
        }
        if(prop.options &&( prop.options['groupName']||prop.options['grouping'])){
          const gname=prop.options['groupName']||'-'
          this.groupExpand[gname] = (!prop.options['grouping'] || this.groupExpand[gname]|| this.inputValues[prop.name]) ? true : false
        }
        this.computeSelectionAccessor(prop)
      })
      this.inputLoaded=true
    },
    exportInputs(){
      const values: { [index: string]: any } = {}
      //convert true boolean to 'true'
      this.props.forEach((prop: any) => {
        if (prop.type === 'Boolean') {
          if(this.inputValues[prop.name]===true||this.inputValues[prop.name]==='true'){
            values[prop.name]='true'
          }else if(prop.defaultValue==='true'){
            //explicit value set if the default would be true
            values[prop.name]='false'
          }
        }else{
          values[prop.name]=this.inputValues[prop.name]
        }
      })
      return values
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
    loadPluginData(data: any) {
      this.props = data.props
      if(data.dynamicProps) {
        this.props.forEach((prop: any) => {
          if (data.dynamicProps[prop.name]) {
            prop.allowed = data.dynamicProps[prop.name]
          }
        })
      }
      this.detail = data
      this.prepareInputs()

      let storageAccess = this.hasKeyStorageAccess();
      if(storageAccess){
        this.notifyHasKeyStorageAccess();
      }
    },
    async loadProvider(provider: any) {
      try{
        const data:any = await getServiceProviderDescription(this.serviceName, provider)
        if (data.props) {
          this.loadPluginData(data)
        }
      }catch(e){
        let message = 'Unknown Error'
        if (e instanceof Error) message = e.message
        this.error=message
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
    isPropInScope(testProp: any): boolean {
      // determine if property is visible in scope
      const testScope = testProp.scope || this.defaultScope
      const allowedScope = this.scope
      if (!allowedScope || !testScope || testScope==='Unspecified') {
        //no specific scope
        return true
      }else if(allowedScope==='Project' && (testScope.startsWith('Project'))){
        return true
      }else if(allowedScope==='Framework' && (testScope==='Framework' || testScope==='Project')){
        return true
      }else if(allowedScope==='Instance' && (testScope.startsWith('Instance'))){
        return true
      }

      return false
    },
    isGroupedProp(testProp: any): boolean {
      // determine if property is visible based on required prop value
      const grouping = testProp.options && testProp.options['grouping']
      const groupName = testProp.options && testProp.options['groupName']

      return grouping || groupName
    },
    hasKeyStorageAccess(): boolean {
      let storageAccess = false;
      this.props.forEach((prop: any, index)=>{
        if(prop.options!=null && prop.options['selectionAccessor']){
          storageAccess = true;
        }
      });
      return storageAccess;
    },
    notifyHasKeyStorageAccess(){
      this.$emit("hasKeyStorageAccess", this.provider)
    },
    notifyHandleAutoComplete(){
      this.$emit("handleAutocomplete")
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
    computedConfig: {
      handler(newValue, oldValue) {
        if (Object.keys(diff(newValue, oldValue)).length > 0)
          this.$emit('change')
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
    isShowConfigForm (): boolean {
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
        visibility[prop.name] = this.isPropVisible(prop) && this.isPropInScope(prop)
        if (!visibility[prop.name]) {
          Vue.delete(visibility, prop.name)
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
    hasGroups(): boolean{
      return this.props.find((prop)=>{
        return this.isGroupedProp(prop)
      }) ? true : false
    },
    groupedProperties(): PropGroup[]{
      const unnamed:PropGroup ={props:[],secondary:false}
      const groups:PropGroup[] =[unnamed]
      const named : {[name:string]: PropGroup} = {}

       this.props.forEach(prop => {
         const name=prop.options && prop.options['groupName']
         const secondary = prop.options && prop.options['grouping']
         const inScope = this.isPropInScope(prop)
         if(!inScope) {
           return
         }else if(!name && !secondary){
            unnamed.props.push(prop)
         }else{
           let gname=name||'-'

           if(!named[gname]){
             named[gname]={props:[prop],secondary:secondary,name:gname}
             groups.push(named[gname])
           }else{
            named[gname].props.push(prop)
            named[gname].secondary=named[gname].secondary||secondary
           }
         }
      })
      return groups
    },
    exportedValues(): any{
      return convertArrayInput(cleanConfigInput(this.exportInputs()))
    },
    computedConfig(): any {
      return Object.assign({}, this.value.config)
    }
  },
  beforeMount () {
    this.loadForMode()
  }
})
</script>
<style lang="scss" scoped>
.header-reset{
  margin:0;
}
</style>

