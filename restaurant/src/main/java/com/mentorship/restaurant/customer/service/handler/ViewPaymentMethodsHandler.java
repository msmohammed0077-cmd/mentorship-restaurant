package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.mapper.PaymentMethodMapper;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewPaymentMethodsHandler {

  private final CustomerRepository customerRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentMethodMapper paymentMethodMapper;

  /** A read: expired saved cards are still listed. */
  @Transactional(readOnly = true)
  public List<PaymentMethodResponse> viewPaymentMethods(Long customerId) {
    ensureCustomerExists(customerId);

    return paymentMethodMapper.toResponseList(
        paymentMethodRepository.findAllByCustomerIdOrdered(customerId));
  }

  private void ensureCustomerExists(Long customerId) {
    if (!customerRepository.existsActiveById(customerId)) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
