import { mount, VueWrapper } from "@vue/test-utils";

import WorkflowGraph from "../WorkflowGraph.vue";

const mockGraphApplyRules = jest.fn();
const mockGraphSetNodes = jest.fn();
const mockGraphGraph = jest.fn(() => ({ nodes: () => [] }));
const mockGenerateRulesFromGraphlib = jest.fn(() => ({
  withDirectives: () => [],
  withConditions: () => [],
}));
const mockDiaGetElements = jest.fn(() => []);
const mockDiaRemoveLinks = jest.fn();
const mockDiaFromGraphLib = jest.fn();
const mockDiaOn = jest.fn();
const mockPaperOn = jest.fn();
const mockPaperTransformToFitContent = jest.fn();

jest.mock("jointjs", () => {
  class MockGraph {
    getElements = mockDiaGetElements;
    removeLinks = mockDiaRemoveLinks;
    fromGraphLib = mockDiaFromGraphLib;
    on = mockDiaOn;
    getNeighbors = jest.fn(() => []);
    getConnectedLinks = jest.fn(() => []);
    remove = jest.fn();
    getCell = jest.fn(() => undefined);
  }

  class MockPaper {
    on = mockPaperOn;
    transformToFitContent = mockPaperTransformToFitContent;
    scale = jest.fn(() => ({ sx: 1 }));
    translate = jest.fn(() => ({ tx: 0, ty: 0 }));
    matrix = jest.fn(() => ({ a: 1, d: 1 }));
  }

  return {
    dia: {
      Graph: MockGraph,
      Paper: MockPaper,
      ToolsView: class {},
    },
    layout: {
      DirectedGraph: {
        layout: jest.fn(),
      },
    },
    shapes: {
      standard: {
        Circle: class {},
        Link: class {},
      },
    },
    linkTools: {
      Remove: class {},
    },
    util: {
      timing: {
        inout: jest.fn(),
      },
    },
    g: {
      Point: class {},
      Polyline: class {},
    },
  };
});

jest.mock("../Graph", () => ({
  WorkflowGraph: class {
    applyRules = mockGraphApplyRules;
    setNodes = mockGraphSetNodes;
    graph = mockGraphGraph;
    generateRulesFromGraphlib = mockGenerateRulesFromGraphlib;
    getNode = jest.fn();
    setEdge = jest.fn();
    removeEdge = jest.fn();
  },
}));

jest.mock("../RuleSetParser", () => ({
  RuleSetParser: {
    ParseRules: jest.fn(() => ({})),
  },
}));

jest.mock("../elements/Step", () => ({
  WorkflowStep: class {
    addTo() {
      return this;
    }
  },
}));

interface MountOptions {
  props?: Record<string, unknown>;
}

type WorkflowGraphWrapper = VueWrapper<any>;

const createWrapper = async (
  options: MountOptions = {},
): Promise<WorkflowGraphWrapper> => {
  const wrapper = mount(WorkflowGraph, {
    props: {
      modelValue: "",
      nodes: [],
      ...options.props,
    },
    global: {
      stubs: {
        AceEditor: true,
        Tab: true,
        Tabs: true,
      },
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper as WorkflowGraphWrapper;
};

const findByTestId = (wrapper: WorkflowGraphWrapper, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`);

const setContainerWidth = (wrapper: WorkflowGraphWrapper, width: number) => {
  Object.defineProperty(wrapper.element, "clientWidth", {
    configurable: true,
    get: () => width,
  });
};

describe("WorkflowGraph", () => {
  const requestAnimationFrameSpy = jest
    .spyOn(window, "requestAnimationFrame")
    .mockImplementation((callback: FrameRequestCallback) => {
      callback(0);
      return 0;
    });

  beforeEach(() => {
    jest.clearAllMocks();
    document.body.style.userSelect = "";
    document.body.style.cursor = "";
  });

  afterAll(() => {
    requestAnimationFrameSpy.mockRestore();
  });

  describe("side panel resizing", () => {
    it("updates the separator value and panel width when nudged with the keyboard", async () => {
      const wrapper = await createWrapper();
      setContainerWidth(wrapper, 1200);

      await findByTestId(wrapper, "workflow-graph-resizer").trigger(
        "keydown.left",
      );
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").attributes(
          "aria-valuenow",
        ),
      ).toBe("620");
      expect(
        findByTestId(wrapper, "workflow-graph-side-panel").attributes("style"),
      ).toContain("width: 620px;");
    });

    it("clamps keyboard nudging to the configured minimum and maximum bounds", async () => {
      const wrapper = await createWrapper();
      setContainerWidth(wrapper, 800);

      await findByTestId(wrapper, "workflow-graph-resizer").trigger(
        "keydown.left",
      );
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").attributes(
          "aria-valuenow",
        ),
      ).toBe("600");

      for (let i = 0; i < 25; i++) {
        await findByTestId(wrapper, "workflow-graph-resizer").trigger(
          "keydown.right",
        );
      }
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").attributes(
          "aria-valuenow",
        ),
      ).toBe("200");
      expect(
        findByTestId(wrapper, "workflow-graph-side-panel").attributes("style"),
      ).toContain("width: 200px;");
    });

    it("resizes the panel while dragging and clears the active resize state on mouseup", async () => {
      const wrapper = await createWrapper();
      setContainerWidth(wrapper, 1200);

      await findByTestId(wrapper, "workflow-graph-resizer").trigger(
        "mousedown",
        { clientX: 500 },
      );
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").classes(
          "workflow-graph-resizer--active",
        ),
      ).toBe(true);
      expect(document.body.style.userSelect).toBe("none");
      expect(document.body.style.cursor).toBe("col-resize");

      window.dispatchEvent(new MouseEvent("mousemove", { clientX: 450 }));
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").attributes(
          "aria-valuenow",
        ),
      ).toBe("650");
      expect(
        findByTestId(wrapper, "workflow-graph-side-panel").attributes("style"),
      ).toContain("width: 650px;");

      window.dispatchEvent(new MouseEvent("mouseup"));
      await wrapper.vm.$nextTick();

      expect(
        findByTestId(wrapper, "workflow-graph-resizer").classes(
          "workflow-graph-resizer--active",
        ),
      ).toBe(false);
      expect(document.body.style.userSelect).toBe("");
      expect(document.body.style.cursor).toBe("");
    });

    it("clears active resize state when unmounted during a drag", async () => {
      const wrapper = await createWrapper();
      setContainerWidth(wrapper, 1200);

      await findByTestId(wrapper, "workflow-graph-resizer").trigger(
        "mousedown",
        { clientX: 500 },
      );
      await wrapper.vm.$nextTick();

      const renderedValue = findByTestId(
        wrapper,
        "workflow-graph-resizer",
      ).attributes("aria-valuenow");
      const renderedPanelStyle = findByTestId(
        wrapper,
        "workflow-graph-side-panel",
      ).attributes("style");
      const renderedResizer = findByTestId(wrapper, "workflow-graph-resizer")
        .element as HTMLElement;
      const renderedSidePanel = findByTestId(
        wrapper,
        "workflow-graph-side-panel",
      ).element as HTMLElement;

      wrapper.unmount();

      expect(document.body.style.userSelect).toBe("");
      expect(document.body.style.cursor).toBe("");

      window.dispatchEvent(new MouseEvent("mousemove", { clientX: 450 }));
      window.dispatchEvent(new MouseEvent("mouseup"));

      expect(document.body.style.userSelect).toBe("");
      expect(document.body.style.cursor).toBe("");
      expect(renderedResizer.getAttribute("aria-valuenow")).toBe(renderedValue);
      expect(renderedSidePanel.getAttribute("style")).toBe(renderedPanelStyle);
    });
  });
});
