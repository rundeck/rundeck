import { flushPromises, mount, VueWrapper } from "@vue/test-utils";
import JobRefStep from "../JobRefStep.vue";

const createWrapper = async (props = {}): Promise<VueWrapper<any>> => {
  const wrapper = mount(JobRefStep, {
    props: {
      step: {
        jobref: {
          name: "",
          group: "",
          project: "",
          uuid: "",
          args: "",
          nodeStep: false,
          ...props,
        },
      },
    },
  });
  await wrapper.vm.$nextTick();
  await flushPromises();
  return wrapper;
};
const assertName = (wrapper: VueWrapper, name: string) => {
  const jobName = wrapper.find("[data-testid=job-ref-name]")
  expect(jobName.text()).toContain(name);
}

describe("JobRefStep", () => {
  describe("Job Name Display", () => {
    it("displays job name with group when both are provided", async () => {
      const wrapper = await createWrapper({
        name: "TestJob",
        group: "TestGroup",
      });

      assertName(wrapper, "TestGroup/TestJob")
    });

    it("displays only job name when no group is provided", async () => {
      const wrapper = await createWrapper({
        name: "TestJob",
      });

      assertName(wrapper, "TestJob")
    });

    it("displays UUID when no name is provided", async () => {
      const wrapper = await createWrapper({
          uuid: "123-456-789",
      });

      assertName(wrapper, "123-456-789")
    });

    it("shows project name in parentheses when provided", async () => {
      const wrapper = await createWrapper({
        project: "TestProject",
        uuid: "123-456-789",
      });

      const projectName = wrapper.find("[data-testid=project]")
      expect(projectName.text()).toContain("(TestProject)");
    });
  });

  describe("Arguments Display", () => {
    it("displays raw args when not in key-value format", async () => {
      const wrapper = await createWrapper({
        args: "simple argument string",
      });

      expect(wrapper.find("[data-testid=non-parsed-args]").text()).toBe("simple argument string");
      expect(wrapper.find("[data-test=parsed-args]").exists()).toBe(false);
    });

    it("parses and displays key-value args correctly", async () => {
      const wrapper = await createWrapper({
        args: "-key1 value1 -key2 value2",
      });

      const keys = wrapper.findAll(".optkey");
      const values = wrapper.findAll(".optvalue");

      expect(keys[0].text().trim()).toBe("key1");
      expect(keys[1].text().trim()).toBe("key2");
      expect(values[0].text().trim()).toBe("value1");
      expect(values[1].text().trim()).toBe("value2");
    });

    it("handles quoted values in args correctly", async () => {
      const wrapper = await createWrapper({
          args: "-key1 \"value with spaces\" -key2 'another value'",
      });

      const keys = wrapper.findAll(".optkey");
      const values = wrapper.findAll(".optvalue");

      expect(keys[0].text().trim()).toBe("key1");
      expect(keys[1].text().trim()).toBe("key2");
      expect(values[0].text().trim()).toBe("value with spaces");
      expect(values[1].text().trim()).toBe("another value");
    });

    it("does not show args section when no args provided", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.find("[data-testid=parsed-args]").exists()).toBe(false);
    });
  });

  describe("Node Step Display", () => {
    it("shows node step indicator when nodeStep is true", async () => {
      const wrapper = await createWrapper({
        nodeStep: true,
      });

      expect(wrapper.find(".fa-hdd").exists()).toBe(true);
      expect(wrapper.find(".info.note").exists()).toBe(true);
      expect(wrapper.find(".info.note").text()).toBe(
        "JobExec.nodeStep.true.label",
      );
    });

    it("does not show node step indicator when nodeStep is false", async () => {
      const wrapper = await createWrapper({
        nodeStep: false,
      });

      expect(wrapper.find(".fa-hdd").exists()).toBe(false);
      expect(wrapper.find(".info.note").exists()).toBe(false);
    });
  });

  describe("Icons", () => {
    it("always displays the book icon", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.find(".glyphicon-book").exists()).toBe(true);
    });
  });
});
