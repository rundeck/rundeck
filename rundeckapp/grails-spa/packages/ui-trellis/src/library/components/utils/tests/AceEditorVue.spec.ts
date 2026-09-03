import { describe, it, expect, jest, beforeEach } from "@jest/globals";
import { shallowMount } from "@vue/test-utils";
import AceEditorVue from "../AceEditorVue.vue";

const mockSetOptions = jest.fn();
const mockSetTheme = jest.fn();
const mockSetValue = jest.fn();
const mockGetValue = jest.fn().mockReturnValue("");
const mockOn = jest.fn();
const mockDestroy = jest.fn();
const mockContainerRemove = jest.fn();
const mockSessionSetUseWorker = jest.fn();
const mockSessionSetMode = jest.fn();
const mockSessionSetValue = jest.fn();
const mockSessionSetUseWrapMode = jest.fn();
const mockGetSession = jest.fn(() => ({
  setUseWorker: mockSessionSetUseWorker,
  setMode: mockSessionSetMode,
  setValue: mockSessionSetValue,
  setUseWrapMode: mockSessionSetUseWrapMode,
}));

const mockEditor = {
  setOptions: mockSetOptions,
  setTheme: mockSetTheme,
  setValue: mockSetValue,
  getValue: mockGetValue,
  getSession: mockGetSession,
  on: mockOn,
  destroy: mockDestroy,
  container: { remove: mockContainerRemove },
  completers: [],
};

jest.mock("ace-builds", () => ({
  edit: jest.fn(() => mockEditor),
  require: jest.fn(() => ({ escapeHTML: jest.fn((s: string) => s) })),
}));

jest.mock("ace-builds/src-noconflict/ext-emmet", () => ({}));

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: jest.fn(() => ({
    matches: false,
    addEventListener: jest.fn(),
    addListener: jest.fn(),
  })),
});

global.MutationObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  disconnect: jest.fn(),
})) as unknown as typeof MutationObserver;

global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  disconnect: jest.fn(),
})) as unknown as typeof ResizeObserver;

const createWrapper = async (options: { props?: Record<string, any> } = {}) => {
  const wrapper = shallowMount(AceEditorVue, {
    props: {
      modelValue: "",
      ...options.props,
    },
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

describe("AceEditorVue", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("minLines prop", () => {
    it("omits minLines/maxLines from editor setOptions on mount when minLines is 0 (resizable, default)", async () => {
      await createWrapper();

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.not.objectContaining({ minLines: expect.anything() }),
      );
      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.not.objectContaining({ maxLines: expect.anything() }),
      );
    });

    it("passes custom minLines to editor setOptions when prop is provided", async () => {
      await createWrapper({ props: { minLines: 5 } });

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ minLines: 5 }),
      );
    });

    it("calls setOptions when minLines prop changes away from 0", async () => {
      const wrapper = await createWrapper({ props: { minLines: 5 } });
      jest.clearAllMocks();

      await wrapper.setProps({ minLines: 20 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).toHaveBeenCalledWith({ minLines: 20 });
    });

    it("does not call setOptions for minLines when staying resizable (minLines 0)", async () => {
      const wrapper = await createWrapper();
      jest.clearAllMocks();

      await wrapper.setProps({ minLines: 0 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).not.toHaveBeenCalled();
    });
  });

  describe("maxLines prop", () => {
    it("passes custom maxLines to editor setOptions when prop is provided alongside a non-zero minLines", async () => {
      await createWrapper({ props: { minLines: 5, maxLines: 30 } });

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ maxLines: 30 }),
      );
    });

    it("calls setOptions when maxLines prop changes and the editor is not resizable", async () => {
      const wrapper = await createWrapper({ props: { minLines: 5 } });
      jest.clearAllMocks();

      await wrapper.setProps({ maxLines: 50 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).toHaveBeenCalledWith({ maxLines: 50 });
    });

    it("does not call setOptions when maxLines prop changes while resizable (minLines 0)", async () => {
      const wrapper = await createWrapper();
      jest.clearAllMocks();

      await wrapper.setProps({ maxLines: 50 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).not.toHaveBeenCalled();
    });
  });

  describe("resizable class", () => {
    it("applies the resizable class to the wrapper when minLines is 0 (default)", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.classes()).toContain("resizable");
    });

    it("does not apply the resizable class to the wrapper when minLines is greater than 0", async () => {
      const wrapper = await createWrapper({ props: { minLines: 5 } });

      expect(wrapper.classes()).not.toContain("resizable");
    });

    it("toggles the resizable class reactively when minLines prop changes", async () => {
      const wrapper = await createWrapper({ props: { minLines: 5 } });
      expect(wrapper.classes()).not.toContain("resizable");

      await wrapper.setProps({ minLines: 0 });

      expect(wrapper.classes()).toContain("resizable");
    });
  });

  describe("resize observer", () => {
    it("observes the editor container for resize when resizable (minLines 0, default)", async () => {
      await createWrapper();

      expect(global.ResizeObserver).toHaveBeenCalled();
      const instance = (global.ResizeObserver as jest.Mock).mock.results[0]
        .value as { observe: jest.Mock };
      expect(instance.observe).toHaveBeenCalled();
    });

    it("does not observe for resize when not resizable", async () => {
      await createWrapper({ props: { minLines: 5 } });

      expect(global.ResizeObserver).not.toHaveBeenCalled();
    });
  });

  describe("init event", () => {
    it("emits init with the editor instance on mount", async () => {
      const wrapper = await createWrapper();

      expect(wrapper.emitted("init")).toHaveLength(1);
      expect(wrapper.emitted("init")![0][0]).toBe(mockEditor);
    });
  });

  describe("update:modelValue event", () => {
    it("emits update:modelValue with new content when editor changes", async () => {
      const wrapper = await createWrapper();
      const changeCall = mockOn.mock.calls.find(
        (c: any[]) => c[0] === "change",
      );
      expect(changeCall).toBeDefined();

      mockGetValue.mockReturnValue("new content");
      (changeCall as any[])[1]();
      await wrapper.vm.$nextTick();

      expect(wrapper.emitted("update:modelValue")).toHaveLength(1);
      expect(wrapper.emitted("update:modelValue")![0][0]).toBe("new content");
    });
  });
});
