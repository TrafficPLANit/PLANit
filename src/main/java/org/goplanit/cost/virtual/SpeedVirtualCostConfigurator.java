package org.goplanit.cost.virtual;

/**
 * Configurator for SpeedConnectoidTravelTimeCost implementation
 * 
 * @author markr
 */
public class SpeedVirtualCostConfigurator extends VirtualCostConfigurator<SpeedConnectoidTravelTimeCost> {
  
  private static final String SET_CONNECTOID_SPEED = "setConnectoidSpeed";
  
  /**
   * Constructor
   * 
   */
  protected SpeedVirtualCostConfigurator() {
    super(SpeedConnectoidTravelTimeCost.class);
  }
  
  /**
   * set the connectoid speed
   * 
   * @param connectoidSpeed the speed
   */
  public void setConnectoidSpeed(final double connectoidSpeed) {
   registerDelayedMethodCall(SET_CONNECTOID_SPEED, connectoidSpeed);
  }
}
