<template>
  <div v-show="displayCard" class="px-3">
    <div class="card w-full p-6">
      <div class="col-md-3"  v-if="result.record !=null">
        <div v-if="result.record !=null && result.record.images.thumbnail!=null && result.record.images.thumbnail.url">
          <div :style="{ backgroundImage: `url( ${result.record.images.thumbnail.url} )` }" class="w-full thumbnail"></div>
        </div>
      </div>
      <div class="col-md-9">
        <div v-if="result.currentVersion">Version {{result.currentVersion}}</div>
        <div class="text-4xl font-bold">{{result.display || result.name}}</div>
        <div style="margin-top:.5em;">{{result.support}}</div>

        <div class="">
          <div class="flexible">
            <div style="margin-bottom:1em;margin-top: 1em;">
              <a
                class="btn btn-sm btn-cta square-button"
                v-if="!result.installed && canInstall && result.installId"
                @click="handleInstall"
              >Install</a>
              <a
                v-if="result.installed"
                @click="handleUninstall"
                class="btn btn-sm  btn-danger square-button"
              >Uninstall</a>
            </div>
            <div
              class="requires-rundeck-version"
              v-if="result.rundeckCompatibility"
            >Requires Rundeck {{result.rundeckCompatibility}}</div>
            <div v-if="result.author" style="margin-bottom:1em;">Author: {{result.author}}</div>
            <div class="plugin-description" v-html="result.description"></div>
            <div v-if="result.artifactType">
              <label>Plugin Type:</label>
              {{result.artifactType}}
            </div>
            <ul class="provides">
              <li v-for="(svc, index) in unqSortedSvcs(result.providesServices)" :key="index">{{svc}}</li>
            </ul>
          </div>
        </div>
        <div class="card-footer">
          <div class v-if="result.support !== 'Enterprise Exclusive'">
            <div class="row">
              <div class="col-xs-12 col-sm-12">
                <div class="links fa-2x">
                  <a v-if="result.sourceLink" :href="result.sourceLink" target="_blank">
                    <i class="fas fa-code-branch"></i>
                  </a>
                  <a
                    v-if="result.record && result.record.post_slug "
                    :href="`https://online.rundeck.com/plugins/${result.record.post_slug}/`"
                    target="_blank"
                  >
                    <i class="fas fa-file-alt"></i>
                  </a>
                  <a v-if="result.docsLink" :href="result.docsLink" target="_blank">
                    <i class="fas fa-file-alt"></i>
                  </a>
                </div>
              </div>
            </div>
          </div>
          <div class="col-xs-12" v-else>
            <a
              href="https://www.rundeck.com/test-drive"
              target="_blank"
              class="btn btn-default btn-md"
            >Learn More</a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { mapState, mapActions } from "vuex";

export default {
  name: "PluginCard",
  props: ["result", "repo"],
  computed: {
    ...mapState("repositories", [
      "canInstall",
      "rdBase",
      "filterSupportType",
      "showWhichPlugins"
    ]),
    displayCard() {
      if (this.showWhichPlugins === null) {
        return true;
      } else if(this.result.support === 'Enterprise Exclusive' && this.showWhichPlugins === true && window._RDPRO_EDITION) {
        return true
      } else if(this.result.support === 'Enterprise Exclusive' && this.showWhichPlugins === false && window._RDPRO_EDITION) {
        return false
      } else {
        return this.showWhichPlugins === this.result.installed;
      }
      // The below is for the Support Type Facet Filter
      // This is temporarily removed
      // console.log("this.filterSupportType", this.filterSupportType);
      // if (!this.filterSupportType || this.filterSupportType.length === 0) {
      //   return true;
      // } else {
      //   if (
      //     this.filterSupportType &&
      //     this.filterSupportType.includes(this.result.support)
      //   ) {
      //     return true;
      //   } else {
      //     return false;
      //   }
      // }
    }
  },
  data() {
    return {};
  },
  methods: {
    ...mapActions("repositories", ["installPlugin", "uninstallPlugin"]),
    unqSortedSvcs: function(serviceList) {
      if (!serviceList) return [];
      var unq = [];
      serviceList.forEach(svc => {
        if (unq.indexOf(svc) === -1) unq.push(svc);
      });
      return unq.sort();
    },

    handleInstall() {
      this.installPlugin({
        repo: this.repo,
        plugin: this.result
      });
    },
    handleUninstall() {
      this.uninstallPlugin({
        repo: this.repo,
        plugin: this.result
      });
    }
  },
  filters: {
    shorten: function(value) {
      if (value.length > 200) {
        return value.substr(0, 140) + "... click to read more";
      } else {
        return value;
      }
    }
  }
};
</script>
<style lang="scss" scoped>
.px-3{
  padding-left: 0.75rem;
  padding-right: 0.75rem;
}
.p-6{
  padding: 1.8rem;
}
.w-full{
  width: 100%;
}
.thumbnail{
  background-size: contain;
  border: 1px solid #e2e8f0;
  background-repeat: no-repeat;
  background-position: center;
  padding-bottom: 95%;
}
.font-bold{
  font-weight: 600;
}
.links{
  display: flex;
  a{
    margin-right: 10px;
  }
}
.text-4xl{
  font-size: 2.25rem;
  line-height: 2.5rem;
}
</style>
