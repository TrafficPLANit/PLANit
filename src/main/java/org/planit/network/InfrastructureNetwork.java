package org.planit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.geo.PlanitJtsUtils;
import org.planit.mode.ModesImpl;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.mode.Mode;
import org.planit.utils.mode.Modes;

/**
 * A network with physical infrastructure layers
 * 
 * @author markr
 *
 */
public class InfrastructureNetwork extends Network {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(InfrastructureNetwork.class.getCanonicalName());

  // Protected

  // Public

  /**
   * class instance containing all modes specific functionality across the layers
   */
  public final Modes modes;

  /** stores the various layers grouped by their supported modes of transport */
  public final InfrastructureLayers infrastructureLayers;

  /** the coordinate reference system used for all layers in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;

  /**
   * Default constructor
   * 
   * @param tokenId
   */
  public InfrastructureNetwork(IdGroupingToken tokenId) {
    this(tokenId, PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public InfrastructureNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId);
    /* for mode management */
    this.modes = new ModesImpl(tokenId);
    /* for layer management */
    this.infrastructureLayers = new InfraStructureLayersImpl(getNetworkGroupingTokenId());
    /* default crs */
    this.coordinateReferenceSystem = coordinateReferenceSystem;
  }

  /**
   * collect the used crs
   * 
   * @return coordinateReferencesystem used by this infrastructure network
   */
  public CoordinateReferenceSystem getCoordinateReferenceSystem() {
    return this.coordinateReferenceSystem;
  }

  /**
   * set the coordinate reference system used for all layers
   * 
   * @return
   */
  public void setCoordinateReferenceSystem(final CoordinateReferenceSystem coordinateReferenceSystem) {
    if (infrastructureLayers.isEachLayerEmpty()) {
      this.coordinateReferenceSystem = coordinateReferenceSystem;
    } else {
      LOGGER.warning("Coordinate Reference System is already set. To change the CRS after instantiation, use transform() method");
    }
  }

  /**
   * change the coordinate system, which will result in an update of all geometries in the network layers from the original CRS to the new CRS. If the network is empty and no CRS
   * is set then this is identical to calling setCoordinateReferenceSystem, otherwise it will change the CRS while the set method will throw an exception
   * 
   * @param newCoordinateReferenceSystem to transform the network to
   * @throws PlanItException
   * @throws FactoryException
   */
  public void transform(final CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    for (InfrastructureLayer layer : infrastructureLayers) {
      layer.transform(coordinateReferenceSystem, newCoordinateReferenceSystem);
    }
  }

  /**
   * collect an infrastructure layer by mode (identical to this.infrastructureLayers.get(mode))
   * 
   * @param mode to collect layer for
   * @return corresponding layer, null if not found)
   */
  public InfrastructureLayer getInfrastructureLayerByMode(Mode mode) {
    return infrastructureLayers.get(mode);
  }

  /**
   * Tries to intialise and create/register infrastructure layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers
   * container. Only possible when the network is still empty and no layers are yet active
   * 
   * @param planitInfrastructureLayerConfiguration to use for configuration
   */
  public void initialiseInfrastructureLayers(InfrastructureLayersConfigurator planitInfrastructureLayerConfiguration) {
    if (!infrastructureLayers.isNoLayers()) {
      LOGGER.warning("unable to initialise infrastructure layers based on provided configuration, since network already has layers defined");
      return;
    }

    /* register layers */
    Map<String, Long> xmlIdToId = new HashedMap<String, Long>();
    for (String layerXmlId : planitInfrastructureLayerConfiguration.infrastructureLayersByXmlId) {
      InfrastructureLayer newLayer = infrastructureLayers.createNew();
      newLayer.setXmlId(layerXmlId);
      xmlIdToId.put(layerXmlId, newLayer.getId());
    }

    /* register modes */
    planitInfrastructureLayerConfiguration.modeToLayerXmlId.forEach((mode, layerXmlId) -> infrastructureLayers.get(xmlIdToId.get(layerXmlId)).registerSupportedMode(mode));

  }

  /**
   * remove any dangling subnetworks from the network's layers if they exist and subsequently reorder the internal ids if needed
   * 
   * @throws PlanItException thrown if error
   * 
   */
  public void removeDanglingSubnetworks() throws PlanItException {
    removeDanglingSubnetworks(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * remove any dangling subnetworks below a given size from the network if they exist and subsequently reorder the internal ids if needed
   * 
   * @param belowSize         remove subnetworks below the given size
   * @param aboveSize         remove subnetworks above the given size (typically set to maximum value)
   * @param alwaysKeepLargest when true the largest of the subnetworks is always kept, otherwise not
   * @throws PlanItException thrown if error
   */
  public void removeDanglingSubnetworks(Integer belowsize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    for (InfrastructureLayer infrastructureLayer : this.infrastructureLayers) {
      infrastructureLayer.removeDanglingSubnetworks(belowsize, aboveSize, alwaysKeepLargest);
    }
  }

}
