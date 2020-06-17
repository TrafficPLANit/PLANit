package org.planit.output.formatter;

import java.util.Iterator;

import org.planit.data.MultiKeyPlanItData;
import org.planit.data.MultiKeyPlanItDataIterator;

/**
 * Iterator which loops through the keys and values stored in the MemoryOutputFormatter
 * 
 * This class is a wrapper for MultiKeyPlanItDataIterator.
 * 
 * @author gman6028
 *
 */
public class MemoryOutputIterator implements Iterator<Object[]> {

  /**
   * Iterator through MultiKeyPlanItData
   */
  private MultiKeyPlanItDataIterator multiKeyPlanItDataIterator;

  /**
   * Constructor
   * 
   * @param multiKeyPlanItData the MultiKeyPlanItData object storing the data, provided by the
   *          MemoryOutputFormatter
   */
  public MemoryOutputIterator(final MultiKeyPlanItData multiKeyPlanItData) {
    multiKeyPlanItDataIterator = multiKeyPlanItData.getIterator();
  }

  /**
   * Returns whether the MemoryOutputFormatter has any more rows
   * 
   * @return true if the MemoryOutputFormatter has any rows, false otherwise
   */
  @Override
  public boolean hasNext() {
    return multiKeyPlanItDataIterator.hasNext();
  }

  /**
   * Returns the next array of keys in the iteration
   * 
   * @return the next array of keys in the iteration
   */
  @Override
  public Object[] next() {
    return multiKeyPlanItDataIterator.next();
  }

  /**
   * Returns an array of keys for the current iteration
   * 
   * @return array of keys for the current iteration
   */
  public Object[] getKeys() {
    return multiKeyPlanItDataIterator.getKeys();
  }

  /**
   * Returns an array of values for the current iteration
   * 
   * @return array of values for the current iteration
   */
  public Object[] getValues() {
    return multiKeyPlanItDataIterator.getValues();
  }

}
