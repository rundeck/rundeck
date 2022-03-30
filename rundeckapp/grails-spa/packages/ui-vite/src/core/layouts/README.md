# Layouts

It can be handy to not only define page components but also layout components that can be
reused across multiple pages. Instead of defining the contents of the page, as the name
suggests, these components define more the general layout. For instance, is it a one
column or a 2 column page? Does it have a left sidebar or right sidebar? Does the layout
include the typical header and footer or is it a completely blank layout maybe with the
page content absolutely centered? Usually there are only 2 or 3 of these layout components
but nonetheless they can be handy abstraction to have.

By default, `DefaultLayout.vue` will be used unless an alternative is specified in
the route meta.

With [`vite-plugin-pages`](https://github.com/hannoeru/vite-plugin-pages) and
[`vite-plugin-vue-layouts`](https://github.com/JohnCampionJr/vite-plugin-vue-layouts),
you can specify the layout in the page's SFCs like this:

```html
<route lang="yaml">
meta:
  layout: HomeLayout
</route>
```
