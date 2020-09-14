package org.planit.interactor;

/**
 * Link Volume Accessor interface
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessor extends InteractorAccessor {


  /**
   * {@inheritDoc}
   */
  @Override
  default Class<? extends InteractorAccessee> getCompatibleAccessee() {
    return LinkVolumeAccessee.class;
  }

  /**
   * provide the accessee instance for this accessort to use
   * 
   * @param accessee the instance being accessed
   */
  void setLinkVolumeAccessee(LinkVolumeAccessee accessee);

}
