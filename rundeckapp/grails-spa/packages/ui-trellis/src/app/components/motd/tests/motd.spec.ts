import { flushPromises, mount } from "@vue/test-utils";
import axios from "axios";
import MessageOfTheDay from "../motd.vue";

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

const mockEventBus = { on: jest.fn(), emit: jest.fn() };

const createWrapper = async (motdHTML: string) => {
  mockedAxios.get.mockResolvedValue({
    data: {
      projects: [
        {
          readme: { motd: motdHTML, motdHTML },
          motdDisplay: ["navbar"],
        },
      ],
    },
  });
  const wrapper = mount(MessageOfTheDay, {
    props: { eventBus: mockEventBus, tabPage: "home" },
    global: {
      stubs: { Drawer: true },
      mocks: { $cookies: { get: jest.fn(), set: jest.fn() } },
    },
  });
  await flushPromises();
  await wrapper.vm.$nextTick();
  return wrapper;
};

const emittedTitle = () =>
  mockEventBus.emit.mock.calls.find(
    ([event]) => event === "motd-message-available",
  )?.[1].title;

describe("MessageOfTheDay", () => {
  beforeEach(() => {
    (window as any)._rundeck = {
      rdBase: "http://localhost:4440",
      projectName: "test-project",
    };
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it("publishes the heading text as the title when the message starts with a heading", async () => {
    await createWrapper(
      '<article class="markdown-body"><h2>Maintenance</h2><p>tonight</p></article>',
    );

    expect(emittedTitle()).toBe("Maintenance");
  });

  it("publishes a null title when the message has no heading", async () => {
    await createWrapper(
      '<article class="markdown-body"><p>hello</p></article>',
    );

    expect(emittedTitle()).toBeNull();
  });

  it("publishes a null title when the heading cannot be parsed", async () => {
    await createWrapper(
      '<article class="markdown-body"><h2 id="x">Maintenance</h2></article>',
    );

    expect(emittedTitle()).toBeNull();
  });

  it("publishes a null title when the message is empty", async () => {
    await createWrapper("");

    expect(emittedTitle()).toBeNull();
  });
});
