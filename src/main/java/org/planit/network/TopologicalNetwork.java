package org.planit.network;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.map.HashedMap;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.geo.PlanitJtsUtils;
import org.planit.utils.id.IdGroupingToken;

/**
 * A network with topological infrastructure layers, meaning that apart from representig a physicla reality the result is topologically meaningful, has nodes, links, and some
 * geographic notion via a coordinate reference system
 * 
 * @author markr
 *
 */
public abstract class TopologicalNetwork<T extends TopologicalLayer, U extends TopologicalLayers<T>> extends InfrastructureNetwork<T,U> {

  /** generated serial id */
  private static final long serialVersionUID = 2402806336978560448L;

  /** the logger to use */
  private static final Logger LOGGER = Logger.getLogger(TopologicalNetwork.class.getCanonicalName());

  // Protected

  // Public

  /** the coordinate reference system used for all layers in this network */
  private CoordinateReferenceSystem coordinateReferenceSystem;
  
  /**
   * Default constructor
   * 
   * @param tokenId to use for id generation
   */
  public TopologicalNetwork(IdGroupingToken tokenId) {
    this(tokenId, PlanitJtsUtils.DEFAULT_GEOGRAPHIC_CRS);
  }

  /**
   * Default constructor
   * 
   * @param tokenId                   to use for id generation
   * @param coordinateReferenceSystem preferred coordinate reference system to use
   */
  public TopologicalNetwork(IdGroupingToken tokenId, CoordinateReferenceSystem coordinateReferenceSystem) {
    super(tokenId);

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
   * @param coordinateReferenceSystem to set
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
   * @throws PlanItException thrown if error
   */
  public void transform(final CoordinateReferenceSystem newCoordinateReferenceSystem) throws PlanItException {
    for (TopologicalLayer layer : infrastructureLayers) {
      layer.transform(coordinateReferenceSystem, newCoordinateReferenceSystem);
    }
  }

  /**
   * Tries to intialise and create/register infrastructure layers via a predefined configuration rather than letting the user do this manually via the infrastructure layers
   * container. Only possible when the network is still empty and no layers are yet active
   * 
   * @param planitInfrastructureLayerConfiguration to use for configuration
   */
  public void initialiseTopologicalLayers(InfrastructureLayersConfigurator planitInfrastructureLayerConfiguration) {
    if (!infrastructureLayers.isNoLayers()) {
      LOGGER.warning("unable to initialise topological layers based on provided configuration, since network already has layers defined");
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
  public void removeDanglingSubnetworks(Integer belowSize, Integer aboveSize, boolean alwaysKeepLargest) throws PlanItException {
    for (TopologicalLayer infrastructureLayer : this.infrastructureLayers) {
      infrastructureLayer.removeDanglingSubnetworks(belowSize, aboveSize, alwaysKeepLargest);
    }
  }

}