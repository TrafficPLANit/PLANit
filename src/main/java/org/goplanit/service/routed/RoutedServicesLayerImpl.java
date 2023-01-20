package org.goplanit.service.routed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.service.routed.RoutedModeServices;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.RoutedServicesLayerModifier;

/**
 * Implementation of the RoutedServicesLayer interface
 * 
 * @author markr
 */
public class RoutedServicesLayerImpl extends ExternalIdAbleImpl implements RoutedServicesLayer {

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(RoutedServicesLayerImpl.class.getCanonicalName());

  /** token to use for id generation */
  private final IdGroupingToken tokenId;

  /** parent layer all routed services are built upon */
  private final ServiceNetworkLayer parentLayer;

  /** Modifier utilities for this layer consolidated in a single class */
  private final RoutedServicesLayerModifierImpl layerModifier;

  /** container for routed services categorised by mode */
  private final Map<Mode, RoutedModeServices> routedServicesByMode;

  /**
   * Local factory method to create an instance of mode specific routed services container
   * 
   * @param tokenId to use
   * @param mode    for which services can be registered
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
    this.layerModifier = new RoutedServicesLayerModifierImpl(this);
    this.routedServicesByMode = new HashMap<>();
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
    this.layerModifier = new RoutedServicesLayerModifierImpl(routedServicesLayerImpl);
    this.routedServicesByMode = new HashMap<>();
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
    if(!parentLayer.supports(mode)){
      LOGGER.warning(String.format("Unable to collect services for mode %s since it is not supported on the parent layer", mode.toString()));
    }
    if (!routedServicesByMode.containsKey(mode)) {
      routedServicesByMode.put(mode, createRoutedModeServices(this.tokenId, mode));
    }
    return routedServicesByMode.get(mode);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerModifier getLayerModifier() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<RoutedModeServices> iterator() {
    return routedServicesByMode.values().iterator();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void logInfo(String prefix) {
    LOGGER.info(String.format("%s [layer: %s]", prefix, getXmlId()));
    for (RoutedModeServices modeServices : this) {
      int numScheduleBasedTrips = 0;
      int numFrequencyBasedTrips = 0;
      LOGGER.info(String.format("%s [layer: %s] [mode: %s] #routedServices: %d", prefix, getXmlId(), modeServices.getMode().getXmlId(), modeServices.size()));
      for(var entry : modeServices){
        numScheduleBasedTrips += entry.getTripInfo().getScheduleBasedTrips().size();
        numFrequencyBasedTrips += entry.getTripInfo().getFrequencyBasedTrips().size();
      }
      LOGGER.info(String.format("%s [layer: %s] [mode: %s] #schedule-trips: %d", prefix, getXmlId(), modeServices.getMode().getXmlId(), numScheduleBasedTrips));
      LOGGER.info(String.format("%s [layer: %s] [mode: %s] #frequency-trips: %d", prefix, getXmlId(), modeServices.getMode().getXmlId(), numFrequencyBasedTrips));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return routedServicesByMode.isEmpty();
  }

}
