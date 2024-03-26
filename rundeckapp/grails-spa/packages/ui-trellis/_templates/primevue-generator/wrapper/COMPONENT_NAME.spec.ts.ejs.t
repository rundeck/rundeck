---
to: <%= h.path.join(`src/library/components/primeVue/${componentName}`, `${componentName}.spec.ts`) %>
unless_exists: true
---

import <%=componentName%> from './<%=componentName%>.vue';

describe('<%=componentName%>', () => {
  // TODO: Replace this test with a suite of tests appropriate for the component you are building.
  it('renders the provided content', () => {
    return true;
  });
});