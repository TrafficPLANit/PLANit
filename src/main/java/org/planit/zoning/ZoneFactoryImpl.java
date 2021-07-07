package org.planit.zoning;

import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntityFactoryImpl;
import org.planit.utils.zoning.Zone;

/**
 * Factory for creating od zones (on container)
 * 
 * @author markr
 */
public abstract class ZoneFactoryImpl<Z extends Zone> extends ManagedIdEntityFactoryImpl<Z> {

  /** container to use */
  protected final CentroidFactoryImpl centroidFactory;

  /** the centroid factory to use
   * 
   * @return centroid factory
   */
  protected CentroidFactoryImpl getCentroidFactory() {
    return centroidFactory;
  }
  
  /**
   * Constructor
   * 
   * @param groupId             to use
   */
  protected ZoneFactoryImpl(final IdGroupingToken groupId) {
    super(groupId);
    this.centroidFactory = new CentroidFactoryImpl(groupId);
  }


}
