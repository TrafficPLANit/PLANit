package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Configurator for Newell Fundamental diagram implementation. The capacity and maximum densities used for each FD can
 * be overwritten, but not the free speed, as the free speed is a given physical value. A change on the link segment
 * level takes precedence over a change on the link segment type: the most specific overwrite is used in the final
 * fundamental diagram applied on the link segment.
 * <p>
 * Without a capacity, the capacity is computed as the point of intersection of the free flow branch and congested
 * branch, which for Newell is defined only by the free flow speed, maximum density and intersection points with the
 * x-axis (density=0). When the capacity is set explicitly or the maximum density point moved, the FD adjusts its
 * backward wave speed to accommodate any change compared to the default computed capacity/maximum density.
 * 
 * @author markr
 */
public class NewellFundamentalDiagramConfigurator extends
        FundamentalDiagramConfigurator<NewellFundamentalDiagramComponent> {

  /**
   * Constructor
   * 
   */
  protected NewellFundamentalDiagramConfigurator() {
    super(NewellFundamentalDiagramComponent.class);
  }

}