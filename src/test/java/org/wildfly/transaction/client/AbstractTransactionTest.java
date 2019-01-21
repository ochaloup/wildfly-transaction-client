package org.wildfly.transaction.client;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.xa.XAResource;

import org.junit.Assert;
import org.junit.Test;


public class AbstractTransactionTest {

    @Test
    public void testOneSecond() {
        AbstractTransaction txn = new TestAbstractTransaction(1);
        Assert.assertEquals("Not existing long running action, the timeout should be at value 1",
                1, txn.getEstimatedRemainingTime());
    }

    @Test
    public void testOneSecondElapsed() throws InterruptedException {
        AbstractTransaction txn = new TestAbstractTransaction(1);
        Thread.sleep(1000);

        Assert.assertEquals("Timeout of one second elapsed already the remaining time shoul be 0",
                0, txn.getEstimatedRemainingTime());
    }

    static class TestAbstractTransaction extends AbstractTransaction {
        private int timeoutS;
    
        TestAbstractTransaction(int timeoutS) {
            this.timeoutS = timeoutS;
        }
    
        @Override
        public void registerSynchronization(Synchronization sync)
                throws RollbackException, IllegalStateException, SystemException {
        }
        
        @Override
        public int getStatus() throws SystemException {
            return 0;
        }
        
        @Override
        public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
            return false;
        }
        
        @Override
        public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
            return false;
        }
        
        @Override
        void verifyAssociation() {
            
        }
        
        @Override
        void unimportBacking() {
            
        }
        
        @Override
        void suspend() throws SystemException {
        }
        
        @Override
        void rollbackAndDissociate() throws IllegalStateException, SystemException {
            
        }
        
        @Override
        public void rollback() throws IllegalStateException, SystemException {
            
        }
        
        @Override
        void resume() throws SystemException {
            
        }
        
        @Override
        void registerInterposedSynchronization(Synchronization synchronization) throws IllegalStateException {
            
        }
        
        @Override
        public Object putResourceIfAbsent(Object key, Object value) throws IllegalArgumentException {
            return null;
        }
        
        @Override
        public void putResource(Object key, Object value) throws NullPointerException {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        boolean importBacking() throws SystemException {
            return false;
        }
        
        @Override
        public Object getResource(Object key) throws NullPointerException {
            return null;
        }
        
        @Override
        Object getKey() {
            return null;
        }
        
        @Override
        void commitAndDissociate() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                SecurityException, SystemException {
            
        }
        
        @Override
        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                SecurityException, SystemException {
            
        }
        
        @Override
        public int getTransactionTimeout() {
            return this.timeoutS;
        }
    }
}
