package onethreeseven.trajsuite.core.model;

/**
 * Exit codes used for command execution.
 *
 * @author Luke Bermingham
 */
public enum ExitCode {
    GOOD(0),
    GENERAL_ERROR(1),
    INVALID_PARAMS(2);

    public final int code;
    ExitCode(int code){
        this.code = code;
    }

}
