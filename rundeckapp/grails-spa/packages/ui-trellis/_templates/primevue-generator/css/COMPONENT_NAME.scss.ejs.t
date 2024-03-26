---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${h.changeCase.lower(componentName)}.scss`) %>
unless_exists: true
---

.p-<%= h.changeCase.lower(componentName) %> {
    background: var(--colors-);
<% if(variants) { %>
    <% variants.split(',').forEach(variant => { %>
    &--<%= variant %> {
        background: var(--colors-);
    }
    <% }) %>
<% } -%>
}