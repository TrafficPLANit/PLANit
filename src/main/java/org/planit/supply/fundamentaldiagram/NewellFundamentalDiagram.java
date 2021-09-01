package org.planit.supply.fundamentaldiagram;

/**
 * Implementation of a Newell (triangular) fundamental diagram for a road section (link segment)
 * 
 * @author markr
 */
public class NewellFundamentalDiagram extends FundamentalDiagramImpl {

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeSpeedKmHour
   */
  public NewellFundamentalDiagram(double freeSpeedKmHour) {
    super(new LinearFundamentalDiagramBranch(freeSpeedKmHour, FundamentalDiagramBranch.DEFAULT_EMPTY_DENSITY_PCU_HOUR),
        new LinearFundamentalDiagramBranch(FundamentalDiagramBranch.DEFAULT_BACKWARD_WAVE_SPEED_KM_HOUR, FundamentalDiagramBranch.DEFAULT_JAM_DENSITY_PCU_HOUR));
  }

  /**
   * Constructor using all defaults except for the free speed to apply
   * 
   * @param freeSpeedKmHour
   */
  public NewellFundamentalDiagram(final LinearFundamentalDiagramBranch freeFlowBranch, final LinearFundamentalDiagramBranch congestedBranch) {
    super(freeFlowBranch, congestedBranch);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinearFundamentalDiagramBranch getFreeFlowBranch() {
    return (LinearFundamentalDiagramBranch) super.getFreeFlowBranch();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LinearFundamentalDiagramBranch getCongestedBranch() {
    return (LinearFundamentalDiagramBranch) super.getCongestedBranch();
  }

  //@formatter:off
  /**
   * {@inheritDoc}
   */
  @Override
  public double getCapacityFlowPcuHour() {
    /* capacity = (k_crit-0)*maxspeed
     * capacity = (k_crit-k_jam)*backwardwavespeed
     * so:
     * (k_crit-0)*maxspeed = (k_crit-k_jam)*backwardwavespeed
     * k_crit(maxspeed -backwardwavespeed) = k_jam *backwardwavespeed
     * k_crit = (k_jam *backwardwavespeed)/(maxspeed -backwardwavespeed)
     * capacity = ((k_jam *backwardwavespeed)/(maxspeed -backwardwavespeed)) * maxspeed  
     */
    double maxSpeed = getMaximumSpeedKmHour();
    double backwardWaveSpeed = getCongestedBranch().getCharateristicWaveSpeedKmHour();
    double kCrit = ((getJamDensityPcuKm()*backwardWaveSpeed)
                    / 
                    (maxSpeed - backwardWaveSpeed))
                    *
                    maxSpeed;
    return kCrit * maxSpeed;
  }

}
