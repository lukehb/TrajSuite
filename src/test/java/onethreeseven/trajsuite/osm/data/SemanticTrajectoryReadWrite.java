package onethreeseven.trajsuite.osm.data;

import onethreeseven.common.model.TimeCategory;
import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.NumericFieldsResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticPt;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.TimeAndPlace;
import onethreeseven.trajsuite.osm.model.tag.Amenity;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test the read {@link onethreeseven.trajsuite.osm.model.SemanticTrajectory} using {@link SemanticTrajectoryParser}
 * and write them using {@link onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter}.
 * @author Luke Bermingham
 */
public class SemanticTrajectoryReadWrite {

    private static File testFile;

    @BeforeClass
    public static void setup() throws Exception{
        testFile = new File(FileUtil.makeAppDir("traj-test"), "semantic_traj_test.txt");
    }

    @AfterClass
    public static void teardown() throws Exception{
        testFile.deleteOnExit();
    }

    @Test
    public void testWriteThenRead() throws Exception{

        AbstractGeographicProjection projection = new ProjectionMercator();
        TimeCategoryPool timeCategoryPool = TimeCategoryPool.TIMES_OF_DAY;
        String testTrajName = "testTraj";

        //write

        SemanticTrajectory traj = new SemanticTrajectory(false, projection);

        double[] latlon = new double[]{16.95, 145.77};
        SemanticPlace place = new SemanticPlace("137", "The Best Place", new Amenity("Heaven"));
        LocalDateTime now = LocalDateTime.now();
        TimeCategory catNow = TimeCategoryPool.TIMES_OF_DAY.resolve(now);
        TimeAndPlace timeAndPlace = new TimeAndPlace(now, catNow, place);
        traj.addGeographic(latlon, timeAndPlace);

        Map<String, SemanticTrajectory> outMap = new HashMap<>();
        outMap.put(testTrajName, traj);

        SpatioCompositeTrajectoryWriter writer = new SpatioCompositeTrajectoryWriter();
        writer.write(testFile, outMap);

        System.out.println("Wrote output semantic trajectory to: " + testFile.getAbsolutePath());
        System.out.println("Beginning reading of semantic trajectory.");

        //read

        //format is like: testTraj, 16.95, 145.77, 2017-05-29T15:48:33.909, AFTERNOON, 137, The Best Place, amenity=heaven

        SemanticTrajectoryParser parser = new SemanticTrajectoryParser(
                projection,
                new IdFieldResolver(0),
                new NumericFieldsResolver(1,2),
                new TemporalFieldResolver(3),
                new SemanticPlaceFieldsResolver(5, 6, 7),
                TimeCategoryPool.TIMES_OF_DAY,
                false);

        Map<String, SemanticTrajectory> readTrajs = parser.parse(testFile);

        Assert.assertTrue(readTrajs.containsKey(testTrajName));

        SemanticTrajectory actualTraj = readTrajs.get(testTrajName);
        SemanticPt actualPt = actualTraj.get(0);

        System.out.println("Read in semantic point: " + actualPt);

        LocalDateTime actualTime = actualPt.getTime();
        SemanticPlace actualPlace = actualPt.getTimeAndPlace().getPlace();

        //lat
        Assert.assertEquals(latlon[0], actualPt.getCoords()[0], 1e-06);
        //lon
        Assert.assertEquals(latlon[1], actualPt.getCoords()[1], 1e-06);
        //time
        Assert.assertTrue(actualTime.equals(now));
        //semantic place
        Assert.assertTrue(actualPlace.equals(place));

    }


}
