<template>
  <div>
    <div v-show="!showLoading">
      <article
        v-for="(blog, index) in blogs"
        :key="index"
        class="blog-article"
        @click="openBlog(blog.absolute_url)"
      >
        <div class="row">
          <div class="col-xs-12">
            <h6 class="pub-date">{{blog.publish_date | moment("MMMM do YYYY")}}</h6>
          </div>
          <div class="col-xs-12 col-sm-3">
            <img :src="blog.featured_image" alt class="img-responsive">
          </div>
          <div class="col-xs-12 col-sm-9">
            <h3 class="blog-title">{{blog.title}}</h3>
            <h6 class="blog-subtitle">{{blog.meta_description}}</h6>
            <div v-html="blog.post_summary"></div>
          </div>
        </div>
      </article>
      <div>
        <div class="articles-footer">
          <a href="https://www.rundeck.com/community-updates">read more</a>
        </div>
      </div>
    </div>
    <div v-show="showLoading">
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
      blogs: []
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
      .get("https://api-stage.rundeck.com/spark/blog/list", {
        params: {
          groupid: 7039074342
        }
      })
      .then(response => {
        this.blogs = response.data.objects;
        this.showLoading = false;
      });
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
    margin: 0;
    padding: 0;
  }
  .pub-date {
    margin-bottom: 0.5em;
  }
  .blog-title {
    line-height: 1em;
    margin-bottom: 0.2em;
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
