package org.goplanit.converter.zoning.connectoid;

/**
 * Where an OD connectoid created for a zone joins the network: at a node the network has, or at a new node at the point
 * of a link nearest the zone, the link broken there
 * <p>
 * Existing nodes leave the network as it is, which suits a network that is written out or compared later. New nodes
 * put the connectoid where the road passes nearest the zone, which suits a network whose nodes lie far apart.
 * </p>
 *
 * @author markr
 */
public enum OdConnectoidAttachment {

  /** at a node the network has */
  EXISTING_NODE,

  /** at the point of a link nearest the zone, the link broken there at a new node */
  NEW_NODE_ON_LINK;
}
