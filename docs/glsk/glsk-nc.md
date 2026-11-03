# NC GLSK

GLSK in NC format are defined by the **NC profiles** (Network Codes), datasets of the CIM information model usually
exchanged as CIMXML files in a ZIP archive. In this format, the GLSK are those of the **countertrade remedial actions**:
they describe how the power adjustment of a bidding zone is shared among its generators and loads.

Two GLSK strategies are supported:

| `GLSKStrategyKind`         | `ShiftMethodKind` | GLSK                                                                                   |
|----------------------------|-------------------|----------------------------------------------------------------------------------------|
| `explicitSchedule`         | `priority`        | [Merit order](/input-data/glsk/glsk.md#merit-order-glsk): each bid is a step of the list |
| `proportionalForGenerator` | `shared`          | [Proportional to the target power](/input-data/glsk/glsk.md#proportional-to-target-power-glsk) of the generators of the zone |

Any other strategy is not supported: the remedial action is ignored and a warning is logged.

## Archive and profiles

The archive is a ZIP file with one file per profile. The importer recognises the profile of a file by the `dcat:keyword`
of its header.

| Keyword | Profile                       | Content                                                                             | Required |
|---------|-------------------------------|-------------------------------------------------------------------------------------|----------|
| `RA`    | Remedial action               | The countertrade remedial actions: strategy, shift method, bidding zone             | Yes      |
| `SIS`   | State instruction schedule    | The bids of the merit order, and the availability schedules of the remedial actions | No       |
| `SSI`   | Steady state instruction      | The availability of the remedial actions                                            | No       |

An archive can be imported if it has an RA profile with a `dcterms:creator` and a `CountertradeRemedialAction`. A
`proportionalForGenerator` remedial action needs nothing else. Each piece of data is only read from the files of its own
profile.

## Header

~~~xml
<md:FullModel rdf:about="urn:uuid:...">
  <dcterms:creator>http://energy.referencedata.eu/EIC/10XES-REE------E</dcterms:creator>
  <dcat:startDate>2029-08-12T00:00:00Z</dcat:startDate>
  <dcat:endDate>2029-08-13T00:00:00Z</dcat:endDate>
  <dcat:keyword>RA</dcat:keyword>
</md:FullModel>
~~~

- **dcterms:creator**: the operator that created the profile. It is the operator of its remedial actions.
- **dcat:startDate**, **dcat:endDate**: the period the profile is valid for. The GLSK read from a profile only apply within it.
  A profile without a window, or with an empty one, is ignored.
- **dcat:keyword**: the type of the profile (`RA`, `SIS` or `SSI`).

The importer does not know the instant a computation is run for: every GLSK it builds carries the time interval during which it
applies, and the one that contains the instant is selected when the GLSK is used.

## Remedial action (RA profile)

~~~xml
<nc:CountertradeRemedialAction rdf:ID="_RA_ID">
  <cim:IdentifiedObject.mRID>RA_ID</cim:IdentifiedObject.mRID>
  <nc:RemedialAction.normalAvailable>true</nc:RemedialAction.normalAvailable>
  <nc:PowerRemedialAction.BiddingZone rdf:resource="http://energy.referencedata.eu/EIC/10YES-REE------0"/>
  <nc:CountertradeRemedialAction.gLSKStrategy rdf:resource="http://entsoe.eu/ns/nc#GLSKStrategyKind.explicitSchedule"/>
  <nc:CountertradeRemedialAction.shiftMethod rdf:resource="http://entsoe.eu/ns/nc#ShiftMethodKind.priority"/>
</nc:CountertradeRemedialAction>
~~~

- **IdentifiedObject.mRID**: ID of the remedial action.
- **RemedialAction.normalAvailable**: availability of the remedial action (see [Availability](#availability)).
- **PowerRemedialAction.BiddingZone**: the bidding zone the GLSK applies to, referred to by its EIC code in the URL of the
  Reference Dataset (`http://energy.referencedata.eu/EIC/<EIC code>`).
- **CountertradeRemedialAction.gLSKStrategy**: the GLSK strategy, `explicitSchedule` or `proportionalForGenerator`.
- **CountertradeRemedialAction.shiftMethod**: `priority` with `explicitSchedule`, and `shared` with `proportionalForGenerator`.
  The strategy prevails: a shift method that does not go with it is only reported.

## Merit order (`explicitSchedule`)

The merit order of a zone is made of the **bids** of its remedial action, defined in the SIS profile. A bid is a
`PowerBidSchedule`, bound to one resource and with a time series of (price, power) pairs.

~~~xml
<nc:PowerBidSchedule rdf:ID="_BID_ID">
  <cim:IdentifiedObject.mRID>BID_ID</cim:IdentifiedObject.mRID>
  <nc:PowerBidSchedule.direction rdf:resource="http://entsoe.eu/ns/nc#BidDirectionKind.up"/>
  <nc:BaseTimeSeries.interpolationKind rdf:resource="http://entsoe.eu/ns/nc#TimeSeriesInterpolationKind.none"/>
  <nc:PowerBidSchedule.PowerRemedialAction rdf:resource="#_RA_ID"/>
</nc:PowerBidSchedule>
<nc:PowerBidScheduleTimePoint rdf:ID="_BID_ID_0">
  <nc:PowerBidScheduleTimePoint.PowerBidSchedule rdf:resource="#_BID_ID"/>
  <nc:PowerBidScheduleTimePoint.atTime>2029-08-12T00:00:00Z</nc:PowerBidScheduleTimePoint.atTime>
  <nc:PowerBidScheduleTimePoint.price>12.5</nc:PowerBidScheduleTimePoint.price>
  <nc:PowerBidScheduleTimePoint.p>389.57</nc:PowerBidScheduleTimePoint.p>
</nc:PowerBidScheduleTimePoint>
<nc:GLSKSchedule rdf:ID="_GLSK_ID">
  <nc:GLSKSchedule.GeneratingUnit rdf:resource="#_RESOURCE_ID"/>
</nc:GLSKSchedule>
<nc:GLSKBidActionDistribution rdf:ID="_DIST_ID">
  <nc:GLSKBidActionDistribution.PowerBidSchedule rdf:resource="#_BID_ID"/>
  <nc:GLSKBidActionDistribution.GLSKSchedule rdf:resource="#_GLSK_ID"/>
</nc:GLSKBidActionDistribution>
~~~

- **PowerBidSchedule.direction**: `up` or `down`. `upAndDown` and `stable` are not supported: the bid is ignored.
- **PowerBidSchedule.PowerRemedialAction**: the remedial action the bid belongs to, which must be an `explicitSchedule` one.
- **GLSKBidActionDistribution** and **GLSKSchedule**: they bind the bid to its resource, through the association of the
  class of the resource (`GeneratingUnit`, `PowerElectronicsUnit`, `EnergyGroup`, `EnergyBlockOrder`, `HydroPump`,
  `ScheduleResource` or `EnergyConsumer`). A bid must be bound to exactly one resource, or it is ignored.
- **PowerBidScheduleTimePoint**:
  - **atTime**: the time from which the pair takes effect (see the interpolation below).
  - **price**: the price of the bid. It only ranks the bid in the merit order, which is by ascending price.
  - **p**: the power the resource can reach: its **maximum** for an `up` bid and its **minimum** for a `down` bid. It is the
    set-point limit of the resource, and not an increment. A time point without `price` or without `p` is ignored.
- **ParticipationFactorTimePoint**: its participation factor is passed as it is to the resource of the step (1 if there is
  none). It does not change the merit order.

At an instant, the bids in force are sorted by ascending price, separately for each direction. The cheapest one is the first
step. When a shift is applied, the steps of its direction are applied in order, and each one takes what it can of what is left,
up to its limit `p`. A resource can appear in several steps (several bids on the same unit, at different prices): it is then
taken from one limit to the next.

The merit order changes over time: it is recomputed whenever a price or a power takes effect, the availability changes or
a profile starts or stops being valid. A period without any bid in force has no GLSK.

### Interpolation

The `interpolationKind` of a bid (`BaseTimeSeries.interpolationKind`) tells which time point is in force at an instant *t*:

| `interpolationKind` | Time point in force at *t*                                                            |
|---------------------|----------------------------------------------------------------------------------------|
| `none`              | The most recent one at or before *t*. None before the first time point                 |
| `next`              | The first one at or after *t*. After the last time point, the last one                 |

A missing `interpolationKind` is read as `none`. `linear` is not supported: the bid is ignored.

## Proportional (`proportionalForGenerator`)

No resource is read from the profiles. The GLSK is **proportional to the target active power of the generators** of the
country of the bidding zone, as they are in the network. A generator is used only if it is connected and in the main
synchronous component, and its shift stays within its own minimum and maximum power. The bids of the SIS profile, if there
are any, are ignored.

## Availability

A remedial action is only used while it is available. During a period when it is not, no GLSK is built for it. This is
established from the three profiles; the last one in this list prevails:

1. **RA profile**: `RemedialAction.normalAvailable`, over the validity window of the profile.
2. **SIS profile**: the availability schedule of the remedial action, a `GenericAvailableSchedule` with its
   `AvailabilityTimePoint`s (`atTime` and `available`), within the validity window of the profile. Only the interpolation `next`
   is supported (a missing kind is read as `next`); a schedule of another kind is ignored.
3. **SSI profile**: `RemedialAction.available`, over the validity window of the profile.

A remedial action that is not available during its validity window is ignored, with a warning.

## Conversion to the GLSK model

The importer translates the NC profiles into the GLSK model shared by all the formats:

| NC profiles                                       | GLSK model                                                                              |
|---------------------------------------------------|-----------------------------------------------------------------------------------------|
| A countertrade remedial action and its bidding zone | A `GlskPoint` for the zone, for each period in which nothing changes                  |
| `proportionalForGenerator`                        | One shift key of business type `B42`, a [proportional GLSK](/input-data/glsk/glsk.md#proportional-to-target-power-glsk) |
| A bid in force (direction, price, `p`)            | One shift key of business type `B45`, a step of the [merit order](/input-data/glsk/glsk.md#merit-order-glsk) |

The resources of a merit order are identified in the profiles by the mRID of their CGMES element (for example their generating
unit), which is not necessarily the id of the network element to scale. For each step, the resource is looked up in the network:

1. a generator, then a load, whose id is the mRID of the resource;
2. otherwise, the generator whose property `CGMES.GeneratingUnit` is the mRID of the resource.

A step whose resource is not found, is the generating unit of several generators, or is not in service (not connected, or
not in the main synchronous component) is ignored, with a warning, and the other steps are kept.