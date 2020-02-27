package org.planit.utils;

import java.util.Iterator;

import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * Iterator which returns the keys and values for each row in a MultiKeyPlanItData object.
 * 
 * @author gman6028
 *
 */
public interface MultiKeyPlanItDataIterator extends Iterator<Object[]> {
  
  /**
   * Returns whether the MultiKeyPlanItData has any more rows
   * 
   * @return true if the MultiKeyPlanItData has any rows, false otherwise
   */
  @Override
  boolean hasNext();
  
  /**
   * Returns the next array of keys in the iteration
   * 
   * @return the next array of keys in the iteration
   */
  @Override
  Object[] next();
  
  /**
   * Returns an array of keys for the current iteration
   * 
   * @return array of keys for the current iteration
   */
  Object[] getKeys();
  
  /**
   * Returns an array of values for the current iteration
   * 
   * @return array of values for the current iteration
   */
  Object[] getValues();
  
  /**
   * Returns an instance of a class which implements this interface
   * 
   * @param isSingleKey flag to indicate whether the MultiKeyPlanItData object has one key or several
   * @param singleKeyMap Map used if the MultiKeyPlanItData object has one key 
   * @param multiKeyMap Map used if the MultiKeyPlanItData object has several keys 
   * @return a class which implements this interface
   */
  public static MultiKeyPlanItDataIterator getInstance(final boolean isSingleKey, final IterableMap<Object, Object[]> singleKeyMap, final MultiKeyMap<Object, Object[]> multiKeyMap) {
    return new MultiKeyPlanItDataIteratorImpl(isSingleKey,  singleKeyMap,  multiKeyMap);
  }

}