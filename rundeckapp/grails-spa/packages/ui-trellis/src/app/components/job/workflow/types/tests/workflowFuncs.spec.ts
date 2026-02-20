import {
  commandsToEditData,
  commandToEditConfig,
  editCommandsToStepsData,
  editToCommandConfig,
  mkid,
} from "../workflowFuncs";
import type {
  StepData,
  StepsData,
  EditStepData,
  StepsEditData,
  ErrorHandlerDefinition,
} from "../workflowTypes";

describe("workflowFuncs", () => {
  describe("mkid", () => {
    it("returns a non-empty string", () => {
      expect(typeof mkid()).toBe("string");
      expect(mkid().length).toBeGreaterThan(0);
    });

    it("returns a unique value on each call", () => {
      expect(mkid()).not.toBe(mkid());
    });
  });

  describe("commandToEditConfig", () => {
    it("converts an exec-command step", () => {
      const step: StepData = {
        exec: "echo hello",
        nodeStep: true,
        description: "Run echo",
      };

      const result = commandToEditConfig(step);

      expect(result.type).toBe("exec-command");
      expect(result.nodeStep).toBe(true);
      expect(result.description).toBe("Run echo");
      expect((result.config as any).adhocRemoteString).toBe("echo hello");
      expect(result.id).toBeDefined();
    });

    it("converts a script-inline step", () => {
      const step: StepData = {
        script: "#!/bin/bash\necho world",
        args: "-v",
        scriptInterpreter: "bash",
        interpreterArgsQuoted: false,
        fileExtension: ".sh",
        nodeStep: true,
      };

      const result = commandToEditConfig(step);

      expect(result.type).toBe("script-inline");
      expect(result.nodeStep).toBe(true);
      expect((result.config as any).adhocLocalString).toBe("#!/bin/bash\necho world");
      expect((result.config as any).argString).toBe("-v");
      expect((result.config as any).scriptInterpreter).toBe("bash");
      expect((result.config as any).interpreterArgsQuoted).toBe(false);
      expect((result.config as any).fileExtension).toBe(".sh");
    });

    it("converts a script-file-url step", () => {
      const step: StepData = {
        scriptfile: "/path/to/script.sh",
        expandTokenInScriptFile: true,
        args: "--flag",
        nodeStep: true,
      };

      const result = commandToEditConfig(step);

      expect(result.type).toBe("script-file-url");
      expect(result.nodeStep).toBe(true);
      expect((result.config as any).adhocFilepath).toBe("/path/to/script.sh");
      expect((result.config as any).expandTokenInScriptFile).toBe(true);
      expect((result.config as any).argString).toBe("--flag");
    });

    it("converts a job reference step", () => {
      const step: StepData = {
        jobref: { group: "", name: "MyJob", uuid: "abc-123", nodeStep: false },
        nodeStep: false,
        description: "Run a job",
      };

      const result = commandToEditConfig(step);

      expect(result.type).toBe("job.reference");
      expect(result.nodeStep).toBe(false);
      expect(result.jobref?.name).toBe("MyJob");
      expect(result.jobref?.uuid).toBe("abc-123");
    });

    it("converts a plugin step with type and configuration", () => {
      const step: StepData = {
        type: "my-plugin",
        configuration: { key: "value" },
        nodeStep: false,
      };

      const result = commandToEditConfig(step);

      expect(result.type).toBe("my-plugin");
      expect(result.config).toEqual({ key: "value" });
      expect(result.nodeStep).toBe(false);
    });

    it("preserves log filters when present", () => {
      const step: StepData = {
        exec: "echo hello",
        nodeStep: true,
        plugins: { LogFilter: [{ type: "my-filter", config: {} }] },
      };

      const result = commandToEditConfig(step);

      expect(result.filters).toEqual([{ type: "my-filter", config: {} }]);
    });

    it("sets empty filters when no plugins", () => {
      const step: StepData = { exec: "echo", nodeStep: true };

      const result = commandToEditConfig(step);

      expect(result.filters).toEqual([]);
    });

    it("includes errorhandler when present", () => {
      const step: StepData = {
        exec: "echo main",
        nodeStep: true,
        errorhandler: {
          type: "exec-command",
          config: { adhocRemoteString: "echo fallback" },
          nodeStep: true,
          id: "eh-1",
          keepgoingOnSuccess: true,
        },
      };

      const result = commandToEditConfig(step);

      expect(result.errorhandler).toBeDefined();
      expect(result.errorhandler!.type).toBe("exec-command");
      expect(result.errorhandler!.keepgoingOnSuccess).toBe(true);
    });
  });

  describe("editToCommandConfig", () => {
    it("converts an exec-command step back to StepData", () => {
      const plugin: EditStepData = {
        type: "exec-command",
        config: { adhocRemoteString: "echo hello" },
        nodeStep: true,
        description: "Run echo",
        id: "1",
      };

      const result = editToCommandConfig(plugin);

      expect(result.exec).toBe("echo hello");
      expect(result.nodeStep).toBe(true);
      expect(result.description).toBe("Run echo");
    });

    it("converts a script-inline step back to StepData", () => {
      const plugin: EditStepData = {
        type: "script-inline",
        config: {
          adhocLocalString: "#!/bin/bash\necho world",
          argString: "-v",
          scriptInterpreter: "bash",
          interpreterArgsQuoted: true,
          fileExtension: ".sh",
        },
        nodeStep: true,
        id: "2",
      };

      const result = editToCommandConfig(plugin);

      expect(result.script).toBe("#!/bin/bash\necho world");
      expect(result.args).toBe("-v");
      expect(result.scriptInterpreter).toBe("bash");
      expect(result.interpreterArgsQuoted).toBe(true);
      expect(result.fileExtension).toBe(".sh");
    });

    it("converts a script-file-url step back to StepData", () => {
      const plugin: EditStepData = {
        type: "script-file-url",
        config: {
          adhocFilepath: "/path/to/script.sh",
          expandTokenInScriptFile: true,
          argString: "--flag",
        },
        nodeStep: true,
        id: "3",
      };

      const result = editToCommandConfig(plugin);

      expect(result.scriptfile).toBe("/path/to/script.sh");
      expect(result.expandTokenInScriptFile).toBe(true);
      expect(result.args).toBe("--flag");
    });

    it("converts a job reference step back to StepData", () => {
      const plugin: EditStepData = {
        type: "job.reference",
        config: {},
        jobref: { group: "", name: "MyJob", uuid: "abc-123", nodeStep: false },
        nodeStep: false,
        description: "Run job",
        id: "4",
      };

      const result = editToCommandConfig(plugin);

      expect(result.jobref?.name).toBe("MyJob");
      expect(result.jobref?.uuid).toBe("abc-123");
    });

    it("converts a plugin step with type back to StepData", () => {
      const plugin: EditStepData = {
        type: "my-plugin",
        config: { key: "value" },
        nodeStep: false,
        id: "5",
      };

      const result = editToCommandConfig(plugin);

      expect(result.type).toBe("my-plugin");
      expect(result.configuration).toEqual({ key: "value" });
    });

    it("preserves log filters in output", () => {
      const plugin: EditStepData = {
        type: "exec-command",
        config: { adhocRemoteString: "echo hello" },
        filters: [{ type: "my-filter", config: {} }],
        nodeStep: true,
        id: "8",
      };

      const result = editToCommandConfig(plugin);

      expect(result.plugins).toEqual({ LogFilter: [{ type: "my-filter", config: {} }] });
    });

    it("does not add plugins when filters is empty", () => {
      const plugin: EditStepData = {
        type: "exec-command",
        config: { adhocRemoteString: "echo hello" },
        filters: [],
        nodeStep: true,
        id: "9",
      };

      const result = editToCommandConfig(plugin);

      expect(result.plugins).toBeUndefined();
    });

    it("includes errorhandler when present", () => {
      const plugin: EditStepData = {
        type: "exec-command",
        config: { adhocRemoteString: "echo main" },
        nodeStep: true,
        id: "10",
        errorhandler: {
          type: "exec-command",
          config: { adhocRemoteString: "echo fallback" },
          nodeStep: true,
          id: "eh-1",
          keepgoingOnSuccess: true,
        },
      };

      const result = editToCommandConfig(plugin);

      expect(result.errorhandler).toBeDefined();
      expect((result.errorhandler as any).exec).toBe("echo fallback");
      expect(result.errorhandler!.keepgoingOnSuccess).toBe(true);
    });
  });

  describe("commandsToEditData", () => {
    it("converts StepsData to StepsEditData", () => {
      const stepsData: StepsData = {
        commands: [
          { exec: "echo hello", nodeStep: true },
          { exec: "echo world", nodeStep: true },
        ],
      };

      const result = commandsToEditData(stepsData);

      expect(result.commands).toHaveLength(2);
      expect(result.commands[0].type).toBe("exec-command");
      expect(result.commands[1].type).toBe("exec-command");
    });

    it("handles empty commands array", () => {
      const stepsData: StepsData = { commands: [] };

      const result = commandsToEditData(stepsData);

      expect(result.commands).toEqual([]);
    });
  });

  describe("editCommandsToStepsData", () => {
    it("converts StepsEditData to StepsData", () => {
      const editData: StepsEditData = {
        commands: [
          { type: "exec-command", config: { adhocRemoteString: "echo a" }, nodeStep: true, id: "1" },
          { type: "exec-command", config: { adhocRemoteString: "echo b" }, nodeStep: true, id: "2" },
        ],
      };

      const result = editCommandsToStepsData(editData);

      expect(result.commands).toHaveLength(2);
      expect(result.commands[0].exec).toBe("echo a");
      expect(result.commands[1].exec).toBe("echo b");
    });

    it("handles empty commands array", () => {
      const editData: StepsEditData = { commands: [] };

      const result = editCommandsToStepsData(editData);

      expect(result.commands).toEqual([]);
    });
  });
});
