<template>
  <div id="remoteEditholder" class="popout">
    <span id="remoteEditHeader">
      <span class="welcomeMessage">
        {{$t('node.remoteEdit.edit')}} <i class="fas fa-hdd"></i>
        <span id="editNodeIdent"> {{ nodename }}</span>
      </span>
    </span>
    <div v-if="!finished" class="toolbar" id="remoteEditToolbar" style="display: inline-block; margin-left: 4px;">
      <span
        class="action"
        @click="remoteEditCompleted"
        title="Close the remote edit box and discard any changes"
        >
          <img src="/static/images/icon-tiny-removex-gray.png" alt="Close remote editing" />
          Close remote editing
      </span>
    </div>
    <div v-else id="remoteEditResultHolder" class="info message">
      <span id="remoteEditResultText" class="info message">
        {{ $t(success? "node.changes.success" : "node.changes.notsaved") }}
      </span>
      <span class="action" @click="remoteEditContinue">
        {{ $t('node.remoteEdit.continue') }}
      </span>
    </div>
    <div v-if="error" id="remoteEditError" class="error note"></div>
    <div id="remoteEditTarget" v-if="remoteUrl">
      <iframe width="640" height="480" :src="remoteUrl" :key="remoteUrl" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { getRundeckContext } from "@/library";
import { getAppLinks } from "@/library";
let rundeckContext = getRundeckContext()

const PROTOCOL = 'rundeck:node:edit';


export default defineComponent({
  name: "NodeRemoteEdit",
  props: {
    nodename: {
      type: String,
      required: false
    },
    remoteUrl: {
      type: String,
      required: false,
    },
  },
  data() {
    return {
      remoteEditStarted: false,
      shouldRefresh: false,
      finished: false,
      success: false,
      error: null,
      remoteEditExpect: false,
      remoteSite: null,
      project: rundeckContext.projectName
    };
  },
  methods: {
    remoteEditContinue() {
      if (this.shouldRefresh) {
        document.location = getAppLinks().frameworkNodes
      } else {
        this.remoteEditCompleted();
      }
    },
    fnRemoteEditExpect(){
      this.remoteEditExpect = true;
      this.remoteSite = this.remoteUrl;
      this.remoteEditStarted = false;
      //@ts-ignore
      Event.observe(window, 'message', this._rdeckNodeEditOnmessage);
    },
    remoteEditCompleted() {
      this.remoteEditClear();
      this.remoteEditStop();
    },
    remoteEditHide(){
      this.finished = false;
    },
    remoteEditClear() {
      this.error = null;
    },
    remoteEditStop() {
      this.remoteEditExpect = false;
      this.remoteSite = null;
      //@ts-ignore
      Event.observe(window, 'message', this._rdeckNodeEditOnmessage);

      this.$emit("remoteEditStop");
    },
    _rdeckNodeEditOnmessage(msg) {
      if (!this.remoteEditExpect || !this.remoteSite || !this.remoteSite.startsWith(msg.origin + "/")) {
        return;
      }
      var data = msg.data;
      if (!this.remoteEditStarted && PROTOCOL + ':started' == data) {
        this.remoteEditStarted = true;
      } else if (PROTOCOL + ':error' == data || data.startsWith(PROTOCOL + ':error:')) {
        var err = data.substring((PROTOCOL + ':error').length);
        if (err.startsWith(":")) {
          err = err.substring(1);
        }
        this._rdeckNodeEditError(msg.origin, err ? err : "(No message)");
      } else if (this.remoteEditStarted) {
        if (PROTOCOL + ':finished:true' == data) {
          this._rdeckNodeEditFinished(true);
        } else if (PROTOCOL + ':finished:false' == data) {
          this._rdeckNodeEditFinished(false);
        } else {
          this._rdeckNodeEditError(null, "Unexpected message received from [" + msg.origin + "]: " + data);
        }
      }
    },
    _rdeckNodeEditError(origin, msg) {
      this.error = origin ? origin + " reported an error: " + msg : msg;
    },
    _rdeckNodeEditFinished(changed) {
      this.success = changed;
    }
  },
  mounted() {
    this.remoteEditClear();
    this.fnRemoteEditExpect();
  },
});
</script>