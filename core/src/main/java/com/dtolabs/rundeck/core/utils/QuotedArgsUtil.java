package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;

import java.util.function.Predicate;

public class QuotedArgsUtil {
    public static final Predicate argsNeedsQuoting =
            DataContextUtils.stringContainsPropertyReferencePredicate
                    .or(CLIUtils::containsSpace)
                    .or(CLIUtils::containsQuote);
}
