package org.goplanit.zoning;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedId;
import org.goplanit.utils.id.ManagedIdEntityFactoryImpl;
import org.goplanit.utils.zoning.Centroid;
import org.goplanit.utils.zoning.CentroidFactory;
import org.goplanit.utils.zoning.Zone;

/**
 * Factory for creating centroids
 * 
 * @author markr
 */
public class CentroidFactoryImpl extends ManagedIdEntityFactoryImpl<Centroid> implements CentroidFactory {

  /**
   * Constructor
   * 
   * @param groupId             to use
   */
  protected CentroidFactoryImpl(final IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CentroidImpl create(){
    return new CentroidImpl(getIdGroupingToken());
  }
  
  /**
   * {@inheritDoc}
   */  
  @Override
  public CentroidImpl create(final Zone parentZone) {
    return new CentroidImpl(getIdGroupingToken(), parentZone);
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public Centroid createUniqueCopyOf(final ManagedId entityToCopy) {
    Centroid copy = (Centroid) entityToCopy.clone();
    copy.recreateManagedIds(getIdGroupingToken());
    return copy;
  }

}
