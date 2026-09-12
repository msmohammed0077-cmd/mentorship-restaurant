# Address Management Pseudocode

Related documents: [Use case](./use-case.md) and [Sequence diagrams](./sequence-diagrams.md).

## Add Address

```text
PROCEDURE AddAddress(customerId, request)

    VALIDATE request.label IS NOT BLANK AND LENGTH <= 100
    VALIDATE request.line IS NOT BLANK AND LENGTH <= 255
    VALIDATE request.city IS NOT BLANK AND LENGTH <= 100
    VALIDATE request.area IS NOT BLANK AND LENGTH <= 100

    customer = customerRepository.findById(customerId)

    IF customer DOES NOT EXIST THEN
        RAISE CustomerNotFoundException("Customer not found")
    END IF

    address = new Address()
    address.customer = customer
    address.label = request.label
    address.line = request.line
    address.city = request.city
    address.area = request.area
    address.note = request.note
    address.isDefault = NOT addressRepository.existsByCustomer_Id(customerId)

    savedAddress = addressRepository.save(address)

    RETURN AddressResponse(savedAddress)

END PROCEDURE
```

## View Addresses

```text
PROCEDURE ViewAddresses(customerId)

    IF customerRepository.existsById(customerId) IS FALSE THEN
        RAISE CustomerNotFoundException("Customer not found")
    END IF

    addresses = addressRepository.findAllByCustomerIdOrdered(customerId)

    RETURN addresses MAPPED TO AddressResponse

END PROCEDURE
```

## Update Address

```text
PROCEDURE UpdateAddress(customerId, addressId, request)

    VALIDATE request.label IS NOT BLANK AND LENGTH <= 100
    VALIDATE request.line IS NOT BLANK AND LENGTH <= 255
    VALIDATE request.city IS NOT BLANK AND LENGTH <= 100
    VALIDATE request.area IS NOT BLANK AND LENGTH <= 100

    address = addressRepository.findById(addressId)

    IF address DOES NOT EXIST THEN
        RAISE AddressNotFoundException("Address not found")
    END IF

    IF address.customer.id != customerId THEN
        RAISE AddressAccessDeniedException("Address belongs to another customer")
    END IF

    address.label = request.label
    address.line = request.line
    address.city = request.city
    address.area = request.area
    address.note = request.note

    RETURN AddressResponse(address)

END PROCEDURE
```

## Set Default Address

```text
PROCEDURE SetDefaultAddress(customerId, addressId)

    address = addressRepository.findById(addressId)

    IF address DOES NOT EXIST THEN
        RAISE AddressNotFoundException("Address not found")
    END IF

    IF address.customer.id != customerId THEN
        RAISE AddressAccessDeniedException("Address belongs to another customer")
    END IF

    IF address.isDefault IS FALSE THEN
        addressRepository.clearDefaultForCustomer(customerId)
        address.isDefault = true
    END IF

    RETURN AddressResponse(address)

END PROCEDURE
```

## Delete Address

```text
PROCEDURE DeleteAddress(customerId, addressId)

    address = addressRepository.findById(addressId)

    IF address DOES NOT EXIST THEN
        RAISE AddressNotFoundException("Address not found")
    END IF

    IF address.customer.id != customerId THEN
        RAISE AddressAccessDeniedException("Address belongs to another customer")
    END IF

    addressRepository.delete(address)

    RETURN no content

END PROCEDURE
```
