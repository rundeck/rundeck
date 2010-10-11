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

/*
 * Format.java
 *
 * Created on April 1, 2004, 3:39 PM
 * $Id: Format.java 7769 2008-02-07 00:50:23Z gschueler $
 */

package com.dtolabs.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting things.  Date formatting primarily, as well
 * as human readable filesizes.
 * @author Greg Schueler <a href="mailto:greg@networkgps.com">greg@networkgps.com</a>
 * @version $Revision: 7769 $
 */
public class Format {
    static long KiB = 1024;
    static long MiB = KiB*1024;
    static long GiB = MiB*1024;
        
    private static final DateFormat dtFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private static final DateFormat dFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    private static final DateFormat tFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
    public static final String stampFormatString = "yyyyMMddHHmmss";
    private static final DateFormat stampFormat = new SimpleDateFormat(stampFormatString);
    /**
     * format a date as a string
     *
     * @param d a Date
     *
     * @return a formatted String
     */
    public static String shortDateTime(Date d) {
        return dtFormat.format(d);
    }
    /**
     * format a date as a string
     *
     * @param d a Date
     *
     * @return a formatted String
     */
    public static String shortDate(Date d) {
        return dFormat.format(d);
    }
    /**
     * format a date as a string
     *
     * @param d a Date
     *
     * @return a formatted String
     */
    public static String shortTime(Date d) {
        return tFormat.format(d);
    }
    /**
     * @return a Date from the given timestamp string, or null if it cannot be parsed
     */
    public static Date fromShortDate(String date) {
        try{
            return dFormat.parse(date);
        }catch (ParseException e){
            return null;
        }
    }
    
    /**
     * Return a 14 digit timestamp of the format: "yyyyMMddHHmmss"
     */
    public static String stamp() {
        return stamp(new Date());
    }
    public static String stamp(Date d){
        return stampFormat.format(d);
    }
    /**
     * @return a Date from the given timestamp string, or null if it cannot be parsed
     */
    public static Date fromStamp(String stamp) {
        try{
            return stampFormat.parse(stamp);
        }catch (ParseException e){
            return null;
        }
    }
    
    /**
     * Format a filesize as a human readable string
     */
    public static String filesize(long length){
        java.text.DecimalFormat form = new java.text.DecimalFormat("#.##");
        form.setDecimalSeparatorAlwaysShown(false);
        if(length < KiB){
            return length+" Bytes";
        }
        if(length/KiB < 1024){
            double rel = (float)length/(float)KiB;
            return form.format(rel)+" KB";
        }
        if(length/MiB < 1024){
            double rel = (float)length/(float)MiB;
            return form.format(rel)+" MB";
        }
        double rel = (float)length/(float)GiB;
        return form.format(rel)+" GB";
    }
    
    public static void main(String args[]){
        long x = 450;
        long y = 71;
        long z = 1;
        while(z<1024*GiB ){
            long j = x*z;
            long i = y*z + (z/4);
            System.out.println(j+" bytes: "+filesize(j)+" % "+(j%z));
            System.out.println(i+" bytes: "+filesize(i)+" % "+(i%z));
            z*=1024;
        }
    }
    
    static long[] times = new long[]{
        60 * 1000L,
        5 * 60 * 1000L,
        30 * 60 * 1000L,
        60 * 60 * 1000L,
        6 * 60 * 60 * 1000L,
        12 * 60 * 60 * 1000L,
        24 * 60 * 60 * 1000L,
        7 * 24 * 60 * 60 * 1000L,
        1 * 30 * 24 * 60 * 60 * 1000L,
        2 * 30 * 24 * 60 * 60 * 1000L,
        3 * 30 * 24 * 60 * 60 * 1000L,
        4 * 30 * 24 * 60 * 60 * 1000L,
        5 * 30 * 24 * 60 * 60 * 1000L,
        6 * 30 * 24 * 60 * 60 * 1000L,
        9 * 30 * 24 * 60 * 60 * 1000L,
        365 * 24 * 60 * 60 * 1000L

    };
    static String[] labels = new String[]{
        "Minute",
        "Five Minutes",
        "Half Hour",
        "Hour",
        "6 Hours",
        "12 Hours",
        "Day",
        "Week",
        "Month",
        "60 Days",
        "90 Days",
        "4 Months",
        "5 Months",
        "6 Months",
        "9 Months",
        "Year"
    };
    
    /**
     * Returns descriptive string describing the timescale between the current
     * time and the given date. The string returned could complete the sentence:
     * "..within the last ____." 
     * Null is returned if the difference is negative or greater 
     * than 1 year.
     *
     * @param earlier a time earlier than now
     */
    public static String describeElapsedTime(java.util.Date earlier){
        return describeElapsedTime(new Date(), earlier);
    }
    /**
     * Returns descriptive string describing the timescale between two dates.
     * The string returned could complete the sentence:
     * "..within the last ____." One of the values:<br/>
     * <ol>
        <li>"Five Minutes"</li>
        <li>"Half Hour"</li>
        <li>"Hour"</li>
        <li>"6 Hours"</li>
        <li>"12 Hours"</li>
        <li>"Day"</li>
        <li>"Week"</li>
        <li>"Month"</li>
         <li>"60 Days"</li>
         <li>"90 Days"</li>
         <li>"4 Months"</li>
         <li>"5 Months"</li>
         <li>"6 Months"</li>
         <li>"9 Months"</li>
        <li>"Year"</li>
     * </ol>
     * Or null.  Null is returned if the difference is negative or greater 
     * than 1 year.
     *
     * @param later the later time
     * @param earlier the earlier time
     */
    public static String describeElapsedTime(java.util.Date later, java.util.Date earlier){
        long now = later.getTime();
        long then = earlier.getTime();
        if(then>now){
            return null;
        }
        long diff = now-then;
        for(int i=0;i<times.length;i++){
            if(diff<=times[i]){
                return labels[i];
            }
        }
        return null;
    }

}
