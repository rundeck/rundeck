interface CustomField {
    key?: string;
    label?: string;
    value?: string;
    desc?: string;
}

interface PropGroup {
    name?: string;
    secondary: boolean;
    props: any[];
}

interface Prop {
    type: string;
    defaultValue: any;
    title: string;
    required: boolean;
    options: any;
    allowed: string;
    name: string;
    desc: string;
    staticTextDefaultValue: string;
}

export type { CustomField, Prop, PropGroup };
