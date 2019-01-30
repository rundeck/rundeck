<template>
  <div>Update Availabe</div>
</template>

<script>
import axios from "axios";
import Trellis, {
  getRundeckContext,
  getSynchronizerToken,
  RundeckBrowser
} from "@rundeck/ui-trellis";

export default {
  name: "VersionNotification",
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
      installIsCurrent: true
    };
  },
  methods: {
    dismissModal() {
      alert("dismiss modal");
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

      Trellis.FilterPrefs.getAvailableFilterPrefs().then(results => {
        console.log("filterPref", results);
      });

      // Trellis.FilterPrefs.setFilterPref('activeTourStep', this.stepIndex).then(() => {
      //     this.setProgress()
      //     this.removeIndicator()
      //     if (step.nextStepUrl) {
      //       window.location.replace(`${window._rundeck.rdBase}${step.nextStepUrl}`)
      //     }
      //   })

      // TODO
      // Make this real
      // Fake a get to API.Rundeck.com
      this.currentReleaseVersion = {
        stringVersion: "3.0.16-SNAPSHOT",
        major: 3,
        minor: 0,
        build: 16
      };

      if (
        this.currentReleaseVersion.stringVersion ===
        this.installedVersion.stringVersion
      ) {
        this.installIsCurrent = true;
      } else {
        if (this.currentReleaseVersion.major > this.installedVersion.major) {
          this.installIsCurrent = false;
        } else {
          if (this.currentReleaseVersion.minor > this.installedVersion.minor) {
            this.installIsCurrent = false;
          } else {
            if (
              this.currentReleaseVersion.build > this.installedVersion.build
            ) {
              this.installIsCurrent = false;
            }
          }
        }
      }
    });

    // axios({
    //   method: "get",
    //   url: `https://api.github.com/repos/rundeck/rundeck/releases`,
    //   params: {
    //     // projects: `${window._rundeck.projectName}`
    //   }
    // }).then(
    //   response => {
    //     console.log("response", response);
    //   },
    //   error => {
    //     console.log("error", error);
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
