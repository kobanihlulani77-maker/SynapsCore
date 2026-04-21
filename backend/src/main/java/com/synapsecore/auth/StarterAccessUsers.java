package com.synapsecore.auth;

import java.util.List;

public final class StarterAccessUsers {

    public static final String STARTER_TENANT_CODE = "STARTER-OPS";

    public record StarterAccessUser(String tenantCode,
                                    String username,
                                    String password,
                                    String actorName,
                                    String fullName) {
    }

    private static final List<StarterAccessUser> USERS = List.of(
        new StarterAccessUser(STARTER_TENANT_CODE, "operations.planner", "planner-2026", "Operations Planner", "Operations Planner"),
        new StarterAccessUser(STARTER_TENANT_CODE, "operations.lead", "lead-2026", "Operations Lead", "Operations Lead"),
        new StarterAccessUser(STARTER_TENANT_CODE, "operations.operator", "operations-operator-2026", "Operations Operator", "Operations Operator"),
        new StarterAccessUser(STARTER_TENANT_CODE, "amina.planner", "amina-2026", "Amina Planner", "Amina Planner"),
        new StarterAccessUser(STARTER_TENANT_CODE, "lebo.planner", "lebo-plan-2026", "Lebo Planner", "Lebo Planner"),
        new StarterAccessUser(STARTER_TENANT_CODE, "thando.planner", "thando-2026", "Thando Planner", "Thando Planner"),
        new StarterAccessUser(STARTER_TENANT_CODE, "ayo.planner", "ayo-2026", "Ayo Planner", "Ayo Planner"),
        new StarterAccessUser(STARTER_TENANT_CODE, "naledi.lead", "naledi-2026", "Naledi Lead", "Naledi Lead"),
        new StarterAccessUser(STARTER_TENANT_CODE, "jordan.lead", "jordan-2026", "Jordan Lead", "Jordan Lead"),
        new StarterAccessUser(STARTER_TENANT_CODE, "ops.director", "ops-director-2026", "Ops Director", "Ops Director"),
        new StarterAccessUser(STARTER_TENANT_CODE, "lebo.ops", "lebo-ops-2026", "Lebo Ops", "Lebo Ops"),
        new StarterAccessUser(STARTER_TENANT_CODE, "north.ops.director", "north-approve-2026", "North Operations Director",
            "North Operations Director"),
        new StarterAccessUser(STARTER_TENANT_CODE, "coast.ops.director", "coast-approve-2026", "Coast Operations Director",
            "Coast Operations Director"),
        new StarterAccessUser(STARTER_TENANT_CODE, "executive.ops.director", "executive-approve-2026", "Executive Operations Director",
            "Executive Operations Director"),
        new StarterAccessUser(STARTER_TENANT_CODE, "integration.lead", "integration-admin-2026", "Integration Lead", "Integration Lead"),
        new StarterAccessUser(STARTER_TENANT_CODE, "integration.operator", "integration-ops-2026", "Integration Operator",
            "Integration Operator")
    );

    private StarterAccessUsers() {
    }

    public static List<StarterAccessUser> all() {
        return USERS;
    }

    public static StarterAccessUser forUsername(String username) {
        return USERS.stream()
            .filter(user -> user.username().equalsIgnoreCase(username))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No starter user configured for username " + username));
    }

    public static StarterAccessUser forActorName(String actorName) {
        return USERS.stream()
            .filter(user -> user.actorName().equalsIgnoreCase(actorName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No starter user configured for actor " + actorName));
    }
}
