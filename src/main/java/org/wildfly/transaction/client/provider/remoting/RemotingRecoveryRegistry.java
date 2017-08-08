/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.transaction.client.provider.remoting;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAResource;

import org.jboss.tm.XAResourceRecovery;
import org.wildfly.transaction.client.LocalTransactionContext;
import org.wildfly.transaction.client.SubordinateXAResource;

/**
 * Providing EJB remote XAResources for periodic recovery being able to recover unfinished ones. 
 */
public class RemotingRecoveryRegistry implements XAResourceRecovery {

    /* 
    private final Set<Connection> connections = ConcurrentHashMap.newKeySet();
    public void addRemoteConnection(Connection connection) {
        System.out.println("Registering recovery registry connection: " + connection);
        connections.add(connection);
        connection.addCloseHandler(this::handleClosed);
    }

    @Override
    public XAResource[] getXAResources() {
        // TODO: think about use of code at SerializedUserTransaction
        List<XAResource> remoteXAResources = new ArrayList<>();
        System.out.println("Called to get XAResources for remote recovery");
        try {
            String nodeName = LocalTransactionContext.getCurrent().getProvider().getNodeName();
            for(Connection connection: connections) {
                System.out.println("Trying to get XAREsource for nodename: " + nodeName + ", connection uri: " + connection.getPeerURI());
                XAResource xaresource = new SubordinateXAResource(connection.getPeerURI(), nodeName);
                remoteXAResources.add(xaresource);
            }
        } catch (Exception e) {
            System.out.println("ERROR: can't get subordinate xaresource!");
            e.printStackTrace();
        }
        return remoteXAResources.toArray(new XAResource[]{});
    }

    void handleClosed(Connection connection, IOException expectedToIgnore) {
        connections.remove(connection);
    } */

    private final Set<URI> uris = ConcurrentHashMap.newKeySet();

    public void addRemoteUri(URI uri) {
        System.out.println("Registering recovery registry uri: " + uri);
        uris.add(uri);
    }
    public Collection<URI> getUris() {
        return uris;
    }
    public void remoteRemoteUri(URI uri) {
        System.out.println("Unregistering recovery registry uri " + uri);
        uris.remove(uri);
    }
    
    @Override
    public XAResource[] getXAResources() {
        // TODO: think about use of code at SerializedUserTransaction
        List<XAResource> remoteXAResources = new ArrayList<>();
        System.out.println("Called to get XAResources for remote recovery");
        try {
            String nodeName = LocalTransactionContext.getCurrent().getProvider().getNodeName();
            for(URI uri: uris) {
                System.out.println("Trying to get XAREsource for nodename: " + nodeName + ", connection uri: " + uri);
                // 1 << 30 = FL_CONFIRMED -> see OutflowHandleManager
                XAResource xaresource = new SubordinateXAResource(uri, 1 << 30, nodeName);
                remoteXAResources.add(xaresource);
            }
        } catch (Exception e) {
            System.out.println("ERROR: can't get subordinate xaresource!");
            e.printStackTrace();
        }
        return remoteXAResources.toArray(new XAResource[]{});
    }
}
