package com.dtolabs.rundeck.util

class StringNumericSort {
    String strValue
    Object numericValue

    StringNumericSort(String strValue, Object numericValue) {
        this.strValue = strValue
        this.numericValue = numericValue
    }

    static void sortNumeric(List<StringNumericSort> list){
        // Sort numbers
        Comparator<StringNumericSort> comparator = new Comparator<StringNumericSort>() {
            @Override
            int compare(StringNumericSort a, StringNumericSort b) {
                return a.numericValue <=> b.numericValue
            }
        };

        Collections.sort(list, comparator)
    }


}
