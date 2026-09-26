package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.model.mapper.PaymentMethodMapper;
import com.mentorship.restaurant.customer.model.request.AddPaymentMethodRequest;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddPaymentMethodHandler {

  private final CustomerRepository customerRepository;
  private final PaymentMethodRepository paymentMethodRepository;
  private final PaymentMethodMapper paymentMethodMapper;

  @Transactional
  public PaymentMethodResponse addPaymentMethod(Long customerId, AddPaymentMethodRequest request) {
    Customer customer =
        customerRepository
            .findActiveById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    PaymentMethod paymentMethod = new PaymentMethod();
    paymentMethod.setCustomer(customer);
    paymentMethod.setBrand(request.getBrand());
    paymentMethod.setLast4(request.getLast4());
    paymentMethod.setExpiryMonth(request.getExpiryMonth());
    paymentMethod.setExpiryYear(request.getExpiryYear());
    paymentMethod.setHolderName(request.getHolderName());
    paymentMethod.setDefault(!paymentMethodRepository.existsByCustomer_Id(customerId));

    return paymentMethodMapper.toResponse(paymentMethodRepository.save(paymentMethod));
  }
}
