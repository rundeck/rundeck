import { mount } from "@vue/test-utils";
import NodeDetailsSimple from "../NodeDetailsSimple.vue";
jest.mock("@/library/rundeckService", () => ({
  getRundeckContext: jest.fn().mockReturnValue({
    rdBase: "mockRdBase",
    projectName: "test-project",
  }),
  url: jest.fn().mockReturnValue({ href: "mockHref" }),
}));

const mockAttributes = {
  osFamily: "Linux",
  osName: "Ubuntu",
  osVersion: "20.04",
  osArch: "x86_64",
  description: "Test Node 1",
  "ui:status:text": "Healthy",
  "ui:status:icon": "fa-check",
  "ns1:attr1": "value1",
  "ns1:attr2": "value2",
  "ns1:attr3": "value3",
  "ns1:attr4": "value4",
  "namespace:key1": "value1",
  "namespace:key2": "value2",
  key3: "value3",
  key4: "value4",
  key5: "value5",
  key6: "value6",
  username: "user",
  hostname: "host",
};

const mountNodeDetailsSimple = async (propsData = {}) => {
  const wrapper = mount(NodeDetailsSimple, {
    props: {
      useNamespace: true,
      nodeColumns: true,
      attributes: mockAttributes,
      tags: ["Tag1", "Tag2"],
      ...propsData,
    },
    global: {
      mocks: {
        $t: (msg) => msg,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("NodeDetailsSimple Component", () => {
  it.each([
    {
      description: "with useNamespace and nodeColumns set to false",
      useNamespace: false,
      nodeColumns: false,
      expectedPlusCount: 19,
      expectedMinusCount: 19,
      expectedChevronCount: 0,
    },
    {
      description: "with useNamespace and nodeColumns set to true",
      useNamespace: true,
      nodeColumns: true,
      expectedPlusCount: 4,
      expectedMinusCount: 4,
      expectedChevronCount: 3,
    },
  ])(
    "renders node attributes correctly with useNamespace set to $useNamespace and nodeColumns set to $nodeColumns",
    async ({
      useNamespace,
      nodeColumns,
      expectedPlusCount,
      expectedMinusCount,
      expectedChevronCount,
    }) => {
      const wrapper = await mountNodeDetailsSimple({
        useNamespace,
        nodeColumns,
        showExcludeFilterLinks: true,
      });
      // General attributes
      const generalAttributes = {
        description: "Test Node 1",
        "ui:status:text": "Healthy",
        "ui:status:icon": "fa-check",
        osFamily: "Linux",
        osName: "Ubuntu",
        osVersion: "20.04",
        osArch: "x86_64",
      };
      Object.entries(generalAttributes).forEach(([key, value]) => {
        const attributeElement = wrapper.find(`td.value:contains(${value})`);
        expect(attributeElement.exists()).toBe(true);
      });
      // Attributes without namespaces
      const attributesWithNoNamespaces = {
        key3: "value3",
        key4: "value4",
        key5: "value5",
        key6: "value6",
      };
      Object.entries(attributesWithNoNamespaces).forEach(([key, value]) => {
        const attributeElement = wrapper.find(`td.setting:contains(${value})`);
        expect(attributeElement.exists()).toBe(true);
      });
      // Check for .glyphicon-plus elements related to attributes without namespaces
      const plusLinks = wrapper.findAll(".setting .glyphicon-plus");
      expect(plusLinks.length).toBe(expectedPlusCount);
      // Check for .glyphicon-minus elements related to attributes without namespaces
      const minusLinks = wrapper.findAll(".setting .glyphicon-minus");
      expect(minusLinks.length).toBe(expectedMinusCount);
      // Attributes with namespaces (only when useNamespace is true)
      if (useNamespace) {
        const attributesWithNamespaces = {
          "namespace:key1": "value1",
          "namespace:key2": "value2",
        };
        Object.entries(attributesWithNamespaces).forEach(([key, value]) => {
          const attributeElement = wrapper.find(
            `tbody.subattrs .value:contains(${value})`,
          );
          expect(attributeElement.exists()).toBe(true);
        });
        // Check for glyphicon-chevron to ensure proper rendering of namespaces
        const chevrons = wrapper.findAll(".namespace .auto-caret");
        expect(chevrons.length).toBe(expectedChevronCount);
      }
    },
  );

  it("renders tags", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const tags = wrapper.findAll(".label-muted");
    expect(tags.length).toBe(2);
    expect(tags.at(0).text()).toContain("Tag1");
    expect(tags.at(1).text()).toContain("Tag2");
  });

  it("renders expandable attributes using class", async () => {
    const wrapper = await mountNodeDetailsSimple();
    const toggleButton = wrapper.find(".textbtn");
    await toggleButton.trigger("click");
    await wrapper.vm.$nextTick();
    const expandedAttributes = wrapper.findAll(".hover-action-holder");
    expect(expandedAttributes.length).toBe(12); // expected output
  });

  it("renders node filter links when useDefaultColumns is true", async () => {
    const wrapper = await mountNodeDetailsSimple({
      useDefaultColumns: true,
    });
    const usernameLink = wrapper.find(
      "[data-testid='node-attribute-link-username']",
    );
    const hostnameLink = wrapper.find(
      "[data-testid='node-attribute-link-hostname']",
    );
    await usernameLink.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    expect(wrapper.emitted().filter[0][0]).toEqual({
      filter: 'username: "user"',
    });
    await hostnameLink.trigger("click");
    expect(wrapper.emitted().filter).toBeTruthy();
    expect(wrapper.emitted().filter.length).toBe(2);
    expect(wrapper.emitted().filter[1][0]).toEqual({
      filter: 'hostname: "host"',
    });
  });
});
