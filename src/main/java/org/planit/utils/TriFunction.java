package org.planit.utils;

/**
 * Function Interface which can process three input objects and produce an output
 * 
 * Used in PlanItIO test cases.  
 * 
 * There is no equivalent functional interface in the java.util.function library so we have created this one.
 * 
 * @author gman6028
 *
 * @param <T>  first object to be processed
 * @param <U> second object to be processed
 * @param <V> third object to be processed
 * @param <U> object generated
 */
@FunctionalInterface
public interface TriFunction<R, S, T, U> {

  /**
   * Performs this operation on the given arguments
   * 
   * @param t first object to be processed
   * @param u second object to be processed
   * @param v third object to be processed
   * @return object of specified type
   */
  public U apply(R r, S s, T t);

}
