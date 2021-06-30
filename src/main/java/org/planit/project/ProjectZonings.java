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
   * Collect the first zonings that are registered (if any). Otherwise return null
   * 
   * @return first zonings that are registered if none return null
   */
  public Zoning getFirst() {
    return isEmpty() ? iterator().next() : null;
  }

}
