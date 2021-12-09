import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import Home from "@/pages/Home.vue";
import s404 from "@/pages/s404.vue";

Vue.use(VueRouter);

export const routes: RouteConfig[] = [
  {
    path: "/",
    name: "Home",
    component: Home,
  },
  {
    path: "/:path(.*)",
    name: "404",
    component: s404,
  },
];

const router = new VueRouter({
  base: "/",
  mode: "history",
  routes,
});

export default router;
