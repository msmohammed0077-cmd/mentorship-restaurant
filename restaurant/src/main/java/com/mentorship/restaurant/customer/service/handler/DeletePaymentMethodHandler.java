package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.PaymentMethodAccessDeniedException;
import com.mentorship.restaurant.customer.exception.PaymentMethodNotFoundException;
import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePaymentMethodHandler {

  private final PaymentMethodRepository paymentMethodRepository;

  /** A hard delete. Deleting the default does not promote another payment method. */
  @Transactional
  public void deletePaymentMethod(Long customerId, Long paymentMethodId) {
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findByIdWithOwner(paymentMethodId)
            .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));

    ensurePaymentMethodBelongsToCustomer(paymentMethod, customerId);
    ensureOwnerNotDeleted(paymentMethod);

    paymentMethodRepository.delete(paymentMethod);
  }

  private void ensurePaymentMethodBelongsToCustomer(PaymentMethod paymentMethod, Long customerId) {
    if (!paymentMethod.getCustomer().getId().equals(customerId)) {
      throw new PaymentMethodAccessDeniedException("Payment method belongs to another customer");
    }
  }

  /** Runs after the ownership check, so the owner is the caller: a soft-deleted caller is a 404. */
  private void ensureOwnerNotDeleted(PaymentMethod paymentMethod) {
    if (paymentMethod.getCustomer().getUser().getUserDeletedAt() != null) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
