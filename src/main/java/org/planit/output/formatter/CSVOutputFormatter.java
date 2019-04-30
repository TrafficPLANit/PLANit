package org.planit.output.formatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.planit.cost.physical.BPRLinkTravelTimeCost;
import org.planit.dto.BprResultDto;
import org.planit.exceptions.PlanItException;
import org.planit.network.physical.LinkSegment;
import org.planit.network.physical.macroscopic.MacroscopicLinkSegment;
import org.planit.network.transport.TransportNetwork;
import org.planit.output.adapter.OutputAdapter;
import org.planit.output.adapter.TraditionalStaticAssignmentLinkOutputAdapter;
import org.planit.output.configuration.OutputTypeConfiguration;
import org.planit.time.TimePeriod;
import org.planit.userclass.Mode;
import org.planit.utils.CsvIoUtils;

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
            SortedMap<Mode, SortedSet<BprResultDto>> resultsForCurrentTimePeriod = null;
            long runId = -1;
            if (outputAdapter instanceof TraditionalStaticAssignmentLinkOutputAdapter) {
                TraditionalStaticAssignmentLinkOutputAdapter traditionalStaticAssignmentLinkOutputAdapter = (TraditionalStaticAssignmentLinkOutputAdapter) outputAdapter;
                resultsForCurrentTimePeriod = saveResultsForCurrentTimePeriod(traditionalStaticAssignmentLinkOutputAdapter, modes);
                runId = traditionalStaticAssignmentLinkOutputAdapter.getRunId();
            } else {
                throw new PlanItException("OutputAdapter is of class " + outputAdapter.getClass().getCanonicalName() + " which has not been defined yet");
            }
            if (!resultsForCurrentTimePeriod.isEmpty()) {
                for (Mode mode : modes) {
                    Set<BprResultDto> results  = resultsForCurrentTimePeriod.get(mode);
                    if  ((results != null) && (!results.isEmpty())) {
                        for (BprResultDto resultDto : results) {
                            CsvIoUtils.printCurrentRecord(printer, runId, timePeriod, mode, resultDto);
                        }
                    }
                }
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
  * @param outputAdapter                OutputAdapter used to retrieve the results of the assignment
  * @param modes                           Set of modes of travel
  * @return                                       Map containing the results for each mode
  * @throws PlanItException            thrown if there is an error
  */
    private SortedMap<Mode, SortedSet<BprResultDto>> saveResultsForCurrentTimePeriod(TraditionalStaticAssignmentLinkOutputAdapter outputAdapter, Set<Mode> modes) throws PlanItException {
        SortedMap<Mode, SortedSet<BprResultDto>> resultsForCurrentTimePeriod = new TreeMap<Mode, SortedSet<BprResultDto>>();
        BPRLinkTravelTimeCost bprLinkTravelTimeCost = (BPRLinkTravelTimeCost) outputAdapter.getPhysicalCost();
        double[] totalNetworkSegmentFlows = outputAdapter.getTotalNetworkSegmentFlows();
        double[] totalNetworkSegmentCosts = outputAdapter.getTotalNetworkSegmentCosts(modes);
        TransportNetwork transportNetwork = outputAdapter.getTransportNetwork();
        Iterator<LinkSegment> linkSegmentIter = transportNetwork.linkSegments.iterator();
        for (Mode mode : modes) {
            SortedSet<BprResultDto> resultsForCurrentModeAndTimePeriod = new TreeSet<BprResultDto>(); // TreeSet implements SortedSet so stores results in order
            double totalCost = 0.0;
            while (linkSegmentIter.hasNext()) {
                MacroscopicLinkSegment linkSegment = (MacroscopicLinkSegment) linkSegmentIter.next();
                int id = (int) linkSegment.getId();
                double flow = totalNetworkSegmentFlows[id];
                if (flow > 0.0) {
                    double cost = totalNetworkSegmentCosts[id];
                    totalCost += flow * cost;
                    BprResultDto bprResultDto = new BprResultDto(linkSegment.getUpstreamVertex().getExternalId(),
                                                                                                    linkSegment.getDownstreamVertex().getExternalId(),
                                                                                                    flow, 
                                                                                                    cost, 
                                                                                                    totalCost,
                                                                                                    linkSegment.getLinkSegmentType().getCapacityPerLane() * linkSegment.getNumberOfLanes(),
                                                                                                    linkSegment.getParentLink().getLength(), 
                                                                                                    linkSegment.getMaximumSpeed(),
                                                                                                    bprLinkTravelTimeCost.getAlpha(linkSegment), 
                                                                                                    bprLinkTravelTimeCost.getBeta(linkSegment));
                    resultsForCurrentModeAndTimePeriod.add(bprResultDto);
                }
            }
            if (!resultsForCurrentModeAndTimePeriod.isEmpty()) {
                resultsForCurrentTimePeriod.put(mode, resultsForCurrentModeAndTimePeriod);
            }
        }
        return resultsForCurrentTimePeriod;
    }
    
}
