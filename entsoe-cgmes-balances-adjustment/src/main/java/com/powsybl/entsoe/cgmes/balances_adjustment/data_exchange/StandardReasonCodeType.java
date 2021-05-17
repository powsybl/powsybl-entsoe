/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * Electronic Data Interchange.
 * The coded motivation of an act.
 *
 * https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public enum StandardReasonCodeType {

    /**
     * The time series has been rejected and replaced with a default time series profile.
     * This reason code may not be used in conjunction with A30.
     */
    A26("Default Time Series applied"),
    /**
    * This provides an indication that the time series has not got a counterpart time series.
    * In the case of an Intermediate Confirmation Report this is advising the recipient that the time series may be rejected at nomination closure if the counterpart time series is not received.
    * In the case of a Final Confirmation Report this is informing the recipient that the time series has been rejected because the counterpart time series has not been forthcoming. </Definition>
    */
    A28("Counterpart time series missing"),
    /**
     * The nominated party's time series has replaced the current time series.
     * This reason code may not be used in conjunction with A26.
     */
    A30("Imposed Time Series from nominated party’s Time Series"),
    /**
     * The message does not balance out to zero. Market rules might require that the message is rejected.
     */
    A54("Global position not in balance"),
    /**
     * The time series has been successfully matched.
     */
    A88("Time series matched"),
    /**
     * It is not possible to perform the necessary action since the required data for this action is not yet available.
     */
    B08("Data not yet available"),
    /**
     * Missing or not validated data.
     */
    B30("Data unverified"),
    /**
     * Data has successfully passed the verification process.
     */
    B31("Data verified");

    private final String description;

    StandardReasonCodeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
