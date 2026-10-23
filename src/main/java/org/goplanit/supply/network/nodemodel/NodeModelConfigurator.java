package org.goplanit.supply.network.nodemodel;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all node model configurator implementations
 * 
 * @author markr
 *
 * @param <T> node model type
 */
public class NodeModelConfigurator<T extends NodeModelComponent> extends Configurator<T> {

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
  public void configure(NodeModelComponent nodeModel) {
    super.configure((T) nodeModel);
  }

}
