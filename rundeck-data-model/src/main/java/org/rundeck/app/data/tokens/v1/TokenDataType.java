package org.rundeck.app.data.tokens.v1;

import lombok.Getter;
import org.rundeck.spi.data.DataType;

public class TokenDataType
        implements DataType<Token>
{
    @Getter final String name = "token";
    @Getter final String version = "1";
//    @Getter final List<DataType<?>> relationTypes = new ArrayList<>();
    @Getter final Class<Token> javaType = Token.class;

}
