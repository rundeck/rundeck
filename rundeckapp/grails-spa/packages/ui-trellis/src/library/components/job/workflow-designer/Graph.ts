// @ts-nocheck
import * as Dagre from "dagre";
import * as GLib from "graphlib";
import { g } from "jointjs";

import {
  DirectiveType,
  isDirective,
  Rule,
  RuleBuilder,
  RuleSet,
} from "./RuleSetParser";
export interface NodeDescription {
  identifier: string;
  label?: string;
  title?: string;
  icon?: {
    class?: string;
    image?: string;
  };
  height: number;
  width: number;
}

export class GNode implements NodeDescription {
  identifier: string;
  label?: string;
  title?: string;
  icon?: {
    class?: string;
    image?: string;
  };
  previous: Array<GraphNode> = [];
  next: Array<GraphNode> = [];
  description: NodeDescription;
  height: number;
  width: number;
  rules: RuleSet = new RuleSet();

  constructor(desc: NodeDescription) {
    this.label = desc.label;
    this.title = desc.title;
    this.icon = desc.icon;
    this.identifier = desc.identifier;
    this.height = desc.height;
    this.width = desc.width;
    this.description = desc;
  }

  isParent(node: GraphNode) {
    return this.next.includes(node);
  }

  isChild(node: GraphNode) {
    return this.previous.includes(node);
  }

  addNext(node: GraphNode) {
    if (!this.next.includes(node)) this.next.push(node);
  }

  removeNext(node: GraphNode) {
    const idx = this.next.indexOf(node);

    if (idx > -1) this.next.splice(idx);
  }

  clearEdges() {
    this.next = [];
    this.previous = [];
  }

  clearRules() {
    this.rules = new RuleSet();
  }

  addPrevious(node: GraphNode) {
    if (!this.previous.includes(node)) this.previous.push(node);
  }

  removePrevious(node: GraphNode) {
    const idx = this.previous.indexOf(node);

    if (idx > -1) this.previous.splice(idx);
  }
}

/** Represents a node with multiple "sub-states" */
class PNode extends GNode {
  nodes: Array<GraphNode> = [];
}

/** Represents a choice node for branching logic */
// TODO: Implement
class CNode extends GNode {
  choices: Array<any> = [];
}

type GraphNode = GNode | PNode | CNode;

interface GraphOpts {
  usePNodes?: boolean;
}

export class WorkflowGraph {
  nodes: Map<string, GraphNode> = new Map();
  labelToIdentifier: Map<string, string> = new Map();
  end?: Node;
  opts: GraphOpts;
  graphlib: GLib.Graph = new GLib.Graph().setGraph({});
  ruleSet: RuleSet;

  constructor(
    nodes: Map<string, NodeDescription>,
    ruleSet?: RuleSet,
    opts: GraphOpts = {},
  ) {
    this.ruleSet = ruleSet || new RuleSet();

    this.opts = opts;

    this.setNodes(nodes);
  }

  setNodes(nodes: Map<string, NodeDescription>) {
    this.graphlib = new GLib.Graph().setGraph({});

    for (const [id, node] of nodes) {
      this.add(new GNode(node));
    }

    this.applyRules(this.ruleSet);
  }

  add(node: GraphNode) {
    const { identifier: id, label } = node;

    if (label) this.labelToIdentifier.set(label, id);

    this.graphlib.setNode(node.identifier, node);
    this.nodes.set(id, node);
  }

  getNode(identifier: string): GraphNode {
    let id = this.labelToIdentifier.get(identifier);

    if (!id) id = identifier;

    const node = this.graphlib.node(id);

    if (!node)
      throw new NodeNotFoundError(
        `Node with identifier [${identifier}] not found!`,
      );
    else return node;
  }

  connect() {
    // for (let [id, node] of this.nodes) {
    //     let {description} = node
    //     if (description.previous) {
    //         for (let p of description.previous) {
    //             const prevNode = this.nodes.get(p)
    //             node.addPrevious(prevNode!)
    //             prevNode!.addNext(node)
    //             this.graphlib.setEdge(prevNode!.identifier, node.identifier)
    //         }
    //     }
    /** Check if we need to convert to a PNode */
    // if (this.opts.usePNodes) {
    //     if(before && before.next) {
    //         console.log('Connecting PNode')
    //         if (!isPNode(before.next)) {
    //             const newPNode = new PNode(before.next.identifier, before.next.description)
    //             before.next.previous = node.previous = before
    //             newPNode.nodes.push(node, before.next)
    //             before.next = newPNode
    //         } else {
    //             before.next.nodes.push(node)
    //         }
    //     } else if (before) {
    //         console.log('Connecting node')
    //         before.next = node
    //     }
    // }
    // }
  }

  roots(): Array<GraphNode> {
    return Array.from(this.nodes.values()).filter(
      (n) => n.previous.length == 0,
    );
  }

  clearEdges() {
    const edges = this.graphlib.edges();

    for (const edge of edges) this.graphlib.removeEdge(edge);

    this.nodes.forEach((n) => n.clearEdges());
  }

  applyRules(rules: RuleSet) {
    this.ruleSet = rules;
    this.clearEdges();

    // Clear existing node rules
    for (const node of this.nodes.values()) node.clearRules();

    for (const rule of rules.withoutWildcard()) {
      this.applyRule(rule);
    }

    const wildCard = [
      new RuleBuilder()
        .addIdentifer("*")
        .addRule({ type: DirectiveType.runInSequence }),
      ...rules.withWildcard(),
    ].pop()!;

    this.applyRule(wildCard);
  }

  applyRule(rule: RuleBuilder) {
    let identifiers: string[] = rule.getIdentifiers();
    let wildCard = false;
    if (identifiers[0] == "*") {
      wildCard = true;
      identifiers = this.graphlib.nodes();
    }

    /** Add conditions to all */
    for (const id of identifiers) {
      this.getNode(id).rules.addRule(rule.conditions());
    }

    for (const d of rule.rules) {
      if (!isDirective(d)) continue;
      if (d.type == "run-in-sequence")
        for (let x = 1; x < identifiers.length; x++) {
          const prev = identifiers[x - 1];
          const cur = identifiers[x];

          const prevNode = this.getNode(prev);
          const curNode = this.getNode(cur);

          if (wildCard && curNode.rules.withDirectives().length) continue;

          this.setEdge(prevNode.identifier, curNode.identifier);
          curNode.rules.addRule(rule.directives());
        }
      else if (d.type == "run-after")
        for (const id of identifiers) {
          const dstNode = this.getNode(id);
          dstNode.rules.addRule(rule.directives());
          for (const src of d.identifiers || []) {
            const srcNode = this.getNode(src);
            this.setEdge(srcNode.identifier, dstNode.identifier);
          }
        }
      else if (d.type == "run-at-start")
        for (const id of identifiers) {
          const dstNode = this.getNode(id);

          if (wildCard && dstNode.rules.withDirectives().length) continue;

          const edges = this.graphlib.inEdges(id) || [];
          edges.forEach((e) => this.removeEdge(e.v, id));
          dstNode.rules.addRule(rule.directives());
        }
    }
  }

  setEdge(source: string, target: string) {
    const srcNode = this.getNode(source);
    const dstNode = this.getNode(target);

    this.graphlib.setEdge(source, target);

    // Prevent graph cycles
    if (!GLib.alg.isAcyclic(this.graphlib))
      this.graphlib.removeEdge(source, target);
    else {
      srcNode.addNext(dstNode);
      dstNode.addPrevious(srcNode);
    }
  }

  removeEdge(source: string, target: string) {
    const srcNode = this.getNode(source);
    const dstNode = this.getNode(target);

    this.graphlib.removeEdge(source, target);
    srcNode.removeNext(dstNode);
    dstNode.removePrevious(srcNode);
  }

  graph(): GLib.Graph {
    const graph = GLib.json.read(GLib.json.write(this.graphlib));

    graph.setNode("START", { label: "Start", height: 20, width: 50 });

    graph.sources().forEach((s) => {
      if (s != "START") graph.setEdge("START", s);
    });

    graph.setNode("END", { label: "End", height: 20, width: 50 });

    graph.sinks().forEach((s) => {
      if (s != "END") graph.setEdge(s, "END");
    });

    return graph;
  }

  generateRules(): Array<RuleBuilder> {
    const rules: any = [];
    for (const [id, node] of this.nodes) {
      if (node.previous.length > 0) {
        //const rule = new RuleBuilder()
        const rule: any = new RuleBuilder();
        rules.push(rule);
        rule.addIdentifer(id);
        rule.addRule({
          type: DirectiveType.runAfter,
          identifiers: node.previous.map((p) => p.identifier),
        });
      } else {
        const rule: any = new RuleBuilder();
        rules.push(rule);
        rule.addIdentifer(id);
        rule.addRule({
          type: DirectiveType.runAtStart,
        });
      }
    }
    return rules;
  }

  generateRulesFromGraphlib(): RuleSet {
    const rules = new RuleSet();
    for (const node of this.graphlib.nodes()) {
      const previous = this.graphlib.predecessors(node);
      if (previous && previous.length > 0) {
        const rule = new RuleBuilder();
        rules.addRule(rule);
        rule.addIdentifer(node);
        rule.addRule({
          type: DirectiveType.runAfter,
          identifiers: previous,
        });
      } else {
        const rule = new RuleBuilder();
        rules.addRule(rule);
        rule.addIdentifer(node);
        rule.addRule({
          type: DirectiveType.runAtStart,
        });
      }
    }

    for (const node of this.nodes.values()) {
      rules.addRules(node.rules.withConditions());
    }

    return rules;
  }
}

function isPNode(node: GraphNode): node is PNode {
  return node.hasOwnProperty("nodes");
}

export function layoutGraph(graph: WorkflowGraph) {
  const g = new Dagre.graphlib.Graph();
  g.setGraph({});
  g.setDefaultEdgeLabel(function () {
    return {};
  });

  for (const [k, node] of graph.nodes) {
    g.setNode(node.identifier, {
      label: node.identifier,
      width: 70,
      height: 20,
    });
  }

  for (const [k, node] of graph.nodes) {
    for (const next of node.next) g.setEdge(node.identifier, next.identifier);
  }
  Dagre.layout(g);

  return g;
}

class NodeNotFoundError extends Error {}
