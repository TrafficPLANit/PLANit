package org.goplanit.component;

import org.goplanit.input.InputBuilderListener;
import org.goplanit.utils.builder.Builder;
import org.goplanit.utils.id.IdGroupingToken;

/**
 * 
 * Builder class to build something of type T which is a PLANit component. As such the input builder listener is required to be notified when an instance is created, hence we store
 * this listener on this builder
 * 
 * @author markr
 *
 * @param <T>class to build
 */
public abstract class PlanitComponentBuilder<T> extends Builder<T> {

  /**
   * the input builder listener that is triggered whenever PLANit components are created
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
   * @param groupId      to use for id generation
   * @param inputBuilder the inputBuilder to use
   */
  protected PlanitComponentBuilder(Class<T> classToBuild, IdGroupingToken groupId, InputBuilderListener inputBuilder) {
    super(classToBuild);
    this.inputBuilder = inputBuilder;
    this.groupId = groupId;
  }

}
