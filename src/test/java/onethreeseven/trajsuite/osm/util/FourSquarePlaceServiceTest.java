package onethreeseven.trajsuite.osm.util;

import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.tag.Amenity;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test {@link FourSquarePlaceService}.
 * @author Luke Bermingham
 */
public class FourSquarePlaceServiceTest {

    private FourSquarePlaceService api = new FourSquarePlaceService();

    @Test
    public void getClosestPlace() throws Exception {
        //James Cook University
        SemanticPlace place = api.getClosestPlace(-16.81830766033288, 145.6874515227246, 200).getExtra();
        Assert.assertTrue(place.getPrimaryTag() instanceof Amenity);
        Assert.assertTrue(place.getPrimaryTag().getValue().toLowerCase().equals("university"));
        System.out.println("University found");
    }

}