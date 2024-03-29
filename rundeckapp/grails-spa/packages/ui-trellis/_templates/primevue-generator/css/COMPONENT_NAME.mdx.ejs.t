---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `Docs.mdx`) %>
unless_exists: true
---

import { Meta, Subtitle, Title, Canvas, Controls, Description, Stories } from "@storybook/blocks";
import * as <%=componentName%>Stories from './<%=componentName%>.stories'

<Meta of={<%=componentName%>Stories} />

<Title />

<Subtitle />
[Component reference on primevue](https://primevue.org/<%=componentName%>/)

## Import

```ts
import <%= h.changeCase.pascal(componentName) %> from "primevue/<%= h.changeCase.lower(componentName) %>";
```

## Playground

<Canvas of={<%=componentName%>Stories.Playground} />
<Controls of={<%=componentName%>Stories.Playground} />

## How to use

// Add a note here about the relevant props to change appearance of component


<Description of={<%=componentName%>Stories} />

<Stories title='' includePrimary={false} />


## Accessibility

// Bring in accessibility notes from primeVue here and/or add aspects important for accessibility