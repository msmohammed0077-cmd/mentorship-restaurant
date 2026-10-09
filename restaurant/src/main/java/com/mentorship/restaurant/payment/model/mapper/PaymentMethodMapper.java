package com.mentorship.restaurant.payment.model.mapper;

import com.mentorship.restaurant.payment.model.entity.PaymentMethod;
import com.mentorship.restaurant.payment.model.response.PaymentMethodResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodMapper {

  public PaymentMethodResponse toResponse(PaymentMethod paymentMethod) {
    return new PaymentMethodResponse(
        paymentMethod.getId(),
        paymentMethod.getBrand(),
        paymentMethod.getLast4(),
        paymentMethod.getExpiryMonth(),
        paymentMethod.getExpiryYear(),
        paymentMethod.getHolderName(),
        paymentMethod.isDefault());
  }

  public List<PaymentMethodResponse> toResponseList(List<PaymentMethod> paymentMethods) {
    return paymentMethods.stream().map(this::toResponse).toList();
  }
}
