package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.PaymentMethodAccessDeniedException;
import com.mentorship.restaurant.customer.exception.PaymentMethodNotFoundException;
import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.model.mapper.PaymentMethodMapper;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetDefaultPaymentMethodHandler {

  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentMethodMapper paymentMethodMapper;

  @Transactional
  public PaymentMethodResponse setDefaultPaymentMethod(Long customerId, Long paymentMethodId) {
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findByIdWithOwner(paymentMethodId)
            .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));

    ensurePaymentMethodBelongsToCustomer(paymentMethod, customerId);
    ensureOwnerNotDeleted(paymentMethod);

    if (!paymentMethod.isDefault()) {
      // The bulk update bypasses the persistence context, but it only touches the old default,
      // never this row, so the entity loaded above is still accurate. Setting the flag on it is
      // written by dirty checking at commit, after the old default has been cleared.
      paymentMethodRepository.clearDefaultForCustomer(customerId);
      paymentMethod.setDefault(true);
    }

    return paymentMethodMapper.toResponse(paymentMethod);
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
