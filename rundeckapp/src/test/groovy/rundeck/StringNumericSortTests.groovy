package rundeck

import com.dtolabs.rundeck.util.StringNumericSort
import org.junit.Test

import static org.junit.Assert.assertEquals

class StringNumericSortTests {

    @Test
    void sortNumericList() {
        def values = ['33.3','22','44.0','11.0','3','1','2']

        def stringNumericList= values.findAll { it.isNumber() }.collect {new StringNumericSort(it, it.toDouble())}
        StringNumericSort.sortNumeric(stringNumericList)
        values = stringNumericList.collect{it.strValue}

        assertEquals 7,values.size()
        assertEquals "[1, 2, 3, 11.0, 22, 33.3, 44.0]" , values.toString()

    }
}
