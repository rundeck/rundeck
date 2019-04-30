<template>
  <div class="card result">
    <div class="card-header">
      <div class="card-title">
        <h4>{{result.display || result.name}}</h4>
        <span class="current-version-number label label-default">{{result.currentVersion}}</span>
      </div>
    </div>
    <div class="card-content">
      <div v-html="result.description"></div>
      <div v-if="result.artifactType">
        <label>Plugin Type:</label>
        {{result.artifactType}}
      </div>
      <!-- <div>
                  <label>Plugin Version:</label>
                  {{result.currentVersion}}
      </div>-->
      <div v-if="result.rundeckCompatibility">
        <strong>Requires Rundeck {{result.rundeckCompatibility}}</strong>
      </div>
      <!-- <div>
                  <label>Author:</label>
                  {{result.author}}
      </div>-->

      <ul class="provides">
        <li
          v-for="(svc, index) in unqSortedSvcs(result.providesServices)"
          :key="index"
          class="label label-default"
        >{{svc}}</li>
      </ul>
      <div>
        <span class="label label-default" v-for="(tag, index) in result.tags" :key="index">{{tag}}</span>
      </div>
    </div>
    <div class="card-footer">
      <div class="button-group">
        <a
          class="btn btn-md btn-block btn-success btn-fill"
          v-if="!result.installed && canInstall && result.installId"
          @click="handleInstall"
        >Install</a>
        <a
          v-if="result.installed"
          @click="handleUninstall"
          class="btn btn-md btn-block btn-fill btn-danger"
        >Uninstall</a>
      </div>
      <div class="support-type">{{result.support}}</div>
    </div>
  </div>
</template>
<script>
import { mapState, mapActions } from "vuex";

export default {
  name: "PluginCard",
  props: ["result", "repo"],
  computed: {
    ...mapState(["canInstall", "rdBase"])
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
.plugin-types {
  list-style: none;
  margin: 0;
  padding: 0;
  li {
    display: inline-block;
    margin-right: 1em;
    margin-bottom: 1em;
  }
}
.card {
  .card-header {
    .card-title {
      h4 {
        margin: 0;
      }
    }
  }
}
.card.result {
  .card-header {
    .card-title {
      h4 {
        margin: 0;
      }
      .current-version-number {
        position: absolute;
        right: -30px;
        top: -30px;
        font-size: 14px;
      }
    }
  }
  .card-content {
    min-height: 300px;
    .provides {
      margin: 1em 0 0;
      padding: 0;
      li {
        padding-top: 5px;
        margin-right: 1em;
        margin-bottom: 1em;
      }
    }
  }
  .card-footer {
    position: absolute;
    bottom: 0;
    width: 100%;
    .button-group {
      margin-bottom: 1em;
      .btn {
        border-radius: 6px;
      }
    }
    .support-type {
      background: black;
      color: white;
      text-align: center;
      font-weight: bold;
      padding: 1em;
      margin: 0 -15px -15px;
      border-bottom-left-radius: 6px;
      border-bottom-right-radius: 6px;
    }
  }
}
</style>
