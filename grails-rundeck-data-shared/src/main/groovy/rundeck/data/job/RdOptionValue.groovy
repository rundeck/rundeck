package rundeck.data.job

import com.dtolabs.rundeck.plugins.option.OptionValue
import org.rundeck.app.data.model.v1.job.option.OptionValueData

class RdOptionValue implements OptionValue, OptionValueData {
    String name
    String value
}
