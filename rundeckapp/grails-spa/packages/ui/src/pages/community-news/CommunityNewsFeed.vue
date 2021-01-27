<template>
  <div>
    <div v-show="error">
      <h4>{{ $t("message.connectionError")}}</h4>
      <p>
        {{ $t("message.refresh") }}
        <a
          href="https://www.rundeck.com/community-updates"
          target="_blank"
        >Rundeck Community News</a>.
      </p>
    </div>
    <div v-show="!showLoading">
      <article
        v-for="(blog, index) in blogs"
        :key="index"
        class="blog-article"
        @click="openBlog(blog.absolute_url)"
      >
        <div class="row">
          <div class="col-xs-12 col-sm-9">
            <div style="padding-bottom:.5em;">
              <h6
                class="pub-date"
                style="padding-bottom:.6em; color: #989898;"
              >{{blog.publish_date | moment("MMMM Do YYYY hh:mm")}}</h6>
              <h3 class="blog-title">{{blog.title}}</h3>
            </div>
            <div>{{blog.meta_description}}</div>

            <!-- <div v-html="blog.post_summary" class="blog-summary"></div> -->
          </div>
          <div class="col-xs-12 col-sm-3">
            <img :src="blog.featured_image" alt class="img-responsive">
          </div>
        </div>
      </article>
      <div>
        <div class="articles-footer">
          <a
            href="https://www.rundeck.com/community-updates"
            target="_blank"
          >{{ $t("message.readMore")}}</a>
        </div>
      </div>
    </div>
    <div v-show="showLoading && !error" style="text-align: center;margin: 10vh 0;">
      <i class="fas fa-spinner fa-5x fa-pulse" style="color:#f7403a;"></i>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "CommunityNewsFeed",
  data() {
    return {
      showLoading: true,
      blogs: [],
      error: false
    };
  },
  methods: {
    openBlog(url) {
      let win = window.open(url, "_blank");
      win.focus();
    }
  },
  mounted() {
    axios
      .get("https://api.rundeck.com/news/v1/blog/list", {
        params: {
          groupid: 7039074342
        }
      })
      .then(
        response => {
          this.blogs = response.data.objects;
          this.showLoading = false;
          this.$cookies.set(
            "communityNewsMostRecentPost",
            this.blogs[0].publish_date,
            60 * 60 * 24 * 7
          );
          this.$cookies.set("communityNews", "false", 60 * 60 * 24);
        },
        error => {
          this.error = true;
          console.log("Error connecting to Rundeck Community News API", error);
        }
      );
  }
};
</script>

<style lang="scss" scoped>
.blog-article {
  cursor: pointer;
  border-top: 1px solid #cfcfca;
  padding-top: 1em;
  margin-top: 1em;
  &:first-of-type {
    margin-top: 0;
  }
  &:last-of-type {
    margin-bottom: 1em;
  }
  h1,
  h2,
  h3,
  h4,
  h5,
  h6 {
    margin: 0 !important;
    padding: 0;
  }
  .pub-date {
    margin-bottom: 0.5em;
  }
  .blog-title {
    line-height: 1em;
    margin-bottom: 0.2em;
    padding-left: 0.4em;
    border-left: 3px solid red;
  }
  .blog-subtitle {
    // font-size: 1.3em;
    font-weight: bold;
    text-transform: uppercase;
    margin-bottom: 1em;
  }
}

.article-link {
  cursor: pointer;
}
.article-link {
  margin-bottom: 0.5em;
  display: inline-block;
  width: 100%;
  border-top: 1px dotted #cecece;
  padding-top: 1em;
  &:first-of-type {
    border-top: 0;
    padding-top: 0;
  }
  &:last-of-type {
    margin-bottom: 0;
  }
}
.articles-footer {
  text-align: center;
  border-top: 1px solid #cfcfca;
  padding-top: 0.8em;
}
</style>
