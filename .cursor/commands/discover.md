---
name: discover
description: Run a structured discovery flow from problem framing through opportunity mapping and validation planning.
argument-hint: "<problem, opportunity, or feature area>"
uses:
  - discovery-process
  - problem-framing-canvas
  - discovery-interview-prep
  - opportunity-solution-tree
  - pol-probe-advisor
outputs:
  - Discovery plan
  - Prioritized assumptions
  - Validation experiment backlog
---

# /discover

Run a full discovery loop without manually stitching together skills.

## Invocation

```text
/discover Reduce onboarding drop-off for new SMB users
```

## Workflow

1. Frame the problem using `problem-framing-canvas`.
2. Plan interview and evidence gathering with `discovery-interview-prep`.
3. Map opportunities and options with `opportunity-solution-tree`.
4. Select validation probes with `pol-probe-advisor`.
5. Synthesize into a concrete execution plan using `discovery-process`.

## Checkpoints

- Confirm target user and business outcome before solutioning.
- Prioritize the top 2-3 assumptions by risk.
- Choose fast experiments before committing engineering.

## Next Steps

- Run `/write-prd` for the most promising validated solution.
- Run `/prioritize` when multiple solution paths survive validation.
