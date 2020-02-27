package org.planit.utils;

import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

/**
 * Implementation of the MultiKeyPlanItDataIterator interface
 * 
 * @author gman6028
 *
 */
public class MultiKeyPlanItDataIteratorImpl implements MultiKeyPlanItDataIterator {

  /**
   * flag to indicate whether the MultiKeyPlanItData object has one key or several
   */
  private boolean isSingleKey;
  
  /**
   * Array of key values for the current row
   */
  private Object[] keys;
  
  /**
   * Array of output values for the current row
   */
  private Object[] values;
  
  /**
   * Map iterator used if the MultiKeyPlanItData object has several keys
   */
  private final MapIterator<MultiKey<? extends Object>, Object[]> multiKeyMapIterator;
  
  /**
   * Map iterator used if the MultiKeyPlanItData object has one key 
   */
  private final MapIterator<Object, Object[]> singleKeyMapIterator;

/**
 * Constructor
 * 
 * @param isSingleKey flag to indicate whether the MultiKeyPlanItData object has one key or several
 * @param singleKeyMap Map used if the MultiKeyPlanItData object has one key 
 * @param multiKeyMap Map used if the MultiKeyPlanItData object has several keys 
 */
  public MultiKeyPlanItDataIteratorImpl(final boolean isSingleKey, final IterableMap<Object, Object[]> singleKeyMap, final MultiKeyMap<Object, Object[]> multiKeyMap) {
    this.isSingleKey = isSingleKey;
    if (isSingleKey) {
      singleKeyMapIterator = (MapIterator<Object, Object[]>) singleKeyMap.mapIterator();
      multiKeyMapIterator = null;
    } else {
      singleKeyMapIterator = null;
      multiKeyMapIterator = (MapIterator<MultiKey<? extends Object>, Object[]>) multiKeyMap.mapIterator();
    }
  }
  
  /**
   * Returns whether the MultiKeyPlanItData has any more rows
   * 
   * @return true if the MultiKeyPlanItData has any rows, false otherwise
   */
  @Override
  public boolean hasNext() {
    boolean hasNext = isSingleKey ? singleKeyMapIterator.hasNext() : multiKeyMapIterator.hasNext();
    if (hasNext) {
      if (isSingleKey) {
        Object singleKey = singleKeyMapIterator.next();
        keys =  new Object[] {singleKey};
        values = singleKeyMapIterator.getValue();
      } else {
        MultiKey<? extends Object> multiKey = multiKeyMapIterator.next();
        keys = multiKey.getKeys();
        values = multiKeyMapIterator.getValue();
      }
    }
    return hasNext;
  }

  /**
   * Returns the next array of keys in the iteration
   * 
   * @return the next array of keys in the iteration
   */
  @Override
  public Object[] next() {
    return keys;
  }

  /**
   * Returns an array of keys for the current iteration
   * 
   * @return array of keys for the current iteration
   */
  @Override
  public Object[] getKeys() {
    return keys;
  }

  /**
   * Returns an array of values for the current iteration
   * 
   * @return array of values for the current iteration
   */
  @Override
  public Object[] getValues() {
    return values;
  }

}