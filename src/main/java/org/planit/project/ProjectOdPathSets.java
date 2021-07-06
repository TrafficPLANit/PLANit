package org.planit.project;

import java.util.TreeMap;

import org.planit.path.OdPathSets;
import org.planit.utils.wrapper.LongMapWrapperImpl;

/**
 * class for registered od path sets
 *
 */
public class ProjectOdPathSets extends LongMapWrapperImpl<OdPathSets> {

  /**
   * Constructor
   */
  protected ProjectOdPathSets() {
    super(new TreeMap<Long, OdPathSets>(), OdPathSets::getId);
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
   * Collect the first od path set that is registered (if any). Otherwise return null
   * 
   * @return first od path set that is registered if none return null
   */
  public OdPathSets getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectOdPathSets clone() {
    return new ProjectOdPathSets(this);
  }

}
