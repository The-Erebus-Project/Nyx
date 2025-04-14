package io.github.vizanarkonin.nyx.Utils;

/**
 * A reference list of loggable user activities.
 * NOTE: Values may contain wildcards for string formatter
 */
public class UserActivities {
    public static final String CREATE_PROJECT   = "Created project - %s (ID %d)";
    public static final String EDIT_PROJECT     = "Edited project - %s (ID %d)";
    public static final String DELETE_PROJECT   = "Deleted project - %s (ID %d)";

    public static final String CREATE_USER      = "Created user - %s (ID %d)";
    public static final String EDIT_USER        = "Edited user - %s (ID %d)";
    public static final String DELETE_USER      = "Deleted user - %s (ID %d)";
    public static final String RESET_PASSWORD   = "Password reset for user %s (ID %d)";
    
    public static final String PASSWORD_CHANGE  = "Password change by user %s (ID %d)";

    public static final String UPLOAD_RESULTS   = "Results upload";
    public static final String DELETE_RESULTS   = "Results deletion";
}
