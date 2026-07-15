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
    it("passes default minLines of 12 to editor setOptions on mount", async () => {
      await createWrapper();

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ minLines: 12 }),
      );
    });

    it("passes custom minLines to editor setOptions when prop is provided", async () => {
      await createWrapper({ props: { minLines: 5 } });

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ minLines: 5 }),
      );
    });

    it("calls setOptions when minLines prop changes", async () => {
      const wrapper = await createWrapper();
      jest.clearAllMocks();

      await wrapper.setProps({ minLines: 20 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).toHaveBeenCalledWith({ minLines: 20 });
    });
  });

  describe("maxLines prop", () => {
    it("passes default maxLines of Infinity to editor setOptions on mount", async () => {
      await createWrapper();

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ maxLines: Infinity }),
      );
    });

    it("passes custom maxLines to editor setOptions when prop is provided", async () => {
      await createWrapper({ props: { maxLines: 30 } });

      expect(mockSetOptions).toHaveBeenCalledWith(
        expect.objectContaining({ maxLines: 30 }),
      );
    });

    it("calls setOptions when maxLines prop changes", async () => {
      const wrapper = await createWrapper();
      jest.clearAllMocks();

      await wrapper.setProps({ maxLines: 50 });
      await wrapper.vm.$nextTick();

      expect(mockSetOptions).toHaveBeenCalledWith({ maxLines: 50 });
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
