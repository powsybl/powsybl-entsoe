/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.report.ReportNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static com.powsybl.commons.test.TestUtil.normalizeLineSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author George Budau {@literal <george.budau at artelys.com>}
 */
public final class BalanceComputationAssert {

    private BalanceComputationAssert() {
    }

    public static void assertReportEquals(String refResourceName, ReportNode reportNode) throws IOException {
        assertReportEquals(BalanceComputationAssert.class.getResourceAsStream(refResourceName), reportNode);
    }

    public static void assertReportEquals(InputStream ref, ReportNode reportNode) throws IOException {
        StringWriter sw = new StringWriter();
        reportNode.print(sw);

        String refLogExport = normalizeLineSeparator(new String(ByteStreams.toByteArray(ref), StandardCharsets.UTF_8));
        String logExport = normalizeLineSeparator(sw.toString());
        assertEquals(refLogExport, logExport);
    }
}
