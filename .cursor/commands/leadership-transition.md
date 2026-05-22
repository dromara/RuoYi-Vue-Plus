---
name: leadership-transition
description: Guide PM to Director to VP/CPO transition planning with role-fit diagnostics and onboarding guidance.
argument-hint: "<current role, target role, and transition scenario>"
uses:
  - altitude-horizon-framework
  - director-readiness-advisor
  - vp-cpo-readiness-advisor
  - executive-onboarding-playbook
outputs:
  - Transition diagnosis
  - Role-readiness plan
  - 30-60-90 leadership actions
---

# /leadership-transition

Use when preparing for or navigating a product leadership step-up.

## Invocation

```text
/leadership-transition Senior PM moving into first Director role at a scaling SaaS
```

## Workflow

1. Anchor leadership model with `altitude-horizon-framework`.
2. Diagnose current readiness using `director-readiness-advisor`.
3. For executive transitions, apply `vp-cpo-readiness-advisor`.
4. Build execution plan with `executive-onboarding-playbook`.

## Checkpoints

- Identify where transition friction is actually occurring (scope, horizon, systems, narrative).
- Clarify decision rights and expectations with stakeholders.
- Define evidence-based milestones for first 30-60-90 days.

## Next Steps

- Re-run quarterly for recalibration.
- Pair with `/strategy` if you also need to reset product direction.
