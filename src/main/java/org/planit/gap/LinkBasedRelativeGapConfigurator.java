package org.planit.gap;

/**
 * Link based relative duality gap function configurator
 * 
 * @author markr
 *
 * @param <T>
 */
public class LinkBasedRelativeGapConfigurator extends GapFunctionConfigurator<LinkBasedRelativeDualityGapFunction> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public LinkBasedRelativeGapConfigurator() {
    super(LinkBasedRelativeDualityGapFunction.class);
  }

}
