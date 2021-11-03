package org.goplanit.cost.virtual;

/**
 * Configurator for FixedConnectoidTravelTimeCost implementation
 * 
 * @author markr
 */
public class FixedVirtualCostConfigurator extends VirtualCostConfigurator<FixedConnectoidTravelTimeCost> {
  
  private static final String SET_FIXED_CONNECTOID_COST = "setFixedConnectoidCost";
  
  /**
   * Constructor
   * 
   */
  protected FixedVirtualCostConfigurator() {
    super(FixedConnectoidTravelTimeCost.class);
  }
  
  /**
   * set the fixed cost used for all relevant link segments
   * 
   * @param fixedConnectoidCost the fixed cost to use
   */  
  public void setFixedConnectoidCost(final double fixedConnectoidCost) {
    registerDelayedMethodCall(SET_FIXED_CONNECTOID_COST, fixedConnectoidCost);
  }
}
