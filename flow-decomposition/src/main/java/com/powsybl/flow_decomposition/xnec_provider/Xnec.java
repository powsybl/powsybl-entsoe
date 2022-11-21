package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

/**
 * @author alejean
 */
public class Xnec {
    /** Associated XNE's ID or name **/
    String xneNameOrId;
    /** Associated Contingency's ID or name */
    String contingencyNameOrId;
    /** Node's ID or name */
    String nodeNameOrId1, nodeNameOrId2;
    /** Connecting TSO(s)'s name */
    Country tsoName1, tsoName2;
    /** True if a remedial action is materialized for this XNEC **/
    boolean isRemedialActionMaterialized;
    /** Maximum current */
    double currentMax1, currentMax2;
    /** Nominal voltage */
    double nominalVoltage1, nominalVoltage2;
    /** Current before RAO */
    double currentBeforeRao;
    /** Current after RAO */
    double currentAfterRao;
    /** Maximum flow for sending and receiving node */
    double flowMax1, flowMax2;
    /** Active power flow on XNEC before RAO */
    double activePowerFlowBeforeRao;
    /** Active power flow on XNEC after RAO, which includes the impact of all agreed XRAs */
    double activePowerFlowAfterRao;

    public Xnec(Branch<?> branch, double currentBeforeRao, double currentAfterRao,
                String contingencyNameOrId, boolean isRemedialActionMaterialized) {
        // Direct data
        this.xneNameOrId = branch.getNameOrId();
        this.contingencyNameOrId = contingencyNameOrId;
        this.nodeNameOrId1 = branch.getTerminal1().toString();
        this.nodeNameOrId2 = branch.getTerminal2().toString();
        this.tsoName1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        this.tsoName2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        this.isRemedialActionMaterialized = isRemedialActionMaterialized;
        this.currentAfterRao = currentAfterRao;
        this.currentBeforeRao = currentBeforeRao;
        this.nominalVoltage1 = branch.getTerminal1().getVoltageLevel().getNominalV();
        this.nominalVoltage2 = branch.getTerminal2().getVoltageLevel().getNominalV();
        this.currentMax1 = branch.getNullableCurrentLimits1().getPermanentLimit();
        this.currentMax2 = branch.getNullableCurrentLimits2().getPermanentLimit();

        // Compute Flow Maximum (equals to Sqrt(3) * Nominal Voltage * Maximum current)
        this.flowMax1 = Math.sqrt(3) * this.nominalVoltage1 * this.currentMax1;
        this.flowMax2 = Math.sqrt(3) * this.nominalVoltage2 * this.currentMax2;

        // Compute Active power flow before and after RAO
        final double flowMax = Math.min(flowMax1, flowMax2);
        final double currentMin = Math.min(this.currentMax1, this.currentMax2);

        this.activePowerFlowBeforeRao = flowMax * this.currentBeforeRao / currentMin;
        this.activePowerFlowAfterRao = flowMax * this.currentAfterRao / currentMin;
    }
}
