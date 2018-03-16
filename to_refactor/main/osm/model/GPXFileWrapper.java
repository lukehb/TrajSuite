package onethreeseven.trajsuite.osm.model;

import com.graphhopper.matching.GPXFile;
import com.graphhopper.util.GPXEntry;
import onethreeseven.common.util.TimeUtil;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.model.LatLonBounds;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Wrapping the graphhopper gpx file in my own implementation.
 * Basically to make the api easier to work with.
 * @author Luke Bermingham
 */
public class GPXFileWrapper extends GPXFile {

    private final Logger logger = Logger.getLogger(GPXFileWrapper.class.getSimpleName());
    private double minLat;
    private double maxLat;
    private double minLon;
    private double maxLon;
    private long minTime;
    private long maxTime;

    public GPXFileWrapper(File gpxFile) {
        super();
        try{
            this.doImport(new FileInputStream(gpxFile));
        } catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
    }

    public GPXFileWrapper(SpatioCompositeTrajectory traj){
        super();

        for (int j = 0; j < traj.size(); j++) {
            double[] latlon = traj.getCoords(j);

            LocalDateTime timeStamp = null;
            if(traj instanceof STTrajectory){
                timeStamp = ((STTrajectory) traj).getTime(j);
            }
            long millis = (timeStamp == null) ? Instant.now().plusMillis(j * 3000).toEpochMilli() :
                    timeStamp.toInstant(ZoneOffset.UTC).toEpochMilli();

            GPXEntry gpxEntry = new GPXEntry(latlon[0], latlon[1], millis);
            getEntries().add(gpxEntry);
        }
    }

    /**
     * Converts a set of trajectories to GPX trails using the
     * trajectories field resolvers. If there are no index to field
     * resolvers provided then the conversion throw an illegal argument exception.
     * @param trajs the trajectories to convert to a gpx trails.
     * @return the converted gpx file
     */
    public static GPXFile[] fromTrajectories(Map<String, ? extends SpatioCompositeTrajectory> trajs){
        GPXFile[] trails = new GPXFile[trajs.size()];

        int i = 0;
        for (Map.Entry<String,? extends SpatioCompositeTrajectory> entry : trajs.entrySet()) {
            SpatioCompositeTrajectory trajectory = entry.getValue();
            if(trajectory.isInCartesianMode()){
                trajectory.toGeographic();
            }
            for (int j = 0; j < trajectory.size(); j++) {
                trails[i] = new GPXFileWrapper(entry.getValue());
            }
            i++;
        }
        return trails;
    }

    public GPXFile doImport(InputStream is) {
        logger.info("Beginning gpx import...");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            NodeList nl = doc.getElementsByTagName("trkpt");
            final int end = nl.getLength();

            int lastProgress = 0;

            for (int index = 0; index < end; index++) {

                int progress = (int)(((double) index) / end * 100);
                if(progress > lastProgress){
                    System.out.println(progress + "%");
                    lastProgress = progress;
                }

                Node n = nl.item(index);
                if (!(n instanceof Element)) {
                    continue;
                }

                Element e = (Element) n;
                double lat = parseLat(e);
                double lon = parseLon(e);

                NodeList timeNodes = e.getElementsByTagName("time");
                if (timeNodes.getLength() == 0) {
                    throw new IllegalStateException("GPX without time is illegal");
                }

                String text = timeNodes.item(0).getTextContent();
                long millis = parseTime(text);

                NodeList eleNodes = e.getElementsByTagName("ele");
                if (eleNodes.getLength() == 0) {
                    getEntries().add(new GPXEntry(lat, lon, millis));
                } else {
                    double ele = Double.parseDouble(eleNodes.item(0).getTextContent());
                    getEntries().add(new GPXEntry(lat, lon, ele, millis));
                }
            }
            return this;
        } catch (Exception e) {
            logger.warning("Failed gpx import.");
            throw new RuntimeException(e);
        }
    }

    protected double parseLat(Element e) {
        double lat = Double.parseDouble(e.getAttribute("lat"));
        if (lat > maxLat) {
            maxLat = lat;
        }
        if (lat < minLat) {
            minLat = lat;
        }
        return lat;
    }

    protected double parseLon(Element e) {
        double lon = Double.parseDouble(e.getAttribute("lon"));
        if (lon > maxLon) {
            maxLon = lon;
        }
        if (lon < minLon) {
            minLon = lon;
        }
        return lon;
    }

    protected long parseTime(String text) {
        LocalDateTime dateTime = TimeUtil.parseDate(text);
        if(dateTime == null){
            throw new IllegalArgumentException("Could not parse date: " + text);
        }
        long millis = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();

        if (millis < minTime) {
            minTime = millis;
        }
        if (millis > maxTime) {
            maxTime = millis;
        }
        return millis;
    }

    public LatLonBounds getBounds() {
        return new LatLonBounds(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Convert the latitude/longitude pts to cartesian coordinates
     * for visualisation as a trajectory.
     *
     * @return the trajectory in non-geographic coordinates
     */
    public STTrajectory toTrajectory() {
        List<GPXEntry> entries = getEntries();
        STTrajectory trajectory = new STTrajectory();
        for (GPXEntry entry : entries) {
            trajectory.addGeographic(new double[]{entry.getLat(), entry.getLon()},
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.getTime()), ZoneId.systemDefault()));
        }
        return trajectory;
    }


}
