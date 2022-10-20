export class RundeckVersion {
  private versionString: string
  private versionData: { [key: string]: any }
  private versionDate: string
  private colorIdentity: string
  private nameIdentity: string
  private iconIdentity: string
  private appId: string
  private serverName?: string
  private nameTilt = 21

  constructor(data: any) {
    this.versionString = data['versionString']
    this.versionData = {}
    this.versionDate = data['versionDate']
    this.colorIdentity = data && data.colorIdentity ? data.colorIdentity : 'minorPoint'
    this.nameIdentity = data && data.nameIdentity ? data.nameIdentity : 'majorMinor'
    this.iconIdentity = data && data.iconIdentity ? data.iconIdentity : 'minorPoint'
    this.appId = data && data['appId'] ? data['appId'] : 'Rundeck'
    this.versionData = this.splitVersion(this.versionString)
    this.serverName = data.serverName
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
    "yellowgreen"
  ]

  static glyphicons2 = [
    'bell',
    'book',
    'briefcase',
    'bullhorn',
    'camera',
    'cutlery',
    'flag',
    'flash',
    'gift',
    'globe',
    'headphones',
    'leaf',
    'music',
    'paperclip',
    'phone',
    'plane',
    'pushpin',
    'tower',
    'glass',
    'knight',
    'tent',
    'apple',
    'lamp',
    'piggy-bank',
    'grain',
    'sunglasses'
  ]

  //sorted
  static glyphicons3 = [
    'apple',
    'bell',
    'book',
    'briefcase',
    'bullhorn',
    'camera',
    'cutlery',
    'flag',
    'flash',
    'gift',
    'glass',
    'globe',
    'grain',
    'headphones',
    'knight',
    'lamp',
    'leaf',
    'music',
    'paperclip',
    'phone',
    'piggy-bank',
    'plane',
    'pushpin',
    'sunglasses',
    'tent',
    'tower'
  ]

  static names4 = [
    'Alicorn',
    'Banshee',
    'Big Foot',
    'Basilisk',
    'Chimera',
    'Chupacabra',
    'Demigorgon',
    'Eloi',
    'Firedrake',
    'Griffin',
    // 'Gorgon',
    'Gremlin',
    'Hobgoblin',
    'Hippogriff',
    'Imp',
    'Jörmungandr',
    'Kraken',
    'Kaiju',
    'Liger',
    'Manticore',
    'Murloc',
    'Nessie',
    'Ogre',
    'Orc',
    'Phoenix',
    'Quintaped',
    // 'R',
    'Sphinx',
    'Satyr',
    // 'T',
    'Unicorn',
    // 'V',
    'Wyvern',

  ]

  splitVersion(versionString: string): { [p: string]: any } {
    let partsa = String(versionString).split(' ')
    let version = partsa.length > 1 ? partsa[0] : versionString
    let parts = String(version).split('-')
    let vparts = parts[0].split('\.')
    let data: { [key: string]: any } = {version: version}
    if (vparts.length > 0) {
      data['major'] = parseInt(vparts[0])
    } else {
      data['major'] = 0
    }
    if (vparts.length > 1) {
      data['minor'] = parseInt(vparts[1])
    } else {
      data['minor'] = 0
    }
    data['majorMinor'] = (data.major * 100) + data.minor
    if (vparts.length > 2) {
      data['point'] = parseInt(vparts[2])
    } else {
      data['point'] = 0
    }
    data['minorPoint'] = (data.minor * 5) + data.point
    let release = 1
    let tag = ''
    if (parts.length > 1 && /^\d+$/.test(parts[1])) {
      release = parseInt(parts[1])
      tag = parts.length > 2 ? parts.slice(2).join('-') : ''
    } else if (parts.length > 1) {
      tag = parts.slice(1).join('-')
    }

    data['semantic'] = parts[0]
    data['tag'] = tag
    data['release'] = release
    data['pointRelease'] = data.point * 20 + release
    data['minorPointRelease'] = (data.minor * 100) + data.point * 20 + release
    data['full'] = data.major * 100 + data.minor * 20 + data.point
    return data
  }

  splitUUID(versionString: string) {
    let partsa = String(versionString).split('-')
    let apart = partsa.length > 0 ? partsa[0].substring(0, 2) : versionString
    let data: { [key: string]: any } = {uuid: versionString}
    for (let i = 0; i < partsa.length; i++) {
      data['uuid' + i] = parseInt(partsa[i].substring(0, 2), 16)
      data['hexuuid' + i] = partsa[i]
    }
    let partsb = partsa.join('')
    let sixes = []
    for (let j = 0 ; (j + 1) * 6 < partsb.length;  j++) {
      data['6let' + j] = partsb.substring(j * 6, (j + 1) * 6)
      sixes.push(partsb.substring(j * 6, (j + 1) * 6))
    }
    data['sixes'] = sixes
    return data
  }

  stripeBg(color:string,px1:number,colorb:string,px2:number):string{
    return "repeating-linear-gradient(" +
      "-45deg, "+
      color+", "+
      color+" "+px1+"px, "+
      colorb+" "+px1+"px, "+
      colorb+" "+px2+"px "+
      ")"
      ;
  };
  /**
   * return value from list % list length
   * @param list
   * @param val number
   */
  inList(list: any[], val: number) {
    return list[val % list.length]
  }

  colorForVersion(val: number) {
    return this.inList(RundeckVersion.csscolors, val)
  }

  nameForVersion(val: number) {
    return this.inList(RundeckVersion.names4, val)
  }


  iconForVersion(val: any) {
    return this.inList(RundeckVersion.glyphicons3, val)
  }

  iconForVersion2(val: number) {
    return this.inList(RundeckVersion.glyphicons2, val)
  }


  data() {
    return this.versionData
  }

  versionColor() {
    return this.colorForVersion(this.versionData[this.colorIdentity])
  }

  versionSemantic() {
    return this.versionData['semantic']
  }

  versionName() {
    return this.nameForVersion(this.versionData[this.nameIdentity] + this.nameTilt)
  }

  versionIcon() {
    return this.iconForVersion(this.versionData[this.iconIdentity])
  }

  text() {
    let sep = ' '
    return [this.versionName(), this.versionColor(), this.versionIcon()].join(sep).toLowerCase()/*.replace(/[^a-z]/g, sep)*/
  }


}
