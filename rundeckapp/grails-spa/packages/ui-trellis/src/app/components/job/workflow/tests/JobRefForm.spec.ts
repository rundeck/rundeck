import mitt from "mitt";
import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import { createTestingPinia } from "@pinia/testing";
import { useNodesStore } from "../../../../../library/stores/NodesStorePinia";
import JobRefForm from "../JobRefForm.vue";

jest.mock("../../../../../library/services/api", () => ({
  api: {
    get: jest.fn(),
    post: jest.fn(),
  },
  rundeckService: {
    baseURL: "http://localhost:4440/api/41/",
    headers: {
      "X-Rundeck-ajax": "true",
      Accept: "application/json",
    },
  },
}));

jest.mock("../../../../../library", () => ({
  getRundeckContext: jest.fn().mockImplementation(() => ({
    rdBase: "http://localhost:4440/",
    projectName: "TestProject",
    rootStore: {
      projects: {
        loaded: true,
        projects: [{ name: "TestProject" }, { name: "OtherProject" }],
      },
    },
    eventBus: mitt(),
    data: {
      nodeData: {},
    },
  })),
}));

const createWrapper = async (
  props = {},
  piniaOptions = {},
): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobRefForm, {
    props: {
      modelValue: {
        description: "",
        nodeStep: false,
        jobref: {
          name: "",
          project: "TestProject",
          group: "",
          args: "",
          failOnDisable: false,
          childNodes: false,
          nodefilters: {
            filter: "",
            dispatch: {
              threadcount: null,
              keepgoing: null,
              rankAttribute: null,
              rankOrder: null,
              nodeIntersect: null,
            },
          },
        },
        ...props,
      },
      modalActive: true,
    },
    global: {
      plugins: [
        createTestingPinia({
          createSpy: jest.fn(),
          ...piniaOptions,
        }),
      ],
      stubs: {
        Modal: {
          template: '<div><slot></slot><slot name="footer"></slot></div>',
        },
        NodeFilterInput: true,
        NodeListEmbed: true,
        UiSocket: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};

describe("JobRefForm", () => {
  describe("Form Initialization", () => {
    it("renders with default values", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.find("#useNameFalse").exists()).toBe(true);
      expect(wrapper.find("#useNameTrue").exists()).toBe(true);
      expect(wrapper.find("#jobProjectField").exists()).toBe(true);
    });

    it("displays project options correctly", async () => {
      const wrapper = await createWrapper();
      const options = wrapper.findAll("#jobProjectField option");

      expect(options).toHaveLength(2);
      expect(options[0].text()).toContain(
        "step.type.jobreference.project.label",
      );
      expect(options[1].text()).toBe("OtherProject");
    });
  });

  describe("Job Reference Type Selection", () => {
    it("toggles input fields based on name/uuid selection", async () => {
      const wrapper = await createWrapper();

      // Initially, UUID fields should be editable and name fields readonly
      expect(
        wrapper.find("[data-testid='jobNameField']").attributes("readonly"),
      ).toBeDefined();
      expect(
        wrapper.find("[data-testid='jobGroupField']").attributes("readonly"),
      ).toBeDefined();
      expect(
        wrapper.find("[data-testid='jobUuidField']").attributes("readonly"),
      ).toBeUndefined();

      // Switch to name-based reference
      await wrapper.find("#useNameTrue").setValue(true);

      // Now name fields should be editable and UUID readonly
      expect(
        wrapper.find("[data-testid='jobNameField']").attributes("readonly"),
      ).toBeUndefined();
      expect(
        wrapper.find("[data-testid='jobGroupField']").attributes("readonly"),
      ).toBeUndefined();
      expect(
        wrapper.find("[data-testid='jobUuidField']").attributes("readonly"),
      ).toBeDefined();
    });
  });

  describe("Form Validation", () => {
    it("shows error when trying to save without required fields", async () => {
      const wrapper = await createWrapper();

      await wrapper.find('[data-testid="save-button"]').trigger("click");

      expect(wrapper.find(".alert-danger").exists()).toBe(true);
      expect(wrapper.find(".alert-danger").text()).toContain("blank.message");
    });

    it("emits save event when form is valid", async () => {
      const wrapper = await createWrapper();

      await wrapper.find("[data-testid='jobUuidField']").setValue("test-uuid");
      await wrapper
        .find("[data-testid='jobProjectField']")
        .setValue("test-job");
      await wrapper.find('[data-testid="save-button"]').trigger("click");
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("save")).toBeTruthy();
      expect(wrapper.emitted("update:modelValue")).toBeTruthy();
    });
  });

  describe("Node Filter Section", () => {
    it("expands node filter section when clicked", async () => {
      const wrapper = await createWrapper(
        {},
        {
          initialState: {
            projects: {
              loaded: true,
              projects: [{ name: "TestProject" }, { name: "OtherProject" }],
            },
          },
        },
      );

      expect(
        wrapper.find("[data-testid='nodeFilterContainer']").classes("in"),
      ).toBe(false);

      await wrapper
        .find("[data-testid='expandNodeFilterBtn']")
        .trigger("click");
      await wrapper.vm.$nextTick();

      expect(
        wrapper.find("[data-testid='nodeFilterContainer']").classes("in"),
      ).toBe(true);
    });

    it("enables/disables node filter options based on filter presence", async () => {
      const wrapper = await createWrapper({
        jobref: {
          nodefilters: {
            filter: "",
          },
        },
      });

      expect(
        wrapper
          .find("[data-testid='nodeThreadcountField']")
          .attributes("disabled"),
      ).toBeDefined();
      expect(
        wrapper
          .find("[data-testid='nodeKeepgoingTrue']")
          .attributes("disabled"),
      ).toBeDefined();

      await wrapper.setProps({
        modelValue: {
          jobref: {
            nodefilters: {
              filter: "some-filter",
            },
          },
        },
      });

      expect(
        wrapper
          .find("[data-testid='nodeThreadcountField']")
          .attributes("disabled"),
      ).toBeUndefined();
      expect(
        wrapper
          .find("[data-testid='nodeKeepgoingTrue']")
          .attributes("disabled"),
      ).toBeUndefined();
    });
  });

  describe("Job Selection Modal", () => {
    it("opens job selection modal when choose button is clicked", async () => {
      const wrapper = await createWrapper();

      await wrapper.find(".act_choose_job").trigger("click");

      expect(wrapper.vm.openJobSelectionModal).toBe(true);
    });

    it("updates job reference when job is selected", async () => {
      const wrapper = await createWrapper();
      const mockJob = {
        id: "test-id",
        jobName: "Test Job",
        groupPath: "Test/Group",
      };

      // Simulate the event bus event that would come from job selection
      wrapper.vm.eventBus.emit("browser-job-item-selection", mockJob);
      await wrapper.vm.$nextTick();

      expect(
        (
          wrapper.find("[data-testid='jobUuidField']")
            .element as HTMLInputElement
        ).value,
      ).toBe("test-id");
      expect(
        (
          wrapper.find("[data-testid='jobNameField']")
            .element as HTMLInputElement
        ).value,
      ).toBe("Test Job");
      expect(
        (
          wrapper.find("[data-testid='jobGroupField']")
            .element as HTMLInputElement
        ).value,
      ).toBe("Test/Group");
    });
  });

  describe("Additional Options", () => {
    it("handles checkbox options correctly", async () => {
      const wrapper = await createWrapper();

      const checkboxes = [
        "#importOptionsCheck",
        "#ignoreNotificationsCheck",
        "#failOnDisableCheck",
        "#childNodesCheck",
      ];

      for (const checkbox of checkboxes) {
        const input = wrapper.find(checkbox);
        await input.setValue(true);
        expect((input.element as HTMLInputElement).checked).toBe(true);
      }
    });
  });

  describe("Modal Interaction", () => {
    it("emits cancel event when cancel button is clicked", async () => {
      const wrapper = await createWrapper();

      await wrapper.find('[data-testid="cancel-button"]').trigger("click");

      expect(wrapper.emitted("cancel")).toBeTruthy();
    });

    it("updates modal visibility when modalActive prop changes", async () => {
      const wrapper = await createWrapper();

      await wrapper.setProps({ modalActive: false });

      expect(wrapper.emitted("update:modalActive")).toBeTruthy();
      expect(wrapper.emitted("update:modalActive")[0]).toEqual([false]);
    });
  });

  describe("Node Filter Input", () => {
    it("updates filter when node filter input changes", async () => {
      const wrapper = await createWrapper(
        {},
        {
          initialState: {
            nodes: {
              nodeFilterStore: {
                setSelectedFilter: jest.fn(),
              },
            },
          },
        },
      );
      const store = useNodesStore();

      await wrapper
        .findComponent({ name: "NodeFilterInput" })
        .vm.$emit("update:modelValue", "new-filter");

      expect(store.nodeFilterStore.setSelectedFilter).toHaveBeenCalledWith(
        "new-filter",
      );
    });
  });
});
