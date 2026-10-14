import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRulesProvider;

/**
 * Fails the image build if the JDK timezone database still has the
 * 2026-11-01 DST transition for the Canadian zones that moved to permanent time.
 */
public class TzdbCheck {
    private static final String[] ZONES = {
        "America/Vancouver", "America/Edmonton", "America/Inuvik", "America/Winnipeg"
    };

    public static void main(String[] args) {
        String version = ZoneRulesProvider.getVersions("UTC").lastEntry().getKey();
        System.out.println("JDK tzdb version: " + version);
        boolean stale = false;
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        for (String zone : ZONES) {
            ZoneOffsetTransition next = ZoneId.of(zone).getRules().nextTransition(from);
            if (next != null) {
                System.err.println(zone + " still transitions at " + next.getDateTimeBefore());
                stale = true;
            }
        }
        if (stale) {
            System.err.println("JDK tzdb is older than 2026e");
            System.exit(1);
        }
        System.out.println("Canadian 2026 time zone rules OK");
    }
}
