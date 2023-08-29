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
            v-if="!renderReadOnly"
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

            v-if="!renderReadOnly"
          >
            <input
                type="radio"
                :name="`${rkey}prop_`+pindex"
                :id="`${rkey}prop_true_`+pindex"
                value="true"
                :disabled="true"
                v-model="currentValue"
                v-else
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

      <dynamic-form-plugin-prop :id="rkey" :fields="modelValue" v-model="currentValue"
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
          v-if="!renderReadOnly"
        >
          <option v-if="!prop.required" value>--None Selected--</option>
          <option
            v-for="opt in prop.allowed"
            v-bind:value="opt"
            v-bind:key="opt"
          ><plugin-prop-val :prop="prop" :value="opt"/></option>
        </select>
        <select
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :id="`${rkey}prop_`+pindex"
            class="form-control input-sm"
            :disabled="true"
            v-else
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
            v-if="!renderReadOnly"
          >
          <input
              :name="`${rkey}prop_`+pindex"
              v-model="currentValue"
              :id="`${rkey}prop_`+pindex"
              size="100"
              type="text"
              class="form-control input-sm"
              :disabled="true"
              v-else
          >
        </div>
        <div class="col-sm-5">
          <select class="form-control input-sm" v-model="currentValue" v-if="!renderReadOnly">
            <option
                v-for="opt in prop.allowed"
                v-bind:value="opt"
                v-bind:key="opt"
            ><plugin-prop-val :prop="prop" :value="opt"/></option>
          </select>
          <select class="form-control input-sm" v-model="currentValue" :disabled="true" v-else>
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
                v-if="!renderReadOnly"
              >
              <input
                  type="checkbox"
                  v-model="currentValue"
                  :value="opt"
                  :id="`${rkey}opt_`+pindex+'_'+oindex"
                  :disabled="true"
                  v-else
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
          v-if="['Integer','Long'].indexOf(prop.type)>=0 && !renderReadOnly"
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
            v-if="!renderReadOnly"
          ></textarea>
          <textarea
              :name="`${rkey}prop_`+pindex"
              v-model="currentValue"
              :id="`${rkey}prop_`+pindex"
              rows="10"
              cols="100"
              class="form-control input-sm"
              :disabled="true"
              v-else
          ></textarea>
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='CODE'">
          <ace-editor
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :lang="prop.options['codeSyntaxMode']"
            :codeSyntaxSelectable="prop.options['codeSyntaxSelectable']==='true' && !renderReadOnly"
            :id="`${rkey}prop_`+pindex"
            height="200"
            width="100%"
            :read-only="renderReadOnly"
          />
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='PASSWORD'">
          <div v-if="!renderReadOnly">
            <input
            :name="`${rkey}prop_`+pindex"
            v-model="currentValue"
            :id="`${rkey}prop_`+pindex"
            size="100"
            type="password"
            autocomplete="new-password"
            class="form-control input-sm"
            >
          </div>
          <div v-else>
            <input
              :name="`${rkey}prop_`+pindex"
              v-model="currentValue"
              :id="`${rkey}prop_`+pindex"
              size="100"
              type="password"
              autocomplete="new-password"
              class="form-control input-sm"
              :disabled="true"
            >
          </div>
         
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
          :disabled="true"
          v-else-if="renderReadOnly"
        >
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

        <TextAutocomplete v-if="contextAutocomplete && !aceEditorEnabled"
                   :target="'#' + rkey  +'prop_' + pindex"
                   :data="jobContext"
                   item-key="name"
                   v-model="currentValue"
                   autocompleteKey="$"
        >
          <template #item="{ items, select, highlight }">
            <li
                v-for="(item, index) in items"
                :key="item.name"
            >
              <a role="button"  @click="select(item.name)">
                <span v-html="highlight(item)"></span> -  {{item.description}}
              </a>
            </li>
          </template>
        </TextAutocomplete>

      </div>
      <div
        v-if="prop.options && prop.options['selectionAccessor']==='PLUGIN_TYPE'"
        class="col-sm-5"
      >
        <select v-model="currentValue" class="form-control input-sm">
          <option disabled value>-- Select Plugin Type --</option>
          <option
            v-for="opt in selectorDataForName"
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
                              :value="keyPath"
                              :read-only="renderReadOnly"/>
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

          <VMarkdownView class="markdown-body" :content="extraDescription" />

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
import {defineComponent} from "vue"
import {VMarkdownView} from "vue3-markdown"

import JobConfigPicker from './JobConfigPicker.vue'
import KeyStorageSelector from './KeyStorageSelector.vue'

import AceEditor from '../utils/AceEditor.vue'
import PluginPropVal from './pluginPropVal.vue'
import { client } from '../../modules/rundeckClient'
import DynamicFormPluginProp from "./DynamicFormPluginProp.vue";
import TextAutocomplete from '../utils/TextAutocomplete.vue'
import type {PropType} from "vue";
import { getRundeckContext } from "../../rundeckService";

interface Prop {
  type: string
  defaultValue: any
  title: string
  required: boolean
  options: any
  allowed: string
  name: string
  desc: string
  staticTextDefaultValue: string
}

export default defineComponent({
  components:{
    DynamicFormPluginProp,
    AceEditor,
    JobConfigPicker,
    VMarkdownView,
    PluginPropVal,
    KeyStorageSelector,
    TextAutocomplete
  },
  props:{
    'prop':{
      type:Object as PropType<Prop>,
      required:true
     },
    'readOnly':{
      type:Boolean,
      default:false,
      required:false
    },
    'modelValue':{
      required:false,
      default:''
     },
    'inputValues':{
      type:Object,
      required:false
     },
    'validation':{
      type:Object as PropType<any>,
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
    'autocompleteCallback':{
      type:Function,
      required:false
    },
    selectorData: {
      type: Object as PropType<any>,
      required: true,
    }
  },
  emits: ['update:modelValue','pluginPropsMounted'],
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
            let output = ''
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
      jobName: '',
      keyPath:'',
      jobContext: [] as any,
      aceEditorEnabled: false,
      renderReadOnly:false
    }
  },
  watch:{
    currentValue:function(newval){
      this.$emit('update:modelValue',newval)
      this.setJobName(newval)
    },
    value:function(newval){
      this.currentValue = newval
    },
    readOnly:function(newval){
        this.renderReadOnly= newval || (this.prop.options && this.prop.options['displayType']==='READONLY')
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
    },
    selectorDataForName(): any[] {
      return this.selectorData[this.prop.name]
    },
    currentValue: {
        get() {
            return this.modelValue
        },
        set(val: any) {
            this.$emit('update:modelValue',val)
            this.setJobName(val)
        }
    }
  },
  mounted(){
    this.$emit("pluginPropsMounted")
    this.setJobName(this.modelValue)
    if (getRundeckContext() && getRundeckContext().projectName) {
      this.keyPath = 'keys/project/' + getRundeckContext().projectName +'/'
    }

    if(this.autocompleteCallback && this.contextAutocomplete){
      let vars = this.autocompleteCallback(this.rkey  +'prop_' + this.pindex)
      let jobContext = [] as any
      vars.forEach( (context: any) => {
        jobContext.push({
          name: context["value"],
          description: context["data"]["title"],
          category: context["data"]["category"]
        })
      });
      this.jobContext = jobContext
    }

    if(this.prop.options && this.prop.options['displayType']==='CODE'){
      this.aceEditorEnabled = true
    }

    this.renderReadOnly= this.readOnly || (this.prop.options && this.prop.options['displayType']==='READONLY')

  }
})
</script>
<style scoped lang="scss">
@import '~vue3-markdown/dist/style.css';

.longlist{
  max-height: 500px;
  overflow-y: auto
}
</style>
