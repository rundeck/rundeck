import * as Util from "util";

import * as Joint from "jointjs";

import {
  WorkflowGraph,
  layoutGraph,
} from "../Graph";
import {
  RuleBuilder,
  RuleSet,
  DirectiveType,
  RuleSetParser,
} from "../RuleSetParser";

describe("Graph", () => {
  it("Graphs", () => {
    const graph = new WorkflowGraph(new Map([]));
    const g = layoutGraph(graph);

    const dia = new Joint.dia.Graph();

    dia.fromGraphLib(g, {
      importNode: function (node: any) {
        return new Joint.shapes.basic.Rect({
          position: { x: node.x, y: node.y },
          size: { width: node.width, height: node.height },
        });
      },
      importEdge: function (edge: any) {
        return new Joint.dia.Link({
          source: { id: edge.v },
          target: { id: edge.w },
        });
      },
    });
  });
  it("apply rules without labels", () => {
    let graph = new WorkflowGraph(new Map([]));
    graph.setNodes(
      new Map([
        ["0", { identifier: "0", height: 20, width: 20 }],
        ["1", { identifier: "1", height: 20, width: 20 }],
      ]),
    );
    let rules = "[a] run-at-start\n[b] run-after:a\n";
    let ruleSet = RuleSetParser.ParseRules(rules);
    //expect error
    try {
      graph.applyRules(ruleSet);
    } catch (e: any) {
      expect(e.message).toEqual("Node with identifier [a] not found!");
    }
  });
  it("generate rules from graphlib empty", () => {
    let graph = new WorkflowGraph(new Map([]));
    graph.setNodes(
      new Map([
        ["0", { identifier: "0", label: "a", height: 20, width: 20 }],
        ["1", { identifier: "1", label: "b", height: 20, width: 20 }],
      ]),
    );
    let rules = "";
    let ruleSet = RuleSetParser.ParseRules(rules);
    graph.applyRules(ruleSet);

    let result = graph.generateRulesFromGraphlib();
    let rulestring =
      result
        .withDirectives()
        .map((r) => r.toString())
        .join("\n") +
      "\n" +
      result
        .withConditions()
        .map((r) => r.toString())
        .join("\n");
    expect(rulestring).toEqual("[0] run-at-start\n[1] run-after:0\n");
  });
  it("generate rules from graphlib loses labels", () => {
    let graph = new WorkflowGraph(new Map([]));
    graph.setNodes(
      new Map([
        ["0", { identifier: "0", label: "a", height: 20, width: 20 }],
        ["1", { identifier: "1", label: "b", height: 20, width: 20 }],
        ["2", { identifier: "2", label: "c", height: 20, width: 20 }],
      ]),
    );
    let rules = "[a] run-at-start\n[b] run-after:a\n[c] run-after:a\n";
    let unlabeledrules = "[0] run-at-start\n[1] run-after:0\n[2] run-after:0\n";
    let ruleSet = RuleSetParser.ParseRules(rules);
    graph.applyRules(ruleSet);

    let result = graph.generateRulesFromGraphlib();
    let rulestring =
      result
        .withDirectives()
        .map((r) => r.toString())
        .join("\n") +
      "\n" +
      result
        .withConditions()
        .map((r) => r.toString())
        .join("\n");
    expect(rulestring).toEqual(unlabeledrules);
  });
  it("generate rules from graphlib keeps conditions", () => {
    let graph = new WorkflowGraph(new Map([]));
    graph.setNodes(
      new Map([
        ["0", { identifier: "0", label: "a", height: 20, width: 20 }],
        ["1", { identifier: "1", label: "b", height: 20, width: 20 }],
        ["2", { identifier: "2", label: "c", height: 20, width: 20 }],
      ]),
    );
    let rules = "[a] run-at-start\n[b] if:option.x=y\n[c] if:option.z=p\n";
    let expectedrules =
      "[0] run-at-start\n[1] run-after:0\n[2] run-after:1\n[b] if:option.x=y\n[c] if:option.z=p";
    let ruleSet = RuleSetParser.ParseRules(rules);
    graph.applyRules(ruleSet);

    let result = graph.generateRulesFromGraphlib();
    let rulestring =
      result
        .withDirectives()
        .map((r) => r.toString())
        .join("\n") +
      "\n" +
      result
        .withConditions()
        .map((r) => r.toString())
        .join("\n");
    expect(rulestring).toEqual(expectedrules);
  });

  it("Last wildcard rule wins", () => {
    const ruleSet = new RuleSet();
    ruleSet.addRule(
      new RuleBuilder()
        .addIdentifer("*")
        .addRule({ type: DirectiveType.runAtStart }),
    );
    ruleSet.addRule(
      new RuleBuilder()
        .addIdentifer("*")
        .addRule({ type: DirectiveType.runInSequence }),
    );

    const graph = new WorkflowGraph(
      new Map([
        ["A", { identifier: "A", height: 20, width: 20 }],
        ["B", { identifier: "B", height: 20, width: 20 }],
        ["C", { identifier: "C", height: 20, width: 20 }],
      ]),
      ruleSet,
    );

    const previous = graph.getNode("C").previous;

    expect(previous.length).toEqual(1);
    expect(previous[0].identifier).toEqual("B");
  });

  it("Wildcard rules only apply to steps with no rules", () => {
    const ruleSet = new RuleSet();
    ruleSet.addRule(
      new RuleBuilder()
        .addIdentifer("*")
        .addRule({ type: DirectiveType.runAtStart }),
    );
    ruleSet.addRule(
      new RuleBuilder()
        .addIdentifer("C")
        .addRule({ type: DirectiveType.runAfter, identifiers: ["B"] }),
    );

    const graph = new WorkflowGraph(
      new Map([
        ["A", { identifier: "A", height: 20, width: 20 }],
        ["B", { identifier: "B", height: 20, width: 20 }],
        ["C", { identifier: "C", height: 20, width: 20 }],
      ]),
      ruleSet,
    );

    let previous = graph.getNode("C").previous;
    expect(previous.length).toEqual(1);
    expect(previous[0].identifier).toEqual("B");

    previous = graph.getNode("B").previous;
    expect(previous.length).toEqual(0);
    previous = graph.getNode("A").previous;
    expect(previous.length).toEqual(0);
  });

  it("Applies implicit run-in-sequence wildcard rule", () => {
    const ruleSet = new RuleSet();

    const graph = new WorkflowGraph(
      new Map([
        ["A", { identifier: "A", height: 20, width: 20 }],
        ["B", { identifier: "B", height: 20, width: 20 }],
        ["C", { identifier: "C", height: 20, width: 20 }],
      ]),
      ruleSet,
    );

    const nodes = ["A", "B", "C"];
    for (let x = 1; x < 3; x++) {
      expect(graph.getNode(nodes[x]).previous[0].identifier).toEqual(
        nodes[x - 1],
      );
    }
  });
});
