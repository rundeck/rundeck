package rundeckapp

import spock.lang.Specification

class BootStrapTest  extends Specification {
    def "Test convertToTokenMap"() {
        given:
        Properties userMap = new Properties().tap {
            load(new StringReader("""\
                |user1=01234567012345670123456700000000
                |user2=01234567012345670123456700000001,role1
                """.stripMargin()))
        }
        when:
        Properties tokenMap = BootStrap.convertToTokenMap(userMap)

        then:
        tokenMap.getProperty("01234567012345670123456700000000") == "user1,api_token_group"
        tokenMap.getProperty("01234567012345670123456700000001") == "user2,role1"
    }
}
