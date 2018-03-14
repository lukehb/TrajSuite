package onethreeseven.trajsuite.core;

import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.Layers;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;
import onethreeseven.trajsuitePlugin.model.WrappedEntityLayer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Load a trajectory from another module using service loader.
 * @author Luke Bermingham
 */
public class GenerateTrajectoryTest {

    @Test
    public void testLoadTrajUsingServiceLoader(){

        //load the traj using the generate trajectories command
        BaseTrajSuiteProgram program = BaseTrajSuiteProgram.getInstance();

        //setup our consumer
        Layers layers = program.getLayers();


        final int nTrajs = 2;
        final int numEntries = 30;

        program.getCLI().doCommand(new String[]{"lc"});

        program.getCLI().doCommand(new String[]{"gt", "-nt", String.valueOf(nTrajs), "-ne", String.valueOf(numEntries)});

        for (WrappedEntityLayer someLayer : layers) {
            System.out.println(someLayer);
        }

        WrappedEntity<STTrajectory> entity = layers.getFirstEntity(STTrajectory.class);

        Assert.assertTrue(entity != null);

        Assert.assertTrue(entity.getModel().size() == numEntries);


    }

}
