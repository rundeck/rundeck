// @ts-nocheck
import { dia } from "jointjs";

export class WorkflowStep extends dia.Element {
  defaults() {
    return {
      ...super.defaults,
      type: "rundeck.WorkflowStep",
      attrs: {
        body: {
          refWidth: "100%",
          refHeight: "100%",
          strokeWidth: 2,
          stroke: "var(--border-color)",
          fill: "var(--background-color-lvl2)",
        },
        // image: {
        //     // refWidth: '30%',
        //     refHeight: -10,
        //     // refY: '50%',
        //     x: 5,
        //     y: 5,
        //     preserveAspectRatio: 'xMidYMin'
        // },
        image: {
          style: {
            height: "100%",
            borderRadius: "5px",
          },
        },
        fo: {
          width: "40",
          refHeight: "100%",
          x: 0,
          y: 0,
          preserveAspectRatio: "xMinYMin",
        },
        label: {
          textVerticalAnchor: "top",
          textAnchor: "left",
          y: 5,
          x: 40,
          fontSize: 14,
          fill: "var(--font-color)",
        },
        title: {
          textVerticalAnchor: "bottom",
          textAnchor: "left",
          refY: "100%",
          refY2: "-5",
          x: 40,
          fontSize: 14,
          fill: "var(--font-color)",
        },
      },
    };
  }

  markup = [
    {
      tagName: "rect",
      selector: "body",
    },
    // {
    //     tagName: 'image',
    //     selector: 'image',
    // },
    {
      tagName: "foreignObject",
      selector: "fo",
      children: [
        {
          tagName: "div",
          namespaceURI: "http://www.w3.org/1999/xhtml",
          style: {
            display: "flex",
            height: "100%",
            width: "40px",
            alignItems: "center",
            padding: "5px",
          },
          children: [
            {
              tagName: "i",
              selector: "icon",
            },
            {
              tagName: "img",
              selector: "image",
            },
          ],
        },
      ],
    },
    {
      tagName: "text",
      selector: "title",
    },
    {
      tagName: "text",
      selector: "label",
    },
  ];
}
