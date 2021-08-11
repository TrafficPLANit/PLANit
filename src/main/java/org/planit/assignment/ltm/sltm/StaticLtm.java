package org.planit.assignment.ltm.sltm;

import java.util.logging.Logger;

import org.planit.assignment.ltm.LtmAssignment;
import org.planit.output.adapter.OutputTypeAdapter;
import org.planit.output.enums.OutputType;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.id.IdGroupingToken;

/**
 * static Link Transmission Model implementation (sLTM) for network loading based on solution method presented in Raadsen and Bliemer (2021) General solution scheme for the Static
 * Link Transmission Model .
 *
 * @author markr
 *
 */
public class StaticLtm extends LtmAssignment {

  /** generated UID */
  private static final long serialVersionUID = 8485652038791612169L;

  /** logger to use */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(StaticLtm.class.getCanonicalName());

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   */
  protected StaticLtm(IdGroupingToken groupId) {
    super(groupId);
  }

  /**
   * Copy Constructor
   * 
   * @param sltm to copy
   */
  protected StaticLtm(StaticLtm sltm) {
    super(sltm);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputTypeAdapter createOutputTypeAdapter(OutputType outputType) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeEquilibration() throws PlanItException {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getIterationIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StaticLtm clone() {
    return new StaticLtm(this);
  }

}
