# 04 - PseudoCode: Modify Cart

## 1. Scope

This pseudocode covers:

- Changing a cart item's quantity.
- Removing a cart item.
- Adding or changing item notes.
- Validating the branch, product, and requested quantity.
- Saving and returning the updated cart.

---

## 2. Main Cart Operation

```text
PROCEDURE ProcessCartRequest(actor, branchId, request)

    VALIDATE actor IS NOT NULL
    VALIDATE branchId IS NOT NULL
    VALIDATE request IS NOT NULL
    VALIDATE request.action IS SUPPORTED

    branch ← FindBranchById(branchId)

    IF branch DOES NOT EXIST OR branch.active = false THEN
        RETURN Error("Branch not found")
    END IF

    IF branch.is_open = false THEN
        RETURN Error("Branch is closed")
    END IF

    actorContext ← ResolveActorContext(actor)

    BEGIN TRANSACTION

        cart ← FindActiveCart(
            customerId = actorContext.customerId,
            createdByUserId = actorContext.userId,
            branchId = branchId,
            source = actorContext.source
        )

        IF cart DOES NOT EXIST THEN
            cart ← CreateCart(actorContext, branch)
        END IF

        SWITCH request.action

            CASE "CHANGE_QUANTITY":
                ChangeCartItemQuantity(
                    cart,
                    request.cartItemId,
                    request.quantity,
                    actorContext
                )

            CASE "REMOVE_ITEM":
                RemoveCartItem(cart, request.cartItemId)

            CASE "ADD_NOTES":
                UpdateCartItemNotes(
                    cart,
                    request.cartItemId,
                    request.notes,
                    actorContext
                )

            DEFAULT:
                ROLLBACK TRANSACTION
                RETURN Error("Unsupported cart action")

        END SWITCH

        RecalculateCartTotals(cart)
        TouchAuditFields(cart, actorContext)
        SaveCart(cart)

    COMMIT TRANSACTION

    updatedCart ← LoadCompleteCart(cart.id)

    RETURN Success(updatedCart)

ON ItemUnavailableException
    ROLLBACK TRANSACTION
    RETURN Error("Selected item is unavailable")

ON InvalidQuantityException
    ROLLBACK TRANSACTION
    RETURN Error("Invalid item quantity")

ON CartAccessDeniedException
    ROLLBACK TRANSACTION
    RETURN Error("Cart cannot be modified by this actor")

ON UnexpectedException
    ROLLBACK TRANSACTION
    LOG exception
    RETURN Error("Unable to update cart")

END PROCEDURE
```

---

## 3. Resolve Actor

```text
FUNCTION ResolveActorContext(actor)

    IF actor.type = "CUSTOMER" THEN
        RETURN {
            customerId: actor.customerId,
            userId: NULL,
            source: "ONLINE",
            auditUserId: actor.customerId,
            adOrgId: actor.adOrgId,
            adClientId: actor.adClientId
        }
    END IF

    IF actor.type = "RESTAURANT_STAFF" THEN
        RETURN {
            customerId: actor.customerId,
            userId: actor.userId,
            source: "RESTAURANT",
            auditUserId: actor.userId,
            adOrgId: actor.adOrgId,
            adClientId: actor.adClientId
        }
    END IF

    RAISE CartAccessDeniedException("Unsupported actor")

END FUNCTION
```

---

## 4. Create Cart

```text
FUNCTION CreateCart(actorContext, branch)

    cart ← NEW CART

    cart.id ← GeneratePrimaryKey()
    cart.uuid ← GenerateUUID()
    cart.customer_id ← actorContext.customerId
    cart.branch_id ← branch.id
    cart.created_by_user_id ← actorContext.userId
    cart.source ← actorContext.source
    cart.status ← "ACTIVE"
    cart.subtotal ← 0
    cart.total ← 0

    InitializeAuditFields(cart, actorContext)
    SaveCart(cart)

    RETURN cart

END FUNCTION
```

---

## 5. Change Item Quantity

```text
PROCEDURE ChangeCartItemQuantity(
    cart,
    cartItemId,
    newQuantity,
    actorContext
)

    VALIDATE newQuantity > 0

    cartItem ← FindCartItemById(cartItemId)

    ValidateCartItemOwnership(cart, cartItem)

    product ← FindProductById(cartItem.product_id)

    IF product DOES NOT EXIST
       OR product.active = false
       OR product.is_available = false THEN
        RAISE ItemUnavailableException
    END IF

    CheckRequestedQuantityAvailability(
        branchId = cart.branch_id,
        productId = product.id,
        requestedQuantity = newQuantity
    )

    cartItem.quantity ← newQuantity
    TouchAuditFields(cartItem, actorContext)
    SaveCartItem(cartItem)

END PROCEDURE
```

---

## 6. Remove Item

```text
PROCEDURE RemoveCartItem(cart, cartItemId)

    cartItem ← FindCartItemById(cartItemId)

    ValidateCartItemOwnership(cart, cartItem)

    DeleteOrDeactivateCartItem(cartItem.id)

END PROCEDURE
```

---

## 7. Add or Change Notes

```text
PROCEDURE UpdateCartItemNotes(
    cart,
    cartItemId,
    notes,
    actorContext
)

    cartItem ← FindCartItemById(cartItemId)

    ValidateCartItemOwnership(cart, cartItem)

    cartItem.notes ← NormalizeNotes(notes)
    TouchAuditFields(cartItem, actorContext)
    SaveCartItem(cartItem)

END PROCEDURE
```

---

## 8. Recalculate Cart Totals

```text
PROCEDURE RecalculateCartTotals(cart)

    subtotal ← 0

    cartItems ← FindActiveCartItems(cart.id)

    FOR EACH cartItem IN cartItems

        lineTotal ← cartItem.quantity × cartItem.unit_price
        subtotal ← subtotal + lineTotal

    END FOR

    cart.subtotal ← RoundMoney(subtotal)
    cart.total ← cart.subtotal

END PROCEDURE
```

---

## 9. Ownership Validation

```text
PROCEDURE ValidateCartItemOwnership(cart, cartItem)

    IF cartItem DOES NOT EXIST
       OR cartItem.active = false THEN
        RAISE Error("Cart item not found")
    END IF

    IF cartItem.cart_id ≠ cart.id THEN
        RAISE CartAccessDeniedException
    END IF

END PROCEDURE
```

---

## 10. Audit Fields

```text
PROCEDURE InitializeAuditFields(entity, actorContext)

    currentTime ← CurrentTimestamp()

    entity.created ← currentTime
    entity.createdby ← actorContext.auditUserId
    entity.updated ← currentTime
    entity.updatedby ← actorContext.auditUserId
    entity.active ← true
    entity.ad_org_id ← actorContext.adOrgId
    entity.ad_client_id ← actorContext.adClientId

END PROCEDURE
```

```text
PROCEDURE TouchAuditFields(entity, actorContext)

    entity.updated ← CurrentTimestamp()
    entity.updatedby ← actorContext.auditUserId

END PROCEDURE
```

---

## 11. Expected Result

```text
SUCCESS RESPONSE

{
    cartId,
    uuid,
    source,
    status,
    customer,
    branch,
    items: [
        {
            cartItemId,
            product,
            quantity,
            unitPrice,
            notes,
            lineTotal
        }
    ],
    subtotal,
    total
}
```
