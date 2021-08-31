package org.planit.interactor;

/**
 * Link Volume Accessor interface
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessor extends InteractorAccessor<LinkVolumeAccessee> {

  /**
   * {@inheritDoc}
   */
  @Override
  default Class<LinkVolumeAccessee> getCompatibleAccessee() {
    return LinkVolumeAccessee.class;
  }

}
