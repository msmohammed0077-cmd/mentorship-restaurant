# Address Management Sequence Diagrams

Related documents: [Use case](./use-case.md) and [Pseudocode](./pseudocode.md).

## Add Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant S as AddressService
    participant H as AddAddressHandler
    participant Customers as CustomerRepository
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: POST /api/v1/addresses?customerId={customerId}
    C->>S: addAddress(customerId, request)
    S->>H: addAddress(customerId, request)
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
        H-->>S: AddressResponse
        S-->>C: AddressResponse
        C-->>Customer: 201 Created
    end
```

## View Addresses

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant S as AddressService
    participant H as ViewAddressesHandler
    participant Customers as CustomerRepository
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: GET /api/v1/addresses?customerId={customerId}
    C->>S: viewAddresses(customerId)
    S->>H: viewAddresses(customerId)
    H->>Customers: existsById(customerId)
    alt customer not found
        H-->>C: CustomerNotFoundException
    else customer found
        Customers-->>H: true
        H->>Addresses: findAllByCustomerIdOrdered(customerId)
        Addresses-->>H: addresses ordered default first, newest first
        H->>M: toResponseList(addresses)
        M-->>H: List<AddressResponse>
        H-->>S: List<AddressResponse>
        S-->>C: List<AddressResponse>
        C-->>Customer: 200 OK
    end
```

## Update Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant S as AddressService
    participant H as UpdateAddressHandler
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: PUT /api/v1/addresses/{addressId}?customerId={customerId}
    C->>S: updateAddress(customerId, addressId, request)
    S->>H: updateAddress(customerId, addressId, request)
    H->>Addresses: findById(addressId)
    alt address not found
        H-->>C: AddressNotFoundException
    else address belongs to another customer
        H-->>C: AddressAccessDeniedException
    else address belongs to customer
        H->>H: replace label, line, city, area, note
        H->>M: toResponse(address)
        M-->>H: AddressResponse
        H-->>S: AddressResponse
        S-->>C: AddressResponse
        C-->>Customer: 200 OK
    end
```

## Set Default Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant S as AddressService
    participant H as SetDefaultAddressHandler
    participant Addresses as AddressRepository
    participant M as AddressMapper

    Customer->>C: PUT /api/v1/addresses/{addressId}/default?customerId={customerId}
    C->>S: setDefaultAddress(customerId, addressId)
    S->>H: setDefaultAddress(customerId, addressId)
    H->>Addresses: findById(addressId)
    alt address not found
        H-->>C: AddressNotFoundException
    else address belongs to another customer
        H-->>C: AddressAccessDeniedException
    else address belongs to customer
        alt selected address is not default
            H->>Addresses: clearDefaultForCustomer(customerId)
            H->>H: address.isDefault = true
        end
        H->>M: toResponse(address)
        M-->>H: AddressResponse
        H-->>S: AddressResponse
        S-->>C: AddressResponse
        C-->>Customer: 200 OK
    end
```

## Delete Address

```mermaid
sequenceDiagram
    actor Customer
    participant C as AddressController
    participant S as AddressService
    participant H as DeleteAddressHandler
    participant Addresses as AddressRepository

    Customer->>C: DELETE /api/v1/addresses/{addressId}?customerId={customerId}
    C->>S: deleteAddress(customerId, addressId)
    S->>H: deleteAddress(customerId, addressId)
    H->>Addresses: findById(addressId)
    alt address not found
        H-->>C: AddressNotFoundException
    else address belongs to another customer
        H-->>C: AddressAccessDeniedException
    else address belongs to customer
        H->>Addresses: delete(address)
        H-->>S: void
        S-->>C: void
        C-->>Customer: 204 No Content
    end
```
