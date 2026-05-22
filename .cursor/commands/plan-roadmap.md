---
name: plan-roadmap
description: Turn strategy and validated opportunities into a sequenced roadmap with clear tradeoffs.
argument-hint: "<time horizon, goals, and candidate initiatives>"
uses:
  - roadmap-planning
  - epic-hypothesis
  - prioritization-advisor
  - user-story-mapping
  - epic-breakdown-advisor
outputs:
  - Prioritized roadmap
  - Epic hypotheses
  - Release slices and sequencing rationale
---

# /plan-roadmap

Create a roadmap that reflects strategy, risk, and delivery reality.

## Invocation

```text
/plan-roadmap Q3-Q4 plan for enterprise reporting and permissions
```

## Workflow

1. Build roadmap context with `roadmap-planning`.
2. Convert initiatives into `epic-hypothesis` statements.
3. Select the right framework via `prioritization-advisor`.
4. Create delivery slices with `user-story-mapping`.
5. Break oversized epics with `epic-breakdown-advisor`.

## Checkpoints

- Ensure every roadmap item ties to an explicit outcome.
- Expose why items are not being prioritized.
- Capture dependencies and sequencing risk.

## Next Steps

- Run `/write-prd` for the top roadmap slice.
- Run `/discover` for high-uncertainty initiatives.
