package org.planit.supply.network.nodemodel;

import org.planit.utils.exceptions.PlanItException;

/**
 * factory for the node model types supported directory by PLANit
 * 
 * @author markr
 *
 */
public class NodeModelConfiguratorFactory {

  /**
   * Create a configurator for given node model type
   * 
   * @param nodeModelType type of node model the configurator is created for
   * @return the created configurator
   * @throws PlanItException thrown if error
   */
  public static NodeModelConfigurator<? extends NodeModel> createConfigurator(final String nodeModelType) throws PlanItException {

    if (nodeModelType.equals(NodeModel.TAMPERE)) {
      return new TampereNodeModelConfigurator();
    }else {
      throw new PlanItException(String.format("unable to construct configurator for given nodeModelType %s", nodeModelType));
    }
  }
}
