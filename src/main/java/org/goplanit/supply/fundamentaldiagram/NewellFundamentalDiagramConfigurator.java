package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegment;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

/**
 * Configurator for Newell Fundamental diagram implementation. We allow one to overwrite the capacity and maximum
 * densities used for each FD but not the free speed, since the free speed is a physical value that is a given. Note
 * that a change on the link segment level takes precedence over a change on the link segment type, i.e., the most specific
 * overwrite is used in the final fundamental diagram applied on the link segment.
 * <p>
 * In absence of a capacity the capacity is computed by the point of intersection of the free flow branch and
 * congested branch, which for Newell is defined by only the free flow speed, maximum density and intersection points
 * with the x-axis (density=0), by explicitly setting the capacity or moving the maximum density point the FD will adjust its
 * backward wave speed to accommodate any change compared to the default computed capacity/maximum density.
 * 
 * @author markr
 */
public class NewellFundamentalDiagramConfigurator extends FundamentalDiagramConfigurator<NewellFundamentalDiagramComponent> {

  /**
   * Constructor
   * 
   */
  protected NewellFundamentalDiagramConfigurator() {
    super(NewellFundamentalDiagramComponent.class);
  }

}