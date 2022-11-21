package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import java.util.*;

/**
 * This class selects for each XNE the associate XNEC with the highest absolute loading
 *
 * @author Alexandre LE JEAN
 */
class XnecProviderHighestLoading implements XnecProvider {

    final private List<Xnec> xnecs;

    @Override
    public List<Branch> getNetworkElements(Network network) {
        treatmentOfTieLine();
        xnecSelection();
        return null;
    }

    /** Give the full list of overloaded XNEC before and after the RAO */
    public XnecProviderHighestLoading(Collection<Xnec> xnecs) {
        this.xnecs = new ArrayList<>(xnecs);
    }

    private void xnecSelection() {
        // Sort XNECs : ascending order in name, then prioritize those with remedial action materialized,
        // then by ascending order in active power flow before RAO
        xnecs.sort((o1, o2) -> {
            if(o1.xneNameOrId.compareTo(o2.xneNameOrId) != 0)
                return o1.xneNameOrId.compareTo(o2.xneNameOrId);
            else if(o1.isRemedialActionMaterialized || o2.isRemedialActionMaterialized)
                return o1.isRemedialActionMaterialized ? -1 : 1;
            else if (o1.activePowerFlowBeforeRao != o2.activePowerFlowBeforeRao)
                return o1.activePowerFlowBeforeRao > o2.activePowerFlowBeforeRao ? -1 : 1;
            else
                return 0;
        });

        // For each XNEC, remove if previous XNEC has the same name or ID,
        // i.e. no remedial action materialized or lower active power flow
        final Iterator<Xnec> itr = xnecs.iterator();
        String old = "";
        while(itr.hasNext()) {
            String next = itr.next().xneNameOrId;
            if (next.equals(old)) {
                itr.remove();
            }
            else {
                old = next;
            }
        }
    }

    private void treatmentOfTieLine() {
        int i = 0;
        while (i < xnecs.size()) {
            int j = i+1;
            while (j < xnecs.size()){
                if(xnecs.get(i).tsoName1.equals(xnecs.get(j).tsoName1)
                        && (xnecs.get(i).nodeNameOrId1.equals(xnecs.get(j).nodeNameOrId1) ||
                        xnecs.get(i).nodeNameOrId1.equals(xnecs.get(j).nodeNameOrId2) ||
                        xnecs.get(i).nodeNameOrId2.equals(xnecs.get(j).nodeNameOrId1) ||
                        xnecs.get(i).nodeNameOrId2.equals(xnecs.get(j).nodeNameOrId2))) {
                    xnecs.get(i).tsoName2 = xnecs.get(j).tsoName1;
                    xnecs.get(i).flowMax2 = xnecs.get(j).flowMax1;
                    xnecs.remove(j);
                }
                else
                    j++;
            }
            i++;
        }
    }
}
