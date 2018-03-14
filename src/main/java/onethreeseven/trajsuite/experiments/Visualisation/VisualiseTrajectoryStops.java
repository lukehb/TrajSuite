package onethreeseven.trajsuite.experiments.Visualisation;

import gov.nasa.worldwind.WorldWindow;
import javafx.stage.Stage;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.STStopTrajectoryParser;
import onethreeseven.datastructures.data.SpatioCompositeTrajectoryWriter;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STStopTrajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.trajsuite.core.model.TrajSuiteProgram;
import onethreeseven.trajsuite.core.util.WWExtrasUtil;
import onethreeseven.trajsuite.core.view.AbstractWWFxApplication;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Visualise trajectory stops
 * @author Luke Bermingham
 */
public class VisualiseTrajectoryStops extends AbstractWWFxApplication {

    private static final TrajSuiteProgram app = (TrajSuiteProgram) BaseTrajSuiteProgram.getInstance();

    /**
     * Should we load a trajectory from a file or generate one?
     */
    private static final boolean loadTrajFromFile = true;

    /**
     * For loading a trajectory
     */
    private static final File trajFile = new File(FileUtil.makeAppDir("traj"), "hike.txt");
    private static final String trajId = "1";

    /**
     * For generating a trajectory
     */
    private static final boolean saveGeneratedResult = true;
    private static final String generatedOutFmt = "generated_n%d_t%d_spd%f_noise%f.txt";
    private static final int nEntriesToGenerate = 10000;
    private static final int nStops = 10;
    private static final long timeStepMillis = 1000L;
    private static final double maxSpeed = 5;
    private static final double avgNoise = 1;
    private static final double startLat = -16.9186;
    private static final double startLon = 145.7781;


    private STStopTrajectory loadFromFile() throws IOException {
        System.out.println("Loading trajectory...");

        Map<String, STStopTrajectory> trajMap = new STStopTrajectoryParser(
                new ProjectionEquirectangular(),
                new IdFieldResolver(0),
                new LatFieldResolver(1),
                new LonFieldResolver(2),
                new TemporalFieldResolver(3),
                new StopFieldResolver(4),
                true).parse(trajFile);
        return trajMap.get(trajId);
    }

    private STStopTrajectory generateTrajectory(){
        System.out.println("Generating trajectory...");
        return DataGeneratorUtil.generateTrajectoryWithStops(
                nEntriesToGenerate,
                nStops,
                timeStepMillis,
                timeStepMillis * 20,
                maxSpeed,
                avgNoise,
                startLat, startLon);
    }

    @Override
    protected TrajSuiteProgram preStart(Stage stage) {
        //GraphicsSettings.setDrawTrajectoryAsPoints(true);
        STStopTrajectory traj = null;
        if(loadTrajFromFile){
            try {
                traj = loadFromFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            traj = generateTrajectory();
            if(saveGeneratedResult){writeGeneratedTraj(traj);}
        }
        if(traj != null){
            //add the trajectories so they can be rendered
            app.getLayers().add(traj);
            System.out.println("Done adding trajectories.");
        }

        return app;
    }

    private void writeGeneratedTraj(STStopTrajectory traj){
        String generatedName = String.format(
                generatedOutFmt,
                nEntriesToGenerate,
                timeStepMillis,
                maxSpeed, avgNoise);
        File outFile = new File(FileUtil.makeAppDir("traj"), generatedName);
        Map<String, STStopTrajectory> trajMap = new HashMap<>();
        trajMap.put(trajId, traj);
        new SpatioCompositeTrajectoryWriter().write(outFile, trajMap);
        System.out.println("Wrote traj to: " + outFile.getAbsolutePath());
    }

    @Override
    public String getTitle() {
        return "Visualise speed enabled trajectories";
    }

    @Override
    public int getStartWidth() {
        return 800;
    }

    @Override
    public int getStartHeight() {
        return 600;
    }


    @Override
    protected void onViewReady(WorldWindow wwd) {
        STStopTrajectory traj = app.getLayers().getFirstEntity(STStopTrajectory.class).getModel();
        WWExtrasUtil.flyTo(traj, wwd);
    }
}
