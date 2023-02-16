<template>
  <span>
      <span class="configpair" v-if="prop.type==='Boolean'">
        <template v-if="value==='true' ||value===true || prop.defaultValue==='true' && (value==='false'||value===false)">
          <span :title="prop.desc">{{prop.title}}: </span>
          <span :class="prop.options && prop.options['booleanTrueDisplayValueClass']||'text-success'" v-if="value==='true'||value===true">
            <plugin-prop-val :prop="prop" :value="value"/>
          </span>
          <span :class="prop.options && prop.options['booleanFalseDisplayValueClass']||'text-success'" v-if="value==='false'||value===false">
            <plugin-prop-val :prop="prop" :value="value"/>
          </span>
        </template>
      </span>
      <span class="configpair" v-else-if="prop.type==='Integer'">
        <span :title="prop.desc">{{prop.title}}:</span>
        <span style="font-family:Courier,monospace">{{value}}</span>
      </span>
      <span class="configpair" v-else-if="['Options', 'Select','FreeSelect'].indexOf(prop.type)>=0">
        <span :title="prop.desc">{{prop.title}}:</span>
        <template v-if="prop.type!=='Options'">
          <span class="text-success"><plugin-prop-val :prop="prop" :value="value"/></span>
        </template>
        <template v-else>
          <span v-if="typeof value==='string'">
          <span v-for="optval in value.split(/, */)" :key="optval" class="text-success">
            <i class="glyphicon glyphicon-ok-circle" v-if="!(prop.options && prop.options['valueDisplayType'])"></i>
            <plugin-prop-val :prop="prop" :value="optval"/>
          </span>
          </span>
          <span v-else-if="value.length>0">
            <span v-for="optval in value" :key="optval" class="text-success">
            <i class="glyphicon glyphicon-ok-circle" v-if="!(prop.options && prop.options['valueDisplayType'])"></i>
            <plugin-prop-val :prop="prop" :value="optval"/>
          </span>
          </span>
        </template>
      </span>
      <span v-else class="configpair">
        <template v-if="prop.options && prop.options['displayType']==='CODE'">
          <expandable >
            <template slot="label"><span :title="prop.desc">{{ prop.title }}:</span> <span class="text-info">{{value.split(/\r?\n/).length}} lines</span></template>
             <ace-editor
              v-model="value"
              :lang="prop.options['codeSyntaxMode']"
              height="200"
              width="100%"
              :readOnly="true"
            />
          </expandable>
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='MULTI_LINE'" >
          <expandable>
            <template slot="label"><span :title="prop.desc">{{ prop.title }}:</span> <span class="text-info">{{value.split(/\r?\n/).length}} lines</span></template>
            <ace-editor
              v-model="value"
              height="200"
              width="100%"
              :readOnly="true"
            />
          </expandable>
        </template>
        <template v-else-if="prop.options && prop.options['displayType']==='DYNAMIC_FORM'" >
          <div class="customattributes"></div>

          <span v-for="custom in getCustomValues()" class="configpair">
            <span title="">{{custom.label}}:</span>
            <span class="text-success"> {{custom.value}}</span>
          </span>
        </template>
        <span v-else>
          <span :title="prop.desc">{{ prop.title }}:</span>
          <span class="text-success" v-if="prop.options && prop.options['displayType']==='PASSWORD'">&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;</span>
          <span class="text-success" v-else>{{ value }}</span>
        </span>
      </span>
    </span>
</template>
<script lang="ts">
import Vue from 'vue'
import Expandable from '../utils/Expandable.vue'
import AceEditor from '../utils/AceEditor.vue'
import PluginPropVal from './pluginPropVal.vue'
export default Vue.extend({
  components:{
    Expandable,
    AceEditor,
    PluginPropVal
  },
  props:{
    value:{
      required:true
    },
    prop:{
      type:Object,required:true
    }
  },
  methods:{
    getCustomValues(): any{
      if(this.value!==null){
        const data = `${this.value}`
        const json = JSON.parse(data)
        return json
      }
      return []
    }
  }
})
</script>
<style>
.customattributes {
  border-bottom: 1px solid #eeeeee;
  margin-top: 10px;
  margin-bottom: 10px;
}

</style>
