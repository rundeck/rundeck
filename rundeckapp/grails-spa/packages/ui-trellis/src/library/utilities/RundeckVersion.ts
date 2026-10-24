interface RundeckVersionOptions {
  versionString?: string;
  versionDate?: string;
  colorIdentity?: string;
  nameIdentity?: string;
  iconIdentity?: string;
  appId?: string;
  serverName?: string;
}

interface VersionData {
  version?: string;
  major: number;
  minor: number;
  majorMinor: number;
  point: number;
  minorPoint: number;
  semantic: string;
  tag: string;
  release: number;
  pointRelease: number;
  minorPointRelease: number;
  full: number;
  [key: string]: string | number | undefined;
}

interface UUIDData extends Record<string, string | number | string[]> {
  uuid0: number;
  sixes: string[];
}

export class RundeckVersion {
  private versionString?: string;
  private versionData!: VersionData;
  private versionDate?: string;
  private colorIdentity: string;
  private nameIdentity: string;
  private iconIdentity: string;
  private appId: string;
  private serverName?: string;
  private nameTilt = 32;

  constructor(data: RundeckVersionOptions) {
    this.versionString = data["versionString"];
    this.versionDate = data["versionDate"];
    this.colorIdentity =
      data && data.colorIdentity ? data.colorIdentity : "minorPoint";
    this.nameIdentity =
      data && data.nameIdentity ? data.nameIdentity : "majorMinor";
    this.iconIdentity =
      data && data.iconIdentity ? data.iconIdentity : "minorPoint";
    this.appId = data && data["appId"] ? data["appId"] : "Rundeck";
    this.versionData = this.splitVersion(this.versionString);
    this.serverName = data.serverName;
  }

  static csscolors = [
    // "aquamarine",
    // "blue",
    "brown",
    // "burlywood",
    // "chartreuse",
    "coral",
    "deeppink",
    // "deepskyblue",
    "fuchsia",
    // "gold",
    "green",
    // "greenyellow",
    // "indigo",
    // "khaki",
    // "lime",
    "olivedrab",
    "orange",
    "orchid",
    "palevioletred",
    // "peachpuff",
    "peru",
    // "pink",
    // "plum",
    // "powderblue",
    "rebeccapurple",
    "red",
    // "rosybrown",
    "salmon",
    "sandybrown",
    // "silver",
    // "skyblue",
    "slategray",
    // "springgreen",
    // "tan",
    // "thistle",
    "turquoise",
    "violet",
    // "wheat",
    // "yellow",
    "yellowgreen",
  ];

  static glyphicons2 = [
    "bell",
    "book",
    "briefcase",
    "bullhorn",
    "camera",
    "cutlery",
    "flag",
    "flash",
    "gift",
    "globe",
    "headphones",
    "leaf",
    "music",
    "paperclip",
    "phone",
    "plane",
    "pushpin",
    "tower",
    "glass",
    "knight",
    "tent",
    "apple",
    "lamp",
    "piggy-bank",
    "grain",
    "sunglasses",
  ];

  //sorted
  static glyphicons3 = [
    "apple",
    "bell",
    "book",
    "briefcase",
    "bullhorn",
    "camera",
    "cutlery",
    "flag",
    "flash",
    "gift",
    "glass",
    "globe",
    "grain",
    "headphones",
    "knight",
    "lamp",
    "leaf",
    "music",
    "paperclip",
    "phone",
    "piggy-bank",
    "plane",
    "pushpin",
    "sunglasses",
    "tent",
    "tower",
  ];

  static names4 = [
    "Alicorn",
    "Banshee",
    "Big Foot",
    "Basilisk",
    "Chimera",
    "Chupacabra",
    "Demigorgon",
    "Eloi",
    "Firedrake",
    "Griffin",
    // 'Gorgon',
    "Gremlin",
    "Hobgoblin",
    "Hippogriff",
    "Imp",
    "Jörmungandr",
    "Kraken",
    "Kaiju",
    "Liger",
    "Manticore",
    "Murloc",
    "Nessie",
    "Ogre",
    "Orc",
    "Phoenix",
    "Quintaped",
    // 'R',
    "Sphinx",
    "Satyr",
    // 'T',
    "Unicorn",
    // 'V',
    "Wyvern",
  ];
  static names5 = [
    "Aconcagua",
    "Aoraki",
    "Carstenz",
    "Chimborazo",
    "Denali", //Dave Darby
    "Elbrus",
    "Erebus",
    "Etna",
    "Everest",
    "Fairweather",
    "Fansiban",
    "Foraker",
    "Fuji",
    "Hood", //Greg Schueler
    "K2",
    "Kilimanjaro",
    "Kinabalu",
    "Kirkjufell",
    "Logan",
    "Matterhorn",
    "Mauna Kea",
    "Mont Blanc",
    "Monte Fitz Roy",
    "Olympus",
    "Parinacota",
    "Provincia", //Luis Toledo
    "Rainier",
    "Shasta",
    "Stanley",
    "Tupungato",
    "Vinson",
    "Whitney",
    "Wycheproof",
  ];

  /** Rundeck 6.x release codenames (constellations) */
  static names6 = [
    "Andromeda",
    "Aquila",
    "Auriga",
    "Carina",
    "Cassiopeia",
    "Centaurus",
    "Columba",
    "Cygnus",
    "Delphinus",
    "Draco",
    "Eridanus",
    "Fornax",
    "Hercules",
    "Hydrus",
    "Indus",
    "Lacerta",
    "Lupus",
    "Lyra",
    "Monoceros",
    "Octans",
    "Ophiuchus",
    "Orion",
    "Pavo",
    "Pegasus",
    "Perseus",
    "Phoenix",
    "Pyxis",
    "Sagittarius",
    "Vela",
    "Volans",
  ];

  splitVersion(versionString?: string): VersionData {
    const partsa = String(versionString).split(" ");
    const version = partsa.length > 1 ? partsa[0] : versionString;
    const parts = String(version).split("-");
    const vparts = parts[0].split(".");
    const data: VersionData = {
      version,
      major: 0,
      minor: 0,
      majorMinor: 0,
      point: 0,
      minorPoint: 0,
      semantic: "",
      tag: "",
      release: 1,
      pointRelease: 0,
      minorPointRelease: 0,
      full: 0,
    };
    if (vparts.length > 0) {
      data["major"] = parseInt(vparts[0]);
    } else {
      data["major"] = 0;
    }
    if (vparts.length > 1) {
      data["minor"] = parseInt(vparts[1]);
    } else {
      data["minor"] = 0;
    }
    data["majorMinor"] = data.major * 100 + data.minor;
    if (vparts.length > 2) {
      data["point"] = parseInt(vparts[2]);
    } else {
      data["point"] = 0;
    }
    data["minorPoint"] = data.minor * 5 + data.point;
    let release = 1;
    let tag = "";
    if (parts.length > 1 && /^\d+$/.test(parts[1])) {
      release = parseInt(parts[1]);
      tag = parts.length > 2 ? parts.slice(2).join("-") : "";
    } else if (parts.length > 1) {
      tag = parts.slice(1).join("-");
    }

    data["semantic"] = parts[0];
    data["tag"] = tag;
    data["release"] = release;
    data["pointRelease"] = data.point * 20 + release;
    data["minorPointRelease"] = data.minor * 100 + data.point * 20 + release;
    data["full"] = data.major * 100 + data.minor * 20 + data.point;
    return data;
  }

  splitUUID(versionString: string): UUIDData {
    const partsa = String(versionString).split("-");
    const data: UUIDData = {
      uuid: versionString,
      uuid0: 0,
      sixes: [],
    };
    for (let i = 0; i < partsa.length; i++) {
      data["uuid" + i] = parseInt(partsa[i].substring(0, 2), 16);
      data["hexuuid" + i] = partsa[i];
    }
    const partsb = partsa.join("");
    const sixes = data.sixes;
    for (let j = 0; (j + 1) * 6 < partsb.length; j++) {
      data["6let" + j] = partsb.substring(j * 6, (j + 1) * 6);
      sixes.push(partsb.substring(j * 6, (j + 1) * 6));
    }
    data["sixes"] = sixes;
    return data;
  }

  stripeBg(color: string, px1: number, colorb: string, px2: number): string {
    return (
      "repeating-linear-gradient(" +
      "-45deg, " +
      color +
      ", " +
      color +
      " " +
      px1 +
      "px, " +
      colorb +
      " " +
      px1 +
      "px, " +
      colorb +
      " " +
      px2 +
      "px " +
      ")"
    );
  }
  /**
   * return value from list % list length
   * @param list
   * @param val number
   */
  inList<T>(list: T[], val: number): T {
    return list[val % list.length];
  }

  colorForVersion(val: number) {
    return this.inList(RundeckVersion.csscolors, val);
  }

  private static nameListForMajor(major: number) {
    return major >= 6 ? RundeckVersion.names6 : RundeckVersion.names5;
  }

  nameForVersion(val: number) {
    const major = this.versionData.major;
    return this.inList(RundeckVersion.nameListForMajor(major), val);
  }
  nameForVersion4(val: number) {
    return this.inList(RundeckVersion.names4, val);
  }

  iconForVersion(val: number) {
    return this.inList(RundeckVersion.glyphicons3, val);
  }

  iconForVersion2(val: number) {
    return this.inList(RundeckVersion.glyphicons2, val);
  }

  data() {
    return this.versionData;
  }

  versionColor() {
    return this.colorForVersion(this.versionData[this.colorIdentity] as number);
  }

  versionSemantic() {
    return this.versionData["semantic"];
  }

  versionName() {
    return this.nameForVersion(
      (this.versionData[this.nameIdentity] as number) + this.nameTilt,
    );
  }

  versionIcon() {
    return this.iconForVersion(this.versionData[this.iconIdentity] as number);
  }

  text() {
    const sep = " ";
    return [this.versionName(), this.versionColor(), this.versionIcon()]
      .join(sep)
      .toLowerCase(); /*.replace(/[^a-z]/g, sep)*/
  }
}
