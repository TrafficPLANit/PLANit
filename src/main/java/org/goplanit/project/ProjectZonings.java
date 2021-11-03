package org.goplanit.project;

import java.util.TreeMap;

import org.goplanit.zoning.Zoning;
import org.goplanit.utils.wrapper.LongMapWrapperImpl;

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
   * {@inheritDoc}
   */
  @Override
  public ProjectZonings clone() {
    return new ProjectZonings(this);
  }

}
