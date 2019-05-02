package org.planit.output.formatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.cost.Cost;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;

/**
 * Output formatter for CSV output, i.e. this class is capable of persisting output in the CSV data type
 * 
 * @author markr
  */
public class CSVOutputFormatter extends BaseOutputFormatter {
    
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(CSVOutputFormatter.class.getName());    
    
    private static final String DEFAULT_OUTPUT_DIRECTORY = "C:\\Users\\Public\\PlanIt\\Csv";
    private static final String DEFAULT_NAME_ROOT = "CSVOutput";
    private static final String DEFAULT_NAME_EXTENSION = ".csv";
    
    private String outputDirectory;
    private String nameRoot;
    private String nameExtension;
    private String outputFileName;
    private CSVPrinter printer;

    public CSVOutputFormatter() {
        outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
        nameRoot = DEFAULT_NAME_ROOT;
        nameExtension = DEFAULT_NAME_EXTENSION;
        outputFileName = null;
    }
    
/**
 * Write data to CSV output file
 * 
 * @param timePeriod                          time period for current results
 * @param modes                                Set of modes covered by current results
 * @param outputTypeConfiguration    output configuration being used
 * @throws PlanItException                 thrown if there is an error
 */
     public void persist(TimePeriod timePeriod, Set<Mode> modes, OutputTypeConfiguration outputTypeConfiguration) throws PlanItException {
        try {
            OutputAdapter outputAdapter = outputTypeConfiguration.getOutputAdapter();
            if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
                TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
                writeResultsForCurrentTimePeriod(traditionalStaticAssignmentLinkOutputAdapter, modes, timePeriod);
            } else {
                throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName() + " which has not been defined yet");
            }
        } catch (Exception e) {
            throw new PlanItException(e);
        }
    }

 /**
  * Open output CSV file.  This method also creates the output file directory if it does not already exist
  * 
  * @throws PlanItException       thrown if output file or directory cannot be opened
  */
    @Override
    public void open() throws PlanItException {
        if (outputFileName == null) {
           outputFileName = generateOutputFileName();
       }
       try {
           printer = new CSVPrinter(new FileWriter(outputFileName), CSVFormat.EXCEL);
           printer.printRecord("Run Id", "Time Period Id", "Mode Id", "Start Node Id", "End Node Id", "Link Flow", "Capacity", "Length", "Speed", "Link Cost",  "Cost to End Node", "alpha", "beta");
       } catch (IOException ioe) {
           throw new PlanItException(ioe);
       }
    }

/**
 * Close output CSV file
 * 
 * @throws PlanItException     thrown if the the output file cannot be closed
 */
    @Override
    public void close() throws PlanItException {
        try {
            printer.close();
        } catch (IOException ioe) {
            throw new PlanItException(ioe);
        }
    }
    
/**
 * Generates the name of the CSV output file from the class properties.
 * 
 * This method also creates the output file directory if it does not already exist.
 * 
 * @return                                      the name of the output CSV file
 * @throws PlanItException           thrown if the output directory cannot be opened
 */
    private String generateOutputFileName() throws PlanItException  {
        try {
            File directory = new File(outputDirectory);
            if (!directory.isDirectory()) {
                Files.createDirectories(directory.toPath());
            }
            String[] files = directory.list();
            int pos = files.length + 1;
            String newFileName = outputDirectory + "\\" + nameRoot + pos + nameExtension;
            return newFileName;
        } catch (Exception e) {
            throw new PlanItException(e);
        }
    }

/**
 * Sets the name of the output file directory
 * 
 * @param outputDirectory      the name of the output file directory
 */
    public void setDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

 /**
  * Sets the root name of the CSV output file
  * 
  * @param nameRoot        root name of CSV output file
  */
    public void setNameRoot(String nameRoot) {
        this.nameRoot = nameRoot;
    }

 /**
  * Sets the extension of the CSV output file
  * 
  * @param nameExtension      the extension of the CSV output file
  */
    public void setNameExtension(String nameExtension) {
        this.nameExtension = nameExtension;
    }

 /**
  * Set the output file name
  * 
  * @param outputFileName    the CSV output file name
  */
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
 /**
  * Write the results for the current time period to the CSV file
  * 
  * @param outputAdapter               TraditionalStaticAssignmentLinkOutputAdapter used to retrieve the results of the assignment
  * @param modes                           Set of modes of travel
  * @param timePeriod                     the current time period
  * @return                                       Map containing the results for each mode
  * @throws PlanItException            thrown if there is an error
  */
    private void writeResultsForCurrentTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter, Set<Mode> modes, TimePeriod timePeriod) throws PlanItException {
        Function<LinkSegment, Double> alphaFunction = null;
        Function<LinkSegment, Double>  betaFunction = null;
        Cost<LinkSegment> physicalCost = outputAdapter.getPhysicalCost();
        double[] totalNetworkSegmentFlows = outputAdapter.getTotalNetworkSegmentFlows();
        TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
        for (Mode mode : modes) {
            double[] totalNetworkSegmentCosts = outputAdapter.getNetworkSegmentCosts(mode);
            if (physicalCost instanceof BPRLinkTravelTimeCost) {
                alphaFunction = (linkSegment) -> {return ((BPRLinkTravelTimeCost) physicalCost).getAlpha(mode, linkSegment);};
                betaFunction = (linkSegment) -> {return ((BPRLinkTravelTimeCost) physicalCost).getBeta(mode, linkSegment);};
            }
            writeResultsForCurrentModeAndTimePeriod(outputAdapter, 
                                                                                     mode, 
                                                                                     timePeriod,
                                                                                     totalNetworkSegmentCosts, 
                                                                                     totalNetworkSegmentFlows,
                                                                                     transportNetwork,
                                                                                     alphaFunction, 
                                                                                     betaFunction);
        }
    }
    
 /**
  * Write results for the current mode and time period to the CSV file
  * 
  * @param outputAdapter                              TraditionalStaticAssignmentLinkOutputAdapter
  * @param mode                                           current mode of travel
  * @param timePeriod                                   current time period
  * @param totalNetworkSegmentCosts        calculated segment costs for the physical network
  * @param totalNetworkSegmentFlows        calculated flows for the network
  * @param transportNetwork                         the transport network
  * @param alphaFunction                              lambda function to retrieve alpha value for a segment 
  * @param betaFunction                                lambda function to retrieve beta value for a segment
  * @throws PlanItException                           thrown if there is an error
  */
    private void writeResultsForCurrentModeAndTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter, 
                                                                                                   Mode mode, 
                                                                                                   TimePeriod timePeriod,
                                                                                                   double[] totalNetworkSegmentCosts, 
                                                                                                   double[] totalNetworkSegmentFlows,
                                                                                                   TransportNetwork transportNetwork,
                                                                                                   Function<LinkSegment, Double> alphaFunction,
                                                                                                   Function<LinkSegment, Double>  betaFunction) throws PlanItException {
        try {
            double totalCost = 0.0;
            Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
            while (linkSegmentIter.hasNext()) {
                MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
                int id = (int) linkSegment.getId();
                double flow = totalNetworkSegmentFlows[id];
                if (flow > 0.0) {
                    double cost = totalNetworkSegmentCosts[id];
                    totalCost += flow * cost;
                    long trafficAssignmentId = outputAdapter.getTrafficAssignmentId();
                    printer.printRecord(trafficAssignmentId, 
                                                    timePeriod.getId(), 
                                                    mode.getId(), 
                                                    linkSegment.getUpstreamVertex().getExternalId(),
                                                    linkSegment.getDownstreamVertex().getExternalId(),
                                                    flow, 
                                                    linkSegment.getLinkSegmentType().getCapacityPerLane(mode.getId()) * linkSegment.getNumberOfLanes(),
                                                    linkSegment.getParentLink().getLength(),
                                                    linkSegment.getMaximumSpeed(mode.getId()),
                                                    cost, 
                                                    totalCost,
                                                    alphaFunction.apply(linkSegment),
                                                    betaFunction.apply(linkSegment));
                }
            }
        } catch (Exception e) {
            throw new PlanItException(e);
        }
    }
    
}
