package org.goplanit.service.routed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.goplanit.service.routed.modifier.RoutedServicesLayerModifierImpl;
import org.goplanit.utils.id.ExternalIdAbleImpl;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdDeepCopyMapper;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.ServiceNetworkLayer;
import org.goplanit.utils.service.routed.RoutedModeServices;
import org.goplanit.utils.service.routed.RoutedService;
import org.goplanit.utils.service.routed.RoutedServicesLayer;
import org.goplanit.utils.service.routed.modifier.RoutedServicesLayerModifier;

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
   * Remove a given routed services by mode
   *
   * @param servicesByMode to remove
   * @return removed entry (if any)
   */
  protected RoutedModeServices removeServicesByMode(RoutedModeServices servicesByMode) {
    return routedServicesByMode.remove(servicesByMode);
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
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedServicesLayerImpl(RoutedServicesLayerImpl other, boolean deepCopy, ManagedIdDeepCopyMapper<RoutedService> routedServiceMapper) {
    super(other);
    this.tokenId = other.tokenId;
    this.parentLayer = other.parentLayer;
    this.layerModifier = new RoutedServicesLayerModifierImpl(other);

    // container wrappers so require clone always
    this.routedServicesByMode = new HashMap<>();
    other.routedServicesByMode.values().forEach(
            modeServices -> routedServicesByMode.put(
                    modeServices.getMode(), deepCopy ? modeServices.deepCloneWithMapping(routedServiceMapper) : modeServices.shallowClone()));
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
  public boolean isServicesByModeEmpty(Mode mode) {
    return !routedServicesByMode.containsKey(mode) || routedServicesByMode.get(mode).isEmpty();
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
    return this.layerModifier;
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
    for (RoutedModeServices modeServices : this) {
      if(modeServices.size()<=0){
        LOGGER.info(String.format("%s[mode: %s] no services present", prefix, modeServices.getMode().getXmlId()));
        continue;
      }

      int numScheduleBasedTrips = 0;
      int numFrequencyBasedTrips = 0;
      for(var entry : modeServices){
        numScheduleBasedTrips += entry.getTripInfo().getScheduleBasedTrips().size();
        numFrequencyBasedTrips += entry.getTripInfo().getFrequencyBasedTrips().size();
      }
      LOGGER.info(String.format("%s[mode: %s] #routedServices: %d  #trip-schedules: %d  #trips-frequency: %d",
          prefix, modeServices.getMode().getXmlId(), modeServices.size(), numScheduleBasedTrips, numFrequencyBasedTrips));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty() {
    return routedServicesByMode.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerImpl shallowClone() {
    return new RoutedServicesLayerImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedServicesLayerImpl deepClone() {
    return new RoutedServicesLayerImpl(this, true, new ManagedIdDeepCopyMapper<>());
  }

}
