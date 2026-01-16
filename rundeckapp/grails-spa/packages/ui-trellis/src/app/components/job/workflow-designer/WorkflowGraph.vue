<template>
  <div style="height: 100%; overflow: hidden">
    <div
      style="
        display: flex;
        height: 100%;
        width: 100%;
        position: relative;
        border: solid 1px black;
      "
    >
      <div style="width: 100%; position: relative">
        <div ref="canvas" style="width: 100%; height: 100%"></div>
        <div style="position: absolute; top: 10px; right: 10px">
          <div class="btn-group">
            <div class="btn btn-default" @click="scaleContentToFit">
              <i class="fa fa-crosshairs" />
            </div>
            <div v-if="!editing" class="btn btn-default" @click="edit">
              <i class="fa fa-pen" style="transform: translate(0, -5px)" />
              <div
                style="
                  border-radius: 500px;
                  padding: 1px;
                  position: absolute;
                  bottom: -5px;
                  left: 5px;
                  font-weight: 800;
                  color: var(--warning-color);
                "
              >
                Beta!
              </div>
            </div>
          </div>
          <div
            v-if="editing"
            class="btn btn-cta"
            @click="commit"
            style="margin-left: 5px"
          >
            Commit
          </div>
          <div
            v-if="editing"
            class="btn btn-default"
            @click="revert"
            style="margin-left: 5px"
          >
            Revert
          </div>
        </div>
        <div style="position: absolute; top: 0">
          <code class="text-danger" style="background-color: #0000">{{
            error
          }}</code>
        </div>
      </div>
      <div
        style="
          width: 600px;
          box-shadow: rgba(0, 0, 0, 0.15) 2px 0px 8px 0px;
          z-index: 100;
          padding: 10px;
          height: 100%;
        "
      >
        <Tabs style="height: 100%">
          <Tab :index="0" title="Rules">
            <div style="padding: 5px; height: 100%">
              <div v-if="editorElm" ref="editor" style="height: 100%"/>
              <AceEditor
                identifier="wf_editor"
                v-else-if="!editorElm"
                v-model="rulesInternal"
                :read-only="false"
                lang="javascript"
                height="500px"
                width="100%"
                @init="aceInit"
              />
            </div>
          </Tab>
        </Tabs>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Tab, Tabs} from '@rundeck/ui-trellis/lib/components/containers/tabs'
import AceEditor from '@rundeck/ui-trellis/lib/components/utils/AceEditor.vue'

import * as Ace from 'ace-builds'

import * as dagre from 'dagre'
import * as graphlib from 'graphlib'
import {Graph} from 'graphlib'
import * as Joint from 'jointjs'
import type {PropType} from 'vue'
import {defineComponent} from 'vue'
import NodeEditor from './components/NodeEditor.vue'

import {WorkflowStep} from './elements/Step'
import GeneratedRules from './GeneratedRules.vue'

import {GNode, NodeDescription, WorkflowGraph} from './Graph'
import {RuleSetParser} from './RuleSetParser'

const ROUTER = "normal";

const MIN_SCALE = 0.05;
const MAX_SCALE = 1;

let transitions = 0;

const SEPERATION_RANK = 50;

const TRANSITION_SPEED = 300;

/**
 * Run callback during future animation frame.
 */
const framesLater = (f: () => void, frames: number) => {
  return new Promise<void>((res, rej) => {
    window.requestAnimationFrame(async () => {
      if (frames >= 0) {
        frames--;
        await framesLater(f, frames);
      } else {
        try {
          f();
          res();
        } catch (e) {
          rej(e);
        }
      }
    });
  });
};

export default defineComponent({
  name: "WorkflowGraph",
  components: {
    AceEditor,
    Tabs,
    Tab,
    GeneratedRules,
    NodeEditor,
  },
  emits: ['update:modelValue'],
  props: {
    modelValue: {
      type: String,
      default: "",
    },
    nodes: {
      type: Array as PropType<Array<{ identifier: string; label: string }>>,
      default: () => [],
    },
    intersectRoot: {
      type: HTMLElement,
    },
    editorElm: {
      type: HTMLElement,
    },
  },

  data() {
    return {
      paper: {} as Joint.dia.Paper,
      dia: {} as Joint.dia.Graph,
      rulesInternal: "" as string,
      rulesOriginal: "" as string,
      rules: "" as string,
      graph: {} as WorkflowGraph,
      error: "",
      selectedNode: undefined as GNode | undefined,
      editing: false,
      editor: {} as Ace.Ace.Editor,
      interactive: false,
      inited: false,
    };
  },

  watch: {
    modelValue(newVal) {
      this.rulesInternal = newVal;
    },
    rulesInternal(newVal) {
      if (newVal != this.rules) {
        this.updateRules(newVal, true)
        this.emitChange(newVal)
      }
    },
    nodes(newVal) {
      this.updateNodes(newVal);
      this.updateRules(this.rulesInternal);
      this.updateGraph();
      // this.layout()
    },
    interactive(newVal) {
      this.dia
        .getElements()
        .filter((e) => !["START", "END"].includes(e.id as string))
        .forEach((e) => e.attr("root/rundeck-interactive", newVal));
    },
  },

  methods: {
    emitChange(newVal: string) {
      this.$emit('update:modelValue', newVal)
    },
    edit() {
      this.editing = true;
      this.interactive = true;
      this.rulesOriginal = this.rulesInternal;
      this.editor.setReadOnly(true);
    },
    commit() {
      this.editing = false;
      this.interactive = false;
      const session = this.editor;
      this.updateRules(this.rules);
      session.setValue(this.rules);
      this.rulesInternal = this.rules
      // manually emit change, since the rulesInternal watcher will skip it, and we also don't want a transition
      this.emitChange(this.rules)
      this.editor.setReadOnly(false);
    },
    revert() {
      this.editing = false;
      this.interactive = false;

      this.rulesInternal = this.rulesOriginal;
      this.updateRules(this.rulesInternal);
      this.editor.setReadOnly(false);
    },
    updateRules(rules: string, transition = false) {
      if (!this.inited) {
        return;
      }
      try {
        const ruleSet = RuleSetParser.ParseRules(rules);
        this.graph.applyRules(ruleSet);
        this.updateGraph(transition);
        // this.layout()
        this.error = "";
      } catch (e: any) {
        this.error = e.message;
      }
    },
    updateOutputRules() {
      const ruleSet = this.graph.generateRulesFromGraphlib();

      const directives = ruleSet
        .withDirectives()
        .map((r) => r.toString())
        .join("\n");
      const conditions = ruleSet
        .withConditions()
        .map((r) => r.toString())
        .join("\n");

      this.rules = `# Autogenerated Directives\n${directives}\n\n# Autogenerated Conditions\n${conditions}`;
    },
    updateNodes(nodes: Array<{ identifier: string; label: string }>) {
      try {
        const nodeMapFrom = nodes.map((entry) => {
          return [entry.identifier, { ...entry, height: 20, width: 50 }] as [
            string,
            NodeDescription,
          ];
        });
        const nodeMap = new Map(nodeMapFrom);
        this.graph.setNodes(nodeMap);
      } catch (e: any) {
        this.error = e.message;
      }
    },
    stopTransitions() {
      this.dia.getElements().forEach((e) => {
        if (e.getTransitions().length > 0) {
          e.getTransitions().forEach((t) => e.stopTransitions(t));
        }
      });
    },
    scaleContentToFit() {
      this.paper.transformToFitContent({
        padding: 25,
        maxScale: 1,
        preserveAspectRatio: true,
      });
    },
    /** Resize element to fit width of rendered SVG text. */
    fitText() {
      this.dia.getElements().forEach((el) => {
        /**
         * Fit model to text.
         * Get the bounding box of the CellView, which includes the text element, then adjust
         * for paper scale and update the model width to fit.
         */
        if (el.isLink()) return;

        const cellView = el.findView(this.paper as Joint.dia.Paper);

        const label = cellView.findBySelector(
          "label",
        )[0] as any as SVGGraphicsElement;
        const title = cellView.findBySelector(
          "title",
        )[0] as any as SVGGraphicsElement;

        const bbox = [
          label.getBBox(),
          title ? title.getBBox() : { width: 0 },
        ].reduce((a, b) => (a.width > b.width ? a : b));

        if (Math.abs(bbox.width + 45 - el.size().width) > 1) {
          const size = [bbox.width + 5 + 40, 40] as const;
          el.size(...size);
        }
      });
    },
    layout(transition = false) {
      this.stopTransitions();

      Joint.layout.DirectedGraph.layout(this.dia as Joint.dia.Graph, {
        dagre,
        graphlib,
        nodeSep: 50,
        rankSep: SEPERATION_RANK,
        edgeSep: 50,
        rankDir: "TB",
        setVertices: !transition
          ? true
          : (link, vertices) => {
              const polyline = new Joint.g.Polyline(
                vertices as Joint.g.Point[],
              );
              polyline.simplify({ threshold: 0.001 });
              const polylinePoints = polyline.points.map((point) =>
                point.toJSON(),
              ); // JSON of points after simplification
              const numPolylinePoints = polylinePoints.length; // number of points after simplification
              // set simplified polyline points as link vertices
              // remove first and last polyline points (= source/target sonnectionPoints)
              // link.set('vertices', polylinePoints.slice(1, numPolylinePoints - 1));

              link.transition(
                "vertices",
                polylinePoints.slice(1, numPolylinePoints - 1),
                {
                  duration: TRANSITION_SPEED,
                  timingFunction: Joint.util.timing.inout,
                  valueFunction: function (a, b) {
                    /** If old and new vertices list has the same length lerp the points
                     * otherwise return the new list.
                     */
                    return function (t) {
                      if (!a) {
                        return b;
                      } else {
                        if (a.length == b.length) {
                          return a.map((p: Joint.g.PlainPoint, i: number) => {
                            const lerp = new Joint.g.Point(p).lerp(
                              new Joint.g.Point(b[i]),
                              t,
                            );
                            return lerp;
                          });
                        } else {
                          return b;
                        }
                      }
                    };
                  },
                },
              );
              transitions++;
              // link.transition('vertices[0]/y', vertices[0].y, {duration: 50, timingFunction: Joint.util.timing.inout})
            },
        setPosition: function (elm, pos) {
          if (!transition) {
            elm.position(pos.x - pos.width / 2, pos.y - pos.height / 2);
            return;
          }

          elm.transition("position/x", pos.x - pos.width / 2, {
            duration: TRANSITION_SPEED,
            timingFunction: Joint.util.timing.inout,
          });
          transitions++;

          elm.transition("position/y", pos.y - pos.height / 2, {
            duration: TRANSITION_SPEED,
            timingFunction: Joint.util.timing.inout,
          });
          transitions++;
        },
      });
      // Ensure nodes are drawn over link arrows
      this.dia.getElements().forEach((e) => e.toFront());
      this.scaleContentToFit();
    },
    updateGraph(transition = false) {
      this.dia.getElements().forEach((e) => this.dia.removeLinks(e));

      const graphlib = this.graph.graph();
      this.updateOutputRules();

      this.dia
        .getElements()
        .filter((e) => e.isElement())
        .forEach((e) => {
          if (!graphlib.nodes().includes(e.id.toString())) {
            e.remove();
          }
        });

      this.dia.fromGraphLib(graphlib, {
        importNode: (n: string, gl: Graph, g: Joint.dia.Graph) => {
          const node = gl.node(n);

          if (["START", "END"].includes(n)) {
            new Joint.shapes.standard.Circle(
              {
                id: n,
                position: { x: node.x, y: node.y },
                size: { width: node.width, height: node.height },
                attrs: {
                  body: {
                    fill: "var(--bg-success)",
                    stroke: "var(--success-color)",
                  },
                  label: {
                    text: node.label,
                    fontWeight: 600,
                    fill: "var(--font-fill)",
                  },
                },
              },
              { draggable: false },
            ).addTo(g);
            return;
          }

          let labelText = `${n}.`;
          if (node.label) labelText += ` ${node.label}`;

          const curNode = g.getCell(n);

          const label = {
            text: labelText,
            "font-size": "14",
          };

          const title = {
            text: node.title || "",
            fontSize: "10",
          };

          let icon = {
            class: node.icon.image ? '' : node.icon.class,
            style: {display: !node.icon.image ? 'block' : 'none'},
          } as any;
          if (node.icon.class?.indexOf('glyphicon') >= 0 || node.icon.class?.indexOf('fas') >= 0 || node.icon.class?.indexOf('fab') >= 0) {
            icon.style.fontSize = '30px';
          }
          const attrs = {
            image: {
              src: node.icon.image || null,
              style: { display: node.icon.image ? "block" : "none" },
            },
            icon,
            label,
            title,
            body: { rx: 5, ry: 5, "stroke-alignment": "inner" },
            root: { "rundeck-interactive": this.interactive },
          };

          if (curNode) {
            curNode.attr(attrs);
          }

          const el = new WorkflowStep({
            id: n,
            position: { x: node.x, y: node.y },
            size: { width: node.width, height: node.height },
            attrs,
          }).addTo(g);

          const cellView = el.findView(this.paper as Joint.dia.Paper);
          const elLabel = cellView.findBySelector(
            "label",
          )[0] as any as SVGGraphicsElement;

          /** Detect change in label element size */
          const reszObs = new ResizeObserver((entries) => {});
          reszObs.observe(elLabel);
        },
        importEdge: (edge: any, gl: Graph, g: Joint.dia.Graph) => {
          const src = g.getCell(edge.v);
          const target = g.getCell(edge.w);

          if (target && target.isElement() && src && src.isElement()) {
            // @ts-ignore

            if (g.getNeighbors(target).indexOf(src) > -1) {
              const link = g
                .getConnectedLinks(target)
                .filter((l) => l.getSourceElement() === src);
              return;
            }
          }

          const link = new Joint.shapes.standard.Link({
            source: { id: edge.v },
            target: { id: edge.w },
            attrs: {
              line: { stroke: "#BCBCBC" },
              root: { "rundeck-interactive": this.interactive },
            },
          })
            .router(ROUTER)
            .addTo(g);
          link.connector("jumpover", { size: 8 });

          /** Add link tools if graph is interactive */
          if (!["START", "END"].some((i) => [edge.v, edge.w].includes(i))) {
            let tools = new Joint.dia.ToolsView({
              tools: [
                new Joint.linkTools.Remove({
                  distance: "50%", // Place button at link mid-point
                  action: (evt, view) => {
                    const src = view.model.source().id as string;
                    const dst = view.model.target().id as string;
                    this.graph.removeEdge(src, dst);
                    this.updateGraph(true);
                    // this.layout(true)
                  },
                }),
              ],
            });
            link
              .findView(this.paper as Joint.dia.Paper)
              .addTools(tools)
              .hideTools();
          } else {
            /** Create dashed link lines for start and end */
            link.attr("line/strokeDasharray", "2,2");
          }
        },
      });

      this.fitText();
      this.layout(transition);

      framesLater(() => {
        this.fitText();
        this.layout(transition);
      }, 2);
    },
    scaleToPoint(nextScale: number, x: number, y: number) {
      if (nextScale >= MIN_SCALE && nextScale <= MAX_SCALE) {
        const currentScale = this.paper.scale().sx;

        const beta = currentScale / nextScale;

        const ax = x - x * beta;
        const ay = y - y * beta;

        const translate = this.paper.translate();

        const nextTx = translate.tx - ax * nextScale;
        const nextTy = translate.ty - ay * nextScale;

        this.paper.translate(nextTx, nextTy);

        const ctm = this.paper.matrix();

        ctm.a = nextScale;
        ctm.d = nextScale;

        this.paper.matrix(ctm);
      }
    },
    handleCanvasMouseWheel(e: any, x: number, y: number, delta: number) {
      e.preventDefault();

      let { originalEvent } = e;

      /** FireFox may throw in horizontal scroll events which we discard */
      if (
        originalEvent.axis &&
        originalEvent.axis == originalEvent.HORIZONTAL_AXIS
      )
        return;

      /** Chrome and Safari */
      if ("wheelDeltaY" in originalEvent)
        delta = originalEvent.wheelDeltaY / 120;

      if (e.shiftKey) {
        const origin = this.paper.translate();
        this.paper.translate(origin.tx + delta * 20, origin.ty);
      } else if (e.ctrlKey) {
        const oldScale = this.paper.scale().sx;
        const newScale = oldScale + delta * 0.1;

        this.scaleToPoint(newScale, x, y);
      } else {
        const origin = this.paper.translate();
        this.paper.translate(origin.tx, origin.ty + delta * 40);
      }
    },
    aceInit(editor: Ace.Ace.Editor) {
      this.editor = editor
    }
  },

  created() {
    this.rulesInternal = this.modelValue
  },

  mounted() {
    let dia = (this.dia = new Joint.dia.Graph());

    const paper = (this.paper = new Joint.dia.Paper({
      el: this.$refs["canvas"],
      async: false,
      model: dia,
      gridSize: 1,
      background: {
        color: "var(--background-color)",
      },
      width: "100%",
      height: "100%",
      interactive: (cellView: Joint.dia.CellView) => {
        if (!this.interactive) return false;

        if (!cellView.model.isElement()) return true;

        if (["START", "END"].includes(cellView.model.id as string))
          return false;

        return true;
      },
    } as Joint.dia.Paper.Options));

    window.addEventListener("resize", () => {
      this.scaleContentToFit();
    });

    paper.on("link:mouseenter", (linkView) => {
      if (this.interactive) linkView.showTools();
    });

    paper.on("link:mouseleave", function (linkView) {
      linkView.hideTools();
    });

    // this.updateGraph()
    // this.layout()

    if (this.intersectRoot) {
      const observer = new IntersectionObserver(
        () => {
          this.dia.getElements().forEach((e) => {
            e.getTransitions().forEach((t) => e.stopTransitions(t));
            e.remove();
          });
          this.updateGraph();
          this.layout();
        },
        { root: this.intersectRoot },
      );
      observer.observe(this.$refs["canvas"] as HTMLElement);
    }

    // let rules = () => {
    //     alert('Rules')
    //     graph.graphlib = dia.toGraphLib({graphlib})
    //     this.rules = graph.generateRulesFromGraphlib().map(r => r.toString()).join('\n')
    //     this.layout()
    // }

    // rules()

    dia.on("remove", (cell) => {
      // TODO: Something
    });

    dia.on("add", function (cell) {
      // TODO: Update graph
    });

    dia.on("transition:end", (...args) => {
      transitions--;
      if (!transitions) {
        this.scaleContentToFit();
      }
    });

    paper.on({
      scale: function (...args) {
        // TODO: Some action on scale event
      },

      "cell:mousewheel": (event, e, x, y, delta) => {
        this.handleCanvasMouseWheel(e, x, y, delta);
      },

      "blank:mousewheel": (e, x, y, delta) => {
        this.handleCanvasMouseWheel(e, x, y, delta);
      },

      "blank:pointermove": (evt, x, y) => {
        const deltaX = (<PointerEvent>evt.originalEvent)?.movementX;
        const deltaY = (<PointerEvent>evt.originalEvent)?.movementY;

        const origin = this.paper.translate();
        this.paper.translate(origin.tx + deltaX, origin.ty + deltaY);
      },

      "link:connect": () => {
        alert("Connect");
      },

      "blank:pointerclick": () => {
        dia
          .getElements()
          .forEach((e) => e.attr("body/rundeck-highlight", false));
        // this.paper.scaleContentToFit({padding: 25})
      },

      "element:pointerclick": (elementView: Joint.dia.ElementView, evt) => {
        if (!this.interactive) return;

        if (["START", "END"].includes(elementView.model.id as string)) return;

        dia
          .getElements()
          .forEach((e) => e.attr("body/rundeck-highlight", false));
        elementView.model.attr("body/rundeck-highlight", true);
        this.selectedNode = this.graph.getNode(elementView.model.id as string);
      },

      "element:pointerdown": function (
        elementView: Joint.dia.ElementView,
        evt,
      ) {
        // @ts-ignore
        if (!this.interactive) return;

        if (["START", "END"].includes(elementView.model.id as string)) return;

        // Ensure captured element and links are drawn on top
        dia.getConnectedLinks(elementView.model).forEach((l) => l.toFront());
        elementView.model.toFront();

        // elementView.model.attr('body/rundeck-selected', true)
        evt.data = elementView.model.position();
      },

      "element:pointermove": (
        elementView: Joint.dia.ElementView,
        evt,
        x,
        y,
      ) => {
        if (!this.interactive) return;

        if (["START", "END"].includes(elementView.model.id as string)) return;

        elementView.model.toFront();

        dia.getConnectedLinks(elementView.model).forEach((l) => {
          /**
           * Move bottom link verts(elbows) with element
           */
          const elPos = elementView.model.position();
          const linkSrcPost = l.getSourcePoint();
          const verts = l.vertices();
          if (linkSrcPost.y > elPos.y && verts.length > 0)
            l.vertices([
              { x: linkSrcPost.x, y: linkSrcPost.y + SEPERATION_RANK },
            ]);

          // Highlight links connected to dragged element
          l.attr("line/rundeck-selected", true);
        });
        elementView.model.attr("body/rundeck-selected", true);

        const intersects = [] as Array<{
          el: Joint.dia.Element;
          intersect: Joint.g.Rect;
        }>;

        dia.getElements().forEach((el) => {
          if (
            elementView.model === el ||
            ["START", "END"].includes(el.id as string)
          )
            return;

          const intersect = elementView.model.getBBox().intersect(el.getBBox());

          if (intersect) intersects.push({ el, intersect });
          else el.attr("body/rundeck-selected", false);
        });

        if (intersects.length) {
          intersects.sort((a, b) => {
            return a.intersect.width * a.intersect.height >
              b.intersect.width * b.intersect.height
              ? 1
              : -1;
          });

          intersects.pop()!.el.attr("body/rundeck-selected", true);
          intersects.forEach((i) => i.el.attr("body/rundeck-selected", false));
        }
      },

      "element:pointerup": (elementView: Joint.dia.ElementView, evt, x, y) => {
        if (!this.interactive) return;
        dia
          .getConnectedLinks(elementView.model)
          .forEach((l) => l.attr("line/rundeck-selected", false));
        elementView.model.attr("body/rundeck-selected", "false");
        let coordinates = new Joint.g.Point(x, y);
        let elementAbove = elementView.model;
        if (!evt.data || elementAbove.position().equals(evt.data)) {
          return;
        }
        // @ts-ignore
        let elementBelow = paper.model
          .findModelsFromPoint(coordinates)
          .find(function (el) {
            // elementView.model.attr('/rundeck-selected', 'false')
            return el.id !== elementAbove.id;
          });

        const intersects = [] as Array<{
          el: Joint.dia.Element;
          intersect: Joint.g.Rect;
        }>;
        dia.getElements().forEach((el) => {
          el.attr("body/rundeck-selected", false);

          if (el.id === elementView.model.id) return;

          const intersect = elementView.model.getBBox().intersect(el.getBBox());

          if (intersect) intersects.push({ el, intersect });
        });

        if (intersects.length) {
          intersects.sort((a, b) => {
            return a.intersect.width * a.intersect.height >
              b.intersect.width * b.intersect.height
              ? 1
              : -1;
          });
          elementBelow = intersects.pop()!.el;
        }

        if (
          elementBelow &&
          ["START", "END"].includes(elementBelow.id as string)
        )
          return;

        // If the two elements are connected already, don't
        // connect them again (this is application-specific though).
        if (
          elementBelow &&
          dia.getNeighbors(elementBelow).indexOf(elementAbove) === -1
        ) {
          this.graph.setEdge(
            elementBelow.id.toString(),
            elementAbove.id as string,
          );

          this.updateGraph(true);
          this.updateOutputRules();

          // Create a connection between elements.

          // var link = new Joint.shapes.standard.Link();
          // link.source(elementBelow);
          // link.target(elementAbove);
          // link.router(ROUTER)
          // link.addTo(dia);

          // Add remove button to the link.
          // var tools = new Joint.dia.ToolsView({
          //     tools: [new Joint.linkTools.Remove()]
          // });
          // @ts-ignore
          // link.findView(this).addTools(tools).hideTools();
        } else {
          // Move the element to the position before dragging.
          elementAbove.position(evt.data.x, evt.data.y);
          this.layout(true);
        }
      },
    });

    const initialNodes = this.nodes.length != 0 ? this.nodes : [];
    const initialRules = this.modelValue || "";

    this.graph = new WorkflowGraph(new Map([]));

    if (this.editorElm) {
      this.editorElm.parentElement?.removeChild(this.editorElm);
      const editor = this.$refs.editor as HTMLElement;
      editor.appendChild(this.editorElm);

      const win = window as any;

      let self = this;
      setTimeout(function () {
        const win = window as any;
        self.editor = win.ace.edit(
          document.getElementById("_id1"),
        ) as Ace.Ace.Editor;
      }, 1000);
    }
    this.inited = true;
    this.updateNodes(initialNodes);
    this.updateRules(initialRules, false);
  },
});
</script>

<style lang="scss">
[rundeck-interactive="true"] {
  * {
    cursor: pointer;
  }
}

[rundeck-interactive="false"] {
  * {
    cursor: not-allowed;
  }
}

[rundeck-selected="true"] {
  // stroke: skyblue !important;
  stroke: var(--info-color) !important;
}

[rundeck-highlight="true"] {
  stroke: var(--primary-color);
  stroke-width: 3;
}

[rundeck-running="true"] {
  stroke: var(--warning-color);
  animation: activeStroke 1s infinite;
}

@keyframes activeStroke {
  0% {
    stroke: var(--border-color);
  }
  50% {
    stroke: var(--warning-color);
  }
  100% {
    stroke: var(--border-color);
  }
}
</style>

<style scoped lang="scss">
:deep(.rdtabs) {
  display: flex;
  flex-direction: column;
}

:deep(.rdtabs__pane) {
  flex-grow: 1;
}
</style>
