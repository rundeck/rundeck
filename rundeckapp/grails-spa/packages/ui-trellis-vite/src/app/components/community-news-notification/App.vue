<template>
  <div>
    <i class="far fa-newspaper" style="margin-right:5px;"></i>
    <span>{{ $t("message.communityNews")}}</span>
    <span v-if="count > 0" class="newNotice">&nbsp;</span>
  </div>
</template>

<script>
import axios from "axios";
import Trellis, {
  getRundeckContext,
  getSynchronizerToken,
  RundeckBrowser
} from "@rundeck/ui-trellis";

export default {
  name: "CommunityNewsNotification",
  data() {
    return {
      RundeckContext: null,
      count: 0
    };
  },
  methods: {},
  mounted() {
    this.RundeckContext = getRundeckContext();
    let cookie = this.$cookies.get("communityNews");
    let mostRecentPost = this.$cookies.get("communityNewsMostRecentPost");
    if (cookie === null) {
      this.pollCommunityNews().then(result => {
        // set the cookie
        if (mostRecentPost && result.blogs.length) {
          let lastBlogPublished = parseInt(mostRecentPost);

          if (result.blogs[0].publish_date > lastBlogPublished) {
            this.setCookie(result.count);
          }
        } else {
          this.setCookie(result.count);
        }
      });
    } else if (cookie === "false") {
      // News has been reviewed,
      // There are no un-viewed news stories as the user has visited community news
      // Move on and do not poll for new news stories.
    } else {
      this.count = cookie;
    }
  },
  methods: {
    setCookie(count) {
      this.$cookies.set("communityNews", count, 60 * 60 * 24);
      this.count = count;
    },
    pollCommunityNews() {
      return new Promise(function(resolve, reject) {
        axios
          .get("https://api.rundeck.com/news/v1/blog/list", {
            params: {
              groupid: 7039074342
            }
          })
          .then(response => {
            resolve({
              blogs: response.data.objects,
              count: response.data.objects.length,
              lastTimestamp: response.data.objects[0].publish_date
            });
          });
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.newNotice {
  display: inline-block;
  width: 1em;
  height: 1em;
  border-radius: 50%;
  background-color: #737373;
  margin-left: 0.4em;
  line-height: 1em;
}
</style>
<style lang="scss">
.sidebar-mini {
  #community-news-notification {
    width: 79px;
    span {
      display: none;
    }
  }
}
</style>

