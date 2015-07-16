package rundeck.services

import org.apache.commons.validator.routines.EmailValidator
import org.apache.commons.validator.routines.InetAddressValidator
import org.apache.commons.validator.routines.RegexValidator

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Extend email validator to only validate email syntax, and not require strict TLD validation
 */
class AnyDomainEmailValidator extends EmailValidator {
    AnyDomainEmailValidator() {
        super(true)
    }

    /**************
     * extracted from commons-validator-1.4.0: org.apache.commons.validator.routines.DomainValidator.java
     **************/
    // Regular expression strings for hostnames (derived from RFC2396 and RFC 1123)
    private static final String DOMAIN_LABEL_REGEX = '\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*';
    /**
     * Modified from original to accept 1+ chars, instead of 2+ chars.
     */
    private static final String TOP_LABEL_REGEX = '\\p{Alpha}+';
    private static final String DOMAIN_NAME_REGEX = '^(?:' + DOMAIN_LABEL_REGEX + '\\.)+' + '(' + TOP_LABEL_REGEX + ')$';
    /**
     * RegexValidator for matching domains.
     */
    private final RegexValidator domainRegex =
            new RegexValidator(DOMAIN_NAME_REGEX);
    /**
     * RegexValidator for matching the a local hostname
     */
    private final RegexValidator hostnameRegex =
            new RegexValidator(DOMAIN_LABEL_REGEX);


    /**************
     * extracted from commons-validator-1.4.0: org.apache.commons.validator.routines.EmailValidator.java
     **************/
    private static final String IP_DOMAIN_REGEX = '^\\[(.*)\\]$';
    private static final Pattern IP_DOMAIN_PATTERN = Pattern.compile(IP_DOMAIN_REGEX);

    /**
     * return true if the domain is an IP address domain, or a simple hostname, or matches a dot-separated domain,
     * otherwise return false
     * @param domain string
     * @return true if valid
     */
    @Override
    protected boolean isValidDomain(final String domain) {
        Matcher ipDomainMatcher = IP_DOMAIN_PATTERN.matcher(domain);

        if (ipDomainMatcher.matches()) {
            InetAddressValidator inetAddressValidator =
                    InetAddressValidator.getInstance();
            return inetAddressValidator.isValid(ipDomainMatcher.group(1));
        } else {
            String[] groups = domainRegex.match(domain);
            if (groups != null && groups.length > 0) {
                return true;
            } else if (hostnameRegex.isValid(domain)) {
                return true;
            }
        }
        return false;
    }
}
