package org.goplanit.supply.network.nodemodel;

import org.goplanit.algorithms.nodemodel.NodeModel;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.exceptions.PlanItRunTimeException;

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
   */
  public static NodeModelConfigurator<? extends NodeModelComponent> createConfigurator(final String nodeModelType){

    if (nodeModelType.equals(NodeModel.TAMPERE)) {
      return new TampereNodeModelConfigurator();
    } else {
      throw new PlanItRunTimeException(String.format(
          "unable to construct configurator for given nodeModelType %s", nodeModelType));
    }
  }
}
