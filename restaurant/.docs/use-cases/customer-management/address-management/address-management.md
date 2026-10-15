# Address Management Use Case

**Access:** a bearer token with `address.read` to list, `address.write` otherwise (#83 part 4).

Related documents: [Sequence diagrams](./sequence-diagrams.md) and [Pseudocode](./pseudocode.md).

## Goal

Allow a customer to manage saved delivery addresses for checkout.

This use case documents the latest merged address-management subtasks from issue #38:

| PR | Branch | Use case |
| --- | --- | --- |
| #53 | `feat/GH-42-add-address` | Add customer address |
| #54 | `feat/GH-46-set-default-address` | Set default customer address |
| #55 | `feat/GH-43-view-addresses` | List customer addresses |
| #56 | `feat/GH-44-update-address` | Update customer address |
| #57 | `feat/GH-45-delete-address` | Delete customer address |

## Actor

Customer

## Preconditions

1. Customer exists and is not soft-deleted (see
   [Delete Customer](../../use-cases/customer-management/delete-customer/delete-customer.md)).
2. The caller is a customer. A token is required (#83 part 4), and the customer is taken from it
   (#83 part 2, ADR 0009): no request names a customer. A caller who is not a customer gets 403
   "Caller is not a customer".

## Business Rules

1. A customer can store zero or more addresses.
2. Address fields are structured as label, line, city, area, and optional note.
3. `label`, `line`, `city`, and `area` are required.
4. An address created while the customer has no saved addresses automatically becomes the default.
5. An address created while the customer already has a saved address is non-default. The customer
   can later select it through the set-default operation.
6. At most one address per customer can be default. The database enforces this with a partial
   unique index on `addresses(customer_id)` where `address_is_default` is true.
7. Setting an address as default clears the previous default for that customer.
8. Updating an address does not change its default status.
9. Deleting an address is a hard delete.
10. Deleting the default address does not promote another address.
11. List responses are scoped to the caller.
12. Address-specific operations treat an `addressId` that belongs to another customer as not found.
13. A soft-deleted customer does not exist: every address operation on their addresses returns
    404 `Customer not found`. Their address rows are kept for history. Update, set-default and
    delete look up the address, not the customer, so that lookup join-fetches the address's owner
    and the owner's user in **one query**, and the service checks `userDeletedAt` in Java
    ([#78](https://github.com/msmohammed0077-cmd/mentorship-restaurant/issues/78)). The address
    checks run first and the soft-delete check last, so for an active customer the answers are
    exactly as before. A consequence is that the customer-level answer is only given for an
    address the caller owns; see *Check order for unknown and soft-deleted customers* below.

## Endpoints

| Action | Endpoint | Success |
| --- | --- | --- |
| Add address | `POST /api/v1/addresses` | `201 Created` with `AddressResponse` |
| View addresses | `GET /api/v1/addresses` | `200 OK` with address list |
| Update address | `PUT /api/v1/addresses/{addressId}` | `200 OK` with `AddressResponse` |
| Set default address | `PUT /api/v1/addresses/{addressId}/default` | `200 OK` with `AddressResponse` |
| Delete address | `DELETE /api/v1/addresses/{addressId}` | `204 No Content` |

## Main Success Scenarios

### Add Address

1. Customer submits a new address.
2. System validates the request body.
3. System loads the caller's active customer (`findActiveById`, which excludes
   soft-deleted customers).
4. System builds an `Address` for that customer.
5. System checks whether the customer already has any addresses.
6. If the customer has no saved address, System marks the new address as default; otherwise it
   leaves it non-default.
7. System saves the address.
8. System returns the saved address.

### View Addresses

1. Customer requests their saved addresses.
2. System verifies the customer exists and is not soft-deleted (`findByIdWithAddresses` filters
   `user.userDeletedAt is null`).
3. System loads all addresses for the customer.
4. System orders default address first, then remaining addresses by newest first.
5. System returns the list. Customers with no addresses receive an empty list.

### Update Address

1. Customer submits the replacement address fields for an existing address.
2. System validates the request body.
3. System loads the address by `addressId` and the caller's customer id, with its owner and the
   owner's user, in one query (`findByIdAndCustomerIdWithOwner`); an address of another customer is
   not found.
4. System verifies the owner, who is the caller, is not soft-deleted.
5. System replaces label, line, city, area, and note.
6. System returns the updated address.

### Set Default Address

1. Customer selects one address as default.
2. System loads the address by `addressId` and the caller's customer id, with its owner and the
   owner's user, in one query (`findByIdAndCustomerIdWithOwner`); an address of another customer is
   not found.
3. System verifies the owner, who is the caller, is not soft-deleted.
4. If the address is already default, System returns it unchanged.
5. Otherwise, System clears the current default address for that customer.
6. System marks the selected address as default.
7. System returns the selected address.

### Delete Address

1. Customer requests deletion of an address.
2. System loads the address by `addressId` and the caller's customer id, with its owner and the
   owner's user, in one query (`findByIdAndCustomerIdWithOwner`); an address of another customer is
   not found.
3. System verifies the owner, who is the caller, is not soft-deleted.
4. System deletes the address.
5. System returns no content.

## Exception Flows

| Condition | Result |
| --- | --- |
| Caller is not a customer | `403 Forbidden` with `Caller is not a customer` |
| Invalid `addressId` type | `400 Bad Request` with `addressId is not a valid value` |
| Blank `label`, `line`, `city`, or `area` | `400 Bad Request` with validation details |
| Soft-deleted customer adding or viewing | `404 Not Found` with `Customer not found` |
| Soft-deleted customer updating, setting default, or deleting their own address | `404 Not Found` with `Customer not found` |
| Unknown address when updating, setting default, or deleting | `404 Not Found` with `Address not found` |
| Address belongs to another customer | `404 Not Found` with `Address not found` (the lookup is scoped to the caller) |

### Check order for soft-deleted customers

Update, set-default and delete read one row — the caller's address with its owner — and decide in
this order: address found for the caller, owner not soft-deleted. The address-level answer therefore
wins whenever the address is not the caller's:

| Caller | Address | Update / set default / delete |
| --- | --- | --- |
| Soft-deleted | Their own | 404 `Customer not found` |
| Any | Does not exist, or another customer's | 404 `Address not found` |

Before the single-query lookup, a separate `existsActiveById` check ran first and answered
`Customer not found` in the last row too. Before #83 part 2, delete looked the address up unscoped
and answered another customer's address with 403 `Address belongs to another customer`; scoping it
to the caller made all three operations answer alike.

## Postconditions

1. Add address creates one `addresses` row for the customer.
2. View addresses makes no data changes.
3. Update address changes only address fields, not default status.
4. Set default address leaves at most one default address for the customer.
5. Delete address removes the selected address row and never changes another address to default.

## Data Model

| Table | Column | Purpose |
| --- | --- | --- |
| `addresses` | `address_id` | Primary key |
| `addresses` | `customer_id` | Owner customer foreign key |
| `addresses` | `address_label` | Customer-facing label, max 100 |
| `addresses` | `address_line` | Street/building details, max 255 |
| `addresses` | `address_city` | City, max 100 |
| `addresses` | `address_area` | Area, max 100 |
| `addresses` | `address_note` | Optional delivery note |
| `addresses` | `address_is_default` | Default-address flag |
| `addresses` | `address_created_at` | Creation timestamp used for ordering |
