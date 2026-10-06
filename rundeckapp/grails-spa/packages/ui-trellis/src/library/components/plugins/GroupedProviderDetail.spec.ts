import { mount, VueWrapper } from "@vue/test-utils";
import GroupedProviderDetail from "./GroupedProviderDetail.vue";

const providers = [
  { name: "ansible-playbook", title: "Ansible Playbook", description: "Run" },
  { name: "ansible-inline", title: "Ansible Inline", description: "Inline" },
];

const stepTypeAttrs = (p: { name: string }) => ({
  "data-node-step-type": p.name,
});

const createWrapper = (props = {}): VueWrapper<any> =>
  mount(GroupedProviderDetail, {
    props: {
      group: { isGroup: true, providers },
      groupName: "Ansible",
      serviceTypeLabel: "Node Step",
      ...props,
    },
    global: {
      stubs: { PluginIcon: true },
    },
  });

describe("GroupedProviderDetail", () => {
  it("binds the providerAttrs result on each provider header", () => {
    const wrapper = createWrapper({ providerAttrs: stepTypeAttrs });

    const headers = wrapper.findAll('[data-testid="grouped-provider-header"]');

    expect(headers.map((h) => h.attributes("data-node-step-type"))).toEqual([
      "ansible-playbook",
      "ansible-inline",
    ]);
  });

  it("renders provider headers without extra attributes when providerAttrs is not set", () => {
    const wrapper = createWrapper();

    const headers = wrapper.findAll('[data-testid="grouped-provider-header"]');

    expect(headers).toHaveLength(2);
    expect(headers[0].attributes("data-node-step-type")).toBeUndefined();
  });
});
