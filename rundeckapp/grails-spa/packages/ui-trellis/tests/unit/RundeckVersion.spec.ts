import 'jest'

import {RundeckVersion} from '../../src/library/utilities/RundeckVersion'

describe('RundeckVersion', () => {

  let mkvid = function (vers: string) {
    return new RundeckVersion({versionString: vers})
  }
  test('basic', () => {

    var data = mkvid('0').data()
    expect(data.major).toEqual(0)
    expect(data.minor).toEqual(0)
    expect(data.point).toEqual(0)
    expect(data.release).toEqual(1)
    expect(data.tag).toEqual('')
  })

  test('fullTest', () => {
    var data = mkvid('2.3.4-5-SNAPSHOT').data()
    expect(data.major).toEqual(2)
    expect(data.minor).toEqual(3)
    expect(data.point).toEqual(4)
    expect(data.release).toEqual(5)
    expect(data.tag).toEqual('SNAPSHOT')
    expect(data.version).toEqual('2.3.4-5-SNAPSHOT')
  })
  test('noReleaseTest', () => {
    var data = mkvid('2.3.4-SNAPSHOT').data()
    expect(data.major).toEqual(2)
    expect(data.minor).toEqual(3)
    expect(data.point).toEqual(4)
    expect(data.release).toEqual(1)
    expect(data.tag).toEqual('SNAPSHOT')
  })
  test('noTagTest', () => {
    var data = mkvid('2.3.4-5').data()
    expect(data.major).toEqual(2)
    expect(data.minor).toEqual(3)
    expect(data.point).toEqual(4)
    expect(data.release).toEqual(5)
    expect(data.tag).toEqual('')
  })
  test('multiTest', () => {
    var data = mkvid('2.3.4-SNAPSHOT (other-data)').data()
    expect(data.major).toEqual(2)
    expect(data.minor).toEqual(3)
    expect(data.point).toEqual(4)
    expect(data.release).toEqual(1)
    expect(data.tag).toEqual('SNAPSHOT')
    expect(data.version).toEqual('2.3.4-SNAPSHOT')
  })
})
