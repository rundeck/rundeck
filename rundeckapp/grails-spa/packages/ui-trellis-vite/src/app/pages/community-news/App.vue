<template>
  <div>
    <div class="col-xs-12">
      <div class="card">
        <div class="card-header">
          <h2 class="card-title">
            {{$t("message.communityNews")}}
            <div class="pull-right">
              <newsletter-subscribe/>
            </div>
          </h2>
        </div>
        <div class="card-content">
          <community-news-feed/>
        </div>
        <div class="card-footer"></div>
      </div>
    </div>
  </div>
</template>

<script>
import CommunityNewsFeed from "./CommunityNewsFeed.vue";
import NewsletterSubscribe from "./NewsletterSubscribe.vue";

export default {
  name: "CommunityNews",
  components: {
    CommunityNewsFeed,
    NewsletterSubscribe
  },
  data() {
    return {
      RundeckContext: null
    };
  },
  methods: {},
  mounted() {}
};
</script>

<style lang="scss" scoped>
</style>

