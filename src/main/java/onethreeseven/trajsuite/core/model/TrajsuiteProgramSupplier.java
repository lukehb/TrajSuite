package onethreeseven.trajsuite.core.model;

import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.ProgramSupplier;

/**
 * Supplier to pass in a {@link TrajSuiteProgram} instead of a {@link BaseTrajSuiteProgram}
 * @author Luke Bermingham
 */
public class TrajsuiteProgramSupplier implements ProgramSupplier {

    private static TrajSuiteProgram inst;

    @Override
    public BaseTrajSuiteProgram supply() {
        if(inst == null){
            inst = new TrajSuiteProgram();
        }
        return inst;
    }
}
