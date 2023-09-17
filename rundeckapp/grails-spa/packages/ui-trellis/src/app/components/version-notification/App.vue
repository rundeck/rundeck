<template>
  <div
    @click="showNotificationModal=true"
    class="version-notification-container"
    v-show="showVersionNotification"
  >
    <div class="sidebar-footer-line-item">
      <i class="fas fa-exclamation-circle"></i>
      <span style="margin-left:5px">{{ $t("message_sidebarNotificationText")}}</span>
    </div>
    <modal
      v-model="showNotificationModal"
      :title="$t('message_updateAvailable')"
      append-to-body
      ref="modal"
    >
      <div>
        <p>{{$t("message_updateHasBeenReleased")}}</p>
        <p>
          {{$t("message_installedVersion")}}
          <strong>{{installedVersion.stringVersion}}</strong>.
        </p>
        <p>
          {{$t("message_currentVersion")}}
          <strong>{{currentReleaseVersion.stringVersion}}</strong>.
        </p>
        <p>This version was released {{formatReleaseDate(currentReleaseVersion.releaseDate)}}.</p>
        <a
          v-if="isOSSVersion"
          href="https://docs.rundeck.com/downloads.html"
          target="_blank"
          style="margin-bottom:1em;"
          class="btn btn-default btn-block btn-success btn-fill"
        >{{$t("message_getUpdate")}}</a>
        <a
          v-else
          href="https://download.rundeck.com"
          target="_blank"
          style="margin-bottom:1em;"
          class="btn btn-default btn-block btn-success btn-fill"
        >{{$t("message_getUpdate")}}</a>
        <p>
          <a
            style="cursor:pointer;"
            @click="hideNotificationForThisVersion"
          >{{$t("message_dismissMessage")}}</a>
        </p>
      </div>
      <template v-slot:footer>
        <div>
          <btn @click="showNotificationModal=false">{{$t("message_close")}}</btn>
        </div>
      </template>
    </modal>
  </div>
</template>

<script>
import axios from "axios";
import Trellis, {
  getRundeckContext,
  getSynchronizerToken,
  RundeckBrowser
} from "../../../library";
import {formatReleaseDate} from "@/app/utilities/DateTimeFormatters";

// import motd from '@/components/motd/motd'

export default {
  name: "VersionNotification",
  components: {
    // motd
  },
  data() {
    return {
      RundeckContext: null,
      isOSSVersion: true,
      showNotificationModal: false,
      installedVersion: {
        stringVersion: "",
        major: null,
        minor: null,
        patch: null
      },
      currentReleaseVersion: {
        stringVersion: "",
        major: null,
        minor: null,
        patch: null
      },
      showVersionNotification: false
    };
  },
  methods: {
      formatReleaseDate,
    hideNotificationForThisVersion() {
      Trellis.FilterPrefs.setFilterPref(
        "hideVersionUpdateNotification",
        this.currentReleaseVersion.stringVersion
      ).then(() => {
        this.showNotificationModal = false;
        this.showVersionNotification = false;
      });
    }
  },
  mounted() {
    this.RundeckContext = getRundeckContext();

    axios({
      method: "get",
      url: `https://api.rundeck.com/news/v1/release`
    }).then(
      response => {
        if (response.data && response.data[0])
          this.currentReleaseVersion = {
            stringVersion: response.data[0].name,
            major: response.data[0].version.major,
            minor: response.data[0].version.minor,
            patch: response.data[0].version.patch,
            releaseDate: response.data[0].version.date
          };
        if (
          window._rundeck.hideVersionUpdateNotification &&
          window._rundeck.hideVersionUpdateNotification ===
            this.currentReleaseVersion.stringVersion
        ) {
          return;
        } else {
          this.RundeckContext.rundeckClient.systemInfoGet().then(response => {
            this.installedVersion = returnVersionInformation(
              response.system.rundeckProperty.version
            );
            // Big If/Else checks are terrible
            // Exlaining this one
            // Let's check to see if we have a match between local and current
            // IF that matches, do nothing
            if (
              this.currentReleaseVersion.stringVersion ===
              this.installedVersion.stringVersion
            ) {
              return;
            } else {
              // CHECK against the major release int, is current bigger than installed?
              if (
                this.currentReleaseVersion.major > this.installedVersion.major
              ) {
                this.showVersionNotification = true;
              } else {
                // CHECK against the minor release int, is current bigger than installed?
                if (
                  this.currentReleaseVersion.minor > this.installedVersion.minor
                ) {
                  this.showVersionNotification = true;
                } else {
                  // CHECK against the patch release int, is current bigger than installed?
                  if (
                    this.currentReleaseVersion.patch >
                    this.installedVersion.patch
                  ) {
                    this.showVersionNotification = true;
                  }
                }
              }
            }
            this.isOSSVersion = (typeof _RDPRO_EDITION === 'undefined')
          });
        }
      },
      error => {
        // eslint-disable-next-line
        console.log("Error connecting to Rundeck Release API", error);
      }
    );
  }
};

function returnVersionInformation(versionString) {
  let returnObj = {};
  let seperatedVersionNumber = versionString.split("-");
  let splitVersionSchema = seperatedVersionNumber[0].split(".");

  returnObj.stringVersion = seperatedVersionNumber[0];
  returnObj.major = parseInt(splitVersionSchema[0]);
  returnObj.minor = parseInt(splitVersionSchema[1]);
  returnObj.patch = parseInt(splitVersionSchema[2]);

  return returnObj;
}
</script>

<style lang="scss" scoped>
.version-notification-container {
  color: white !important;
  background-color: #000000;
  border-bottom: 1px solid #3c3c3c;
  padding: 0.5em 0;
  text-align: left;
  cursor: pointer;
}
.modal-footer .btn {
  padding-bottom: 5px;
}
</style>

<style lang="scss">
.sidebar-mini #sidebar-bottom .version-notification-container {
  width: 79px;
  .sidebar-footer-line-item span {
    display: none;
  }
}
</style>
