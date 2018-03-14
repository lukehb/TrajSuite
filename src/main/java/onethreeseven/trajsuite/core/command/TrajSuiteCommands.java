package onethreeseven.trajsuite.core.command;

import com.beust.jcommander.JCommander;
import onethreeseven.jclimod.AbstractCommandsListing;
import onethreeseven.jclimod.CLICommand;
import onethreeseven.trajsuite.core.model.TrajsuiteLayers;
import java.util.ArrayList;

/**
 * Registers all the commands in the Core package.
 * @author Luke Bermingham
 */
public class TrajSuiteCommands extends AbstractCommandsListing {

    @Override
    protected CLICommand[] createCommands(JCommander jCommander, Object... objects) {

        ArrayList<CLICommand> commands = new ArrayList<>();

        for (Object constructorData : objects) {
            if(constructorData instanceof TrajsuiteLayers){
                //commands.add(new ListEntityHierarchyCommand((TrajsuiteLayers) constructorData));
                break;
            }
        }

        CLICommand[] commandArr = new CLICommand[commands.size()];

        return commands.toArray(commandArr);
    }
}
