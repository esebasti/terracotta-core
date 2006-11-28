/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.collections;

import com.tc.util.Stringifier;

/**
 * A {@link CollectionMismatch}that is used when one collection is missing an object that's present in the other
 * collection.
 */
class MissingObjectCollectionMismatch extends CollectionMismatch {
  public MissingObjectCollectionMismatch(Object originating, boolean originatingIsInCollectionOne,
                                         int originatingIndex, Stringifier describer) {
    super(originating, null, originatingIsInCollectionOne, originatingIndex, -1, describer);
  }

  public String toString() {
    return "Missing object: there is no counterpart in " + comparedAgainstCollection() + " for " + originatingString();
  }
}