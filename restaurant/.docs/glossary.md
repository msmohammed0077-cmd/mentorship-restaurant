# Glossary

The shared "ubiquitous language." When a term appears in code, Linear, or these docs, it means what it says here. One definition per term.

_Owner: TODO · Last reviewed: TODO_

<!-- Examples below show the intended format — replace/extend with your own. -->

| Term | Definition |
| --- | --- |
| Cart (Basket) | Ephemeral, editable list of items a customer is assembling before checkout. Not yet a committed order; no payment taken. |
| Order | A committed, immutable record created at checkout from a Cart. Has a lifecycle — see [state machines](./domain/state-machines.md). |
| Payout | Money the platform pays *out* to a restaurant or courier — distinct from the customer Payment coming *in*. |
| Proof of Delivery | Evidence a courier delivered the order (photo, code, signature). |
| <!-- TODO --> | |
