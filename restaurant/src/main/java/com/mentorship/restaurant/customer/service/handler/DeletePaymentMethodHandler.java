package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.PaymentMethodAccessDeniedException;
import com.mentorship.restaurant.customer.exception.PaymentMethodNotFoundException;
import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePaymentMethodHandler {

  private final CustomerRepository customerRepository;
  private final PaymentMethodRepository paymentMethodRepository;

  /** A hard delete. Deleting the default does not promote another payment method. */
  @Transactional
  public void deletePaymentMethod(Long customerId, Long paymentMethodId) {
    ensureCustomerExists(customerId);

    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findActiveById(paymentMethodId)
            .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));

    ensurePaymentMethodBelongsToCustomer(paymentMethod, customerId);

    paymentMethodRepository.delete(paymentMethod);
  }

  private void ensureCustomerExists(Long customerId) {
    if (!customerRepository.existsActiveById(customerId)) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }

  private void ensurePaymentMethodBelongsToCustomer(PaymentMethod paymentMethod, Long customerId) {
    if (!paymentMethod.getCustomer().getId().equals(customerId)) {
      throw new PaymentMethodAccessDeniedException("Payment method belongs to another customer");
    }
  }
}
