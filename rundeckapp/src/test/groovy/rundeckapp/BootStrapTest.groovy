package rundeckapp

import spock.lang.Specification

class BootStrapTest  extends Specification {
    def "Test convertToTokenMap"() {
        given:
        Properties userMap = new Properties().tap {
            load(new StringReader("""\
                |user1=${token("0")}
                |user2=${token("1")},role1
                |user3=${token("2")},role1,role2
                |user4=${token("3")};${token("4")},role1
                """.stripMargin()))
        }
        when:
        Properties tokenMap = BootStrap.convertToTokenMap(userMap)

        then:
        tokenMap.getProperty(token("0")) == "user1,api_token_group"
        tokenMap.getProperty(token("1")) == "user2,role1"
        tokenMap.getProperty(token("2")) == "user3,role1,role2"
        tokenMap.getProperty(token("3")) == "user4,role1"
        tokenMap.getProperty(token("4")) == "user4,role1"
    }

    private String token(String suffix) {
        return "0123456701234567012345670000000${suffix}"
    }
}
