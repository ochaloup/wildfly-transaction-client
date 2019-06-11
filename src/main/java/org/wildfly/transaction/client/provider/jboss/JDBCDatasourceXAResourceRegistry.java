/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.transaction.client.provider.jboss;

import org.wildfly.common.annotation.NotNull;
import org.wildfly.transaction.client.LocalTransaction;
import org.wildfly.transaction.client.SimpleXid;
import org.wildfly.transaction.client.XAResourceRegistry;
import org.wildfly.transaction.client._private.Log;
import org.wildfly.transaction.client.spi.LocalTransactionProvider;

import javax.sql.DataSource;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: see {@link FileSystemXAResourceRegistry}
 */
final class JDBCDatasourceXAResourceRegistry implements XAResourceRegistryProvider {

    /**
     * Empty utility array.
     */
    private static final XAResource[] EMPTY_IN_DOUBT_RESOURCES = new XAResource[0];

    /**
     * Key for keeeping the xa resource registry associated with a local transaction
     */
    private static final Object XA_RESOURCE_REGISTRY_KEY = new Object();

    /**
     * The local transaction provider associated with this file system XAResource registry
     */
    private final LocalTransactionProvider provider;

    /**
     * <p>
     * A set of in doubt resources, i.e., outflowed resources whose prepare/rollback/commit operation was not
     * completed normally, or resources that have been recovered from in doubt registries. See
     * {@link XAResourceRegistryFile#resourceInDoubt} and {@link XAResourceRegistryFile#loadInDoubtResources}.
     * </p>
     * <p>
     * It is a set because we could have an in doubt resource reincide in failure to complete
     * </p>
     */
    private final Set<XAResource> inDoubtResources = Collections.synchronizedSet(new HashSet<>());

    private final DataSource ds;
    private final String tableName;

    /**
     * Creates a FileSystemXAResourceRegistry.
     *
     * @param relativePath the path recovery dir is relative to
     */
    JDBCDatasourceXAResourceRegistry (DataSource ds, String tableName, LocalTransactionProvider provider) {
        this.provider = provider;
        this.ds = ds;
        this.tableName = tableName;
    }

    /**
     * {@inheritDoc}
     */
    public XAResourceRegistry getXAResourceRegistry(LocalTransaction transaction) throws SystemException {
        XAResourceRegistry registry = (XAResourceRegistry) transaction.getResource(XA_RESOURCE_REGISTRY_KEY);
        if (registry != null)
            return registry;
        registry = new XAResourceRegistryJDBCDatasource(transaction.getXid());
        transaction.putResource(XA_RESOURCE_REGISTRY_KEY, registry);
        return registry;
    }

    /**
     * {@inheritDoc}
     */
    public XAResource[] getInDoubtXAResources() {
        try {
            recoverInDoubtRegistries();
        } catch (IOException e) {
            throw Log.log.unexpectedExceptionOnXAResourceRecovery(e);
        }
        return inDoubtResources.isEmpty() ? EMPTY_IN_DOUBT_RESOURCES : inDoubtResources.toArray(
                    new XAResource[inDoubtResources.size()]);
    }

    /**
     * Recovers closed registries files from file system. All those registries are considered in doubt.
     *
     * @throws IOException if there is an I/O error when reading the recovered registry files
     */
    private void recoverInDoubtRegistries() throws IOException {
    	Connection sqlConnection = ds.getConnection();
    	Statement statement = sqlConnection.createStatement();
    	ResultSet rs = statement.executeQuery("SELECT Lname FROM " + tableName " + WHERE Snum = 2001");

        final File recoveryDir = xaRecoveryPath.toFile();
        if (!recoveryDir.exists()) {
            return;
        }
        final String[] xaRecoveryFileNames = recoveryDir.list();
        if (xaRecoveryFileNames == null) {
            Log.log.listXAResourceRecoveryFilesNull(recoveryDir);
            return;
        }
        for (String xaRecoveryFileName : xaRecoveryFileNames) {
            // check if file is not open already
            if (!openFilePaths.contains(xaRecoveryFileName))
                new XAResourceRegistryFile(xaRecoveryFileName, provider);
        }
    }


    /**
     * Represents a single file in the file system that records all outflowed resources per a specific local transaction.
     */
    private final class XAResourceRegistryJDBCDatasource extends XAResourceRegistry {

        /**
         * Path to the registry file.
         */
        @NotNull
        private final Path filePath;

        /**
         * The file channel, if non-null, it indicates that this registry represents a current, on-going transaction,
         * if null, then this registry represents an registry file recovered from file system.
         */
        private final FileChannel fileChannel;

        /**
         * Keeps track of the XA outflowed resources stored in this registry, see {@link #addResource} and
         * {@link #removeResource}.
         */
        private final Set<XAResource> resources = Collections.synchronizedSet(new HashSet<>());


        /**
         * Creates a XA  recovery registry for a transaction. This method assumes that there is no file already
         * existing for this transaction, and, furthermore, it is not thread safe  (the creation of this object is
         * already thread protected at the caller).
         *
         * @param xid the transaction xid
         * @throws SystemException if the there was a problem when creating the recovery file in file system
         */
        XAResourceRegistryJDBCDatasource(Xid xid) throws SystemException {
            xaRecoveryPath.toFile().mkdir(); // create dir if non existent
            final String xidString = SimpleXid.of(xid).toHexString('_');
            this.filePath = xaRecoveryPath.resolve(xidString);
            openFilePaths.add(xidString);
            try {
                fileChannel = FileChannel.open(filePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE_NEW);
                fileChannel.lock();
                Log.log.xaResourceRecoveryFileCreated(filePath);
            } catch (IOException e) {
                throw Log.log.createXAResourceRecoveryFileFailed(filePath, e);
            }
        }

        /**
         * Reload a registry that is in doubt, i.e., the registry is not associated yet with a current
         * transaction in this server, but with a transaction of a previous jvm instance that is now
         * being recovered.
         * This will happen only if the jvm crashes before a transaction with XA outflowed resources is
         * fully prepared. In this case, any lines in the registry can correspond to in doubt outflowed
         * resources. The goal is to reload those resources so they can be recovered.
         *
         * @param inDoubtFilePath the file path of the in doubt registry
         * @throws IOException if there is an I/O error when realoding the registry file
         */
        private XAResourceRegistryFile(String inDoubtFilePath, LocalTransactionProvider provider) throws IOException {
            this.filePath = xaRecoveryPath.resolve(inDoubtFilePath);
            this.fileChannel = null; // no need to open file channel here
            openFilePaths.add(inDoubtFilePath);
            loadInDoubtResources(provider.getNodeName());
            Log.log.xaResourceRecoveryRegistryReloaded(filePath);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void addResource(XAResource resource, URI uri) throws SystemException {
            assert fileChannel != null;
            try {
                assert fileChannel.isOpen();
                fileChannel.write(ByteBuffer.wrap((uri.toString() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8)));
                fileChannel.force(true);
            } catch (IOException e) {
                throw Log.log.appendXAResourceRecoveryFileFailed(uri, filePath, e);
            }
            this.resources.add(resource);
            Log.log.xaResourceAddedToRecoveryRegistry(uri, filePath);
        }

        /**
         * {@inheritDoc}
         * The registry file is closed and deleted if there are no more resources left.
         *
         * @throws XAException if there is a problem deleting the registry file
         */
        @Override
        protected void removeResource(XAResource resource) throws XAException {
            if (resources.remove(resource)) {
                if (resources.isEmpty()) {
                    // delete file
                    try {
                        if (fileChannel != null) {
                            fileChannel.close();
                        }
                        Files.delete(filePath);
                        openFilePaths.remove(filePath.toString());
                    } catch (IOException e) {
                        throw Log.log.deleteXAResourceRecoveryFileFailed(XAException.XAER_RMERR, filePath, resource, e);
                    }
                    Log.log.xaResourceRecoveryFileDeleted(filePath);
                }
                // remove resource from in doubt list, in case the resource was in doubt
                inDoubtResources.remove(resource);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void resourceInDoubt(XAResource resource) {
            inDoubtResources.add(resource);
        }

        /**
         * Loads in doubt resources from recovered registry file.
         *
         * @throws IOException if an I/O error occurs when reloading the resources from the file
         */
        private void loadInDoubtResources(String nodeName) throws IOException {
            assert fileChannel == null;
            final List<String> uris;
            try {
                uris = Files.readAllLines(filePath);
            } catch (IOException e) {
                throw Log.log.readXAResourceRecoveryFileFailed(filePath, e);
            }
            for (String uriString : uris) {
                // adding a line separator at the end of each uri entry results in an extra empty line
                if (uriString.isEmpty())
                    continue;
                final URI uri;
                try {
                    uri = new URI(uriString);
                } catch (URISyntaxException e) {
                    throw Log.log.readURIFromXAResourceRecoveryFileFailed(uriString, filePath, e);
                }
                final XAResource xaresource = reloadInDoubtResource(uri, nodeName);
                inDoubtResources.add(xaresource);
                Log.log.xaResourceRecoveredFromRecoveryRegistry(uri, filePath);
            }
        }
    }
}
