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

## Permissions

Each endpoint requires one permission; Keycloak realm roles bundle them (ADR 0008). The development baseline is `restaurant/keycloak/realm-restaurant.json`; an admin can compose further roles in the Keycloak console. `ADMIN` holds every permission.

| Permission | Customer | Restaurant Owner |
|---|:-:|:-:|
| `customer.read`, `customer.write` | ✓ | |
| `address.read`, `address.write` | ✓ | |
| `payment-method.read`, `payment-method.write` | ✓ | |
| `cart.read`, `cart.write` | ✓ | |
| `order.create`, `order.cancel`, `order.rate` | ✓ | |
| `order.read` | ✓ | ✓ |
| `order.accept`, `order.reject`, `order.prepare` | | ✓ |

The other actors have no role yet. A Restaurant Cashier would be `order.read`, `order.accept`, `order.reject` and `order.prepare`, once a staff table links them to their restaurant.
