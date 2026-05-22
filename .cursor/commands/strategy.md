---
name: strategy
description: Build product strategy from positioning through opportunity and roadmap decisions.
argument-hint: "<product, market, and strategic question>"
uses:
  - product-strategy-session
  - positioning-workshop
  - problem-statement
  - opportunity-solution-tree
  - roadmap-planning
outputs:
  - Strategy narrative
  - Core strategic choices
  - Sequenced roadmap direction
---

# /strategy

Run an end-to-end strategy workflow with decision-quality outputs.

## Invocation

```text
/strategy B2B analytics add-on for mid-market ecommerce brands
```

## Workflow

1. Clarify customer and category with `positioning-workshop`.
2. Lock the core problem with `problem-statement`.
3. Expand options via `opportunity-solution-tree`.
4. Orchestrate a full strategy pass with `product-strategy-session`.
5. Sequence commitments using `roadmap-planning`.

## Checkpoints

- Separate strategy (choices) from execution backlog.
- Call out explicit tradeoffs and non-goals.
- Confirm metrics and leading indicators for each strategic bet.

## Next Steps

- Run `/plan-roadmap` for release-level sequencing.
- Run `/write-prd` for top-priority initiatives.
