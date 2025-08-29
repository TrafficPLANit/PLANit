package org.goplanit.assignment.ltm.sltm.conjugate;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.goplanit.network.transport.ConjugateTransportModelNetwork;
import org.goplanit.output.formatter.CsvFileOutputFormatter;
import org.goplanit.utils.arrays.ArrayUtils;
import org.goplanit.utils.exceptions.PlanItRunTimeException;
import org.goplanit.utils.graph.directed.ConjugateDirectedVertex;
import org.goplanit.utils.graph.directed.ConjugateEdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.acyclic.ConjugateACyclicSubGraph;
import org.goplanit.utils.math.Precision;
import org.goplanit.utils.misc.StringUtils;
import org.goplanit.utils.network.virtual.physical.conjugate.ConjugateConnectoidNode;
import org.goplanit.utils.zoning.OdZone;

/**
 * Track conjugate edge segment based data, i.e., original network turns in a conjugate bush form.
 * <p>
 * For now we only track (turn/conjugate segment) sending flows to minimise bookkeeping and memory usage,
 * splitting rates are deduced from the sending flows when needed.
 *
 * todo: (not todo) NOTE to self, last synced with LabelledBushTurnData implementation on 22/11
 * 
 * @author markr
 *
 */
public class ConjugateBushTurnData implements Iterable<Map.Entry<ConjugateEdgeSegment,Double>>{

  /** logger to use */
  private static final Logger LOGGER = Logger.getLogger(ConjugateBushTurnData.class.getCanonicalName());

  /** track known conjugate bush (turn) sending flows s_ab by conjugate link segment */
  private final Map<ConjugateEdgeSegment, Double> turnSendingFlows;

  private final ConjugateDestinationBush parent;

  /**
   * Register turn sending flow on the container
   * 
   * @param turn            the turn
   * @param turnSendingFlow flow of turn
   */
  private void registerTurnSendingFlow(ConjugateEdgeSegment turn, double turnSendingFlow) {
    turnSendingFlows.put(turn, turnSendingFlow);
  }

  /**
   * Constructor
   *
   * @param parent parent bush
   */
  ConjugateBushTurnData(final ConjugateDestinationBush parent) {
    this.turnSendingFlows = new HashMap<>();
    this.parent = parent;
  }

  /**
   * copy constructor.
   * 
   * @param bushTurnData to copy
   */
  public ConjugateBushTurnData(ConjugateBushTurnData bushTurnData) {
    this.turnSendingFlows = new HashMap<>(bushTurnData.turnSendingFlows);
    this.parent = bushTurnData.parent;
  }

  /**
   * Update the turn sending flow for a given turn
   *
   * @param turnSegment      conjugate of turn
   * @param turnSendingFlow  to update
   * @param force if not forced perform additional checks to see if valid before applying
   * @return true when turn has any labelled turn sending flow left after setting flow, false when labelled turn
   *        sending flow no longer exists
   */
  public boolean setTurnSendingFlow(
          final ConjugateEdgeSegment turnSegment,
          double turnSendingFlow,
          boolean force) {

    if (Double.isNaN(turnSendingFlow)) {
      LOGGER.severe(String.format("Turn (%s) sending flow is NAN, shouldn't happen - consider identifying issue " +
              "as turn flow cannot be updated properly, reset to 0.0 flow",
          turnSegment.getIdsAsString()));
      turnSendingFlow = 0.0;
    }else if(!force){
      // not forced, so apply some additional checking in situation of low flows and negative flows
      // note forced may be helpful for small positive flows that otherwise would be regarded as zero flow with the
      // below checks
      if(!Precision.positive(turnSendingFlow)) {
        // when negative flow but extremely close to zero, remove the turn flow and continue
        removeTurnData(turnSegment);
        return false;
      }else if(turnSendingFlow < 0) {
        // too negative, warn user as this is unexpected behaviour possibly beyond a rounding situation
        var originalTurnSegments = turnSegment.getOriginalAdjacentEdgeSegments();
        LOGGER.warning(String.format(
                "** Turn (%s to %s) sending flow negative (%.9f) on bush (%s), this is not allowed, removing turn flow",
                originalTurnSegments.first().getXmlId(), originalTurnSegments.second().getXmlId(),
                turnSendingFlow, parent.getRootZoneVertex().getParent().getParentZone().getIdsAsString()));
        removeTurnData(turnSegment);
        return false;
      }
    }

    registerTurnSendingFlow(turnSegment, turnSendingFlow);
    return true;
  }

  /**
   * Add turn sending flow for a given turn (can be negative). Do not force registration of zero flows or negative flows
   * by performing additional checks on the flow change proposed before applying
   * 
   * @param turnSegment      the turn
   * @param flowPcuH         to add
   * @return the new labelled turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turnSegment, double flowPcuH) {
    return addTurnSendingFlow(turnSegment, flowPcuH, flowPcuH>0);
  }

  /**
   * Add turn sending flow for a given turn (can be negative).
   *
   * @param turnSegment      the turn
   * @param flowPcuH         to add
   * @param force            force registration of this flow change when true, regardless of what change is provided
   * @return the new labelled turn sending flow after adding the given flow
   */
  public double addTurnSendingFlow(final ConjugateEdgeSegment turnSegment, double flowPcuH, boolean force) {
    Double newSendingFlow = flowPcuH + getTurnSendingFlowPcuH(turnSegment);
    boolean hasRemainingFlow = setTurnSendingFlow(turnSegment, newSendingFlow, force);
    newSendingFlow = hasRemainingFlow ? newSendingFlow : 0.0;
    return newSendingFlow;
  }

  /**
   * Remove the turn entirely
   * 
   * @param turnSegment the turn
   */
  public void removeTurnData(final ConjugateEdgeSegment turnSegment) {
    turnSendingFlows.remove(turnSegment);
  }

  /**
   * Get the turn sending flow for a given turn
   * 
   * @param turnSegment the turn
   * @return turn sending flow, 0 if not present
   */
  public double getTurnSendingFlowPcuH(final ConjugateEdgeSegment turnSegment) {
    Double existingSendingFlow = turnSendingFlows.get(turnSegment);
    if (existingSendingFlow != null) {
      return existingSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Total sending flows s_a from given original segment collected by means of the conjugate node
   * 
   * @param node conjugate node to use
   * @return sending flow s_a
   */
  public double getTotalSendingFlowFromPcuH(final ConjugateDirectedVertex node) {
    double totalSendingFlow = 0;
    for (var turn : node.getExitEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(turn);
      totalSendingFlow += s_ab;
    }
    return totalSendingFlow;
  }

  /**
   * Verify if any sending flows s_a from given original segment exists
   *
   * @param node conjugate node to use
   * @return true when sending flow s_a exists, false otherwise
   */
  public boolean containsSendingFlow(final ConjugateDirectedVertex node) {
    for (var turn : node.getExitEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(turn);
      if(s_ab > 0){
        return true;
      }
    }
    return false;
  }



  /**
   * Collect the accepted flow towards a conjugate node (original edge segment) in the bush, if not present,
   * zero flow is returned
   * 
   * @param conjVertex                                        conjugate node to collect accepted flow towards to
   * @param originalNetworkSegmentFlowAcceptanceFactors to convert sending flow to accepted flow (based on original
   *                                                    edge segment ids)
   * @return bush sending flow found
   */
  public double getTotalAcceptedFlowToPcuH(
          final ConjugateDirectedVertex conjVertex, double[] originalNetworkSegmentFlowAcceptanceFactors) {

    if (conjVertex instanceof ConjugateConnectoidNode && conjVertex.getOriginalEdgeSegment() == null) {
      /* sink/source, hence it must be a root vertex connected to an origin */
      return getTotalSendingFlowFromPcuH(conjVertex);
    }

    double totalAcceptedFlow = 0;
    for (var turn : conjVertex.getEntryEdgeSegments()) {
      double s_ab = getTurnSendingFlowPcuH(turn);

      double v_ab = s_ab;
      if(turn.hasOriginalEntryEdgeSegment()) {
        var originalEntrySegment = turn.getOriginalAdjacentEdgeSegments().first();
        v_ab *= originalNetworkSegmentFlowAcceptanceFactors[(int) originalEntrySegment.getId()];
      }
      totalAcceptedFlow += v_ab;
    }
    return totalAcceptedFlow;
  }

  /**
   * Verify if the turn sending flow for a given turn is positive
   * 
   * @param turnSegment the turn
   * @return true when present, false otherwise
   */
  public boolean containsTurnSendingFlow(final ConjugateEdgeSegment turnSegment) {
    return getTurnSendingFlowPcuH(turnSegment) > 0;
  }

  /**
   * Collect the splitting rates for a given conjugate node (original link segment). Splitting rates are based on
   * the current turn sending flows s_ab. In case no flows are present zero splitting rates for all turns are returned.
   *
   * @param conjugateVertex to collect bush splitting rates for
   * @param dag to use in case no splitting rates are available based on turn sending flows
   * @return splitting rates in primitive array in order of which one iterates over the outgoing edge segments of the
   * conjugate node
   */
  public double[] getSplittingRates(final ConjugateDirectedVertex conjugateVertex, ConjugateACyclicSubGraph dag) {
    if(!conjugateVertex.hasExitEdgeSegments()){
      return null;
    }
    var turns = conjugateVertex.getExitEdgeSegments();

    /* determining number of edge segment is costly, instead use edges (which is larger or equal) and then copy
     * result */
    double[] splittingRates = new double[conjugateVertex.getNumberOfEdges()];

    double totalSendingFlow = 0;
    int index = 0;
    for (var turn : turns) {
      double s_ab = getTurnSendingFlowPcuH(turn);
      splittingRates[index++] = s_ab;
      totalSendingFlow += s_ab;
    }
    if(totalSendingFlow > 0) {
      ArrayUtils.divideBy(splittingRates, totalSendingFlow, 0);
    }else{
      // backup
      int containsIndex = 0;
      boolean success = false;
      for (var turn : turns) {
        if(dag.containsEdgeSegment(turn)) {
          splittingRates[containsIndex] = 1;
          LOGGER.fine(String.format("...redirect 100%% of flow to (%s)", turn.getIdsAsString()));
          success = true;
          break;
        }
        ++containsIndex;
      }
      if(!success){
        LOGGER.warning("splitting rates cannot be constructed from turn flows, unable to salvage by " +
            "using DAG information");
      }
    }

    /* truncate */
    splittingRates = Arrays.copyOf(splittingRates, index);
    return splittingRates;
  }

  /**
   * Collect the splitting rate for a given conjugate link segment. Splitting rates are based on the current turn
   * sending flows s_ab.
   * <p>
   * When collecting multiple splitting rates with the same in link, do not use this method but instead collect all
   * splitting rates at once and then filter the ones you require it is computationally more efficient.
   * 
   * 
   * @param turnSegment the turn to collect splitting rate for
   * @return splitting rate, when turn is not present or not used, zero is returned
   */
  public double getSplittingRate(final ConjugateEdgeSegment turnSegment) {
    double turnSendingFlow = getTurnSendingFlowPcuH(turnSegment);
    if (turnSendingFlow > 0) {
      double totalSendingFlow = getTotalSendingFlowFromPcuH(turnSegment.getUpstreamVertex());
      if (totalSendingFlow < turnSendingFlow) {
        var originalPair = turnSegment.getOriginalAdjacentEdgeSegments();
        LOGGER.severe(String.format("Total sending flow (%.10f) smaller than turn (%s,%s) sending flow (%.10f), " +
                        "this shouldn't happen",
                totalSendingFlow, originalPair.first().getXmlId(), originalPair.second().getXmlId(), turnSendingFlow));
      }
      return turnSendingFlow / totalSendingFlow;
    } else {
      return 0;
    }
  }

  /**
   * Shallow copy
   *
   * @return shallow copy
   */
  public ConjugateBushTurnData shallowClone() {
    return new ConjugateBushTurnData(this);
  }

  /**
   * Verify if any turn flows have been registered
   * 
   * @return true if so, false otherwise
   */
  public boolean hasTurnFlows() {
    return turnSendingFlows.isEmpty();
  }

  @Override
  public Iterator<Map.Entry<ConjugateEdgeSegment,Double>> iterator() {
    return turnSendingFlows.entrySet().iterator();
  }

  // todo: ugly as heck but for now allows for warm starting bushes
  public void writeToCsv(Path filePathWithoutFile){
    if(parent == null){
      return;
    }
    String bushDataFileName = StringUtils.removeAllNonAlphaNumeric(this.parent.getRootZone().getIdsAsString()) + ".csv";
    var fullPath = Path.of(filePathWithoutFile.toAbsolutePath().toString(), bushDataFileName).toAbsolutePath();
    String[] headers = {"CONJ_EDGESEGMENT_XML_ID", "TURN_FLOW"};
    try (FileWriter writer = new FileWriter(fullPath.toString());
         CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {

        for( var entry : turnSendingFlows.entrySet()){
          csvPrinter.printRecord(entry.getKey().getXmlId(), String.format("%.6f", entry.getValue()));
        }
      csvPrinter.flush(); // Ensure all data is written to the file
      System.out.println("CSV file '" + fullPath + "' written successfully.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // todo: not for production
  public static Map<ConjugateEdgeSegment, Double> readTurnFlowDataFromCsv(
      Path filePathWithoutFile, OdZone rootZone, ConjugateTransportModelNetwork conjNetwork){
    String bushDataFileName = StringUtils.removeAllNonAlphaNumeric(rootZone.getIdsAsString()) + ".csv";
    var fullPath = Path.of(filePathWithoutFile.toAbsolutePath().toString(), bushDataFileName).toAbsolutePath();

    Map<ConjugateEdgeSegment, Double> resultMap = new HashMap<>();

    String[] headers = {"CONJ_EDGESEGMENT_XML_ID", "TURN_FLOW"};
    try (FileReader reader = new FileReader(fullPath.toString());
         CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader(headers).setSkipHeaderRecord(true).build())) {

      for(var record : csvParser){
        var conjSegmentXmlId = record.get(0);
        ConjugateEdgeSegment conjSegment =
            conjNetwork.getInfrastructureNetwork().getTransportLayers().getFirst().getLinkSegments().getByXmlId(
                conjSegmentXmlId);
        if(conjSegment == null) {
          conjSegment = conjNetwork.getVirtualNetwork().getLayer().getConnectoidSegments().getByXmlId(
              conjSegmentXmlId);
        }
        if(conjSegment == null){
          throw new PlanItRunTimeException("invalid segment");
        }
        resultMap.put(conjSegment, Double.parseDouble(record.get(1)));
      }
      System.out.println("CSV file '" + fullPath + "' parsed successfully.");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return resultMap;
  }
}
