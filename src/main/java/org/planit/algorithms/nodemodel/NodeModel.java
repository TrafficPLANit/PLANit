package org.planit.algorithms.nodemodel;

import org.planit.supply.network.nodemodel.TampereNodeModelComponent;

/**
 * Node model algorithm base interface
 * 
 * @author markr
 *
 */
public interface NodeModel {

  /**
   * Short hand for Tampere node model class type
   */
  public static final String TAMPERE = TampereNodeModelComponent.class.getCanonicalName();

}
