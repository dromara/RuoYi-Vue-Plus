---
name: prioritize
description: Select what to work on next using the right prioritization method for your context.
argument-hint: "<candidate initiatives, constraints, and decision context>"
uses:
  - prioritization-advisor
  - feature-investment-advisor
  - acquisition-channel-advisor
  - finance-based-pricing-advisor
  - recommendation-canvas
outputs:
  - Ranked options
  - Decision rationale
  - Explicit tradeoffs and follow-up actions
---

# /prioritize

Prioritize initiatives with context-aware financial and strategic rigor.

## Invocation

```text
/prioritize Q2 backlog for activation, retention, and pricing experiments
```

## Workflow

1. Choose the right framework with `prioritization-advisor`.
2. Evaluate feature-level returns using `feature-investment-advisor`.
3. Factor channel quality via `acquisition-channel-advisor`.
4. Assess pricing implications using `finance-based-pricing-advisor`.
5. Capture final recommendation in `recommendation-canvas`.

## Checkpoints

- Separate reversible from irreversible decisions.
- Identify assumptions that could flip ranking outcomes.
- Call out confidence level for each ranking decision.

## Next Steps

- Run `/discover` for top risky bets.
- Run `/plan-roadmap` for approved initiatives.
