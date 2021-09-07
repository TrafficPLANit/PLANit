package org.planit.interactor;

/**
 * Implementing class requires access to one or more traffic assignment components. USed by for example cost implementations on traffic assignment to allow them access to traffic
 * assignment components without the need to have access to the actual assignment allowing the cost component to remain agnostic to the underlying assignment while still being able
 * to access its components that it requires
 * 
 * @author markr
 *
 */
public interface TrafficAssignmentComponentAccessor extends InteractorAccessor<TrafficAssignmentComponentAccessee> {

  /**
   * {@inheritDoc}
   */
  @Override
  default Class<TrafficAssignmentComponentAccessee> getCompatibleAccessee() {
    return TrafficAssignmentComponentAccessee.class;
  }
}
