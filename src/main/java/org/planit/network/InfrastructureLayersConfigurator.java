package org.planit.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;
import org.planit.utils.mode.PredefinedMode;

/**
 * Configurator class that allows one to create and modify a setup for the mapping of modes and 
 * infrastucture layers without actually creating them yet. The final result can be used to
 * instantiate the infrastructure layers on the actual infrastructure network when the time is right.
 * 
 * We also provide some default suggestions for quick setups avoiding complicated manual configurations.
 * 
 * @author markr
 *
 */
public class InfrastructureLayersConfigurator {
  
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(InfrastructureLayersConfigurator.class.getCanonicalName());
  
  /** track unique proposed layers */
  protected final Set<String> infrastructureLayersByXmlId = new HashSet<String>();
  
  /** track mode to layer mapping */
  protected final Map<Mode,String> modeToLayerXmlId = new TreeMap<Mode,String>();
  
  /**
   * proposed on_street layer id
   */
  public static final String ON_STREET_LAYER_XML_ID = "on-street";

  /**
   * proposed active layer id
   */  
  public static final String ACTIVE_LAYER_XML_ID = "active";
  
  /**
   * proposed rail layer id
   */  
  public static final String RAIL_LAYER_ID = "rail";
  
  /**
   * proposed all-in-one layer id
   */  
  public static final String ALL_IN_ONE_LAYER_ID = "all";
  
  /** create a configuration that maps all passed in modes to a single proposed all-in-one layer
   * @param modes
   * @return configuration reflective of all-in-one approach
   */
  public static InfrastructureLayersConfigurator createAllInOneConfiguration(Modes modes) {
    InfrastructureLayersConfigurator allInOne = new InfrastructureLayersConfigurator();
    
    allInOne.addLayer(ALL_IN_ONE_LAYER_ID);
    modes.forEach( mode -> allInOne.setModeToLayer(mode, ALL_IN_ONE_LAYER_ID));
        
    return allInOne;
  }
  
  /** create a multi-layer configuration that maps all passed in modes to either a rail, active, or on-street layer.
   * Note that only predefined PLANit modes are automatically mapped. Custom modes must be manually mapped afterwards
   * to the appropriate layer.
   * 
   * The mapping works as follows:
   * on-street layer:
   * <ul>
   * <li>bus</li>
   * <li>car HOV</li>
   * <li>car</li>
   * <li>car share</li>
   * <li>goods</li>
   * <li>heavy goods</li>
   * <li>large heavy goods</li>
   * <li>light rail</li>
   * <li>motor bike</li>
   * <li>tram </li>
   * </ul>
   * <p>
   * active layer:
   * <ul>
   * <li>bicycle</li>
   * <li>pedestrian</li>
   * </ul>
   * </p>
   * <p>
   * rail layer:
   * <ul>
   * <li>subway</li>
   * <li>train</li>
   * </ul>
   * </p>
   * 
   * Note that by default tram and lightrail are marked as on on-street because we cannot assume they are always separate from road modes. We only
   * idetnify modes on separate layers when these layers are truly separate pieces of infrastructure. 
   * 
   * 
   * @param modes
   * @return configuration reflective of all-in-one approach
   */
  public static InfrastructureLayersConfigurator createMultiLayerConfiguration(Collection<PredefinedMode> predefinedModes) {
    InfrastructureLayersConfigurator multiLayerConfiguration = new InfrastructureLayersConfigurator();
    
    /* layers */
    multiLayerConfiguration.addLayer(ON_STREET_LAYER_XML_ID);
    multiLayerConfiguration.addLayer(RAIL_LAYER_ID);
    multiLayerConfiguration.addLayer(ACTIVE_LAYER_XML_ID);
    
    /* mode mapping */
    for(PredefinedMode mode : predefinedModes) {
      switch (mode.getPredefinedModeType()) {
        /* active */
        case BICYCLE:
          multiLayerConfiguration.setModeToLayer(mode, ACTIVE_LAYER_XML_ID);
          break;
        case PEDESTRIAN:
          multiLayerConfiguration.setModeToLayer(mode, ACTIVE_LAYER_XML_ID);
          break;
        /* on-street */
        case BUS:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case CAR:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case CAR_HIGH_OCCUPANCY:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case CAR_SHARE:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case GOODS_VEHICLE:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case HEAVY_GOODS_VEHICLE:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case LARGE_HEAVY_GOODS_VEHICLE:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case LIGHTRAIL:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case MOTOR_BIKE:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;
        case TRAM:
          multiLayerConfiguration.setModeToLayer(mode, ON_STREET_LAYER_XML_ID);
          break;          
        /* rail */
        case SUBWAY:
          multiLayerConfiguration.setModeToLayer(mode, RAIL_LAYER_ID);
          break;
        case TRAIN:
          multiLayerConfiguration.setModeToLayer(mode, RAIL_LAYER_ID);
          break;            
        case CUSTOM:
          LOGGER.warning("custom predefined mode type indicates the mode is in fact not predefined, ignored");
        default: 
          LOGGER.warning(String.format("invalid predefined mode type %s encountered, ignored", mode.getPredefinedModeType().value()));
      }      
    }

    
    

        
    return multiLayerConfiguration;
  }  
  
  
  /** add a proposed layer
   * 
   * @param layerXmlId to add
   * @return true if not already present, false otherwise
   */
  public boolean addLayer(String layerXmlId) {
    return infrastructureLayersByXmlId.add(layerXmlId);
  }
  
  /** remove a proposed layer, this means that all modes mapped to this layer are no longer mapped at all
   * 
   * @param layerXmlId to remove
   * @return true if removed, false otherwise
   */
  public boolean removeLayer(String layerXmlId) throws PlanItException {
    if(layerXmlId == null) {
      return false;
    }
    
    /* remove modes mapped to layer */
    Iterator<Mode> modeIter = modeToLayerXmlId.keySet().iterator();
    while(modeIter.hasNext()) {
      if(layerXmlId.equals(modeToLayerXmlId.get(modeIter.next()))){
        modeIter.remove();
      }
    }    
    return infrastructureLayersByXmlId.remove(layerXmlId);
  }  
  

  /** set mode to a layer
   * @param mode to add
   * @param layerXmlId to use
   * @return previous layer the mode was added to (if any)
   */
  public String setModeToLayer(Mode mode, String layerXmlId) {
    if(!infrastructureLayersByXmlId.contains(layerXmlId)) {
      LOGGER.warning(String.format("layer %s not registered, can only register a mode for a layer that has been registered",layerXmlId));
    }
    return modeToLayerXmlId.put(mode, layerXmlId);
  }
  
  /** verify if mode is assigned to the layer
   * @param mode to verify
   * @param layerXmlId to check
   * @return true when mapped, false otherwise
   */
  public boolean isModeMappedToLayer(Mode mode, String layerXmlId) {
    return modeToLayerXmlId.get(mode).equals(layerXmlId);
  }  
  
  /** remove mode from any layer
   * @param mode to remove
   * @return layer the mode was mapped to, null otherwise
   */
  public String removeMode(Mode mode) {
    return modeToLayerXmlId.remove(mode);
  }   
}
