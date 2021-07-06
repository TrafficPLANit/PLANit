package org.planit.project;

import java.util.TreeMap;

import org.planit.demands.Demands;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered demands on project
 *
 */
public class ProjectDemands extends LongMapWrapperImpl<Demands> {

  /**
   * Constructor
   */
  protected ProjectDemands() {
    super(new TreeMap<Long, Demands>(), Demands::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectDemands(ProjectDemands other) {
    super(other);
  }

  /**
   * Collect the first demands that are registered (if any). Otherwise return null
   * 
   * @return first demands that are registered if none return null
   */
  public Demands getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectDemands clone() {
    return new ProjectDemands(this);
  }

}
