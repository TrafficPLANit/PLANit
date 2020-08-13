package org.planit.sdinteraction.smoothing;

import org.planit.assignment.TrafficAssignmentComponentFactory;
import org.planit.assignment.TrafficComponentBuilder;
import org.planit.input.InputBuilderListener;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * Base builder for all smoothing implementations
 * 
 * @author markr
 *
 * @param <T> smoothing to build
 */
public abstract class SmoothingBuilder<T extends Smoothing> extends TrafficComponentBuilder<T> {

  /**
   * Constructor
   * 
   * @param classToBuild of derived smoothing type
   * @param groupId      id token
   * @param inputBuilder the input builder
   */
  protected SmoothingBuilder(Class<T> classToBuild, IdGroupingToken groupId, InputBuilderListener inputBuilder) {
    super(classToBuild, groupId, inputBuilder);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T build() throws PlanItException {
    TrafficAssignmentComponentFactory<Smoothing> smoothingFactory = new TrafficAssignmentComponentFactory<Smoothing>(Smoothing.class);
    smoothingFactory.addListener(getInputBuilderListener(), TrafficAssignmentComponentFactory.TRAFFICCOMPONENT_CREATE);

    @SuppressWarnings("unchecked")
    T smoothing = (T) smoothingFactory.create(getClassToBuild().getCanonicalName(), new Object[] { getGroupIdToken() });

    invokeDelayedMethodCalls(smoothing);
    return smoothing;
  }

}
