# Design Direction Decision

## Decision

**MINOR TARGETED IMPROVEMENTS RECOMMENDED. NO REDESIGN REQUIRED.**

Preserve the current visual identity, screen layouts, five-destination navigation, and existing features. The source and test evidence reviewed does not justify a broad redesign, new navigation destination, or feature expansion.

The previous weighted scoring exercise selected “Minimal task-focused diagnostic UI” using subjective scores that were not validated with users. Those scores do not establish that the current design is deficient and are withdrawn as a basis for a design decision. This document replaces that speculative ranking.

## What is already working

- The root navigation has five destinations and is covered by Compose navigation tests.
- Scan initiation is available from Home and Servers. The separate `ScanScreen` is not a root destination, but this does not make the core scan task inaccessible (`NAV-001`).
- The product retains direct external opening rather than an unrequested details page.
- English and Bengali string resources have matching 305-key sets.
- The source uses a centralized theme/typography setup and Material 3. Source inspection does not prove rendered visual or accessibility conformance.
- Existing UI tests cover selected locale rendering, navigation, and external-open behavior; they do not substitute for device or participant testing.

## Supported targeted corrections

1. Distinguish History with no records from History with no matching filter result (`UX-003`).
2. Correct Saved empty-state wording that refers to a details screen not present in the tested navigation (`UX-002`).
3. Localize dynamic enum/status labels and accessibility descriptions, and use locale-aware History formatting (`LOC-001`, `A11Y-001`). Validate wording, speech, and rendering on devices before claiming localization or accessibility completeness.

These are focused content/state improvements; the reports do not prescribe a layout or navigation replacement. Other observations—including adding a sort control, changing search hints, introducing per-row Undo, and changing grouped status labels—remain optional pending user evidence.

## Explicitly not recommended on current evidence

- Do not add a sixth Scan tab or a separate Scan route just because a `ScanScreen` composable exists.
- Do not replace the five-destination structure, add a drawer, or create a full-screen Server Details destination.
- Do not change colors, typography, screen hierarchy, or component style without a rendered defect or tested task need.
- Do not claim a ping-measurement defect: the primary value combines protocol-specific timings, while the UI says “Latency,” not “Ping” (`BDIX-001`).
- Do not add durable background work or notifications unless a product requirement and lifecycle test establish the need (`BDIX-003`).
- Do not represent untested TalkBack, touch-target, visual contrast, low-end performance, or competitor-app behavior as defects.

## Decision conditions and regression risks

Revisit the direction only if a reproducible defect, applicable accessibility failure, or task study shows that an existing task is materially blocked. Any targeted change must preserve current scan entry points, direct external-open behavior, existing theme preference, and five-destination navigation. Validate resource formatting in both locales, filtered and truly empty History states, and existing navigation/scan behavior to avoid regressions.

The evidence, source locations, confidence, severity, priority, and limitations are recorded in [14-research-evidence-register.md](./14-research-evidence-register.md). The recommendation inventory is in [13-final-prioritized-recommendations.md](./13-final-prioritized-recommendations.md).
