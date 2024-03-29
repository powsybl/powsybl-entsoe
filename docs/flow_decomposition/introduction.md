# Introduction

Flow decomposition methodology has been formerly described in a decision [published by ACER](https://eepublicdownloads.entsoe.eu/clean-documents/nc-tasks/CORE%20-%2074%20-%20ACER%20decision%20-%20Annex%201.pdf) (European Union Agency for the Cooperation of Energy Regulators).

European power system is based on zonal management. Flow decomposition is a tool designed to give insights on
impacts of internal and cross zonal exchange of power on the flows (and associated constraints) on the network devices.
It is an important part of the cost sharing methodology for remedial actions costs sharing between TSOs.

The aim of flow decomposition algorithm is to provide for each network element a decomposition of the active flow into different parts:
- Allocated flow: flow due to electricity market exchanges. This includes import/export flows and transit flows.
- Internal flow: flow due to electricity exchange inside the network element's zone.
- Loop flow: flow due to electricity exchange inside another zone.
- PST flow: flow due to a shift commanded by the action of an active phase shifting transformer on the network.
- Xnode flow: flow due to all unmerged interconnections and HVDC connections modelled as dangling lines in IIDM.

> This decomposition does not reflect the exact reality of how electric flows act on a real network, but it
> is a useful approximation needed for some cross zonal coordination processes.