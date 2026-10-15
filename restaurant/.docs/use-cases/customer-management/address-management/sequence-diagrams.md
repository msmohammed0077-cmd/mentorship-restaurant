# Address Management Sequence Diagrams

Related documents: [Use case](./address-management.md) and [Pseudocode](./pseudocode.md).

The controller takes the customer from the token (`caller.customerId`, #83 part 2); a caller who is
not a customer is refused with 403 before the service runs. Soft-delete checks are left out here; the
use case lists them.

## Add Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant H as AddressService
    participant Customers as CustomerRepository
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: POST /api/v1/addresses
    C->>H: addAddress(caller.customerId, request)
    H->>Customers: findById(customerId)
    alt customer not found
        H-->>C: CustomerNotFoundException
    else customer found
        Customers-->>H: Customer
        H->>H: create Address and copy request fields
        H->>Addresses: existsByCustomer_Id(customerId)
        Addresses-->>H: hasSavedAddress
        H->>H: isDefault = NOT hasSavedAddress
        H->>Addresses: save(Address)
        Addresses-->>H: saved Address
        H->>M: toResponse(saved Address)
        M-->>H: AddressResponse
        H-->>C: AddressResponse
        C-->>Customer: 201 Created
    end
```

## View Addresses

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant H as AddressService
    participant Customers as CustomerRepository
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: GET /api/v1/addresses
    C->>H: viewAddresses(caller.customerId)
    H->>Customers: existsById(customerId)
    alt customer not found
        H-->>C: CustomerNotFoundException
    else customer found
        Customers-->>H: true
        H->>Addresses: findAllByCustomerIdOrdered(customerId)
        Addresses-->>H: addresses ordered default first, newest first
        H->>M: toResponseList(addresses)
        M-->>H: List<AddressResponse>
        H-->>C: List<AddressResponse>
        C-->>Customer: 200 OK
    end
```

## Update Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant H as AddressService
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: PUT /api/v1/addresses/{addressId}
    C->>H: updateAddress(caller.customerId, addressId, request)
    H->>Addresses: findByIdAndCustomerIdWithOwner(addressId, customerId)
    alt not found, or another customer's
        H-->>C: AddressNotFoundException (404)
    else the caller's address
        H->>H: replace label, line, city, area, note
        H->>M: toResponse(address)
        M-->>H: AddressResponse
        H-->>C: AddressResponse
        C-->>Customer: 200 OK
    end
```

## Set Default Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant H as AddressService
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: PUT /api/v1/addresses/{addressId}/default
    C->>H: setDefaultAddress(caller.customerId, addressId)
    H->>Addresses: findByIdAndCustomerIdWithOwner(addressId, customerId)
    alt not found, or another customer's
        H-->>C: AddressNotFoundException (404)
    else the caller's address
        alt selected address is not default
            H->>Addresses: clearDefaultForCustomer(customerId)
            H->>H: address.isDefault = true
        end
        H->>M: toResponse(address)
        M-->>H: AddressResponse
        H-->>C: AddressResponse
        C-->>Customer: 200 OK
    end
```

## Delete Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant H as AddressService
    participant Addresses as AddressRepository

    Customer->>C: DELETE /api/v1/addresses/{addressId}
    C->>H: deleteAddress(caller.customerId, addressId)
    H->>Addresses: findByIdAndCustomerIdWithOwner(addressId, customerId)
    alt not found, or another customer's
        H-->>C: AddressNotFoundException (404)
    else the caller's address
        H->>Addresses: delete(address)
        H-->>C: void
        C-->>Customer: 204 No Content
    end
```
