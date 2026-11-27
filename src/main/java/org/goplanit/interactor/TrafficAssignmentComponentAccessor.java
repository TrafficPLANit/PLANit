package org.goplanit.interactor;

/**
 * Implementing class requires access to one or more traffic assignment components. Used, for example, by cost
 * implementations on a traffic assignment, to access the components they require without access to the assignment
 * itself, so the cost component remains agnostic of the underlying assignment
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
