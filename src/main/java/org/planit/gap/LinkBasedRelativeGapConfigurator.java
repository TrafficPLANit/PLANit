package org.planit.gap;

/**
 * Link based relative duality gap function configurator
 * 
 * @author markr
 *
 */
public class LinkBasedRelativeGapConfigurator extends GapFunctionConfigurator<LinkBasedRelativeDualityGapFunction> {

  /**
   * Constructor
   * 
   */
  public LinkBasedRelativeGapConfigurator() {
    super(LinkBasedRelativeDualityGapFunction.class);
  }

}
