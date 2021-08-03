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
   * {@inheritDoc}
   */
  @Override
  public ProjectDemands clone() {
    return new ProjectDemands(this);
  }

}
