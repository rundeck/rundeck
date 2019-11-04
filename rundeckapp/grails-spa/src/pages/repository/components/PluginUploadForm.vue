<template>
  <div>
    <div class="col-xs-12">
      <div class="input-group input-group-lg">
        <span class="input-group-addon" id="sizing-addon1">Choose a file</span>

        <span class="control-fileupload">
          <span class="label">{{fileName}}</span>
          <input
            class
            type="file"
            id="files"
            ref="files"
            multiple
            v-on:change="handleFilesUploads()"
          >
        </span>

        <span class="input-group-btn">
          <button class="btn btn-default" v-on:click="submitFiles()">Install</button>
        </span>
      </div>
      <!-- /input-group -->
    </div>
  </div>
</template>
<script>
import { mapState, mapActions } from "vuex";
import axios from "axios";
export default {
  name: "UploadPluginForm",
  computed: {
    fileName() {
      if (this.files && this.files[0] && this.files[0].name) {
        return this.files[0].name;
      } else {
        return "";
      }
    }
  },
  methods: {
    submitFiles() {
      // Initialize the form data and iteate over any
      // file sent over appending the files to the form data.
      let formData = new FormData();
      for (var i = 0; i < this.files.length; i++) {
        let file = this.files[i];
        formData.append("pluginFile", file);
      }
      this.$store.dispatch("overlay/openOverlay", {
        loadingMessage: "Installing",
        loadingSpinner: true
      });
      axios({
        method: "post",
        headers: {
          "x-rundeck-ajax": true,
          "Content-Type": "multipart/form-data"
        },
        data: formData,
        url: `${window._rundeck.rdBase}plugin/uploadPlugin`,
        withCredentials: true
      }).then(response => {
        this.$store.dispatch("overlay/openOverlay");
        if (response.data.err) {
          this.$alert({
            title: "Error Uploading",
            content: response.data.err
          });
        } else {
          this.$alert({
            title: "Plugin Installed",
            content: response.data.msg
          });
        }
      });
    },
    handleFilesUploads() {
      this.files = this.$refs.files.files;
    }
  },
  data() {
    return {
      files: ""
    };
  },
  created() {}
};
</script>
<style lang="scss" scoped>
/* input [type = file]
----------------------------------------------- */

input[type="file"] {
  display: block !important;
  right: 1px;
  top: 1px;
  height: 46px;
  opacity: 0;
  width: 100%;
  background: none;
  position: absolute;
  overflow: hidden;
  z-index: 2;
}

.control-fileupload {
  display: block;
  border: 1px solid #d6d7d6;
  background: #fff;
  // border-radius: 4px;
  width: 100%;
  height: 46px;
  line-height: 36px;
  padding: 0px 10px 2px 10px;
  overflow: hidden;
  position: relative;
  border-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;

  &:before,
  input,
  .label {
    cursor: pointer !important;
  }
  /* File upload button */
  &:before {
    padding: 4px 12px;
    margin-bottom: 0;
    margin-right: 10px;
    margin-top: 4px;
    font-size: 14px;
    line-height: 20px;
    color: #333333;
    text-align: center;
    text-shadow: 0 1px 1px rgba(255, 255, 255, 0.75);
    vertical-align: middle;
    cursor: pointer;
    background-color: #f5f5f5;
    background-image: linear-gradient(to bottom, #ffffff, #e6e6e6);
    background-repeat: repeat-x;
    border: 1px solid #cccccc;
    border-color: rgba(0, 0, 0, 0.1) rgba(0, 0, 0, 0.1) rgba(0, 0, 0, 0.25);
    border-bottom-color: #b3b3b3;
    border-radius: 4px;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.2),
      0 1px 2px rgba(0, 0, 0, 0.05);
    transition: color 0.2s ease;
    content: "Browse";
    display: block;
    position: absolute;
    z-index: 1;
    top: 2px;
    right: 2px;
    line-height: 20px;
    text-align: center;
  }
  &:hover,
  &:focus {
    &:before {
      color: #333333;
      background-color: #e6e6e6;
      color: #333333;
      text-decoration: none;
      background-position: 0 -15px;
      transition: background-position 0.2s ease-out;
    }
  }

  .label {
    line-height: 35px;
    color: #999999;
    font-size: 20px;
    font-weight: normal;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    position: relative;
    z-index: 1;
    margin-right: 90px;
    margin-bottom: 0px;
    margin-top: 4px;
    cursor: text;
  }
}
</style>
