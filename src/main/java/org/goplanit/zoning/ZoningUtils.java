package org.goplanit.zoning;

import org.goplanit.utils.misc.Pair;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLink;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;
import org.goplanit.utils.network.layer.physical.Node;
import org.goplanit.utils.zoning.Connectoid;
import org.goplanit.utils.zoning.OdZone;

/**
 * Utilities for Zoning
 *
 * @author markr
 */
public class ZoningUtils {


  /**
   * To make sure a connectoid is not directly attached to a physical node, create a stub
   * based on provided parameters and then overlay new connectoid on upstream vertex
   * @param zoning to create connectoid one
   * @param zone connectoid is attached to
   * @param connectoidLengthKm length of connectoid
   * @param networkLayer layer to attach stub to
   * @param stubNode physical node to attach stub link to
   * @param stubLinkLengthKm stub link length
   * @param numberOfLanesPerDirection lanes for one or both directional link segments
   * @param linkSegmentType type to use for link segments
   * @param towardStub when true create segment towards stub node
   * @param towardConnectoid when true create segment away from stub node toward connectoid
   * @return created connectoid and stub link. NodeA is the node the connectoid is overlayed on
   */
  public static Pair<Connectoid, MacroscopicLink> createOdConnectoidOnNewPhysicalStub(
      Zoning zoning,
      OdZone zone,
      double connectoidLengthKm,
      MacroscopicNetworkLayer networkLayer,
      Node stubNode,
      double stubLinkLengthKm,
      int numberOfLanesPerDirection,
      MacroscopicLinkSegmentType linkSegmentType,
      boolean towardStub,
      boolean towardConnectoid){

    var upstreamNodeFromStub = networkLayer.getNodes().getFactory().registerNew();
    upstreamNodeFromStub.setXmlId("before0");

    var stubLink = networkLayer.getLinks().getFactory().registerNew(
        upstreamNodeFromStub, stubNode, stubLinkLengthKm, true /* registerOnNodes */);

    if(towardStub) {
      var linkSegmentToStub = networkLayer.getLinkSegments().getFactory().registerNew(
          stubLink, towardStub, true);
      linkSegmentToStub.setNumberOfLanes(numberOfLanesPerDirection);
      linkSegmentToStub.setLinkSegmentType(linkSegmentType);
    }
    if(towardConnectoid) {
      var linkSegmentToConnectoid = networkLayer.getLinkSegments().getFactory().registerNew(
          stubLink, towardConnectoid, true);
      linkSegmentToConnectoid.setNumberOfLanes(numberOfLanesPerDirection);
      linkSegmentToConnectoid.setLinkSegmentType(linkSegmentType);
    }

    var odConnectoid = zoning.getOdConnectoids().getFactory().registerNew(
        upstreamNodeFromStub, zone, connectoidLengthKm);
    return Pair.of(odConnectoid, stubLink);
  }
}
