package org.planit.project;

import java.util.TreeMap;

import org.planit.utils.wrapper.LongMapWrapperImpl;
import org.planit.zoning.Zoning;

/**
 * Class for registered zonings on project
 *
 */
public class ProjectZonings extends LongMapWrapperImpl<Zoning> {

  /**
   * Constructor
   */
  protected ProjectZonings() {
    super(new TreeMap<Long, Zoning>(), Zoning::getId);
  }

  /**
   * Copy constructor
   * 
   * @param other to copy
   */
  protected ProjectZonings(ProjectZonings other) {
    super(other);
  }

  /**
   * Collect the first zonings that are registered (if any). Otherwise return null
   * 
   * @return first zonings that are registered if none return null
   */
  public Zoning getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProjectZonings clone() {
    return new ProjectZonings(this);
  }

}
