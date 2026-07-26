package com.dapfintech.audit.constants;

public final class AuditActions {

    private AuditActions() {
    }

    public static final String LOGIN =
            "LOGIN";

    public static final String CREATE_CUSTOMER =
            "CREATE_CUSTOMER";

    public static final String UPDATE_CUSTOMER =
            "UPDATE_CUSTOMER";

    public static final String CREATE_EMPLOYEE =
            "CREATE_EMPLOYEE";

    public static final String UPDATE_EMPLOYEE =
            "UPDATE_EMPLOYEE";

    public static final String ACTIVATE_EMPLOYEE =
            "ACTIVATE_EMPLOYEE";

    public static final String DEACTIVATE_EMPLOYEE =
            "DEACTIVATE_EMPLOYEE";

    public static final String CREATE_LOAN =
            "CREATE_LOAN";

    public static final String APPROVE_LOAN =
            "APPROVE_LOAN";

    public static final String COLLECTION_DONE =
            "COLLECTION_DONE";

    public static final String CREATE_VISIT =
            "CREATE_VISIT";

    public static final String PERMISSIONS_UPDATED =
            "PERMISSIONS_UPDATED";

    public static final String PASSWORD_RESET =
            "PASSWORD_RESET";
    public static final String CREATE_MARKET =
            "CREATE_MARKET";

    public static final String UPDATE_MARKET =
            "UPDATE_MARKET";

    public static final String ACTIVATE_MARKET =
            "ACTIVATE_MARKET";

    public static final String DEACTIVATE_MARKET =
            "DEACTIVATE_MARKET";

    public static final String DELETE_MARKET =
            "DELETE_MARKET";

    public static final String ASSIGN_EMPLOYEE_TO_MARKET =
            "ASSIGN_EMPLOYEE_TO_MARKET";

    public static final String UNASSIGN_EMPLOYEE_FROM_MARKET =
            "UNASSIGN_EMPLOYEE_FROM_MARKET";
}