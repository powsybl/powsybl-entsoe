package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XnecProviderUnionTests {
    @Test
    void testUnionSinglepProviderOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line3 = "FGEN  11 FLOAD 11 3";
        String line4 = "FGEN  11 FLOAD 11 4";
        String line5 = "FGEN  11 FLOAD 11 5";
        String line6 = "FGEN  11 FLOAD 11 6";
        String line7 = "FGEN  11 FLOAD 11 7";
        String line8 = "FGEN  11 FLOAD 11 8";
        String line9 = "FGEN  11 FLOAD 11 9";
        XnecProvider xnecProvider5percPtdf = new XnecProvider5percPtdf();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProvider5percPtdf));
        Set<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(lineFrBe)));
        assertTrue(branchList.contains(network.getBranch(lineBeFr)));
        assertTrue(branchList.contains(network.getBranch(line1)));
        assertTrue(branchList.contains(network.getBranch(line2)));
        assertTrue(branchList.contains(network.getBranch(line3)));
        assertTrue(branchList.contains(network.getBranch(line4)));
        assertTrue(branchList.contains(network.getBranch(line5)));
        assertTrue(branchList.contains(network.getBranch(line6)));
        assertTrue(branchList.contains(network.getBranch(line7)));
        assertTrue(branchList.contains(network.getBranch(line8)));
        assertTrue(branchList.contains(network.getBranch(line9)));
        assertEquals(11, branchList.size());
    }

    @Test
    void testUnionMultipleProvidersOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderInterCo = new XnecProviderInterconnection();
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(line8, line1))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderInterCo, xnecProviderId));
        Set<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(lineFrBe)));
        assertTrue(branchList.contains(network.getBranch(lineBeFr)));
        assertTrue(branchList.contains(network.getBranch(line1)));
        assertTrue(branchList.contains(network.getBranch(line8)));
        assertEquals(4, branchList.size());
    }

    @Test
    void testUnionMultipleProvidersWithDuplicateBranchIdOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderInterCo = new XnecProviderInterconnection();
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(line8, line1, lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderInterCo, xnecProviderId));
        Set<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(lineFrBe)));
        assertTrue(branchList.contains(network.getBranch(lineBeFr)));
        assertTrue(branchList.contains(network.getBranch(line1)));
        assertTrue(branchList.contains(network.getBranch(line8)));
        assertEquals(4, branchList.size());
    }

    @Test
    void testUnionSingleProviderOnContingencyState() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId));
        Set<Branch> branchList = xnecProvider.getNetworkElements(lineFrBe, network);
        assertTrue(branchList.contains(network.getBranch(line1)));
        assertTrue(branchList.contains(network.getBranch(line8)));
        assertEquals(2, branchList.size());
        assertEquals(1, xnecProvider.getContingencies(network).size());
    }

    @Test
    void testUnionMultipleProvidersOnContingencyState() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line2, lineBeFr, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2));
        Set<Branch> branchList = xnecProvider.getNetworkElements(lineFrBe, network);
        assertTrue(branchList.contains(network.getBranch(line1)));
        assertTrue(branchList.contains(network.getBranch(line2)));
        assertTrue(branchList.contains(network.getBranch(line8)));
        assertTrue(branchList.contains(network.getBranch(lineBeFr)));
        assertEquals(4, branchList.size());
        assertEquals(1, xnecProvider.getContingencies(network).size());
    }

    @Test
    void testUnionMultipleProvidersOnContingencyStateDifferentContingency() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(line8))
            .addNetworkElementsAfterContingencies(Set.of(line2, lineBeFr, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2, xnecProviderId1));
        PowsyblException exception = assertThrows(PowsyblException.class, () -> xnecProvider.getNetworkElements(lineFrBe, network));
        assertEquals("Contingency 'FLOAD 11 BLOAD 11 1' definition is not unique across different providers", exception.getMessage());
        exception = assertThrows(PowsyblException.class, () -> xnecProvider.getContingencies(network));
        assertEquals("Contingency 'FLOAD 11 BLOAD 11 1' definition is not unique across different providers", exception.getMessage());
    }
}
