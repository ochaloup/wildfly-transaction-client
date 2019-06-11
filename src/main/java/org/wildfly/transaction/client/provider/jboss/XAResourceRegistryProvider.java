package org.wildfly.transaction.client.provider.jboss;

import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.wildfly.transaction.client.LocalTransaction;
import org.wildfly.transaction.client.XAResourceRegistry;

public interface XAResourceRegistryProvider {
    /**
     * Returns the XAResourceRegistry for {@code transaction}.
     *
     * @param transaction the transaction
     * @return the XAResourceRegistry for {@code transaction}. If there is no such registry, a new one is created.
     * @throws SystemException if an unexpected failure occurs when creating the registry
     */
    XAResourceRegistry getXAResourceRegistry(LocalTransaction transaction) throws SystemException;

    /**
     * Returns a list containing all in doubt xa resources. A XAResource is considered in doubt if:
     * <ul>
     *     <li>it failed to prepare on a two-phase commit by throwing an exception</li>
     *     <li>it failed to commit or rollback in a one-phase commit by throwing an exception</li>
     * </ul>
     * An in doubt resource is no longer considered in doubt if it succeeded to rollback without an exception.
     *
     * Notice that in doubt xa resources are kept after the server shuts down, guaranteeing that they can eventually be
     * recovered, even if in a different server JVM instance than the one that outflowed the resource. This mechanism
     * assures proper recovery and abortion of the original in-doubt outflowed resource, that belongs to an external
     * remote server.
     *
     * @return a list of the in doubt xa resources
     */
    XAResource[] getInDoubtXAResources();
}
