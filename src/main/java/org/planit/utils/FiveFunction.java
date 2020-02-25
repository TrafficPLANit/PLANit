package org.planit.utils;

/**
 * Function Interface which can process five input objects to generate another one.
 * 
 * Used in the PlanItIO unit tests.  
 * 
 * There is no equivalent functional interface in the java.util.function library so we have created this one.
 * 
 * @author gman6028
 *
 * @param <R> first object to be processed
 * @param <S> second object to be processed
 * @param <T> third object to be processed
 * @param <U> fourth object to be processed
 * @param <V> fifth object to be processed
 * @return W object to return
 */
@FunctionalInterface
public interface FiveFunction<R, S, T, U, V, W> {
  
  public W apply(R r, S s, T t, U u, V v);

}
