package org.wildfly.transaction.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class XAResourceRegistryProviderRegister {
    private static final Set<XAResourceRegistryProvider> register = new CopyOnWriteArraySet<>();

    public static void register(XAResourceRegistryProvider xaResourceRegistryProvider) {
        register.add(xaResourceRegistryProvider);
    }

    public static void deregister(XAResourceRegistryProvider xaResourceRegistryProvider) {
        register.remove(xaResourceRegistryProvider);
    }

    public static Set<XAResourceRegistryProvider> getProviders() {
        return register;
    }
}
