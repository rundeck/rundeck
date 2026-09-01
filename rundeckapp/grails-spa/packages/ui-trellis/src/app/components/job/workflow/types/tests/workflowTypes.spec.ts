import { createStepsData } from "../workflowTypes";

describe("workflowTypes", () => {
  describe("createStepsData", () => {
    it("preserves all steps as-is, with no filtering", () => {
      const workflow = {
        commands: [
          { exec: "echo hello", nodeStep: true },
          {
            type: "conditional",
            conditionGroups: [[{ key: "job.id", operator: "==", value: "abc" }]],
            subSteps: [{ exec: "echo conditional" }],
            nodeStep: true,
          },
          { exec: "echo goodbye", nodeStep: true },
        ],
      };

      const result = createStepsData(workflow);

      expect(result.commands).toHaveLength(3);
      expect(result.commands[0].exec).toBe("echo hello");
      expect(result.commands[1].type).toBe("conditional");
      expect(result.commands[2].exec).toBe("echo goodbye");
    });

    it("handles empty workflow", () => {
      const workflow = { commands: [] };

      const result = createStepsData(workflow);

      expect(result.commands).toEqual([]);
    });

    it("handles undefined commands", () => {
      const workflow = {};

      const result = createStepsData(workflow);

      expect(result.commands).toEqual([]);
    });
  });
});
