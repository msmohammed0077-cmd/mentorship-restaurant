# Use Cases

Table of contents for detailed use cases. **Each use case is its own folder** and may include its own diagrams — a sequence/flow diagram, a state machine for the entity it drives, or neither.

_Owner: TODO · Last reviewed: TODO_

> New use case? Copy [`_use-case-template.md`](./_use-case-template.md) into a new folder.

## Layout

```text
use-cases/
  _use-case-template.md              the skeleton every spec follows
  <area>/
    <use-case>/
      <use-case>.md                  the spec — folder and file share the name, kebab-case
      images/                        diagram exports; prefer inline Mermaid in the spec
      implementation-plan.md         gitignored working notes, not part of the spec
```

[`add-cart-item`](./cart-management/add-cart-item/add-cart-item.md) is the reference: match its shape rather than inventing a new one.

## Index

### Cart Management

| Use case | Spec |
| --- | --- |
| Add Cart Item | [add-cart-item](./cart-management/add-cart-item/add-cart-item.md) |
| Modify Cart | [modify-cart](./cart-management/modify-cart/modify-cart.md) |
| Remove Cart Item | [remove-cart-item](./cart-management/remove-cart-item/remove-cart-item.md) |
| View Cart | [view-cart](./cart-management/view-cart/view-cart.md) |
| Clear Cart | [clear-cart](./cart-management/clear-cart/clear-cart.md) |

### Customer Management

Tracked under [#68](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/68).

| Use case | Spec |
| --- | --- |
| Create Customer | [create-customer](./customer-management/create-customer/create-customer.md) |
| Get Customer | [get-customer](./customer-management/get-customer/get-customer.md) |
| Update Customer (profile + password) | [update-customer](./customer-management/update-customer/update-customer.md) |
| Delete Customer | [delete-customer](./customer-management/delete-customer/delete-customer.md) |
| Address Management | [address-management](./customer-management/address-management/address-management.md) |
| Payment Methods | [payment-methods](./customer-management/payment-methods/payment-methods.md) |

### Order Management

Tracked under [#34](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/34).

| Use case | Spec |
| --- | --- |
| Update Order Status | [update-order-status](./order-management/update-order-status/update-order-status.md) |
| Restaurant Accept / Reject Order | [accept-reject-order](./order-management/accept-reject-order/accept-reject-order.md) |
| View Order History | [view-order-history](./order-management/view-order-history/view-order-history.md) |

Cancel and rate orders are implemented but have no spec yet; checkout and view order detail are still open.
