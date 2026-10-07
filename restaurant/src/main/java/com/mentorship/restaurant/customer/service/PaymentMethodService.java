package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.PaymentMethodAccessDeniedException;
import com.mentorship.restaurant.customer.exception.PaymentMethodNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.model.mapper.PaymentMethodMapper;
import com.mentorship.restaurant.customer.model.request.AddPaymentMethodRequest;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.PaymentMethodRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

  private final PaymentMethodRepository paymentMethodRepository;
  private final CustomerRepository customerRepository;
  private final CustomerService customerService;
  private final PaymentMethodMapper paymentMethodMapper;

  @Transactional
  public PaymentMethodResponse addPaymentMethod(Long customerId, AddPaymentMethodRequest request) {
    Customer customer = customerService.findActiveCustomer(customerId);

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

  /** A read: expired saved cards are still listed. */
  @Transactional(readOnly = true)
  public List<PaymentMethodResponse> viewPaymentMethods(Long customerId) {
    Customer customer =
        customerRepository
            .findByIdWithPaymentMethods(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    return paymentMethodMapper.toResponseList(customer.getPaymentMethods());
  }

  @Transactional
  public PaymentMethodResponse setDefaultPaymentMethod(Long customerId, Long paymentMethodId) {
    PaymentMethod paymentMethod = findCustomersPaymentMethod(paymentMethodId, customerId);

    if (!paymentMethod.isDefault()) {
      // The bulk update bypasses the persistence context, but it only touches the old default,
      // never this row, so the entity loaded above is still accurate. Setting the flag on it is
      // written by dirty checking at commit, after the old default has been cleared.
      paymentMethodRepository.clearDefaultForCustomer(customerId);
      paymentMethod.setDefault(true);
    }

    return paymentMethodMapper.toResponse(paymentMethod);
  }

  /** A hard delete. Deleting the default does not promote another payment method. */
  @Transactional
  public void deletePaymentMethod(Long customerId, Long paymentMethodId) {
    paymentMethodRepository.delete(findCustomersPaymentMethod(paymentMethodId, customerId));
  }

  /** Loads the payment method, then checks the caller owns it and is not soft-deleted. */
  private PaymentMethod findCustomersPaymentMethod(Long paymentMethodId, Long customerId) {
    PaymentMethod paymentMethod =
        paymentMethodRepository
            .findByIdWithOwner(paymentMethodId)
            .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found"));
    if (!paymentMethod.getCustomer().getId().equals(customerId)) {
      throw new PaymentMethodAccessDeniedException("Payment method belongs to another customer");
    }
    // Runs after the ownership check, so the owner is the caller: a soft-deleted caller is a 404.
    customerService.ensureActive(paymentMethod.getCustomer());
    return paymentMethod;
  }
}
