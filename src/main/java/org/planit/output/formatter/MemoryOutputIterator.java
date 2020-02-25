package org.planit.output.formatter;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.planit.data.MultiKeyPlanItData;

/**
 * Iterator which loops through the keys and values stored in the MemoryOutputFormatter
 * 
 * This class uses the MapIterator and MultiKey classes to get the keys and values for the current iteration.
 * 
 * Note that this iterator has no next() method.   The two public methods getKeys() and getValues() serve the equivalent role.
 * 
 * @author gman6028
 *
 */
public class MemoryOutputIterator {
  
  private final MapIterator<MultiKey<? extends Object>, Object[]> mapIterator;
  private MultiKey<? extends Object> multiKey;
  
  /**
   * Constructor
   * 
   * @param multiKeyPlanItData the MultiKeyPlanItData object storing the data, provided by the MemoryOutputFormatter
   */
  public MemoryOutputIterator(final MultiKeyPlanItData multiKeyPlanItData) {
    this.mapIterator = multiKeyPlanItData.getIterator();
  }
  
  /**
   * Returns whether the MemoryOutputFormatter has any more rows
   * 
   * @return true if the MemoryOutputFormatter has any rows, false otherwise
   */
  public boolean hasNext() {
    boolean hasNext = mapIterator.hasNext();
    if (hasNext) {
      multiKey = mapIterator.next();
    }
    return hasNext;
  }

/**
 * Returns an array of keys for the current iteration
 * 
 * @return array of keys for the current iteration
 */
  public Object[] getKeys() {
    return multiKey.getKeys();
  }
  
/**
 * Returns an array of values for the current iteration
 * 
 * @return array of values for the current iteration
 */
  public Object[] getValues() {
    return mapIterator.getValue();
  }
}