package org.goplanit.project;

import java.util.TreeMap;

import org.goplanit.path.OdPathSets;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered od path sets
 *
 */
public class ProjectOdPathSets extends LongMapWrapperImpl<OdPathSets> {

  /**
   * Constructor
   */
  protected ProjectOdPathSets() {
    super(new TreeMap<>(), OdPathSets::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectOdPathSets(ProjectOdPathSets other) {
    super(other);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectOdPathSets shallowClone() {
    return new ProjectOdPathSets(this);
  }

}
