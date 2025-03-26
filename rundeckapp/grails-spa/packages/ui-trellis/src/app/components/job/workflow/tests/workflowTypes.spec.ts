import { describe, expect, it } from "@jest/globals";
import {
  createStrategyData,
  exportPluginData,
  WorkflowData,
} from "../types/workflowTypes";

describe("workflowTypes", () => {
  it("createStrategyData empty data", () => {
    const wfData = {} as WorkflowData;
    expect(createStrategyData(wfData)).toEqual({ type: undefined, config: {} });
  });
  it("createStrategyData strategy type only", () => {
    const wfData = {
      strategy: "s",
    } as WorkflowData;
    expect(createStrategyData(wfData)).toEqual({ type: "s", config: {} });
  });
  it.each([{}, { a: "b" }])(
    "createStrategyData strategy type with config %p",
    (config: any) => {
      const wfData = {
        strategy: "s",
        pluginConfig: {
          WorkflowStrategy: {
            s: config,
          },
        },
      } as WorkflowData;
      expect(createStrategyData(wfData)).toEqual({ type: "s", config });
    },
  );
  it("exportPluginData includes strategy data", () => {
    const strategy = { type: "s", config: { a: "b" } };
    expect(exportPluginData(strategy, null)).toEqual({
      pluginConfig: {
        WorkflowStrategy: {
          s: {
            a: "b",
          },
        },
      },
      strategy: "s",
    });
  });
});
