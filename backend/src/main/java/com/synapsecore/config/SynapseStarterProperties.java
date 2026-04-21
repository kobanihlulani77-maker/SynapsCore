package com.synapsecore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "synapsecore.starter")
public class SynapseStarterProperties {

    private boolean autoSeedOnEmpty = true;
    private boolean allowDefaultTenantFallback = true;
    private boolean seedStarterInventoryOnTenantOnboarding = true;
    private boolean seedStarterConnectorsOnTenantOnboarding = true;
    private boolean allowTenantAdminTenantOnboarding = true;

    public boolean isAutoSeedOnEmpty() {
        return autoSeedOnEmpty;
    }

    public void setAutoSeedOnEmpty(boolean autoSeedOnEmpty) {
        this.autoSeedOnEmpty = autoSeedOnEmpty;
    }

    public boolean isAllowDefaultTenantFallback() {
        return allowDefaultTenantFallback;
    }

    public void setAllowDefaultTenantFallback(boolean allowDefaultTenantFallback) {
        this.allowDefaultTenantFallback = allowDefaultTenantFallback;
    }

    public boolean isSeedStarterInventoryOnTenantOnboarding() {
        return seedStarterInventoryOnTenantOnboarding;
    }

    public void setSeedStarterInventoryOnTenantOnboarding(boolean seedStarterInventoryOnTenantOnboarding) {
        this.seedStarterInventoryOnTenantOnboarding = seedStarterInventoryOnTenantOnboarding;
    }

    public boolean isSeedStarterConnectorsOnTenantOnboarding() {
        return seedStarterConnectorsOnTenantOnboarding;
    }

    public void setSeedStarterConnectorsOnTenantOnboarding(boolean seedStarterConnectorsOnTenantOnboarding) {
        this.seedStarterConnectorsOnTenantOnboarding = seedStarterConnectorsOnTenantOnboarding;
    }

    public boolean isAllowTenantAdminTenantOnboarding() {
        return allowTenantAdminTenantOnboarding;
    }

    public void setAllowTenantAdminTenantOnboarding(boolean allowTenantAdminTenantOnboarding) {
        this.allowTenantAdminTenantOnboarding = allowTenantAdminTenantOnboarding;
    }
}
