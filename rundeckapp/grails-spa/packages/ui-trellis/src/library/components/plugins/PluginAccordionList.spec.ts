import { mount, VueWrapper } from "@vue/test-utils";
import PluginAccordionList from "./PluginAccordionList.vue";

const provider = (name: string) => ({
  name,
  title: `${name} title`,
  description: `${name} description`,
});

const groupedProviders = {
  highlighted: {
    "Command step": { isGroup: false, providers: [provider("exec-command")] },
  },
  nonHighlighted: {
    Ansible: {
      isGroup: true,
      providers: [provider("ansible-playbook"), provider("ansible-inline")],
    },
  },
};

const stepTypeAttrs = (p: { name: string }) => ({
  "data-node-step-type": p.name,
});

const createWrapper = (props = {}): VueWrapper<any> =>
  mount(PluginAccordionList, {
    props: {
      groupedProviders,
      commonStepsHeading: "Common steps",
      ...props,
    },
    global: {
      stubs: { PluginIcon: true },
    },
  });

describe("PluginAccordionList", () => {
  it("binds the providerAttrs result on a single provider header", () => {
    const wrapper = createWrapper({ providerAttrs: stepTypeAttrs });

    const headers = wrapper.findAll('[data-testid="plugin-accordion-header"]');

    expect(headers[0].attributes("data-node-step-type")).toBe("exec-command");
  });

  it("does not bind provider attributes on a group header", () => {
    const wrapper = createWrapper({ providerAttrs: stepTypeAttrs });

    const headers = wrapper.findAll('[data-testid="plugin-accordion-header"]');

    expect(headers[1].attributes("data-node-step-type")).toBeUndefined();
  });

  it("renders headers without provider attributes when providerAttrs is not set", () => {
    const wrapper = createWrapper();

    const headers = wrapper.findAll('[data-testid="plugin-accordion-header"]');

    expect(headers).toHaveLength(2);
    expect(headers[0].attributes("data-node-step-type")).toBeUndefined();
  });

  it("emits select with the group and key when a header is clicked", async () => {
    const wrapper = createWrapper({ providerAttrs: stepTypeAttrs });

    await wrapper
      .findAll('[data-testid="plugin-accordion-header"]')[0]
      .trigger("click");

    expect(wrapper.emitted("select")).toEqual([
      [
        {
          group: groupedProviders.highlighted["Command step"],
          key: "Command step",
        },
      ],
    ]);
  });
});
