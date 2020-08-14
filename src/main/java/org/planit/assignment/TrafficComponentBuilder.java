package org.planit.assignment;

import org.planit.input.InputBuilderListener;
import org.planit.utils.builder.Builder;
import org.planit.utils.id.IdGroupingToken;

/**
 * 
 * Builder class to build something of type T which is a traffic component. As such the input builder listener is required to be notified when an instance is created, hence we
 * store this listener on this builder
 * 
 * @author markr
 *
 * @param <T>class to build
 */
public abstract class TrafficComponentBuilder<T> extends Builder<T> {

  /**
   * the input builder listener that is triggered whenever traffic assignment components are created
   */
  private final InputBuilderListener inputBuilder;

  /**
   * id grouping token
   */
  protected IdGroupingToken groupId;

  /**
   * collect the input builder
   * 
   * @return inputBuilderListener
   */
  protected InputBuilderListener getInputBuilderListener() {
    return inputBuilder;
  }

  /**
   * collect the group id token
   * 
   * @return group id token
   */
  protected IdGroupingToken getGroupIdToken() {
    return groupId;
  }

  /**
   * Constructor
   * 
   * @param classToBuild to have access to type of T
   */
  protected TrafficComponentBuilder(Class<T> classToBuild, IdGroupingToken groupId, InputBuilderListener inputBuilder) {
    super(classToBuild);
    this.inputBuilder = inputBuilder;
    this.groupId = groupId;
  }

}
