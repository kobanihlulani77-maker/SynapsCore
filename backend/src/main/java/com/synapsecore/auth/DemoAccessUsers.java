package com.synapsecore.auth;

import java.util.List;

public final class DemoAccessUsers {

    public static final String DEFAULT_TENANT_CODE = "SYNAPSE-DEMO";

    public record DemoAccessUser(String tenantCode, String username, String password, String actorName, String fullName) {
    }

    private static final List<DemoAccessUser> USERS = List.of(
        new DemoAccessUser(DEFAULT_TENANT_CODE, "operations.planner", "planner-2026", "Operations Planner", "Operations Planner"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "operations.lead", "lead-2026", "Operations Lead", "Operations Lead"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "amina.planner", "amina-2026", "Amina Planner", "Amina Planner"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "lebo.planner", "lebo-plan-2026", "Lebo Planner", "Lebo Planner"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "thando.planner", "thando-2026", "Thando Planner", "Thando Planner"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "ayo.planner", "ayo-2026", "Ayo Planner", "Ayo Planner"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "naledi.lead", "naledi-2026", "Naledi Lead", "Naledi Lead"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "jordan.lead", "jordan-2026", "Jordan Lead", "Jordan Lead"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "ops.director", "ops-director-2026", "Ops Director", "Ops Director"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "lebo.ops", "lebo-ops-2026", "Lebo Ops", "Lebo Ops"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "north.ops.director", "north-approve-2026", "North Operations Director",
            "North Operations Director"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "coast.ops.director", "coast-approve-2026", "Coast Operations Director",
            "Coast Operations Director"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "executive.ops.director", "executive-approve-2026", "Executive Operations Director",
            "Executive Operations Director"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "integration.lead", "integration-admin-2026", "Integration Lead", "Integration Lead"),
        new DemoAccessUser(DEFAULT_TENANT_CODE, "integration.operator", "integration-ops-2026", "Integration Operator",
            "Integration Operator")
    );

    private DemoAccessUsers() {
    }

    public static List<DemoAccessUser> all() {
        return USERS;
    }

    public static DemoAccessUser forUsername(String username) {
        return USERS.stream()
            .filter(user -> user.username().equalsIgnoreCase(username))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No demo user configured for username " + username));
    }

    public static DemoAccessUser forActorName(String actorName) {
        return USERS.stream()
            .filter(user -> user.actorName().equalsIgnoreCase(actorName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No demo user configured for actor " + actorName));
    }
}
