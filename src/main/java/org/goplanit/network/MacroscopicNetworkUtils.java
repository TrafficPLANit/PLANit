package org.goplanit.network;

import org.goplanit.network.layer.macroscopic.MacroscopicGridNetworkLayerGenerator;
import org.goplanit.utils.function.Tap;
import org.goplanit.utils.geo.PlanitJtsCrsUtils;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.mode.Mode;
import org.goplanit.utils.mode.PredefinedModeType;
import org.goplanit.utils.network.layer.MacroscopicNetworkLayer;
import org.goplanit.utils.network.layer.NetworkLayer;
import org.goplanit.utils.network.layer.macroscopic.MacroscopicLinkSegmentType;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Utilities related to the Macroscopic Network class
 *
 * @author markr
 */
public class MacroscopicNetworkUtils {

  private static final Logger LOGGER = Logger.getLogger(MacroscopicNetworkUtils.class.getCanonicalName());

  /**
   * Create a macroscopic network instance using the id token provided and in addition generate a simple grid-based
   * network layer for the predefined car mode, where each link is bi-directional and has a single link segment type
   * with access for car (nothing else set). For a more sophisticated grid generator configure the dedicated generator
   * class MacroscopicGridNetworkLayerGenerator by overriding its defaults that are used here.
   *
   * @param tokenId to use
   * @param rows    in the grid
   * @param columns in the grid
   * @return created grid network
   */
  public static MacroscopicNetwork createSimpleGrid(final IdGroupingToken tokenId, int rows, int columns) {
    var network = new MacroscopicNetwork(tokenId);
    network.setCoordinateReferenceSystem(PlanitJtsCrsUtils.CARTESIANCRS);
    var carMode = network.getModes().getFactory().registerNew(PredefinedModeType.CAR);

    MacroscopicGridNetworkLayerGenerator.create(rows, columns, network.getTransportLayers(), carMode).generate();
    return network;
  }

  /**
   * Based on a given network, generate an id grouping token for the conjugate version of this network
   * embedding information about the original in the description
   *
   * @param network to use as reference
   * @return created token
   */
  public static IdGroupingToken generateDerivedConjugateIdGroupingToken(
          MacroscopicNetwork network) {
    return IdGenerator.createIdGroupingToken(
            "Conjugate for original network (" + network.getIdsAsString()+")");
  }

  /**
   * Take reference mode and make sure all expansion modes are added to the supported modes of the network and add
   * them as access modes to all link segment types where the reference mode is supported
   *
   * @param network network to expand mode support on
   * @param referenceModeType the mode to use as a reference
   * @param expansionModeTypes all the modes to expand to
   */
  public static void expandModeSupport(
      MacroscopicNetwork network, PredefinedModeType referenceModeType, PredefinedModeType... expansionModeTypes) {

    var refMode = network.getModes().get(referenceModeType);
    if(refMode == null){
      LOGGER.warning(String.format("Reference mode (%s) to expand not available on network, abort mode expansion",
          referenceModeType));
    }

    var expansionModeList = new ArrayList<Mode>();
    for(var expModeType : expansionModeTypes){
      var expMode = network.getModes().get(expModeType);
      if(expMode == null){
        expMode = network.getModes().getFactory().registerNew(expModeType);
      }
      if(expMode == null){
        LOGGER.severe(String.format("Unable to create expansion mode from type %s", expModeType));
        continue;
      }
      expansionModeList.add(expMode);
    }

    // add expansion modes to each link segment type where the ref mode is allowed
    network.getTransportLayers().stream().filter(l -> l.supports(refMode))
        // pass through where we add it to supported modes on layer as well if ref mode is present
        .map((Tap<MacroscopicNetworkLayer>) l -> l.registerSupportedModes(expansionModeList))
        // then add to all link types of that layer as well when ref mode supports it
        .flatMap(l -> l.getLinkSegmentTypes().stream()).filter(lt -> lt.isModeAllowed(refMode)).forEach(
            lt -> expansionModeList.forEach(em -> lt.registerModeOnAccessGroup(em, lt.getAccessProperties(refMode))));
  }
}
