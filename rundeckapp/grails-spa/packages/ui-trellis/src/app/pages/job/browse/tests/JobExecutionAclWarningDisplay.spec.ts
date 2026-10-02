import { mount } from "@vue/test-utils";
import JobExecutionAclWarningDisplay from "../tree/JobExecutionAclWarningDisplay.vue";

const WARNING = '[data-testid="execution-acl-warning"]';

// The job browse row passes `{ job, meta: <the meta entry's data> }` as socket-data,
// so for the `executionAclValid` meta the component sees `itemData.meta.valid`.
const mountWidget = (itemData: Record<string, unknown>) =>
  mount(JobExecutionAclWarningDisplay, {
    props: { itemData },
    global: {
      mocks: {
        // Mirrors vue-i18n named interpolation so the test asserts the user is passed through
        $t: (key: string, args?: Record<string, unknown>) =>
          args ? `${key}:${JSON.stringify(args)}` : key,
      },
    },
  });

describe("JobExecutionAclWarningDisplay", () => {
  it("renders the warning when the scheduled job's stored owner is not authorized to run it", () => {
    const wrapper = mountWidget({
      job: { id: "job-1" },
      meta: { valid: false, user: "devread" },
    });

    const warning = wrapper.find(WARNING);
    expect(warning.exists()).toBe(true);
    expect(warning.attributes("title")).toBe(
      'job.execution.acl.invalid.warning.title:{"user":"devread"}',
    );
  });

  it("uses the missing-owner message when the job has no saved user", () => {
    // there is nobody to grant access to, so the grant-or-re-save wording
    // would be telling the viewer to do something impossible
    const wrapper = mountWidget({ job: { id: "job-1" }, meta: { valid: false } });

    const warning = wrapper.find(WARNING);
    expect(warning.exists()).toBe(true);
    expect(warning.attributes("title")).toBe(
      "job.execution.acl.missing.owner.warning.title",
    );
  });

  it("renders nothing when the stored owner is still authorized", () => {
    const wrapper = mountWidget({
      job: { id: "job-1" },
      meta: { valid: true, user: "admin" },
    });

    expect(wrapper.find(WARNING).exists()).toBe(false);
  });

  it("renders nothing when the meta entry is absent", () => {
    const wrapper = mountWidget({ job: { id: "job-1" } });

    expect(wrapper.find(WARNING).exists()).toBe(false);
  });

  it("renders nothing when no item data is supplied at all", () => {
    const wrapper = mount(JobExecutionAclWarningDisplay, {
      global: { mocks: { $t: (key: string) => key } },
    });

    expect(wrapper.find(WARNING).exists()).toBe(false);
  });
});
