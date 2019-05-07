<template>
  <div v-show="displayCard">
    <div class="card result flex-col">
      <div class="card-header">
        <span class="current-version-number label label-default">{{result.currentVersion}}</span>
        <h5 class="support-type">{{result.support}}</h5>
        <h3 class="card-title">{{result.display || result.name}}</h3>
        <h5 class="requires-rundeck-version">
          <span v-if="result.support === 'Enterprise Exclusive'">Requires Enterprise</span>
          <span v-if="result.rundeckCompatibility">Requires Rundeck {{result.rundeckCompatibility}}</span>
        </h5>
      </div>
      <div class="card-content flex-grow">
        <div class="flexible">
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
          <div class="col-xs-12 col-sm-6">
            <div class="links fa-2x">
              <a v-if="result.sourceLink" :href="result.sourceLink" target="_blank">
                <i class="fas fa-file-code"></i>
              </a>
              <a
                v-if="result.record && result.record.post_slug "
                :href="`https://online.rundeck.com/plugins/${result.record.post_slug}`"
                target="_blank"
              >
                <i class="fas fa-file-alt"></i>
              </a>
              <a v-if="result.docsLink" :href="result.docsLink" target="_blank">
                <i class="fas fa-file-alt"></i>
              </a>
            </div>
          </div>
          <div class="col-xs-12 col-sm-6">
            <div class="button-group">
              <a
                class="btn btn-lg btn-success"
                v-if="!result.installed && canInstall && result.installId"
                @click="handleInstall"
              >Install</a>
              <a
                v-if="result.installed"
                @click="handleUninstall"
                class="btn btn-lg btn-danger"
              >Uninstall</a>
            </div>
          </div>
        </div>
        <div class="col-xs-12" v-else>
          <a
            href="https://www.rundeck.com/test-drive"
            target="_blank"
            class="btn btn-default btn-lg btn-block"
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
    ...mapState([
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
    ...mapActions(["installPlugin", "uninstallPlugin"]),
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
    padding: 3em 2em 2em;
    border-radius: 7px 7px 0 0;
    .card-title {
      margin: 0;
      color: white;
      font-weight: bold;
      font-size: 2em;
    }
    .support-type {
      color: white;
      // font-weight: bold;
      margin: 0 0 0.25em;
      // text-transform: uppercase;
    }
    .current-version-number {
      position: absolute;
      right: 1em;
      top: 1em;
      padding: 0.2em 1em;
      font-size: 18px;
      border-radius: 20px;
    }
    .requires-rundeck-version {
      color: #f7403a;
      // text-transform: capitalize;
      // font-weight: bold;
      margin: 0.7em 0 0;
      height: 20px;
    }
  }
  .card-content {
    padding: 2em 2em 1em;
    // min-height: 250px;
    .provides {
      list-style: none;
      margin: 2em 0 0;
      padding: 0;
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
    margin-bottom: 1em;
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
</style>
<style lang="scss">
.card.result .card-content p {
  font-size: 1.3em;
  line-height: 1.2em;
  // font-weight: bold;
}
</style>
