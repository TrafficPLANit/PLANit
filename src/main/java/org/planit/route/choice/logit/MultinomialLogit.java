package org.planit.route.choice.logit;

import org.planit.exceptions.PlanItException;
import org.planit.trafficassignment.TrafficAssignmentComponentFactory;

/** MNL choice model implementation
 *
 * @author markr
 *
 */
public class MultinomialLogit extends LogitChoiceModel {

	/** generated UID */
	private static final long serialVersionUID = -7602543264466240409L;

	// register to be eligible in PLANit
    static {
        try {
            TrafficAssignmentComponentFactory.registerTrafficAssignmentComponentType(MultinomialLogit.class);
        } catch (final PlanItException e) {
            e.printStackTrace();
        }
    }

	/** Constructor
	 * @param trafficComponentCreateListener
	 */
	protected MultinomialLogit() {
		super();
	}

}
