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
  <div >
    <div v-if="codeSyntaxSelectable" class="ace_text_controls form-inline">
      <label>
        Syntax Mode:
        <select class="form-control" v-model="modeInternal">
          <option value="-">-None-</option>
          <option v-for="mode in aceModes" :value="mode" :key="mode">{{mode}}</option>
        </select>
      </label>
    </div>
    <div v-if="softWrapControl"  class="checkbox ace_text_controls">
      <input type="checkbox" v-model="wrapInternal" id="ace_wrap"><label for="ace_wrap">Soft Wrap</label>
    </div>
    <ace v-model="valueInternal"
         @init="aceEditorInit"
         :identifier="identifier"
         :lang="modeInternal"
         :soft-wrap="wrapInternal"
         :theme="theme"
         :height="height"
         :width="width"
    ></ace>
  </div>
</template>
<script lang="ts">
import {defineComponent} from 'vue'

import Ace from './AceEditorVue.vue'

import 'ace-builds/src-noconflict/ext-language_tools'
import 'ace-builds/src-noconflict/mode-batchfile'
import 'ace-builds/src-noconflict/mode-diff'
import 'ace-builds/src-noconflict/mode-dockerfile'
import 'ace-builds/src-noconflict/mode-golang'
import 'ace-builds/src-noconflict/mode-groovy'
import 'ace-builds/src-noconflict/mode-html'
import 'ace-builds/src-noconflict/mode-java'
import 'ace-builds/src-noconflict/mode-javascript'
import 'ace-builds/src-noconflict/mode-json'
import 'ace-builds/src-noconflict/mode-markdown'
import 'ace-builds/src-noconflict/mode-perl'
import 'ace-builds/src-noconflict/mode-php'
import 'ace-builds/src-noconflict/mode-powershell'
import 'ace-builds/src-noconflict/mode-properties'
import 'ace-builds/src-noconflict/mode-python'
import 'ace-builds/src-noconflict/mode-ruby'
import 'ace-builds/src-noconflict/mode-sh'
import 'ace-builds/src-noconflict/mode-sql'
import 'ace-builds/src-noconflict/mode-xml'
import 'ace-builds/src-noconflict/mode-yaml'
import 'ace-builds/src-noconflict/theme-chrome'
import 'ace-builds/src-noconflict/theme-tomorrow_night'
import 'ace-builds/src-noconflict/theme-tomorrow_night_eighties'

export default defineComponent({
  name: 'ace-editor',
  components: {Ace},
  props: {
    identifier: String,
    modelValue: String,
    height: String,
    width: String,
    lang: String,
    theme: String,
    codeSyntaxSelectable: Boolean,
    softWrapControl: Boolean,
    readOnly: Boolean
  },
  emits: ['update:modelValue'],
  data () {
    return {
      valueInternal: this.modelValue || '',
      modeInternal: this.lang || 'text',
      wrapInternal: false,
      localAce: null,
      aceModes: [
        'batchfile',
        'diff',
        'dockerfile',
        'golang',
        'groovy',
        'html',
        'java',
        'javascript',
        'json',
        'markdown',
        'perl',
        'php',
        'powershell',
        'properties',
        'python',
        'ruby',
        'sh',
        'sql',
        'xml',
        'yaml'
      ]
    }
  },
  watch: {
    valueInternal (newValue, oldValue) {
      this.$emit('update:modelValue', newValue)
    },
    modelValue (newValue, oldValue) {
      this.valueInternal = newValue
    },
    lang (newValue, oldValue) {
      this.modeInternal = newValue
    }
  },
  methods: {
    aceEditorInit (ace: any) {
      this.localAce = ace
      if(this.readOnly){
        ace.setReadOnly(true)
      }
    }
  }
})
</script>
