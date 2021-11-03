package org.goplanit.interactor;

import org.goplanit.utils.network.layer.physical.LinkSegment;

/**
 * Link Volume accessee object.
 * 
 * @author markr
 *
 */
public interface LinkVolumeAccessee extends TrafficAssignmentComponentAccessee {

  /**
   * {@inheritDoc}
   */
  @Override
  default Class<LinkVolumeAccessor> getCompatibleAccessor() {
    return LinkVolumeAccessor.class;
  }

  /**
   * Get the total flow across a link over all modes
   * 
   * @param linkSegment the specified link segment
   * @return the total flow across this link segment
   */
  public double getLinkSegmentVolume(LinkSegment linkSegment);

  /**
   * Get total link segment flows for all link segments
   * 
   * @return link segment flows for all modes
   */
  public double[] getLinkSegmentVolumes();

}
