<template>
  <div>
    <div v-if="unauthorized">

      <alert type="warning"   >
        <p><b>{{$t('unauthorized.status.help.1')}}</b></p>
        <p>{{$t('unauthorized.status.help.2')}}</p>
        <p>{{$t('unauthorized.status.help.3')}}</p>

        <form method="POST" :action="projectAclConfigPageUrl">
          <input type="hidden" name="fileText" :value="aclExample"/>

          <i18n path="unauthorized.status.help.4" tag="p" >
            <button type="submit">{{ $t('acl.config.link.title') }}</button>
          </i18n>
          <details>
            <summary>{{$t('acl.example.summary')}}</summary>
            <pre>
by:
  urn: project:{{project}}
for:
  storage:
    - match:
        path: 'keys/project/{{project}}/.*'
      allow: [read]
description: Allow access to key storage
          </pre>
          </details>

        </form>


      </alert>

    </div>

  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { getRundeckContext, RundeckContext } from "@rundeck/ui-trellis";

export default Vue.extend({
  name: "ProjectNodeSourcesHelp",
  props:{
    eventBus:{type:Vue,required:false}
  },
  data(){
    return {
      count:0,
      unauthorized: false,
      project: "",
      projectAclConfigPageUrl:"",
      aclExample: ""
    }
  },
  computed: {
    empty: function(): any {
      return this.count===0;
    },
  },
  watch: {
    unauthorized: function(val, oldVal) {
      this.$forceUpdate();
    }
  },

  mounted(){
    this.project = getRundeckContext().projectName;
    this.projectAclConfigPageUrl  = "/project/"+this.project+"/admin/acls/create"
    this.aclExample = "by:\n" +
      "  urn: project:" + this.project + "\n" +
      "for:\n" +
      "  storage:\n" +
      "    - match: \n" +
      "        path: 'keys/project/" + this.project + "/.*'\n" +
      "      allow: [read] \n" +
      "description: Allow access to key storage"

    this.eventBus.$on('nodes-unauthorized',(count: number)=>{
      if(count>0){
        this.unauthorized=true;
      }else{
        this.unauthorized=false;
      }

    });

  }
});
</script>

<style scoped>

</style>
