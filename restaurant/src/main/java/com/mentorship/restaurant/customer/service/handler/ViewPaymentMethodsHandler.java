package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.PaymentMethodMapper;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewPaymentMethodsHandler {

  private final CustomerRepository customerRepository;
  private final PaymentMethodMapper paymentMethodMapper;

  /** A read: expired saved cards are still listed. */
  @Transactional(readOnly = true)
  public List<PaymentMethodResponse> viewPaymentMethods(Long customerId) {
    Customer customer =
        customerRepository
            .findByIdWithPaymentMethods(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    return paymentMethodMapper.toResponseList(customer.getPaymentMethods());
  }
}
