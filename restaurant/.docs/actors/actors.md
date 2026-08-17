# Actors

_Owner: TODO · Last reviewed: TODO_

| Actor Name         | Actor Type     | Description                                                                                                               |
| ------------------ | -------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Customer           | Primary        | End user on the demand side — browses restaurants, builds and pays for orders, tracks delivery, rates the experience.     |
| Delivery Man       | Primary        | Last-mile courier — accepts delivery jobs, picks up from the restaurant, delivers to the customer, earns per trip.        |
| Customer Support   | Administrative | Internal agent who resolves issues after the fact — refunds, disputes, "where's my order," account help.                  |
| System             | Administrative | Automated, time-triggered behavior — auto-assign nearest driver, auto-cancel unaccepted orders, scheduled notifications.  |
| System Admin       | Administrative | Platform operator — manages users/restaurants, configures the platform, monitors health, handles escalations.            |
| Payment Gateway    | External       | Third-party service (Stripe, PayPal, etc.) that processes charges, payouts, and refunds via API.                          |
| Restaurant Cashier | Secondary      | Restaurant-floor staff who accepts/rejects incoming orders and updates prep status (preparing → ready).                   |
| Restaurant Owner   | Secondary      | Restaurant manager who sets up menu, pricing, and hours, and reviews sales, analytics, and payouts.                       |

<!-- A per-actor permissions/RBAC matrix would live here too — TODO. -->
