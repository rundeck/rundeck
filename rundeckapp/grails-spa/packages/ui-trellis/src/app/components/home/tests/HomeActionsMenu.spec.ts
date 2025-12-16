import { mount, VueWrapper } from "@vue/test-utils";
import HomeActionsMenu from "../HomeActionsMenu.vue";
import { Dropdown, Btn } from "uiv";
import { AuthzMeta } from "../types/projectTypes";

const createDefaultMeta = (): AuthzMeta => ({
  name: "authz",
  data: { project: { admin: true }, types: { job: { create: true } } },
});

jest.mock("@/library", () => ({
  getRundeckContext: jest
    .fn()
    .mockReturnValue({ rdBase: "http://localhost:4440" }),
}));

const mountHomeActionsMenu = async (meta?: AuthzMeta): Promise<VueWrapper<any>> => {
  const wrapper = mount(HomeActionsMenu, {
    props: {
      project: {
        name: "example",
        meta: [meta || createDefaultMeta()],
      },
      index: 0,
    },
  });

  // Wait for the next Vue tick to allow for asynchronous rendering
  await wrapper.vm.$nextTick();

  return wrapper;
};

describe("HomeActionsMenu", () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it("displays edit configuration link for admin", async () => {
    const wrapper = await mountHomeActionsMenu();

    expect(
      wrapper
        .find('[href="http://localhost:4440/project/example/configure"]')
        .exists(),
    ).toBe(true);
  });

  it("does not display edit configuration link for non-admin", async () => {
    const meta = createDefaultMeta();
    meta.data.project.admin = false;
    const wrapper = await mountHomeActionsMenu(meta);

    expect(
      wrapper
        .find('[href="http://localhost:4440/project/example/configure"]')
        .exists(),
    ).toBe(false);
  });

  it("displays create job link for users with create permissions", async () => {
    const wrapper = await mountHomeActionsMenu();

    expect(
      wrapper
        .find('[href="http://localhost:4440/project/example/job/create"]')
        .exists(),
    ).toBe(true);
  });

  it("does not display create job link for users without create permissions", async () => {
    const meta = createDefaultMeta();
    meta.data.types.job.create = false;
    meta.data.project.admin = false;

    const wrapper = await mountHomeActionsMenu(meta);

    // Assert that the create job link is not displayed
    expect(
      wrapper
        .find('[href="http://localhost:4440/project/example/job/create"]')
        .exists(),
    ).toBe(false);
  });
});
