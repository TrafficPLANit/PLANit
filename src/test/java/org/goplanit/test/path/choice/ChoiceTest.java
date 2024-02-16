package org.goplanit.test.path.choice;

import org.goplanit.choice.logit.MultinomialLogit;
import org.goplanit.choice.weibit.Weibit;
import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.math.Precision;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test for the MNL logit implementation
 */
public class ChoiceTest {

    private static final IdGroupingToken testToken = IdGenerator.createIdGroupingToken("ChoiceTest");

    private static Weibit weibit;
    private static MultinomialLogit mnl;

    /**
     * {@inheritDoc}
     */
    @BeforeAll
    public static void setUp() throws Exception {
        weibit = new Weibit(testToken);
        mnl = new MultinomialLogit(testToken);
    }

    /**
     * {@inheritDoc}
     */
    @AfterAll
    public static void tearDown() {
    }

    //@formatter:off
    @BeforeEach
    public void intialise() {
    }
    //@formatter:on

    @Test
    public void MnlTest() {
        mnl.setScalingFactor(1);

        /* single option = 100% probability */
        assertEquals(1.0,mnl.computeProbabilities(new double[]{1.0})[0], Precision.EPSILON_6);
        assertEquals(1.0,mnl.computeProbabilities(new double[]{2.0})[0], Precision.EPSILON_6);

        /* two identical absolute differences in utility --> should yield same probabilities in both cases */
        var result_diff_1 = mnl.computeProbabilities(new double[]{1.0, 2.0});
        var result2_diff_1 = mnl.computeProbabilities(new double[]{99.0, 100.0});
        assertEquals(result_diff_1.length, 2);
        assertEquals(result2_diff_1.length, 2);
        assertNotEquals(result_diff_1[0], result_diff_1[1], Precision.EPSILON_6);
        assertEquals(result_diff_1[0], result2_diff_1[0], Precision.EPSILON_6);
        assertEquals(result_diff_1[1], result2_diff_1[1], Precision.EPSILON_6);

        /* check actual result */
        assertEquals(0.7310585786300049, result_diff_1[0], Precision.EPSILON_6);
        assertEquals(0.2689414213699951, result_diff_1[1], Precision.EPSILON_6);

        /* change scale parameter */
        mnl.setScalingFactor(10);

        var result_diff_10 = mnl.computeProbabilities(new double[]{1.0, 2.0});
        assertEquals(result_diff_10.length, 2);
        /* check actual result */
        assertEquals(0.999954, result_diff_10[0], Precision.EPSILON_6);
        assertEquals(0.000046, result_diff_10[1], Precision.EPSILON_6);
    }

    @Test
    public void WeibitTest() {
        weibit.setScalingFactor(1);

        /* single option = 100% probability */
        assertEquals(1.0,weibit.computeProbabilities(new double[]{1.0})[0], Precision.EPSILON_6);
        assertEquals(1.0,weibit.computeProbabilities(new double[]{2.0})[0], Precision.EPSILON_6);

        var result_diff_1 = weibit.computeProbabilities(new double[]{1.0, 2.0});
        var result2_diff_1 = weibit.computeProbabilities(new double[]{99.0, 100.0});
        assertEquals(result_diff_1.length, 2);
        assertEquals(result2_diff_1.length, 2);
        assertNotEquals(result_diff_1[0], result_diff_1[1], Precision.EPSILON_6);

        /* two identical absolute differences in utility --> should NOT yield same probabilities in weibit which uses on ratio between differences to work out probabilities*/
        assertNotEquals(result_diff_1[0], result2_diff_1[0], Precision.EPSILON_6);
        assertNotEquals(result_diff_1[1], result2_diff_1[1], Precision.EPSILON_6);
        /* ... now use equal ratios between two options, then should yield same probabilities in weibit */
        result2_diff_1 = weibit.computeProbabilities(new double[]{5.0, 10.0});
        assertEquals(result_diff_1[0], result2_diff_1[0], Precision.EPSILON_6);
        assertEquals(result_diff_1[1], result2_diff_1[1], Precision.EPSILON_6);

        /* check actual result */
        assertEquals(0.66666667, result_diff_1[0], Precision.EPSILON_6);
        assertEquals(0.33333333, result_diff_1[1], Precision.EPSILON_6);

        /* change scale parameter */
        weibit.setScalingFactor(10);

        var result_diff_10 = weibit.computeProbabilities(new double[]{1.0, 2.0});
        assertEquals(result_diff_10.length, 2);
        /* check actual result --> scaling works same so due to increased scaling factor differences are more important and we gravitate towards cheaper alternative more  */
        assertEquals(0.9990243, result_diff_10[0], Precision.EPSILON_6);
        assertEquals(0.0009756, result_diff_10[1], Precision.EPSILON_6);
    }

    @Test
    public void MnlPerceivedCostTest() {
        mnl.setScalingFactor(2);
        assertEquals(3.6931471805599454, mnl.computePerceivedCost(3,4, false), Precision.EPSILON_6);
        assertEquals(1613.715173971, mnl.computePerceivedCost(3,4, true), Precision.EPSILON_6);
    }

    @Test
    public void WeibitPerceivedCostTest() {
        weibit.setScalingFactor(2);
        assertEquals(1.791759469228055, weibit.computePerceivedCost(3,4, false), Precision.EPSILON_6);
        assertEquals(36, weibit.computePerceivedCost(3,4, true), Precision.EPSILON_6);
    }

}
