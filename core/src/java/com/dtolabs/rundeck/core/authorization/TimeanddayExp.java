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

import org.apache.log4j.Category;


/**
 *
 * TimeanddayExp class to represent time contraints applied to an acl.
 * the time contraint supports dayofweek,  hour, and minute similar
 * to how crontab supports them.
 * "* * *"
 * "1-5 8-17 *" 
 * etc.
 * @author  Chuck Scott <a href="mailto:chuck@dtosolutions.com">chuck@dtosolutions.com</a>
 */
public class TimeanddayExp {

   static Category logger = Category.getInstance(TimeanddayExp.class.getName());

   //day of week expression datamember
   private String dayExp;
   private void setDayExp(String dayExp) throws NumberFormatException {
      checkExp(dayExp, "day");
      this.dayExp = dayExp;
   }

   /**
    * getter for day expression
    * @return String
    */
   public String getDayExp() {return this.dayExp;}

   //hour of day expression datamember
   private String hourExp;

   /**
    * setter for hour expression
    * @param hourExp
    */
   public void setHourExp(String hourExp) {
      checkExp(dayExp, "hour");
      this.hourExp = hourExp;
   }

   /**
    * getter for hour expression
    * @return String
    */
   public String getHourExp() {return this.hourExp;}

   //minute of hour expression datamember
   private String minuteExp;

   /**
    * setter for minute expression
    * @param minuteExp
    */
   public void setMinuteExp(String minuteExp) {
      checkExp(dayExp, "minute");
      this.minuteExp = minuteExp;
   }

   /**
    * getter for minute expression
    * @return String
    */
   public String getMinuteExp() {return this.minuteExp;}

   /**
    * checks expressions for day, hour, minute 
    * @param exp
    * @param type
    * @throws NumberFormatException
    */
   public static void checkExp(String exp, String type) throws NumberFormatException {

      // check for basic wildcard
      if (exp.equals("*")) {
         return;
      }

      // check for comma list or hyphened range
      int comma = exp.indexOf(',');
      int hyphen = exp.indexOf('-');

      if (comma >=0 && hyphen >= 0) {
         throw new NumberFormatException("day expression: " + exp + " is not valid");
      }

      int min = 0;
      int max = 6;
      if (type.equals("day")) {
         // day can be between 0 and 6 (sun thru sat)
         min = 0;
         max = 6;
      } else if (type.equals("hour")) {
         // hour between 0 and 23
         min = 0;
         max = 23;
      } else if (type.equals("minute")) {
         // minute between 0 and 59
         min = 0;
         max = 59;
      } else {
         throw new NumberFormatException("unexpected error, type: " + type); 
      }

      // check for a specific time unit, no regexp
      if (comma == -1 && hyphen == -1) {
         int unit = Integer.parseInt(exp);
         if (! checkInRange(unit, min, max) ) {
            throw new NumberFormatException(type+": " +
                                            Integer.toString(unit) +
               " cannot be greater than "+max + " or less than "+min);
         }
      } else if (comma > -1) {
         //process a list of times seperated by comma
         String[] expList = exp.split(",");
         for (int i=0; i<expList.length; i++) {
            int unit = Integer.parseInt(expList[i]);
            if (! checkInRange(unit, min, max) ) {
               throw new NumberFormatException(type+": " +
                                               Integer.toString(unit) +
                  " cannot be greater than "+max + " or less than "+min);
            }
         }
      } else if (hyphen > -1) {
         //process a time range, only one hyphen allowed
         String[] expRange = exp.split("-");
         if (expRange.length > 2) {
            throw new NumberFormatException("expression: " + exp + " of type " + type + " is not valid");
         }
         int unit0 = Integer.parseInt(expRange[0]);
         int unit1 = Integer.parseInt(expRange[1]);
         if (! checkInRange(unit0, min, max) ) {
            throw new NumberFormatException(type+": " +
                                            Integer.toString(unit0) +
               " cannot be greater than "+max + " or less than "+min);
         }
         if (! checkInRange(unit1, min, max) ) {
            throw new NumberFormatException(type+": " +
                                            Integer.toString(unit1) +
               " cannot be greater than "+max + " or less than "+min);
         }
      } else {
         //not possible
         throw new NumberFormatException("unexpected error");
      }
   }

   // checks if the integer (unit) is within specificed min/max range
   // for types: day, hour, minute
   /**
     * checks if the integer (unit) is within specificed min/max range
     * for types: day, hour, minute
     * @param unit
     * @param min
     * @param max
     * @return boolean 
     */
   public static boolean checkInRange(int unit, int min, int max) {
      //throws NumberFormatException {

      
      if (unit < min || unit > max) {
         return false;
         //throw new NumberFormatException(type+": " + new Integer(unit).toString() +
            //" cannot be greater than "+max + " or less than "+min);
      }
      return true;
   }

   
   /**
    * constructor supporting timeandday represented in a single String.
    * this String is expected to have whitespace seperated 
    * day, hour, minute expressions similar to crontab
    *
    * @param timeandday
    * @throws NumberFormatException
    */
   public TimeanddayExp(String timeandday) throws NumberFormatException {
      this(
         TimeanddayExp.parseDayExp(timeandday),
         TimeanddayExp.parseHourExp(timeandday),
         TimeanddayExp.parseMinuteExp(timeandday)
      );
   }

   /**
    * constructor supporting day, hour, and minute expressions
    * @param dayExp
    * @param hourExp
    * @param minuteExp
    * @throws NumberFormatException
    */
   public TimeanddayExp(String dayExp, String hourExp, String minuteExp) 
      throws NumberFormatException {

      if (null == dayExp || "".equals(dayExp)) 
         throw new NumberFormatException("day expression is null or empty string");
      this.setDayExp(dayExp);

      if (null == hourExp || "".equals(hourExp))
         throw new NumberFormatException("hour expression is null or empty string");
      this.setHourExp(hourExp);

      if (null == minuteExp || "".equals(minuteExp))
         throw new NumberFormatException("minute expression is null or empty string");
      this.setMinuteExp(minuteExp);

   }

   /**
    * dumps this object in its crontab like String representation
    * @return String
    */
   public String toString() {
      return this.getDayExp() + " " + this.getHourExp() + " " + this.getMinuteExp();
   }

  /**
    * parsed day expression from input String
    * @return String
    */
   public static String parseDayExp(String timeandday) {
      return timeandday.split("\\s")[0];
   }

  /**
    * parsed hour expression from input String
    * @return String
    */
   public static String parseHourExp(String timeandday) {
      return timeandday.split("\\s")[1];
   }

  /**
    * parsed minute expression from input String
    * @return String
    */
   public static String parseMinuteExp(String timeandday) {
      return timeandday.split("\\s")[2];
   }

  /**
    * determine if two acls TimeanddayExp's are equivalent
    * the role TimeanddayExp  may have regexp where the target TimeanddayExp 
    * will not.
    *
    * @param r role based TimeandDay object
    * @param t target based TimeandDay object
    * @return boolean
    */
   public static boolean match(TimeanddayExp r, TimeanddayExp t) {

      logger.debug("match(), compare TimeanddayExp objects");

      // compare role and target times
      String rDay = r.getDayExp();
      String rHour = r.getHourExp();
      String rMinute = r.getMinuteExp();

      String tDay  = t.getDayExp();
      String tHour = t.getHourExp();
      String tMinute = t.getMinuteExp();

      logger.debug("rDay: " + rDay);
      logger.debug("tDay: " + tDay);
      logger.debug("rHour: " + rHour);
      logger.debug("tHour: " + tHour);
      logger.debug("rMinute: " + rMinute);
      logger.debug("tMinute: " + tMinute);

      if (! matchTime(rDay, tDay) ) {
         logger.debug("days do not match");
         return false;
      }
      if (! matchTime(rHour, tHour) ) {
         logger.debug("hours do not match");
         return false;
      }
      if (! matchTime(rMinute, tMinute) ) {
         logger.debug("minutes do not match");
         return false;
      }

      return true;
   }

   //compare role and target times, subject to rules similar to crontab
   private static boolean matchTime(String rTime, String tTime) {
      boolean success = false;

      logger.debug("matchTime(), comparing time strings rTime: " + 
         rTime + " tTime: " + tTime);

      if (rTime.equals("*")) {
         logger.debug("matchTime(), rTime contains wildcard, returning true");
         return true;
      }
      int rtime = Integer.parseInt(rTime);

      int comma = tTime.indexOf(',');
      int hyphen = tTime.indexOf('-');
      if (comma >= 0) {
         logger.debug("comparing against list of times");
         String[] timeList = tTime.split(",");
         for (int i=0; i<timeList.length; i++) {
            if (rtime == Integer.parseInt(timeList[i])) {
               return true;
            }
         }
         success=false;
      } else if (hyphen == 0) {
         logger.debug("comparing against range of times");
         String[] timeRange = tTime.split("-");
         int min = Integer.parseInt(timeRange[0]);
         int max = Integer.parseInt(timeRange[1]);
         if (! checkInRange(rtime, min, max) ) {
            return false;
         }
         success=true;
      } else {
         logger.debug("comparing against exact time");
         int ttime = Integer.parseInt(tTime);
         if (rtime != ttime) {
            return false;
         }
         success=true;
      }
      logger.debug("returning: " + success);
      return success;

   }
}
