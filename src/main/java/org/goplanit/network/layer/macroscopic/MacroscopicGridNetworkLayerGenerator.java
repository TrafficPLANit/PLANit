package org.goplanit.network.layer.macroscopic;

import java.util.logging.Logger;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.goplanit.network.layer.NetworkLayerGenerator;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layers.MacroscopicNetworkLayers;
import org.goplanit.utils.unit.Unit;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Generate a grid based network layer for specified modes of a given size
 * 
 * @author markr
 *
 */
public class MacroscopicGridNetworkLayerGenerator implements NetworkLayerGenerator {

  /** Logger to use */
  private static final Logger LOGGER = Logger.getLogger(MacroscopicGridNetworkLayerGenerator.class.getCanonicalName());

  /**
   * num rows in grid
   */
  private final int rows;

  /**
   * num cols in grid
   */
  private final int columns;

  /**
   * container to register layer on
   */
  private final MacroscopicNetworkLayers layersContainer;

  /** supported modes */
  private final Mode[] modes;

  /**
   * Create the nodes of the grid starting at (0,0) for row=0,col=0, then at (0,1000) for row=1, col=0, etc.
   * 
   * @param networkLayer to use
   * @param geoFactory   to use
   * @throws PlanItException thrown if error
   */
  private void createNodes(MacroscopicNetworkLayer networkLayer, final GeometryFactory geoFactory) throws PlanItException {
    for (int colIndex = 0; colIndex < columns; ++colIndex) {
      for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
        long yMeters = Math.round(Unit.KM.convertTo(Unit.METER, rowIndex));
        long xMeters = Math.round(Unit.KM.convertTo(Unit.METER, colIndex));
        var node = networkLayer.getNodes().getFactory().registerNew();
        node.setPosition(geoFactory.createPoint(new Coordinate(xMeters, yMeters)));
        node.setXmlId(String.valueOf(node.getId()));
      }
    }
  }

  /**
   * Create the vertical links of the grid first starting at (0,0)-to(0,1) for link 0 etc. and do this for all columns. Then create the horizontal links of the grid starting at
   * (0,0)-to(1,0) for link starting with id (rows-1)*cols
   * 
   * @param networkLayer to use
   * @param geoFactory   to use
   * @throws PlanItException thrown if error
   */
  private void createLinks(MacroscopicNetworkLayer networkLayer, final GeometryFactory geoFactory) throws PlanItException {
    /* vertical links */
    int offset = 0;
    for (int colIndex = 0; colIndex < columns; ++colIndex, offset += rows) {
      for (int rowIndex = 1; rowIndex < rows; ++rowIndex) {
        long nodeAId = offset + rowIndex - 1;
        long nodeBId = nodeAId + 1;
        var nodeA = networkLayer.getNodes().get(nodeAId);
        var nodeB = networkLayer.getNodes().get(nodeBId);
        var newLink = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true /* register on node */);

        if (newLink == null) {
          LOGGER.severe(String.format("Unable to create link for nodes with internal ids (A:%d, B:%d)", nodeAId, nodeBId));
          continue;
        }
        newLink.setXmlId(String.valueOf(newLink.getId()));
      }

    }

    /* horizontal links */
    offset = 0;
    for (int colIndex = 0; colIndex < columns - 1; ++colIndex, offset += rows) {
      for (int rowIndex = 0; rowIndex < rows; ++rowIndex) {
        long nodeAId = offset + rowIndex;
        long nodeBId = nodeAId + rows;
        var nodeA = networkLayer.getNodes().get(nodeAId);
        var nodeB = networkLayer.getNodes().get(nodeBId);
        var newLink = networkLayer.getLinks().getFactory().registerNew(nodeA, nodeB, 1, true /* register on node */);

        if (newLink == null) {
          LOGGER.severe(String.format("Unable to create link for nodes with internal ids (A:%d, B:%d)", nodeAId, nodeBId));
          continue;
        }
        newLink.setXmlId(String.valueOf(newLink.getId()));
      }

    }
  }

  /**
   * Create the link segments of the grid for all links in both directions
   * 
   * @param networkLayer to use
   * @param geoFactory   to use
   * @throws PlanItException thrown if error
   */
  private void createLinkSegments(MacroscopicNetworkLayer networkLayer, final GeometryFactory geoFactory) throws PlanItException {
    var defaultLinkSegmentType = networkLayer.getLinkSegmentTypes().getFirst();
    boolean registerOnNodes = true;
    for (var link : networkLayer.getLinks()) {
      networkLayer.getLinkSegments().getFactory().registerNew(link, defaultLinkSegmentType, true /* A->B */, registerOnNodes);
      var linkSegment = networkLayer.getLinkSegments().getFactory().registerNew(link, defaultLinkSegmentType, false /* B->A */, registerOnNodes);
      linkSegment.setXmlId(String.valueOf(linkSegment.getId()));
    }
  }

  /**
   * Populate the network layer by means of a grid
   * 
   * @param networkLayer
   * @throws PlanItException thrown if error
   */
  private void populateGrid(final MacroscopicNetworkLayer networkLayer) throws PlanItException {
    GeometryFactory geoFactory = JTSFactoryFinder.getGeometryFactory();
    createNodes(networkLayer, geoFactory);
    createLinks(networkLayer, geoFactory);
    createLinkSegments(networkLayer, geoFactory);
  }

  /**
   * Constructor
   * 
   * @param rows            to use
   * @param columns         to use
   * @param layersContainer to use
   * @param modes           to support
   */
  protected MacroscopicGridNetworkLayerGenerator(int rows, int columns, final MacroscopicNetworkLayers layersContainer, final Mode... modes) {
    this.rows = rows;
    this.columns = columns;
    this.layersContainer = layersContainer;
    this.modes = modes;
  }

  /**
   * Generate a macroscopic network layer based on the configured grid
   */
  @Override
  public MacroscopicNetworkLayer generate() {
    var networkLayer = layersContainer.getFactory().registerNew(modes);
    createDefaultLinkSegmentType(networkLayer);
    try {
      populateGrid(networkLayer);
    } catch (Exception e) {
      LOGGER.severe(e.getMessage());
      LOGGER.severe("Unable to populate grid for macroscopic network layer");
    }
    return networkLayer;
  }

  /**
   * Create the default link segment type on the given layer, supported modes are registered but nothing else
   * 
   * @param networkLayer to register on
   */
  private void createDefaultLinkSegmentType(MacroscopicNetworkLayer networkLayer) {
    var linkSegmentType = networkLayer.getLinkSegmentTypes().getFactory().registerNew("default");
    var accessGroupProperties = AccessGroupPropertiesFactory.create(modes);
    linkSegmentType.addAccessGroupProperties(accessGroupProperties);
  }

  /**
   * create the generator with a number of rows and columns. It is assumed that the grid has coordinates in Cartesian form in meters. A single link segment type is created with the
   * name "default" but without setting capacity, max density or access group information. This is left to the invoked of this method to further specify.
   * 
   * @param rows            to use
   * @param columns         to use
   * @param layersContainer to register on
   * @param modes           to support
   */
  public static MacroscopicGridNetworkLayerGenerator create(int rows, int columns, final MacroscopicNetworkLayers layersContainer, final Mode... modes) {
    return new MacroscopicGridNetworkLayerGenerator(rows, columns, layersContainer, modes);
  }
}
