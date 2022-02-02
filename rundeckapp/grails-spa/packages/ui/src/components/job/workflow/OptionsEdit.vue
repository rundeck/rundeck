<template>
  <!--<div v-if="modelData">-->
  <div class="optEditForm">
    <div id="optedit_281fcef2">
      <div class="row">
        <div class="col-sm-12">
          <span class="h4">Add New Option</span>
        </div>
      </div>
      <div class="form-group">

        <label for="opttype_281fcef2" class="col-sm-2 control-label    ">
          Option Type
        </label>

        <div class="col-sm-10">
          <select class="form-control select" name="optionType" v-model="modelData.optionType" @change="setOptionType(modelData.optionType)" id="opttype_281fcef2">
            <option v-for="option in optionsList" v-bind:value="option.value">
              {{ option.key }}
            </option>
          </select>
        </div>
      </div>

      <div class="form-group" v-if="isFileType()">
        <div class="col-sm-10 col-sm-offset-2">
          <i class="rdicon icon-small plugin"></i>
          Temporary File
          <span class="text-strong">
              <span class="">Stores uploaded files temporarily on the file system for the duration of the execution.</span>
            </span>
        </div>
      </div>
      <div class="form-group">
        <label for="optname_281fcef2" class="col-sm-2 control-label    ">Option Name</label>
        <div class="col-sm-10">
          <input type="text" v-model="modelData.name" name="name" class="form-control restrictOptName"  size="40" placeholder="Option Name" id="optname_281fcef2">
        </div>
      </div>
      <div class="form-group">

        <label for="optlabel_281fcef2" class="col-sm-2 control-label    ">Option Label</label>

        <div class="col-sm-10">
          <input type="text" class="form-control" v-model="modelData.label" name="label" id="opt_label" value="" size="40" placeholder="Option Label">
        </div>
      </div>
      <div class="form-group ">

        <label class="col-sm-2 control-label" for="optdesc_281fcef2">Description</label>
        <div class="col-sm-10">
          <rd-ace-editor
            :name="editor"
            v-model="modelData.description"
            lang="markdown"
            :id="editor"
            height="160"
            width="100%"
            class="border"
          />
          <div class="help-block">
            The description will be rendered with Markdown.
            <a href="http://en.wikipedia.org/wiki/Markdown" target="_blank" class="text-info">
              <i class="glyphicon glyphicon-question-sign"></i>
            </a>
          </div>
        </div>
      </div>
      <div class="form-group" v-if="!isFileType()">
        <label class="col-sm-2 control-label">Default Value</label>
        <div class="col-sm-10">
          <input type="text" class="form-control" name="defaultValue" id="opt_defaultValue" size="40" placeholder="Default value" v-model="defaultValue">
        </div>
      </div>
      <div class="form-group" v-if="!isFileType()">
        <label>Allowed Values</label>
        <label class="col-sm-2 control-label">Allowed Values</label>
        <div class="col-sm-10">
          <div v-for="(type,n) in optionsSummary" :key="type.name" class="radio">
            <input type="radio" v-model="modelData.type" :value="type.name" :id="'ctype_'+n">
            <label class="radio-inline" :for="'ctype_'+n">
              {{type.description.title}}
            </label>
          </div>
        </div>
      </div>
      <div class="form-group" v-if="!isFileType()">
        <div class="col-sm-2 control-label text-form-label">
          <span>Restrictions</span>
        </div>
        <div class="col-sm-10">
          <div class="radio">
            <input type="radio" name="enforcedType" @click=""
                   :checked="!schedulesEnabled"
                   id="enforcedType_none"/>
            <label for="enforcedType_none">
              None
            </label>
            <span class="text-strong">Any values can be used</span>
          </div>
          <div class="radio">
            <input type="radio" name="enforcedType" @click=""
                   :checked="schedulesEnabled"
                   id="enforcedType_enforced"/>
            <label for="enforcedType_enforced">
              <span>Enforced from Allowed Values</span>
            </label>
          </div>
          <div class="radio">
            <input type="radio" name="enforcedType" @click=""
                   :checked="schedulesEnabled"
                   id="etregex_"/>
            <label for="etregex_">
              <span>Match Regular Expression</span>
            </label>
          </div>

        </div>
      </div>
      <div class="form-group" v-if="!isFileType()">
        <div class="col-sm-2 control-label text-form-label">
          <span>Input Type</span>
        </div>
        <div class="col-sm-10">
          <div class="radio">
            <input type="radio" name="scheduleDefinitionsEnabled" @click=""
                   :checked="!schedulesEnabled"
                   id="scheduleDefinitionsFalse"/>
            <label for="scheduleDefinitionsFalse">
              <span>Plain text</span>
            </label>
          </div>
          <div class="radio">
            <input type="radio" name="scheduleDefinitionsEnabled" @click=""
                   :checked="schedulesEnabled"
                   id="scheduleDefinitionsTrue"/>
            <label for="scheduleDefinitionsTrue">
              <span>Date The date will pass to your job as a string formatted this way: mm/dd/yy HH:MM</span>
            </label>
          </div>
          <div class="radio">
            <input type="radio" name="scheduleDefinitionsEnabled" @click=""
                   :checked="schedulesEnabled"
                   id="scheduleDefinitionsna"/>
            <label for="scheduleDefinitionsna">
              <span>Secure † Password input, value exposed in scripts and commands.</span>
            </label>
          </div>
          <div class="radio">
            <input type="radio" name="scheduleDefinitionsEnabled" @click=""
                   :checked="schedulesEnabled"
                   id="scheduleDefinitionsba"/>
            <label for="scheduleDefinitionsba">
              <span>Secure Remote Authentication † Password input, value not exposed in scripts or commands, used only by Node Executors for authentication.</span>
            </label>
          </div>
        </div>
      </div>
      <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          Required
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input type=radio
                   :value="false"
                   name="required"
                   v-model="modelData.required"
                   id="requiredFalse"/>
            <label for="requiredFalse">
              No
            </label>
          </div>
          <div class="radio radio-inline">
            <input type="radio"
                   name="required"
                   :value="true"
                   v-model="modelData.required"
                   id="requiredTrue"/>
            <label for="requiredTrue">
              Yes
            </label>
          </div>
          <span class="help-block">
                Require this option to have a non-blank value when running the Job

          </span>
        </div>
      </div>
      <div class="form-group">
        <div class="col-sm-2 control-label text-form-label">
          Should be hidden
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input type=radio
                   :value="false"
                   name="hidden"
                   v-model="modelData.hidden"
                   id="hiddenFalse"/>
            <label for="hiddenFalse">
              No
            </label>
          </div>
          <div class="radio radio-inline">
            <input type="radio"
                   name="hidden"
                   :value="true"
                   v-model="modelData.hidden"
                   id="hiddenTrue"/>
            <label for="hiddenTrue">
              Yes
            </label>
          </div>
          <span class="help-block">
                Should be hidden from job run page
          </span>
        </div>
      </div>
      <div class="form-group" v-if="!isFileType()">
        <div class="col-sm-2 control-label text-form-label">
          Multi-valued
        </div>

        <div class="col-sm-10">
          <div class="radio radio-inline">
            <input type="radio"
                   name="excludeFilterUncheck"
                   :value="true"
                   v-model="modelData.excludeFilterUncheck"
                   id="excludeFilterTrue"/>
            <label for="excludeFilterTrue">
              Yes
            </label>
          </div>

          <div class="radio radio-inline">
            <input type=radio
                   :value="false"
                   name="excludeFilterUncheck"
                   v-model="modelData.excludeFilterUncheck"
                   id="excludeFilterFalse"/>
            <label for="excludeFilterFalse">
              No
            </label>
          </div>

          <span class="help-block">
                Allow multiple input values to be chosen.
          </span>
        </div>
      </div>
    </div>
    <section id="preview_6f8b5487" class="section-separator-solo" v-if="modelData.name && !isFileType()">
      <div class="row">
        <label class="col-sm-2 control-label">Usage</label>
        <div class="col-sm-10 opt_sec_nexp_disabled" style="">
          <span class="text-strong">The option values will be available to scripts in these forms:</span>
          <div>
            Bash: <code>$<span data-bind="text: bashVarPreview">RD_OPTION_WE</span></code>
          </div>
          <div>
            Commandline Arguments: <code>$<!-- -->{option.<span data-bind="text: name">we</span>}</code>
          </div>
          <div>
            Commandline Arguments (unquoted): <code>$<!-- -->{unquotedoption.<span data-bind="text: name">we</span>}</code>
            Warning! Relying on unquoted arguments could make this job vulnerable to command injection. Use with care.
          </div>
          <div>
            Script Content: <code>@option.<span data-bind="text: name">we</span>@</code>
          </div>
        </div>

        <div class="col-sm-10 opt_sec_nexp_enabled" style="display:none;" v-if="modelData.name && isFileType()">
          <span class="warn note">Secure authentication option values are not available to scripts or commands</span>
        </div>
      </div>
    </section>
    <section id="file_preview_6f8b5487" style="" class="section-separator-solo" v-if="modelData.name && isFileType()">
      <div class="row">
        <label class="col-sm-2 control-label">Usage</label>
        <div class="col-sm-10">
          <span class="text-info">The local file path will be available to scripts in these forms:</span>
          <div>
            Bash: <code>$<span data-bind="text: fileBashVarPreview">RD_FILE_WE</span></code>
          </div>
          <div>
            Commandline Arguments: <code>$<!-- -->{file.<span data-bind="text: name">we</span>}</code>
          </div>
          <div>
            Script Content: <code>@file.<span data-bind="text: name">we</span>@</code>
          </div>

          <span class="text-info">The original file name:</span>
          <div>
            Bash: <code>$<span data-bind="text: fileFileNameBashVarPreview">RD_FILE_WE_FILENAME</span></code>
          </div>
          <div>
            Commandline Arguments: <code>$<!-- -->{file.<span data-bind="text: name">we</span>.fileName}</code>
          </div>
          <div>
            Script Content: <code>@file.<span data-bind="text: name">we</span>.fileName@</code>
          </div>
          <span class="text-info">The file content SHA-256 value:</span>
          <div>
            Bash: <code>$<span data-bind="text: fileShaBashVarPreview">RD_FILE_WE_SHA</span></code>
          </div>
          <div>
            Commandline Arguments: <code>$<!-- -->{file.<span data-bind="text: name">we</span>.sha}</code>
          </div>
          <div>
            Script Content: <code>@file.<span data-bind="text: name">we</span>.sha@</code>
          </div>
        </div>

      </div>
    </section>
    <div class="floatr" style="margin:10px 0;">

      <input type="hidden" name="newoption" value="true" id="newoption">
      <span class="btn btn-default btn-sm" @click="cancelNewOption" title="Cancel adding new option">Cancel</span>
      <span class="btn btn-primary btn-sm" @click="saveOption" title="Save the new option">Save</span>
      <script type="text/javascript">

        fireWhenReady('optname_46fae357',function(){
          jQuery('#optname_46fae357').focus();
        });

      </script>


      <span class="text-warning cancelsavemsg" style="display:none;">
                Discard or save changes to this option before completing changes to the job
            </span>
    </div>
  </div>
  <!-- </div>-->
</template>
<script lang="ts">
import {_genUrl} from '@/utilities/genUrl'
import axios from 'axios'
import InlineValidationErrors from '../../form/InlineValidationErrors.vue'
import Vue from 'vue'
import Component from 'vue-class-component'
import {Prop, Watch} from 'vue-property-decorator'
import AceEditor from '@rundeck/ui-trellis/lib/components/utils/AceEditor.vue'

import {
  getRundeckContext,
  getAppLinks
} from '@rundeck/ui-trellis'
import {RundeckBrowser} from "@rundeck/client";
Vue.component('rd-ace-editor', AceEditor)
const client: RundeckBrowser = getRundeckContext().rundeckClient
const rdBase = getRundeckContext().rdBase

@Component({components: {InlineValidationErrors}})
export default class OptionsEdit extends Vue {
  @Prop({required: true})
  value: any

  @Prop({required: true})
  eventBus!: Vue

  labelColClass = 'col-sm-2 control-label'
  fieldColSize = 'col-sm-10'
  modelData: any = {optionType: "file", name: null}
  optionsList: any = [{key:"Text", value:""}, {key:"File", value:"file"}]
  optionsSummary: any = {}


  async mounted() {
    this.modelData = Object.assign({name: null, optionType:"", hidden: false, required: false}, this.value)
    this.loadOptionsSummary()
  }


  async loadOptionsSummary() {
    let result = await axios.get(_genUrl(getAppLinks().editOptsEdit, { newoption: true}))
    if (result.status >= 200 && result.status < 300) {
      this.optionsSummary = JSON.parse(JSON.stringify(result.data.optionValuesPlugins))
    }
  }

  async saveOption() {
    let result = await client.sendRequest({
      method: 'POST',
      url: _genUrl(
        getAppLinks().editOptsSave,
        this.modelData)
    })
    if (result.status != 200) {
      console.log('Error saving option: ' + result.status + ': ' + (result.parsedBody?.msg || 'Unknown error'));
    }
  }
  setOptionType(type:string){
    Vue.set(this.modelData, 'optionType', type)
  }
  cancelNewOption(){
    Vue.set(this.modelData, 'newOption', false)
  }

  isFileType() {
    return this.modelData.optionType === 'file'

  }

  @Watch('modelData', {deep: true})
  wasChanged() {
    this.$emit('input', this.modelData)
  }
}
</script>
