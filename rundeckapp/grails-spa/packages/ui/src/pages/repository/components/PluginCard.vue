<template>
  <div v-show="displayCard">
    <div class="card result flex-col">
      <div class="card-header">
        <div v-if="result.currentVersion">Version {{result.currentVersion}}</div>
        <h3 class="card-title">{{result.display || result.name}}</h3>
        <div style="margin-top:.5em;">{{result.support}}</div>
      </div>
      <div class="card-content flex-grow">
        <div class="flexible">
          <div style="margin-bottom:1em;">
            <a
              class="btn btn-sm btn-block btn-success square-button"
              v-if="!result.installed && canInstall && result.installId"
              @click="handleInstall"
            >Install</a>
            <a
              v-if="result.installed"
              @click="handleUninstall"
              class="btn btn-sm btn-block btn-danger square-button"
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
            class="btn btn-default btn-sm btn-block"
          >Learn More</a>
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
.artifact {
  max-width: 33.33333333%;
}
.card.result {
  .card-header {
    background: #20201f;
    padding: 1em;
    border-radius: 7px 7px 0 0;
    color: white;
    .card-title {
      margin: 0.5em 0 0;
      color: white;
      font-weight: bold;
      font-size: 1.4em;
      line-height: 1.1em;
    }
  }
  .card-content {
    padding: 1em;
    .requires-rundeck-version {
      text-transform: uppercase;
      margin: 0 0 0.7em;
    }
    .provides {
      list-style: none;
      margin: 2em 0 0;
      padding: 0;
      font-size: 12px;
      li {
        display: inline-block;
        margin-right: 1em;
        margin-bottom: 1em;
        background-color: #d8d8d8;
        padding: 6px 10px 5px;
        border-radius: 50px;
        color: #6e6e6e;
      }
    }
  }
  .card-footer {
    // margin-bottom: 1em;
    padding: 0 2em auto;
    border-radius: 0 0 7px 7px;
    .links {
      a {
        color: #20201f;
        text-decoration: none;
        margin-right: 0.6em;
      }
    }
    .btn {
      border-radius: 6px;
      font-weight: bold;
      padding: 5px 30px;
    }
    .button-group {
      text-align: right;
    }
  }
}
.btn.square-button {
  border-radius: 5px;
}
</style>
