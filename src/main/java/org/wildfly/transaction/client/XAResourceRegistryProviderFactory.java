package org.wildfly.transaction.client;

public final class XAResourceRegistryProviderFactory {
    private static XAResourceRegistryProvider INSTANCE;

    public static XAResourceRegistryProvider getInstance() {
        return INSTANCE;
    }

    public static void register(XAResourceRegistryProvider xaResourceRegistryProvider) {
        INSTANCE = xaResourceRegistryProvider;
    }
}
