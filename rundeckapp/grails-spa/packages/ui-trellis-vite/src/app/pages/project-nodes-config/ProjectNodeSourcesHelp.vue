<template>
  <div>
    <div v-if="unauthorized">

      <alert type="warning"   >
        <p><b>{{$t('unauthorized.status.help.1')}}</b></p>
        <p>{{$t('unauthorized.status.help.2')}}</p>
        <p>{{$t('unauthorized.status.help.3')}}</p>

        <div>
          <form method="POST" :action="projectAclConfigPageUrl">
            <input type="hidden" name="fileText" :value="aclExample"/>

          <i18n path="unauthorized.status.help.4" tag="p" >
            <button class="btn btn-sm btn-default" type="submit">{{ $t('acl.config.link.title') }}</button>
          </i18n>
          <details>
            <summary>{{$t('acl.example.summary')}}</summary>
            <pre>{{aclExample}}
            </pre>
          </details>
         </form>
        </div>

        <form method="POST" :action="systemAclConfigPageUrl">
          <input type="hidden" name="fileText" :value="systemAclExample"/>
          <input type="hidden" name="fileType" value="storage"/>

          <i18n path="unauthorized.status.help.5" tag="p" >
            <button class="btn btn-sm btn-default" type="submit">{{ $t('acl.config.system.link.title') }}</button>
          </i18n>
          <details>
            <summary>{{$t('acl.example.summary')}}</summary>
            <pre>{{systemAclExample}}
            </pre>
          </details>
        </form>

      </alert>

    </div>

  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { getRundeckContext } from "@/library/rundeckService";

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
      systemAclConfigPageUrl:"",
      aclExample: "",
      systemAclExample: "",
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
    if(window._rundeck.data) {
      this.projectAclConfigPageUrl  = window._rundeck.data.projectAclConfigPageUrl
      this.systemAclConfigPageUrl =  window._rundeck.data.systemAclConfigPageUrl
    }

    this.project = getRundeckContext().projectName;
    this.aclExample = "by:\n" +
      "  urn: project:" + this.project + "\n" +
      "for:\n" +
      "  storage:\n" +
      "    - match: \n" +
      "        path: 'keys/project/" + this.project + "/.*'\n" +
      "      allow: [read] \n" +
      "description: Allow access to key storage"

    this.systemAclExample = "by:\n" +
      "  urn: project:" + this.project + "\n" +
      "context: \n" +
      "   application: rundeck\n" +
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
