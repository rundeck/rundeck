<template>
  <div class="row">
    <div class="col-xs-12 col-sm-8">
      <span v-if="isInstalled || installed">
        <span @click="uninstall" class="btn btn-sm btn-danger">Uninstall</span>
        <span
          @click="install"
          class="btn btn-sm btn-warning"
          v-if="updateAvailable"
        >Update Available</span>
      </span>
      <span v-else>
        <span @click="install" class="btn btn-sm btn-info">Install</span>
      </span>
    </div>
    <div class="col-xs-12 col-sm-4">
      <a :href="pluginUrl" target="_blank" class="btn btn-sm btn-default pull-right">Docs</a>
    </div>
  </div>
</template>
<script>
import axios from "axios";
import _ from "lodash";
export default {
  name: "InstallButton",
  props: ["plugin", "installedPlugins", "installedPluginIds", "repo"],
  data() {
    return {
      installed: false
    };
  },
  computed: {
    isInstalled() {
      return this.installedPluginIds.includes(this.plugin.object_id);
    },
    updateAvailable() {
      if (this.isInstalled) {
        let plugin = _.find(this.installedPlugins, {
          artifactId: this.plugin.object_id
        });

        let installedPluginVersion = parseInt(
          plugin.version.replace(/\D/g, "")
        );
        let remotePluginVersion = parseInt(
          this.plugin.current_version.replace(/\D/g, "")
        );

        if (remotePluginVersion > installedPluginVersion) {
          return true;
        } else {
          return false;
        }
      }
    },
    pluginUrl() {
      return `https://online.rundeck.com/plugins/${this.plugin.post_slug}`;
    },
    sourceUrl() {
      if (this.plugin.source_link === " ") {
        return false;
      } else {
        return this.plugin.source_link;
      }
    }
  },
  methods: {
    install() {
      this.errors = null;
      const rdBase = window._rundeck.rdBase;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${rdBase}repository/${this.repo}/install/${
          this.plugin.object_id
        }`,
        withCredentials: true
      })
        .then(response => {
          this.installed = true;
        })
        .catch(error => {
          this.errors = error.response.data;
        });
    },
    uninstall() {
      this.errors = null;
      const rdBase = window._rundeck.rdBase;
      const apiVer = window._rundeck.apiVersion;
      axios({
        method: "post",
        headers: { "x-rundeck-ajax": true },
        url: `${rdBase}api/${apiVer}/plugins/uninstall/${
          this.plugin.object_id
        }`,
        withCredentials: true
      })
        .then(response => {
          this.installed = false;
        })
        .catch(error => {
          this.errors = error.response.data;
        });
    }
  }
};
</script>
<style lang="scss" scoped>
span {
  display: inline-block;
}
</style>
