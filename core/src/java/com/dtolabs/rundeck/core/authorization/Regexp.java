/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
  * compiles a regular experession for a provided time and provides matching capability for authorization
  * @author  Chuck Scott <a href="mailto:chuck@dtosolutions.com">chuck@dtosolutions.com</a>
  */
public class Regexp {
   
   /**
    * check time based expression 
    * @param exp
    * @param type
    */
   public static void checkExp(final String exp, final String type)
      throws PatternSyntaxException {
      if (exp.equals("*"))
         return;
      Pattern.compile(exp);
   }
 
   /**
    * compare target entity against role entity using pattern matching
    * @param rEntity
    * @param tEntity
    * @return boolean
    */
   public static boolean match(final String rEntity, final String tEntity)
      throws PatternSyntaxException {

      if (rEntity.equals("*"))
         return true;

      final Pattern pattern = Pattern.compile(rEntity);

      final Matcher matcher = pattern.matcher((CharSequence)tEntity);

      return matcher.matches();
   }
}

