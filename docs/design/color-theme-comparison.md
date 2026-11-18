# BDIX Tester — Four-Theme Comparison Matrix

## 1. Visual & Functional Comparison

| Dimension                | Midnight Indigo _(Default)_                   | Graphite & Amber                                | Deep Emerald                             | Warm Premium                          |
| ------------------------ | --------------------------------------------- | ----------------------------------------------- | ---------------------------------------- | ------------------------------------- |
| **Visual Identity**      | Modern Technology, Crisp, Composed            | Technical Instrument, Bold, Structured          | Natural Technology, Calm, Balanced       | Editorial, Minimalist, Refined        |
| **Primary Hue Family**   | Refined Indigo (`#3A41C6` / `#939BFF`)        | Controlled Amber (`#9A4D00` / `#F5A623`)        | Deep Emerald (`#0B6B56` / `#52D6AE`)     | Deep Burgundy (`#7E1F34` / `#EB8C9E`) |
| **Secondary Hue Family** | Soft Lavender (`#5E50A6` / `#C1B5FF`)         | Neutral Graphite (`#47474D` / `#C6C3BC`)        | Muted Teal (`#1B6572` / `#7CD0DC`)       | Muted Sand (`#6F5A44` / `#D4C0A6`)    |
| **Light Canvas**         | Soft Cool White (`#F6F7FB`)                   | Warm White (`#FAF9F6`)                          | Soft Neutral Mint (`#F5F8F6`)            | Warm Ivory (`#FAF6F0`)                |
| **Dark Canvas**          | Deep Midnight Navy (`#0A0E1E`)                | Deep Graphite (`#121214`)                       | Deep Forest Charcoal (`#0B1511`)         | Deep Warm Charcoal (`#161211`)        |
| **Light Text Ink**       | Deep Navy (`#10152B`)                         | Deep Charcoal (`#1A1A1D`)                       | Deep Forest (`#0E1F17`)                  | Deep Charcoal (`#211C19`)             |
| **Dark Text Ink**        | Off-White (`#F0F2FB`)                         | Warm White (`#F4F2EE`)                          | Soft Neutral (`#ECF4EF`)                 | Soft Ivory (`#F5EFE8`)                |
| **CTA Gradient (Light)** | `#3A41C6 → #5B52D6` (`#FFFFFF` ink)           | `#9A4D00 → #AD5804` (`#FFFFFF` ink)             | `#0B6B56 → #137079` (`#FFFFFF` ink)      | `#7E1F34 → #992B44` (`#FFFFFF` ink)   |
| **CTA Gradient (Dark)**  | `#939BFF → #B5A8FF` (`#0C103E` ink)           | `#F5A623 → #E28E12` (`#241400` ink)             | `#52D6AE → #4DC7CF` (`#042C20` ink)      | `#EB8C9E → #D97287` (`#3D0916` ink)   |
| **Status Separation**    | Indigo brand vs Green/Amber/Red               | Amber CTA vs Distinct Gold Warning + Green/Red  | Emerald brand vs Botanical Green Success | Burgundy brand vs Green/Amber/Red     |
| **Best Suited For**      | Everyday diagnostics & default brand identity | High-clarity technical scanning & dark-room use | Extended low-fatigue browsing            | Minimalist editorial comfort          |

## 2. Shared Architectural Guarantees Across All Four Themes

- **Eight Full Configurations:** Every preset provides a dedicated Light `ColorScheme`, Dark `ColorScheme`, Light `BdixSemanticColors`, and Dark `BdixSemanticColors`.
- **Appearance Independence:** Each preset works seamlessly with `System Default`, `Light`, and `Dark` settings (`AppThemePreset` × `ThemePreference`).
- **Zero Legacy Overlap:** None of the four themes inherit or fall back to the removed Cyan-Sky palette or Android dynamic wallpaper colors.
- **Identical Semantic Structure:** All 4 themes implement the exact same Foundation, Status, Interaction, and Component token contract (`BdixSemanticColors` + Material 3 `ColorScheme`).
