<template>
  <div class="container-fluid" v-show="showVersionNotification">
    <div class="row">
      <div class="col-xs-12">
        <div class="alert alert-danger">
          <div class="row">
            <div class="col-xs-12 col-sm-1">
              <span>
                <i class="fas fa-exclamation-circle fa-5x"></i>
              </span>
            </div>
            <div class="col-xs-12 col-sm-10 notification-text">
              <h3>There is a newer version of Rundeck available.</h3>
              <p>Your version of Rundeck ({{installedVersion.stringVersion}}) is older than the current version ({{currentReleaseVersion.stringVersion}}).</p>

              <a
                href="https://docs.rundeck.com/downloads.html"
                class="btn btn-default btn-white"
              >Please update here</a>
            </div>
            <div class="col-xs-12 col-sm-1">
              <button @click="dismissModal" type="button" class="close">
                <i class="fas fa-times-circle"></i>
              </button>
            </div>
          </div>
          <!-- <button type="button" class="close">
            <i class="fas fa-times-circle"></i>
          </button>-->
          <!-- <span class="icon" data-notify="icon">
            <i class="fas fa-exclamation-circle fa-2x"></i>
          </span>-->
          <!-- <span class="message">
            <h3>There is a newer version of Rundeck available.</h3>
            Your version of Rundeck ({{installedVersion.stringVersion}}) is older than the current version ({{currentReleaseVersion.stringVersion}}).
            <a
              href="https://www.rundeck.com/"
              class="btn btn-default btn-block"
            >Please update here</a>.
          </span>-->
        </div>
      </div>
      <modal v-model="showVersionNotificationDismissModal" title="Dismiss Notification" ref="modal">
        <div>Dismiss the update availablity notification until the next release?</div>
        <div slot="footer">
          <btn @click="hideNotificationForThisVersion">Dismiss</btn>
          <btn class="pull-right" @click="showVersionNotificationDismissModal=false">Close</btn>
        </div>
      </modal>
    </div>
  </div>
</template>

<script>
import axios from "axios";
import Trellis, {
  getRundeckContext,
  getSynchronizerToken,
  RundeckBrowser
} from "@rundeck/ui-trellis";
import { setTimeout } from "timers";

console.log("Trellis", Trellis);

// import motd from '@/components/motd/motd'

export default {
  name: "VersionNotification",
  components: {
    // motd
  },
  data() {
    return {
      RundeckContext: null,
      showVersionNotificationDismissModal: false,
      installedVersion: {
        stringVersion: "",
        major: null,
        minor: null,
        build: null
      },
      currentReleaseVersion: {
        stringVersion: "",
        major: null,
        minor: null,
        build: null
      },
      showVersionNotification: false
    };
  },
  methods: {
    dismissModal() {
      this.showVersionNotificationDismissModal = true;
    },
    hideNotificationForThisVersion() {
      Trellis.FilterPrefs.setFilterPref(
        "hideVersionUpdateNotification",
        this.currentReleaseVersion.stringVersion
      ).then(() => {
        this.showVersionNotificationDismissModal = false;
        this.showVersionNotification = false;
      });
    }
  },
  mounted() {
    this.RundeckContext = getRundeckContext();

    this.RundeckContext.rundeckClient.userProfileGet().then(response => {
      console.log("this is the user", response);
    });

    this.RundeckContext.rundeckClient.systemInfoGet().then(response => {
      console.log("response", response);
      this.installedVersion = returnVersionInformation(
        response.system.rundeckProperty.version
      );

      // TODO
      // Make this real
      // Fake a get to API.Rundeck.com
      this.currentReleaseVersion = {
        stringVersion: "3.0.18-SNAPSHOT",
        major: 3,
        minor: 0,
        build: 18
      };
      // Big If/Else checks are terrible
      // Exlaining this one
      // IF filterPrefs('hideVersionUpdateNotication') has been set, check that against the version that's most recent coming from the API
      // IF it matches, do nothing, move on. Done.
      if (
        window._rundeck.hideVersionUpdateNotification &&
        window._rundeck.hideVersionUpdateNotification ===
          this.currentReleaseVersion.stringVersion
      ) {
        return;
      } else {
        // ELSE let's check to see if we have a match between local and current
        // IF that matches, do nothing
        if (
          this.currentReleaseVersion.stringVersion ===
          this.installedVersion.stringVersion
        ) {
          return;
        } else {
          // CHECK against the major release int, is current bigger than installed?
          if (this.currentReleaseVersion.major > this.installedVersion.major) {
            this.showVersionNotification = true;
          } else {
            // CHECK against the minor release int, is current bigger than installed?
            if (
              this.currentReleaseVersion.minor > this.installedVersion.minor
            ) {
              this.showVersionNotification = true;
            } else {
              // CHECK against the build release int, is current bigger than installed?
              if (
                this.currentReleaseVersion.build > this.installedVersion.build
              ) {
                this.showVersionNotification = true;
              }
            }
          }
        }
      }
    });

    // axios({
    //   method: "get",
    //   url: `https://api-stage.rundeck.com/spark/v1/release`
    // }).then(
    //   response => {
    //     console.log(" axios response", response);
    //   },
    //   error => {
    //     console.log("axios error", error);
    //   }
    // );
  }
};

function returnVersionInformation(versionString) {
  let returnObj = {};
  let seperatedVersionNumber = versionString.split("-");
  let splitVersionSchema = seperatedVersionNumber[0].split(".");

  returnObj.stringVersion = seperatedVersionNumber[0];
  returnObj.major = parseInt(splitVersionSchema[0]);
  returnObj.minor = parseInt(splitVersionSchema[1]);
  returnObj.build = parseInt(splitVersionSchema[2]);

  return returnObj;
}
</script>

<style lang="scss" scoped>
.alert.alert-danger {
  color: white !important;
  .notification-text {
    h3 {
      margin: 0;
    }
    .btn-white {
      color: white;
      border-color: white;
      &:hover,
      &:active {
        background-color: white;
        color: #ff8f5e;
      }
    }
  }
}
</style>
