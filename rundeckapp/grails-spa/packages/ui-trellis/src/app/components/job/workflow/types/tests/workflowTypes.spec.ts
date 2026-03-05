import { createStepsData } from "../workflowTypes";
import type { StepsData } from "../workflowTypes";

describe("workflowTypes", () => {
  describe("createStepsData", () => {
    describe("with filterConditionals = false (default)", () => {
      it("preserves all steps including conditional steps", () => {
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

    describe("with filterConditionals = true", () => {
      it("filters out conditional steps by type field", () => {
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

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(2);
        expect(result.commands[0].exec).toBe("echo hello");
        expect(result.commands[1].exec).toBe("echo goodbye");
      });

      it("filters out conditional steps by duck-typing (conditionGroups + subSteps)", () => {
        const workflow = {
          commands: [
            { exec: "echo hello", nodeStep: true },
            {
              conditionGroups: [[{ key: "job.id", operator: "==", value: "abc" }]],
              subSteps: [{ exec: "echo conditional" }],
              nodeStep: true,
            },
            { exec: "echo goodbye", nodeStep: true },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(2);
        expect(result.commands[0].exec).toBe("echo hello");
        expect(result.commands[1].exec).toBe("echo goodbye");
      });

      it("preserves non-conditional steps", () => {
        const workflow = {
          commands: [
            { exec: "echo exec", nodeStep: true },
            { script: "#!/bin/bash", nodeStep: true },
            { scriptfile: "/path/to/script", nodeStep: true },
            { jobref: { name: "MyJob", uuid: "123", group: "" }, nodeStep: false },
            { type: "my-plugin", configuration: { key: "value" }, nodeStep: false },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(5);
        expect(result.commands[0].exec).toBe("echo exec");
        expect(result.commands[1].script).toBe("#!/bin/bash");
        expect(result.commands[2].scriptfile).toBe("/path/to/script");
        expect(result.commands[3].jobref.name).toBe("MyJob");
        expect(result.commands[4].type).toBe("my-plugin");
      });

      it("handles all steps being filtered out", () => {
        const workflow = {
          commands: [
            {
              type: "conditional",
              conditionGroups: [[{ key: "job.id", operator: "==", value: "abc" }]],
              subSteps: [{ exec: "echo conditional 1" }],
              nodeStep: true,
            },
            {
              type: "conditional",
              conditionGroups: [[{ key: "job.name", operator: "!=", value: "test" }]],
              subSteps: [{ exec: "echo conditional 2" }],
              nodeStep: true,
            },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toEqual([]);
      });

      it("recursively filters conditional error handlers", () => {
        const workflow = {
          commands: [
            {
              exec: "echo main",
              nodeStep: true,
              errorhandler: {
                type: "conditional",
                conditionGroups: [[{ key: "job.id", operator: "==", value: "abc" }]],
                subSteps: [{ exec: "echo error" }],
                nodeStep: true,
              } as any,
            },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(1);
        expect(result.commands[0].exec).toBe("echo main");
        expect(result.commands[0].errorhandler).toBeUndefined();
      });

      it("preserves non-conditional error handlers", () => {
        const workflow = {
          commands: [
            {
              exec: "echo main",
              nodeStep: true,
              errorhandler: {
                exec: "echo fallback",
                nodeStep: true,
                keepgoingOnSuccess: true,
              } as any,
            },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(1);
        expect(result.commands[0].exec).toBe("echo main");
        expect(result.commands[0].errorhandler).toBeDefined();
        expect((result.commands[0].errorhandler as any).exec).toBe("echo fallback");
      });

      it("recursively filters nested plugin configurations with commands", () => {
        const workflow = {
          commands: [
            {
              type: "workflow-plugin",
              configuration: {
                commands: [
                  { exec: "echo nested", nodeStep: true },
                  {
                    type: "conditional",
                    conditionGroups: [[{ key: "job.id", operator: "==", value: "abc" }]],
                    subSteps: [{ exec: "echo nested conditional" }],
                    nodeStep: true,
                  },
                ],
              },
              nodeStep: false,
            },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(1);
        expect(result.commands[0].type).toBe("workflow-plugin");
        expect((result.commands[0] as any).configuration.commands).toHaveLength(1);
        expect((result.commands[0] as any).configuration.commands[0].exec).toBe("echo nested");
      });

      it("handles empty workflow with filtering enabled", () => {
        const workflow = { commands: [] };

        const result = createStepsData(workflow, true);

        expect(result.commands).toEqual([]);
      });

      it("handles undefined commands with filtering enabled", () => {
        const workflow = {};

        const result = createStepsData(workflow, true);

        expect(result.commands).toEqual([]);
      });

      it("handles null commands array", () => {
        const workflow = { commands: null as any };

        const result = createStepsData(workflow, true);

        expect(result.commands).toEqual([]);
      });
    });

    describe("complex filtering scenarios", () => {
      it("filters mixed workflow with multiple conditional and regular steps", () => {
        const workflow = {
          commands: [
            { exec: "echo start", nodeStep: true },
            {
              type: "conditional",
              conditionGroups: [[{ key: "job.id", operator: "==", value: "1" }]],
              subSteps: [{ exec: "conditional 1" }],
              nodeStep: true,
            },
            { script: "#!/bin/bash\necho script", nodeStep: true },
            {
              conditionGroups: [[{ key: "job.id", operator: "==", value: "2" }]],
              subSteps: [{ exec: "conditional 2" }],
              nodeStep: true,
            },
            { jobref: { name: "Job1", uuid: "uuid-1", group: "" }, nodeStep: false },
            {
              type: "conditional",
              conditionGroups: [[{ key: "job.id", operator: "==", value: "3" }]],
              subSteps: [{ exec: "conditional 3" }],
              nodeStep: true,
            },
            { exec: "echo end", nodeStep: true },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(4);
        expect(result.commands[0].exec).toBe("echo start");
        expect(result.commands[1].script).toBe("#!/bin/bash\necho script");
        expect(result.commands[2].jobref!.name).toBe("Job1");
        expect(result.commands[3].exec).toBe("echo end");
      });

      it("handles deeply nested structures", () => {
        const workflow = {
          commands: [
            {
              type: "workflow-plugin",
              configuration: {
                commands: [
                  {
                    exec: "level 2",
                    nodeStep: true,
                    errorhandler: {
                      type: "conditional",
                      conditionGroups: [[{ key: "job.id", operator: "==", value: "x" }]],
                      subSteps: [{ exec: "should be filtered" }],
                      nodeStep: true,
                    } as any,
                  },
                ],
              },
              nodeStep: false,
              errorhandler: {
                exec: "top level error",
                nodeStep: true,
              } as any,
            },
          ],
        };

        const result = createStepsData(workflow, true);

        expect(result.commands).toHaveLength(1);
        expect((result.commands[0] as any).configuration.commands).toHaveLength(1);
        expect((result.commands[0] as any).configuration.commands[0].errorhandler).toBeUndefined();
        expect(result.commands[0].errorhandler).toBeDefined();
        expect((result.commands[0].errorhandler as any).exec).toBe("top level error");
      });
    });
  });
});
