# BDIX Tester — Accessibility ও Inclusive Design Audit

**তারিখ:** 2026-11-18 | **মানদণ্ড:** WCAG 2.2 AA-কে native Android design-এর সঙ্গে মিলিয়ে দেখা হয়েছে; WCAG web pixel rule-কে Android dp-এর সমার্থক ধরা হয়নি।

## Summary

Source-level accessibility foundation ভালো: Material 3 controls, semantic status colors, content descriptions, visible navigation labels, LTR wrappers, 48dp chips ও reduce-motion preference আছে। Theme test-এর finite contrast pairs এবং Bengali UI smoke test pass করেছে। **TalkBack, physical touch, rendered contrast, large font, low vision, switch access বা device test করা হয়নি**; WCAG conformance declaration দেওয়া যাচ্ছে না.

## WCAG/Android assessment

| Area | Evidence / status | Finding |
|---|---|---|
| 1.4.3 Contrast minimum | `ThemeSystemContrastAndMigrationTest` 8 theme-এ বহু text pair ≥4.5:1, UI pair ≥3:1 assert করে; pass | Verified সীমিত token coverage; rendered alpha/composite/focus/disabled/screens not covered |
| 1.4.11 Non-text contrast | outlines/input/ring token pair test আছে | সব icon/selected/disabled state screenshot-এ যাচাই হয়নি |
| 1.4.1 Use of color | StatusBadge icon + text + semantic color | ইতিবাচক; “Not Working” broad category পৃথক state বুঝতে বাধা দিতে পারে (`BDX-05`) |
| 1.4.4 Resize text / 1.4.10 Reflow | Typography `sp`; content vertical scroll; name/URL ellipsis | 1.3x/2.0x ও 320dp-এ clipping, focus order runtime test হয়নি |
| 2.5.8 Target Size (Minimum) | WCAG 2.2 web target 24×24 CSS px; native Android target aim 48dp; chips 48dp, Settings rows 64dp, icon buttons `minimumInteractiveComponentSize` | Source positive; actual hit bounds/adjacency physical device-এ মাপা হয়নি |
| 2.4.7 / 2.4.11 Focus | Compose Material controls ব্যবহার | Keyboard/switch focus indication/sequence manual verify বাকি |
| 4.1.2 Name/role/value | clickable row-তে `Role.Button`, switches, URL action labels, status semantics আছে | `StatusBadge` hard-coded English `contentDescription` (`BDX-04`); full TalkBack audit বাকি |
| 4.1.3 Status messages | Home scan progress/current endpoint live region | Speech over-announcement hypothesis (`BDX-13`); TalkBack test দরকার |
| Motion | custom Reduce Animations preference; Home active motion gated; status dot reduced-motion মানে | system animation/accessibility preference ও সব decorative animation end-to-end যাচাই হয়নি |

## Bengali typography ও bilingual layout

- `Type.kt`: Bengali family Noto Sans Bengali, `disableLetterSpacing=true`, Bengali `lineHeightScale=1.14`; English Plus Jakarta Sans, numeric/URL JetBrains Mono।
- `fc-scan`-এ তিন bundled TTF-ই static Regular metadata; medium/bold synthetic rendering-এর বাস্তব ফল screenshot/device-এ দেখা হয়নি।
- `LtrContent` URL/IP/metrics-কে LTR রাখে; test UI-তে `bn-rBD` qualifier-এ Home/Servers/OTT/Settings crash ছাড়া render হয়েছে।
- **Verified gap (`BDX-04`):** enum-driven `TestStatus`, `ServerProtocol`, `ServerCategory`, `SyncStatus` visible English; StatusBadge semantics English; History date `Locale.US`।
- **Validation:** Bengali long server names, transliteration, mixed-direction punctuation, numeric grouping, font scale 2.0, keyboard input, system locale change।

## Good existing practice

- Visible nav label এবং selected-state semantics আছে; 5 navigation items UI test-এ দেখা যায়।
- URL tap-এর localized `onClickLabel`, “open in external browser”; icon-only toolbar actions-এর localized descriptions।
- Haptic feedback action-এর অতিরিক্ত feedback, primary action-কে একমাত্র color cue করা হয়নি।
- 48dp/64dp source-level touch targets; no detail page navigation surprise—external open test-verified।
- Reduced motion scan ring-এর repeated animation কমায়।

## Required manual/automated test set (পরবর্তী অনুমোদিত implementation-এর পর)

1. TalkBack: Home idle/running/completed, scan control, each status chip, server card/bookmark/open, saved/history empty/no-match, destructive confirm, sync states।
2. 200% font/Display size; বাংলা + English/URL + long labels; portrait, landscape, split-screen; clipping/overlap/focus visibility।
3. Accessibility Scanner/Compose semantics tree; touch target bounds ও keyboard/switch navigation।
4. Dark/light সব preset-এ rendered screenshot contrast: text, icon, outline, focus, disabled, pressed, alpha overlays।
5. Reduced-motion ON/OFF ও Android system motion settings; scan live region announcements count।
6. Bengali UI assertions শুধু node উপস্থিত নয়—localized visible text/semantics ও date value assert করবে।

## Cross-reference

`BDX-04` localization/semantics; `BDX-05` state meaning; `BDX-13` live-region hypothesis; `BDX-02` notification/OS permission; `BDX-14` QA documentation drift. Finding register-এর priority/confidence বিস্তারিত `complete-ux-audit.md`-এ।
