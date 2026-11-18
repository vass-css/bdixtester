# BDIX Tester — Color Theme Research & Visual Direction Analysis

## 1. Purpose & Context

BDIX Tester is a local-first Android network diagnostic utility tailored for Bangladesh broadband users (BDIX FTP, Movie, Live TV, and 26 OTT platforms), supporting English and Bangla (`bn`) locales across Light and Dark appearances. Replacing the previous single-identity Cyan-Sky palette required establishing four distinct, cohesive, production-grade visual identities that balance diagnostic clarity with refined aesthetic range.

## 2. Research & Aesthetic Direction Synthesis

### Theme 01 — Midnight Indigo (Default & Migration Target)

- **Visual Identity:** Premium, sophisticated, modern technology.
- **Color Direction:** Deep Midnight Navy, Refined Indigo, Soft Lavender, Cool Neutral Surfaces.
- **Light Mode Atmosphere:** Soft cool white canvas (`#F6F7FB`) paired with pale indigo containers (`#ECEFFA`), crisp elevated white cards (`#FFFFFF`), deep midnight navy typography (`#10152B`), and high-contrast refined indigo primary actions (`#3A41C6`).
- **Dark Mode Atmosphere:** Deep midnight navy backdrop (`#0A0E1E`) avoiding pure `#000000` crush, layered navy surfaces (`#10152B` → `#252F54`), luminous indigo primary (`#939BFF`) paired with dark navy ink (`#0C103E`), and soft lavender secondary accents (`#C1B5FF`).
- **Rationale:** Serves as the flagship default and legacy migration target because indigo/navy conveys network reliability and precision while keeping diagnostic status colors (emerald green, amber, crimson) visually unmistakable.

### Theme 02 — Graphite & Amber

- **Visual Identity:** Professional, technical, bold, refined instrument.
- **Color Direction:** Charcoal, Graphite, Warm White, Controlled Amber, Neutral Gray.
- **Light Mode Atmosphere:** Warm off-white background (`#FAF9F6`) with soft stone-graphite containers (`#F1EFE9`), deep charcoal text (`#1A1A1D`), neutral graphite secondary controls (`#47474D`), and controlled deep amber primary accents (`#9A4D00` — calibrated to avoid harsh saturated yellow).
- **Dark Mode Atmosphere:** Deep graphite canvas (`#121214`) with stepped charcoal layers (`#19191C` → `#323238`), warm off-white text (`#F4F2EE`), neutral silver-graphite secondary tokens (`#C6C3BC`), and balanced warm amber (`#F5A623` paired with `#241400` content ink) reserved for primary CTAs, active indicators, and key telemetry accents.
- **Rationale:** Appeals to power users and network engineers who prefer an industrial hardware-instrument aesthetic with restrained amber highlights.

### Theme 03 — Deep Emerald

- **Visual Identity:** Calm, refined, natural technology.
- **Color Direction:** Forest Green, Deep Emerald, Muted Teal, Neutral Slate, Soft Green-Slate Surfaces.
- **Light Mode Atmosphere:** Soft neutral-mint canvas (`#F5F8F6`), subtle green-tinted surfaces (`#EAF1ED`), deep forest typography (`#0E1F17`), controlled deep emerald-pine primary (`#0B6B56`), and muted slate-teal secondary (`#1B6572`).
- **Dark Mode Atmosphere:** Deep forest-charcoal background (`#0B1511`), layered slate-green surfaces (`#111E19` → `#263D34`), crisp off-white typography (`#ECF4EF`), and balanced emerald accent (`#52D6AE` with `#042C20` ink).
- **Brand vs. Status Separation Strategy:** To ensure brand primary emerald never confuses users when reading server status, **Reachable / Success** badges use distinct botanical/leaf green tokens (`#166F2D` / `#D4F5DC` in Light, `#6CE284` / `#114B1F` in Dark) and **always** combine color with explicit iconography (`Icons.Rounded.CheckCircle`) and text labels.

### Theme 04 — Warm Premium

- **Visual Identity:** Elegant, distinctive, minimalist editorial refinement.
- **Color Direction:** Warm Ivory, Soft Stone, Muted Sand, Deep Burgundy, Warm Neutral Gray.
- **Light Mode Atmosphere:** Warm ivory background (`#FAF6F0`), soft stone surfaces (`#F1EAE0`) with crisp warm-white cards (`#FFFEFC`), deep charcoal ink (`#211C19`), deep burgundy primary (`#7E1F34`), and muted sand secondary (`#6F5A44`) without excessive brown saturation or ornamental gradients.
- **Dark Mode Atmosphere:** Deep warm charcoal canvas (`#161211`), warm slate surfaces (`#1E1918` → `#3B312E`), soft ivory typography (`#F5EFE8`), refined burgundy-rose accent (`#EB8C9E` with `#3D0916` ink), and warm sand secondary (`#D4C0A6`).
- **Rationale:** Delivers a calm, editorial reading experience for extended browsing across 750+ BDIX servers and OTT directories.

## 3. Cross-Cutting Design Decisions

- **Dynamic Color Disabled:** Material You wallpaper extraction is permanently disabled so all four themes preserve exact chromatic harmony and deterministic WCAG 2.2 AA compliance.
- **Two-Tone Gradients + Flat Tonal Cards:** Primary CTAs and the hero scan gauge use subtle two-tone theme gradients derived solely from each theme's tokens, while server cards, settings groups, and metric tiles use clean flat tonal surfaces with hairline borders.
- **Semantic Consistency Across Locales:** Typography colors and container contrast ratios are validated for both Latin (`Plus Jakarta Sans` / `JetBrains Mono`) and Bengali (`Noto Sans Bengali`) rendering.
