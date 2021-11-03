package org.goplanit.supply.fundamentaldiagram;

import org.goplanit.utils.builder.Configurator;
import org.goplanit.utils.exceptions.PlanItException;

/**
 * Base class for all fundamental diagram configurator implementations
 * 
 * @author markr
 *
 * @param <T> fundamental diagram type
 */
public class FundamentalDiagramConfigurator<T extends FundamentalDiagramComponent> extends Configurator<T> {

  /**
   * Constructor
   * 
   * @param instanceType to configure on
   */
  public FundamentalDiagramConfigurator(Class<T> instanceType) {
    super(instanceType);
  }

  /**
   * Needed to avoid issues with generics, although it should be obvious that T extends FundamentalDiagram
   * 
   * @param fundamentalDiagram the instance to configure
   */
  @SuppressWarnings("unchecked")
  @Override
  public void configure(FundamentalDiagramComponent fundamentalDiagram) throws PlanItException {
    super.configure((T) fundamentalDiagram);
  }

}
