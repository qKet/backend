package com.exam.payment.service;

import com.exam.payment.dto.PaymentConfirmRequestDTO;
import com.exam.payment.dto.PaymentDTO;

import java.util.List;

public interface PaymentService {
    PaymentDTO confirm(PaymentConfirmRequestDTO request, String userId, String clientIp);
    List<PaymentDTO> getMyPayments(String userId);
    PaymentDTO cancelPayment(Long paymentId, String userId, String clientIp);
    void deletePayment(Long paymentId, String userId, String clientIp);
}
