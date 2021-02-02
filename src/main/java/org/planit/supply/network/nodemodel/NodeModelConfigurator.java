package org.planit.supply.network.nodemodel;

import org.planit.utils.builder.Configurator;
import org.planit.utils.exceptions.PlanItException;

/**
 * Base class for all node model configurator implementations
 * 
 * @author markr
 *
 * @param <T> node model type
 */
public class NodeModelConfigurator<T extends NodeModel> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public NodeModelConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends NodeModel
   * 
   * @param nodeModel the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(NodeModel nodeModel) throws PlanItException {
    super.configure((T) nodeModel);
  }

}
