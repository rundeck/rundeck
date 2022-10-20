<template>
  <div>
    <template v-if="prop.type==='Boolean'">
      <div class="col-xs-10 col-xs-offset-2" v-if="prop.defaultValue!=='true'">
        <div class="checkbox" >
          <input
            type="checkbox"
            :name="`${rkey}prop_`+pindex"
            :id="`${rkey}prop_`+pindex"
            value="true"
            v-model="currentValue"
          >
          <label :for="`${rkey}prop_`+pindex">{{prop.title}}</label>
        </div>

      </div>
      <label
        :class="'col-sm-2 control-label input-sm '+(prop.required ? 'required' : '')"
        :for="`${rkey}prop_`+pindex"
        v-if="prop.defaultValue==='true'"
      >{{prop.title}}</label>
      <div class="col-xs-10" v-if="prop.defaultValue==='true'">
          <label :for="`${rkey}prop_true_`+pindex" class="radio-inline">
          <input
            type="radio"
            :name="`${rkey}prop_`+pindex"
            :id="`${rkey}prop_true_`+pindex"
            value="true"
            v-model="currentValue"
          >
            <plugin-prop-val :prop="prop" :value="'true'"/>
          </label>
          <label :for="`${rkey}prop_false_`+pindex" class="radio-inline">
          <input
            type="radio"
            :name="`${rkey}prop_`+pindex"
            :id="`${rkey}prop_false_`+pindex"
            value="false"
            v-model="currentValue"
          >
            <plugin-prop-val :prop="prop" :value="'false'"/>
          </label>
      </div>
    </template>
    <template v-else-if="prop.options && prop.options['displayType']==='DYNAMIC_FORM'">

      <input  type="hidden" :id="rkey" :name="prop.name">

      <dynamic-form-plugin-prop :id="rkey" :fields="value" v-model="currentValue"
           :hasOptions="hasAllowedValues()"
           :options="parseAllowedValues()"
           :element="rkey" :name="prop.name"></dynamic-form-plugin-prop>

    </template>
    <template v-else>
      <label
        :class="'col-sm-2 control-label input-sm '+(prop.required ? 'required' : '')"
        :for="`${rkey}prop_`+pindex"
      >{{prop.title}}</label>
      <div class="col-sm-10" v-if="prop.type==='Select'">
        <select
          :name="`${rkey}prop_`+pindex"
          v-model="currentValue"
          :id="`${rkey}prop_`+pindex"
          class="form-control input-sm"
        >
          <option v-if="!prop.required" value>--None Selected--</option>
          <option
            v-for="opt in prop.allowed"
            v-bind:value="opt"
            v-bind:key="opt"
          ><plugin-prop-val :prop="prop" :value="opt"/></option>
        </select>
      </div>
      <template v-else-if="prop.type==='FreeSelect'">
        <div class="col-sm-5">
          <input
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :id="`${rkey}prop_`+pindex"
            size="100"
            type="text"
            class="form-control input-sm"
          >
        </div>
        <div class="col-sm-5">
          <select class="form-control input-sm" v-model="currentValue">
            <option
                v-for="opt in prop.allowed"
                v-bind:value="opt"
                v-bind:key="opt"
            ><plugin-prop-val :prop="prop" :value="opt"/></option>
          </select>
        </div>
      </template>
      <template v-else-if="prop.type==='Options'">
        <div class="col-sm-10">
          <div :class="{longlist:prop.allowed && prop.allowed.length>20}">
            <div
              class="checkbox"
              v-for="(opt,oindex) in prop.allowed"
              v-bind:key="opt"
            >
              <input
                type="checkbox"
                v-model="currentValue"
                :value="opt"
                :id="`${rkey}opt_`+pindex+'_'+oindex"
              >
              <label
                :for="`${rkey}opt_`+pindex+'_'+oindex"
              ><plugin-prop-val :prop="prop" :value="opt"/></label>
            </div>
          </div>
        </div>
      </template>
      <div :class="inputColSize(prop)" v-else>
        <input
          :name="`${rkey}prop_`+pindex"
          v-model.number="currentValue"
          :id="`${rkey}prop_`+pindex"
          size="100"
          type="number"
          class="form-control input-sm"
          v-bind:class="contextAutocomplete ? 'context_var_autocomplete' : ''"
          v-if="['Integer','Long'].indexOf(prop.type)>=0"
        >
        <template v-else-if="prop.options && prop.options['displayType']==='MULTI_LINE'">
          <textarea
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :id="`${rkey}prop_`+pindex"
            rows="10"
            cols="100"
            class="form-control input-sm"
            v-bind:class="contextAutocomplete ? 'context_var_autocomplete' : ''"
          ></textarea>
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='CODE'">
          <ace-editor
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :lang="prop.options['codeSyntaxMode']"
            :codeSyntaxSelectable="prop.options['codeSyntaxSelectable']==='true'"
            :id="`${rkey}prop_`+pindex"
            height="200"
            width="100%"
          />
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='PASSWORD'">
          <input
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :id="`${rkey}prop_`+pindex"
            size="100"
            type="password"
            autocomplete="new-password"
            class="form-control input-sm"
          >
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='STATIC_TEXT'">
          <span
            v-if="prop.options['staticTextContentType']==='text/html'"
          >{{prop.staticTextDefaultValue}}</span>
          <span
            v-if="prop.options['staticTextContentType']==='text/markdown'"
          >{{prop.staticTextDefaultValue}}</span>
          <span v-else>{{prop.staticTextDefaultValue}}</span>
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='RUNDECK_JOB'">
          <input
            :name="`${rkey}prop_`+pindex"
            :id="`${rkey}prop_`+pindex"
            readonly
            size="100"
            class="form-control input-sm"
            v-bind:class="contextAutocomplete ? 'context_var_autocomplete' : ''"
            v-bind:title="currentValue"
            v-bind:value="jobName"
          >
        </template>
        <input
          :name="`${rkey}prop_`+pindex"
          v-model="currentValue"
          :id="`${rkey}prop_`+pindex"
          size="100"
          type="text"
          class="form-control input-sm"
          v-bind:class="contextAutocomplete ? 'context_var_autocomplete' : ''"
          v-else
        >
      </div>
      <div
        v-if="prop.options && prop.options['selectionAccessor']==='PLUGIN_TYPE'"
        class="col-sm-5"
      >
        <select v-model="currentValue" class="form-control input-sm">
          <option disabled value>-- Select Plugin Type --</option>
          <option
            v-for="opt in propsComputedSelectorData[prop.name]"
            v-bind:value="opt.value"
            v-bind:key="opt.key"
          >{{opt.key}}</option>
        </select>
      </div>
      <div v-if="prop.options && prop.options['selectionAccessor']==='RUNDECK_JOB'" class="col-sm-5">
        <job-config-picker v-model="currentValue" :btnClass="`btn-primary`"></job-config-picker>
      </div>
      <div v-if="prop.options && prop.options['selectionAccessor']==='STORAGE_PATH'" class="col-sm-5">
        <key-storage-selector v-model="currentValue" :storage-filter="prop.options['storage-file-meta-filter']"
                              :allow-upload="true"
                              :value="keyPath"/>

      </div>
      <slot
        v-else-if="prop.options && prop.options['selectionAccessor'] "
        name="accessors"
        :prop="prop"
        :inputValues="inputValues"
        :accessor="prop.options['selectionAccessor']"
      ></slot>
    </template>

    <div class="col-sm-10 col-sm-offset-2 help-block" v-if="prop.desc">
      <details class="more-info" :class="extendedCss" v-if="extraDescription">
          <summary>
              <span :class="descriptionCss" >{{shortDescription}}</span>
              <span class="more-indicator-verbiage btn-link btn-xs">More &hellip; </span>
              <span class="less-indicator-verbiage btn-link btn-xs">Less </span>
          </summary>

          <markdown-it-vue class="markdown-body" :content="extraDescription" />

      </details>
      <div class="help-block" v-else>{{prop.desc}}</div>
    </div>
    <div
      class="col-sm-10 col-sm-offset-2 text-warning"
      v-if="validation && !validation.valid && validation.errors[prop.name]"
    >{{validation.errors[prop.name]}}</div>
  </div>
</template>
<script lang="ts">
import Vue from "vue"
import MarkdownItVue from 'markdown-it-vue'
// import 'markdown-it-vue/dist/markdown-it-vue.css'

import JobConfigPicker from './JobConfigPicker.vue'
import KeyStorageSelector from './KeyStorageSelector.vue'

import AceEditor from '../utils/AceEditor.vue'
import PluginPropVal from './pluginPropVal.vue'
import { client } from '../../modules/rundeckClient'
import DynamicFormPluginProp from "./DynamicFormPluginProp.vue";
export default Vue.extend({
  components:{
    DynamicFormPluginProp,
    AceEditor,
    JobConfigPicker,
    MarkdownItVue,
    PluginPropVal,
    KeyStorageSelector
  },
  props:{
    'prop':{
      type:Object,
      required:true
     },
    'value':{
      required:false,
      default:''
     },
    'inputValues':{
      type:Object,
      required:false
     },
    'validation':{
      type:Object,
      required:false
     },
    'rkey':{
      type:String,
      required:false,
      default:''
     },
    'pindex':{
      type:Number,
      required:false,
      default:0
     },
    'descriptionCss':{
        type:String,
        default:'',
        required:false
    },
    'extendedCss':{
        type:String,
        default:'',
        required:false
    },
    'contextAutocomplete':{
        type:Boolean,
        default:false,
        required:false
    },
  },
  methods:{
    inputColSize(prop: any) {
      if (prop.options && prop.options['selectionAccessor']) {
        return 'col-sm-5'
      }
      return 'col-sm-10'
    },
    setJobName(jobUuid: string) {
      if((jobUuid && jobUuid.length > 0) && (this.prop.options && this.prop.options['displayType']==='RUNDECK_JOB')) {
        client.jobInfoGet(jobUuid).then(response => {
          if(response.name) {
            var output = ''
            if(response.group) output += response.group+"/"
            output += response.name + " ("+response.project+")"
            this.jobName = output
          }
        })
      }
    },
    parseAllowedValues(){
      return JSON.stringify(this.prop.allowed);
    },
    hasAllowedValues(){
      if(this.prop.allowed!=null){
        return 'true';
      }
      return 'false';
    }
  },
  data(){
    return{
      currentValue: this.value,
      jobName: '',
      keyPath:'',
    }
  },
  watch:{
    currentValue:function(newval){
      this.$emit('input',newval)
      this.setJobName(newval)
    },
    value:function(newval){
      this.currentValue = newval
    }
  },
  computed:{
    shortDescription() :string{
      const desc = this.prop.desc
        if (desc && desc.indexOf("\n") > 0) {
            return desc.substring(0, desc.indexOf("\n"));
        }
        return desc;
    },
    extraDescription() :string|null{
      const desc = this.prop.desc
        if (desc && desc.indexOf("\n") > 0) {
            return desc.substring(desc.indexOf("\n") + 1);
        }
        return null;
    }
  },
  mounted(){
    this.$emit("pluginPropsMounted")
    this.setJobName(this.value)
    if (window._rundeck && window._rundeck.projectName) {
      this.keyPath = 'keys/project/' + window._rundeck.projectName +'/'
    }
  }
})
</script>
<style scoped lang="scss">
.longlist{
  max-height: 500px;
  overflow-y: auto
}
</style>
