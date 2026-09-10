import { flushPromises, mount } from "@vue/test-utils";
import ProjectSelect from "../ProjectSelect.vue";

jest.mock("../../../../rundeckService", () => ({
  getAppLinks: jest.fn(() => ({
    menuHome: "http://localhost",
  })),
  url: jest.fn((path: string) => ({ href: `http://localhost${path}` })),
}));

jest.mock("perfect-scrollbar", () =>
  jest.fn().mockImplementation(() => ({
    update: jest.fn(),
    destroy: jest.fn(),
  })),
);

beforeAll(() => {
  window._rundeck = {
    rootStore: {
      projects: {
        loaded: true,
        projects: [{ name: "Project A", label: "Project A Label" }] as any,
        search: jest.fn(
          () => [{ name: "Project A", label: "Project A Label" }] as any,
        ),
        load: jest.fn(),
      },
    },
  } as any;
});

afterAll(() => {
  Reflect.deleteProperty(window, "_rundeck");
});

const createWrapper = async () => {
  const wrapper = mount(ProjectSelect, {
    props: {
      mode: "multi",
      selectedProjects: [],
    },
    global: {
      stubs: {
        Skeleton: {
          template: "<div><slot /></div>",
        },
        RecycleScroller: {
          props: ["items"],
          template:
            '<div><slot v-for="item in items" :item="item" :key="item.name" /></div>',
        },
      },
    },
  });

  await flushPromises();
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("ProjectSelect keyboard support", () => {
  it("emits a selection update when pressing Enter on a project item", async () => {
    const wrapper = await createWrapper();

    await wrapper
      .find('[data-testid="projectItemProject A"]')
      .trigger("keydown.enter");

    expect(wrapper.emitted("update:selection")).toHaveLength(1);
    expect(wrapper.emitted("update:selection")?.[0][0]).toEqual(["Project A"]);
  });
});
