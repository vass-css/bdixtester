# BDIX Tester Usability Testing Protocol

## 1. Research questions

1. Can users reach and start a scan without coaching?
2. Do they understand availability, latency, ping and download throughput as different things?
3. Can they identify a server relevant to their ISP and the basis of its result?
4. Can they recover from timeout, unsupported, restricted, cancellation and offline/network-change outcomes?
5. Can users find, save, reopen and revisit server results?
6. Do Bengali-first and accessibility users complete the same essential tasks?

## 2. Participant groups

Recruit a varied sample rather than treating one persona as representative:

- Beginner users unfamiliar with ISP/server terminology.
- Experienced internet users who regularly troubleshoot connections.
- Bengali-first users who prefer Bangla in apps.
- Users unfamiliar with networking terms, including “ping.”
- Users with low vision, motor, hearing/cognitive accessibility needs and TalkBack experience.
- Users of lower-end Android devices or constrained mobile data.

Record language preference, network context, Android familiarity, accessibility settings and device tier with consent. Do not infer skill from age. This is a protocol; no participants or results are claimed.

## 3. Task scenarios

Use neutral prompts. Avoid explaining the interface or naming the control.

1. **Find an available server:** “You want to find a server that responds on your current connection. Show me what you would use and tell me what the result means.”
2. **Interpret failure:** Present a real/controlled timeout or unsupported state. “What do you think happened, and what would you do next?”
3. **Compare response times (only if the interface exposes comparable values):** “Which displayed result appears to have the shorter response time? How can you tell whether these results use the same measurement?” If no comparison control or comparable values are exposed, do not treat inability to sort as task failure; record this as an unmet task hypothesis for a separate demand test.
4. **Save a favorite:** “Keep this server so you can return to it later.”
5. **Find previous result:** “Find the result from your earlier test. Now show what happens when your search/filter has no match.”
6. **Distinguish measurements:** Show availability, response latency and a separately run download speed. “Which tells you whether a server responded? Which tells you download throughput?”
7. **Recover from an unsuccessful scan:** “The scan did not find a responding server. Try to understand the reason and recover.”
8. **Optional realistic transitions:** switch Wi-Fi/mobile data, cancel, background/return, open an external endpoint, return to app.

Do not fabricate timeout/server outcomes; use a test fixture or clearly controlled environment and label it.

## 4. Metrics and operational definitions

| Metric | Definition |
|---|---|
| Task completion rate | Completed without moderator intervention / attempted tasks |
| Time on task | From task prompt completion to correct goal state; report medians and range |
| Error rate | Wrong action, wrong interpretation or recoverable detour per task |
| First-click success | First intentional control/action leads toward the goal |
| Navigation success | Reaches intended destination without backtracking or moderator help |
| Abandonment | User stops before completion; capture stated reason |
| User confidence | Post-task rating (e.g. 1–5) plus “what made you confident?” |
| Comprehension accuracy | Correct explanation of the measurement/status shown |
| Accessibility task completion | Task completed with participant's usual assistive technology, without moderator action |
| Recovery success | User chooses an appropriate next action after an unsuccessful scan |

Set task-specific success criteria before sessions. Report denominators, missing data and assistance; do not convert qualitative sample outcomes into population claims.

## 5. Session protocol

1. Obtain consent; explain that the interface, not the participant, is being evaluated.
2. Record device/model/API, build, network, locale, font scale, assistive technology and test data.
3. Use think-aloud selectively; do not prompt toward a UI element.
4. Present tasks in counterbalanced order where order effects matter.
5. Capture screen and moderator notes only with consent; redact endpoints/personal data.
6. Ask confidence and interpretation questions immediately after each task.
7. Debrief; invite terminology and trust concerns.
8. Code findings by task, participant segment, severity, frequency and evidence. Keep one-off observations distinct from repeated patterns.

## 6. Accessibility-specific tasks

With users who choose their assistive technology: navigate the five destinations, start/monitor/cancel scan, identify status and latency basis, filter/search, save/open a server, and recover from error. Do not turn off a participant's usual settings. Test TalkBack speech, focus order, magnification/font scaling and switch/keyboard input on real devices separately.

## 7. Reporting

Report task outcomes and qualitative themes separately. Include task script, build/device conditions, sample characteristics (privacy-preserving), deviations, limitations, and anonymized evidence. No usability result is asserted by this protocol.

