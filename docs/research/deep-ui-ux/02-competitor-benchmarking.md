# Competitor Benchmarking

**Evidence class:** Publisher-provided public pages and store listings, not hands-on tests. All claims below are publisher-provided; none are independently verified app behavior.  
**Accessed:** 2026-11-18. Competitor evidence IDs and exact URLs: [14-research-evidence-register.md](./14-research-evidence-register.md).

## 1. Scope and evidence rules

Fifteen products/services were included: the nine named diagnostics products, five Bangladesh-focused products discovered through Google Play, and X-plore as an FTP/file-server analogue. Evidence was limited to the public pages in the register. No app was installed; no interaction workflow, screenshot design, accessibility, localization quality, user review trend, or performance was directly tested. A publisher claim means “the listing says it offers this,” not that the current build was verified.

| Product / source | Evidence reviewed | Publicly stated focus | BDIX Tester relevance | Evidence status |
|---|---|---|---|---|
| [Ookla Speedtest](https://www.speedtest.net/apps/android) | Official product page (`COMP-001`) | Internet speed and network testing | Separate test metrics are a hypothesis; not a BDIX-server browser | Publisher-provided, retrieved; workflow unverified |
| [FAST.com](https://fast.com/) | Official site (`COMP-002`) | Speed-test service | Different task; no competitor workflow inference | Publisher-provided, retrieved |
| [Meteor](https://play.google.com/store/apps/details?id=meteor.test.and.grade.internet.connection.speed) | Google Play (`COMP-003`) | Listing claims speed/network and app-performance information | Result explanation is a pattern hypothesis, not a verified UI feature | Publisher-provided store listing, retrieved |
| [Opensignal](https://www.opensignal.com/apps) | Official product page (`COMP-004`) | Coverage, speed/video tests and signal maps | Contrast with server reachability; not a direct equivalent | Publisher-provided, retrieved |
| [nPerf](https://www.nperf.com/en/) and [Android listing](https://play.google.com/store/apps/details?id=com.nperf.tester) (`COMP-005`, `COMP-006`) | Official site and Play listing | Official site links to the Android application; listing is separate evidence | Do not infer feature equivalence from an app link | Publisher-provided pages, retrieved; workflow unverified |
| [Network Analyzer Lite](https://play.google.com/store/apps/details?id=net.techet.netanalyzerlite.an) | Google Play (`COMP-007`) | Listing describes network tools | Diagnostic breadth is a category-level comparison only | Publisher-provided store listing, retrieved |
| [Fing](https://www.fing.com/app/) and [Google Play](https://play.google.com/store/apps/details?id=com.overlook.android.fing) (`COMP-008`, `COMP-009`) | Official product page and Play listing | Official page claims network discovery/data features | Discovery is adjacent; app workflows unverified | Publisher-provided pages, retrieved |
| [WiFiman](https://play.google.com/store/apps/details?id=com.ubnt.usurvey) | Google Play (`COMP-010`) | Listing description only | Network utility category comparison | Publisher-provided store listing, retrieved |
| [HE.NET Network Tools](https://play.google.com/store/apps/details?id=net.he.networktools) | Google Play (`COMP-011`) | Listing description only | Network tool taxonomy comparison | Publisher-provided store listing, retrieved |
| [BDIX Tester: BD Movie servers](https://play.google.com/store/apps/details?id=com.shubhobrata.roy.bdixtester) | Google Play (`COMP-012`) | Listing-level Bangladesh/local-server testing claim | Market claim only; not this repository’s application | Publisher-provided store listing, retrieved |
| [BDIX Finder](https://play.google.com/store/apps/details?id=com.shazin.bdixfinder) | Google Play (`COMP-013`) | Listing claims BDIX discovery and ISP-related testing | Local category language only | Publisher-provided store listing, retrieved |
| [BDIX Server: ISP Server Tester](https://play.google.com/store/apps/details?id=com.devsground.bdix) | Google Play (`COMP-014`) | Listing-level ISP/server-testing claim | Closest stated task overlap; not independently verified | Publisher-provided store listing, retrieved |
| [BDIX Network Tester](https://play.google.com/store/apps/details?id=bdix.tester) | Google Play (`COMP-015`) | Listing-level network endpoint testing claim | Local category language only | Publisher-provided store listing, retrieved |
| [LAN Streamer - BDIX Server](https://play.google.com/store/apps/details?id=com.niloythings.lanstreamer) | Google Play (`COMP-016`) | Listing describes local-media/ISP finder | Adjacent media discovery; not assumed to be a speed test | Publisher-provided store listing, retrieved |
| [X-plore File Manager](https://www.lonelycatgames.com/apps/xplore) | Official product page (`COMP-017`) | Official page describes file-manager/network protocol support including FTP | Adjacent endpoint handoff only | Publisher-provided, retrieved; workflow unverified |

**Counting note:** The rows above total 15 products; nPerf is one product supported by two source pages. There are 16 `COMP` source records because the local/product evidence includes BDIX Tester, BDIX Finder, BDIX Server: ISP Server Tester, BDIX Network Tester and LAN Streamer, while the named international set includes separate Meteor and Opensignal products.

## 2. Comparative capability evidence

Legend: **Claimed** = public page says so; **not established** = the evidence does not establish support. It is not a negative feature finding.

| Product | Diagnostic measure / discovery | Result/history | ISP or server specificity | Actual workflow evidence |
|---|---|---|---|---|
| Ookla | Internet speed testing claimed | Result focus stated | BDIX catalog not established | None |
| FAST.com | Download speed; help describes optional expanded results | Measurement focus stated | BDIX catalog not established | Documentation only |
| Meteor | Mobile network/app-experience framing | Experience focus stated | BDIX catalog not established | None |
| Opensignal | Coverage/network experience | Coverage focus stated | BDIX catalog not established | None |
| nPerf | Speed, browsing/streaming tests stated | Test-result focus stated | BDIX catalog not established | Product page only |
| Network Analyzer | Network diagnostic utilities stated | Not established | Local network tools, not BDIX catalog evidence | Play description only |
| Fing | Network/device discovery stated | Not established | Device/network rather than BDIX server mapping | Official page/listing |
| WiFiman | Wi-Fi tools stated | Not established | No BDIX catalog evidence | Play description only |
| HE.NET | Network tools stated | Not established | No BDIX catalog evidence | Play description only |
| BDIX Tester | Listing describes BDIX/ISP server checks | Listing-level claims only | Bangladesh/ISP focus claimed | None |
| BDIX Finder | Listing describes BDIX server finding | Listing-level claims only | Local discovery focus claimed | None |
| BDIX Server | Listing describes ISP/server checking and history | Listing-level claims only | ISP focus claimed | None |
| BDIX Network Tester | Listing describes network/server test | Listing-level claims only | Bangladesh-specific title/description | None |
| LAN Streamer | Listing describes local server/media discovery | Listing-level claims only | Local media/server adjacency | None |
| X-plore | FTP and other file access protocols on official page | Not a diagnostic history tool | FTP endpoint access | Official feature documentation only |

## 3. Reusable pattern hypotheses (not verified interface findings)

These are candidates for targeted inspection or testing, not assertions about exact visual implementation.

| Pattern hypothesis | Problem it may solve | Is it relevant? | Possible BDIX adaptation | Do not copy | Supporting evidence |
|---|---|---|---|---|---|
| Separate measurement names and result context | Users may mistake latency for throughput or general Internet quality | Relevant to `primaryLatencyMs` semantics | If helpful, name protocol and measurement basis; distinguish availability, response latency, and throughput | Speedometer/gauge conventions that imply a throughput test where none ran | `COMP-001`–`COMP-006`; app finding `BDIX-001` |
| Keep the primary diagnostic action easy to identify | Utility users need a clear “start” affordance | Existing scan entry points are present | Preserve the current scan flow and five destinations; no extra root destination indicated | Copying a competitor dashboard or adding navigation without task evidence | `COMP-001`–`COMP-006`; app finding `NAV-001` |
| Explain discovery in local ISP/server terms | General diagnostics do not answer every local server-discovery need | Potentially relevant | Consider existing search/filter affordances only if users need them | Treating competitor store claims as proven catalog accuracy or proof of local user needs | `COMP-012`–`COMP-016`; app findings `UX-001`, `UX-007`, `BDIX-002` |
| Preserve external handoff behavior | Users may open endpoints in another app | Existing app decision to preserve | Keep the current direct external open; do not infer competitor workflow | Assuming all endpoints are valid or that a competitor supports the same handoff | `COMP-017`; app finding `UX-002` |
| Group multiple diagnostic tools by task | Broad network utilities can be hard to scan | Category-level comparison only | No category redesign recommendation; preserve the existing catalogue | Importing unrelated utility taxonomies | `COMP-007`, `COMP-011`; app findings `UX-001`, `BDIX-002` |
| Persist prior results | Repeated testing may benefit from comparison and recovery | App has History; value of comparison untested | Preserve History; targeted filtered-empty copy correction only | Assuming listing history claims show good UX | `COMP-014` as a public claim only; app findings `UX-003`, `UX-006` |

## 4. Limits and conclusion

No evidence supports a visual ranking of competitor dashboard, typography, color, iconography, screen reader support, touch targets, or navigation. Public screenshot links exposed by storefront listings were not visually opened. Store page descriptions, publisher documentation, screenshots, and hands-on inspection are different evidence classes and are not conflated here. User ratings/reviews were not systematically sampled. Every feature statement is publisher-provided or store-provided; competitor runtime behavior is unavailable/unverified.

The strongest transferable lesson is not a competitor's visual style: keep the exact measure, server identity, status, and next action legible. BDIX Tester must preserve its distinct ISP-to-server discovery and external-open tasks rather than imitate broad Internet speed-test products.

