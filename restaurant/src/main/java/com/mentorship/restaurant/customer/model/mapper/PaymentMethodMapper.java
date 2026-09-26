package com.mentorship.restaurant.customer.model.mapper;

import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import com.mentorship.restaurant.customer.model.response.PaymentMethodResponse;
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
