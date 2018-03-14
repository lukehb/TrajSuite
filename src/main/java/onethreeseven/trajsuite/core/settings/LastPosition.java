package onethreeseven.trajsuite.core.settings;

import gov.nasa.worldwind.geom.Position;
import javafx.util.StringConverter;
import onethreeseven.trajsuitePlugin.settings.BaseSetting;
import onethreeseven.trajsuitePlugin.settings.ProgramSettings;

/**
 * Setting for the last geographic position the program was navigated to.
 * @author Luke Bermingham
 */
public class LastPosition extends BaseSetting<Position> {

    private static final StringConverter<Position> convert = new StringConverter<>() {
        @Override
        public String toString(Position object) {
            return object.latitude.degrees + "," + object.longitude.degrees + "," + object.elevation;
        }

        @Override
        public Position fromString(String input) {
            String[] split = input.split(",");
            double lat = Double.parseDouble(split[0]);
            double lon = Double.parseDouble(split[1]);
            double ele = Double.parseDouble(split[2]);
            return Position.fromDegrees(lat, lon, ele);
        }
    };

    private static final Position defaulPos = Position.fromDegrees(-16.9186,145.7781, 100000);
    private static final String defaultPosString = convert.toString(defaulPos);

    protected LastPosition() {
        super("Start-up",
                "Last Geographic Position",
                "The last geographic location the user navigated to, " +
                        "stored so the program can load up here next time it is started.",
                defaulPos);
    }

    @Override
    public Position getSetting() {
        String storedSetting = ProgramSettings.getSetting(category, settingName, defaultPosString);
        return convert.fromString(storedSetting);
    }

    @Override
    public StringConverter<Position> getConverter() {
        return convert;
    }
}
