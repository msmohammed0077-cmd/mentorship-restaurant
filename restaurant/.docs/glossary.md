# Glossary

The shared "ubiquitous language." When a term appears in code, Linear, or these docs, it means what it says here. One definition per term.

_Owner: TODO · Last reviewed: TODO_

<!-- Examples below show the intended format — replace/extend with your own. -->

| Term | Definition |
| --- | --- |
| Cart (Basket) | Ephemeral, editable list of items a customer is assembling before checkout. Not yet a committed order; no payment taken. |
| Order | A committed, immutable record created at checkout from a Cart. Has a lifecycle — state machines are not documented yet (TODO: `domain/state-machines.md`). |
| Open (restaurant) | A restaurant taking new business: its items can be added to a cart and checked out. A manual flag (`restaurant_is_open`) the restaurant or the admin sets, not opening hours. A new restaurant starts closed. Closing does not affect orders already placed. |
| Payout | Money the platform pays *out* to a restaurant or courier — distinct from the customer Payment coming *in*. |
| Proof of Delivery | Evidence a courier delivered the order (photo, code, signature). |
| <!-- TODO --> | |
