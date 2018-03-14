package onethreeseven.trajsuite.core.model;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import onethreeseven.geo.model.LatLonBounds;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.model.Layers;


/**
 * The TrajSuite program
 * @author Luke Bermingham
 */
public class TrajSuiteProgram extends BaseTrajSuiteProgram {


    //parameters which are set once they become available
    private WorldWindow wwd;

    protected TrajSuiteProgram() {
        super();
        System.out.println("Welcome to TrajSuite, type lc to list commands.");
        //listen for user input
        this.getCLI().startListeningForInput();
    }



    @Override
    protected Layers makeLayers() {
        return new TrajsuiteLayers();
    }

    public void shutdown(){
        //shutdown cli
        getCLI().shutdown();
        //shutdown worldwind
        if(wwd != null){
            wwd.shutdown();
        }
    }

    public TrajsuiteLayers getLayers() {
        return (TrajsuiteLayers) layers;
    }

    public void setWwd(WorldWindow wwd) {
        this.wwd = wwd;
    }

    private long lastFlyToTime = 0;
    private final static long flyToCoolDownMs = 5000;

    public void flyTo(BoundingCoordinates model){
        if(System.currentTimeMillis() - lastFlyToTime > flyToCoolDownMs){
            if(this.wwd != null){
                try{
                    if(model != null){
                        LatLonBounds bounds = model.getLatLonBounds();
                        if(bounds != null){
                            LatLon centroid = Sector.fromDegrees(bounds.getMinLat(), bounds.getMaxLat(), bounds.getMinLon(), bounds.getMaxLon()).getCentroid();
                            double ele = wwd.getView().getEyePosition().elevation;
                            wwd.getView().goTo(new Position(centroid, ele), ele);
                            lastFlyToTime = System.currentTimeMillis();
                        }
                    }
                }catch (Exception ignore){
                    System.err.println("Could not fly to model location: " + model);
                }

            }
        }

    }

}
