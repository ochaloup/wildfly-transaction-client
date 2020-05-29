package org.wildfly.transaction.client;

import javax.transaction.xa.XAResource;

public interface XAResourceRegistryProvider {
    XAResource[] getInDoubtXAResources();
}
