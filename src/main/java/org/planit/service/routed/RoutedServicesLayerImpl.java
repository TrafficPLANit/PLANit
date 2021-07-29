package org.planit.service.routed;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.network.layer.ServiceNetworkLayer;

/**
 * Implementation of the RoutedServicesLayer interface
 * 
 * @author markr
 */
public class RoutedServicesLayerImpl extends ExternalIdAbleImpl implements RoutedServicesLayer {

  /** token to use for id generation */
  private final IdGroupingToken tokenId;

  /** parent layer all routed services are built upon */
  private final ServiceNetworkLayer parentLayer;

  /** container for routed services categorised by mode */
  private final Map<Mode, RoutedModeServices> routedServicesByMode;

  /**
   * Local factory method to create an instance of mode specific routed services container
   * 
   * @param mode for which services can be registered
   * @return created instance
   */
  private static RoutedModeServicesImpl createRoutedModeServices(final IdGroupingToken tokenId, final Mode mode) {
    return new RoutedModeServicesImpl(tokenId, mode);
  }

  /**
   * Generate id for instances of this class based on the token and class identifier
   * 
   * @param tokenId to use
   * @return generated id
   */
  protected static long generateId(IdGroupingToken tokenId) {
    return IdGenerator.generateId(tokenId, RoutedServicesLayer.ROUTED_SERVICES_LAYER_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param tokenId     to use for id generation
   * @param parentLayer the parent layer these routed services are built upon
   */
  public RoutedServicesLayerImpl(final IdGroupingToken tokenId, final ServiceNetworkLayer parentLayer) {
    super(generateId(tokenId));
    this.tokenId = tokenId;
    this.parentLayer = parentLayer;
    this.routedServicesByMode = new HashMap<Mode, RoutedModeServices>();
  }

  /**
   * Copy constructor
   * 
   * @param routedServicesLayerImpl to copy
   */
  public RoutedServicesLayerImpl(RoutedServicesLayerImpl routedServicesLayerImpl) {
    super(routedServicesLayerImpl);
    this.tokenId = routedServicesLayerImpl.tokenId;
    this.parentLayer = routedServicesLayerImpl.parentLayer;
    this.routedServicesByMode = new HashMap<Mode, RoutedModeServices>();
    routedServicesLayerImpl.routedServicesByMode.values().forEach(modeServices -> routedServicesByMode.put(modeServices.getMode(), modeServices));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long recreateManagedIds(IdGroupingToken tokenId) {
    long newId = generateId(tokenId);
    setId(newId);
    return newId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerImpl clone() {
    return new RoutedServicesLayerImpl(this);
  }

  /**
   * The parent layer of this routed services layer
   * 
   * @return parent layer
   */
  public final ServiceNetworkLayer getParentLayer() {
    return parentLayer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedModeServices getServicesByMode(Mode mode) {
    if (!routedServicesByMode.containsKey(mode)) {
      routedServicesByMode.put(mode, createRoutedModeServices(this.tokenId, mode));
    }
    return routedServicesByMode.get(mode);
  }

}
