package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.AddPaymentMethodRequest;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
import com.mentorship.restaurant.customer.service.handler.AddPaymentMethodHandler;
import com.mentorship.restaurant.customer.service.handler.DeletePaymentMethodHandler;
import com.mentorship.restaurant.customer.service.handler.SetDefaultPaymentMethodHandler;
import com.mentorship.restaurant.customer.service.handler.ViewPaymentMethodsHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

  private final AddPaymentMethodHandler addPaymentMethodHandler;
  private final ViewPaymentMethodsHandler viewPaymentMethodsHandler;
  private final SetDefaultPaymentMethodHandler setDefaultPaymentMethodHandler;
  private final DeletePaymentMethodHandler deletePaymentMethodHandler;

  public PaymentMethodResponse addPaymentMethod(Long customerId, AddPaymentMethodRequest request) {
    return addPaymentMethodHandler.addPaymentMethod(customerId, request);
  }

  public List<PaymentMethodResponse> viewPaymentMethods(Long customerId) {
    return viewPaymentMethodsHandler.viewPaymentMethods(customerId);
  }

  public PaymentMethodResponse setDefaultPaymentMethod(Long customerId, Long paymentMethodId) {
    return setDefaultPaymentMethodHandler.setDefaultPaymentMethod(customerId, paymentMethodId);
  }

  public void deletePaymentMethod(Long customerId, Long paymentMethodId) {
    deletePaymentMethodHandler.deletePaymentMethod(customerId, paymentMethodId);
  }
}
