/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.TestDNACursor;
import com.tc.object.TestDNAWriter;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.objectserver.core.api.ManagedObjectState;
import com.tc.objectserver.persistence.HeapStorageManagerFactory;
import com.tc.objectserver.persistence.Persistor;
import com.tc.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestCase;

public abstract class AbstractTestManagedObjectState extends TestCase {
  protected ObjectID                            objectID;
  protected ManagedObjectChangeListenerProvider listenerProvider;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.listenerProvider = new NullManagedObjectChangeListenerProvider();
    ManagedObjectStateFactory.disableSingleton(true);
    Persistor persistor = new Persistor(HeapStorageManagerFactory.INSTANCE);
    persistor.start();
    ManagedObjectStateFactory.createInstance(this.listenerProvider, persistor);
    this.objectID = new ObjectID(2000);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ManagedObjectStateFactory.disableSingleton(false);
    this.objectID = null;
    this.listenerProvider = null;
  }

  protected ManagedObjectState createManagedObjectState(final String className, final TestDNACursor cursor,
                                                        final ObjectID id) throws Exception {
    return createManagedObjectState(className, cursor, ObjectID.NULL_ID, id);
  }

  protected ManagedObjectState createManagedObjectState(final String className, final TestDNACursor cursor,
                                                        final ObjectID parentID, final ObjectID id) throws Exception {
    final ManagedObjectState state = ManagedObjectStateFactory.getInstance().createState(id, parentID,
                                                                                         className, cursor);
    return state;
  }

  public void basicTestUnit(final String className, final byte type, final TestDNACursor cursor, final int objCount)
      throws Exception {
    basicTestUnit(className, type, cursor, objCount, true);
  }

  public void basicTestUnit(final String className, final byte type, final TestDNACursor cursor, final int objCount,
                            final boolean verifyReadWrite) throws Exception {
    final ManagedObjectState state = createManagedObjectState(className, cursor, new ObjectID(1), new ObjectID(2));
    state.apply(this.objectID, cursor, new ApplyTransactionInfo());

    // API verification
    basicAPI(className, type, cursor, objCount, state);

    // dehydrate
    basicDehydrate(cursor, objCount, state);

    // writeTo, readFrom and equal
    if (verifyReadWrite) {
      basicReadWriteEqual(type, state);
    }
  }

  protected void basicAPI(final String className, final byte type, final TestDNACursor cursor, final int objCount,
                          final ManagedObjectState state) {
    Assert.assertEquals("BackReferences object size", objCount, state.getObjectReferences().size());
    Assert.assertTrue(state.getType() == type);
    Assert.assertTrue("ClassName:" + state.getClassName(), state.getClassName().equals(className));

  }

  protected void basicDehydrate(final TestDNACursor cursor, final int objCount, final ManagedObjectState state) {
    final TestDNAWriter dnaWriter = new TestDNAWriter();
    state.dehydrate(this.objectID, dnaWriter, DNAType.L1_FAULT);
    cursor.reset();
    cursor.next();
    while (cursor.next()) {
      final Object action = cursor.getAction();
      Assert.assertTrue(dnaWriter.containsAction(action));
    }
  }

  protected void basicReadWriteEqual(final byte type, final ManagedObjectState state) throws Exception {
    final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    final TCObjectOutputStream out = new TCObjectOutputStream(bout);
    state.writeTo(out);
    final ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
    final TCObjectInputStream in = new TCObjectInputStream(bin);
    final ManagedObjectState state2 = ManagedObjectStateFactory.getInstance().readManagedObjectStateFrom(in, type);
    Assert.assertTrue(state.equals(state2));
  }

}
