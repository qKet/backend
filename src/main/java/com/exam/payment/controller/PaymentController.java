package com.exam.payment.controller;

import com.exam.auth.dto.UserDTO;
import com.exam.common.exception.BusinessException;
import com.exam.common.exception.ErrorCode;
import com.exam.common.util.WebUtil;
import com.exam.payment.dto.PaymentConfirmRequestDTO;
import com.exam.payment.dto.PaymentDTO;
import com.exam.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /***********************************
     *  URL      :  "/payments/confirm"
     *  이름      :   confirm
     *  기능      :   토스페이먼츠 결제 최종 승인 + 좌석 예매 확정
     *  method   :   POST
     *  param    :   PaymentConfirmRequestDTO, HttpSession, HttpServletRequest
     *  return   :   PaymentDTO
     ************************************/
    @PostMapping("/confirm")
    public PaymentDTO confirm(@RequestBody PaymentConfirmRequestDTO request,
                               HttpSession session,
                               HttpServletRequest servletRequest) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return paymentService.confirm(request, loginUser.getUserId(), WebUtil.getClientIp(servletRequest));
    }

    /***********************************
     *  URL      :  "/payments/my"
     *  이름      :   myPayments
     *  기능      :   내 결제 내역 조회 (마이페이지, 최신순)
     *  method   :   GET
     *  param    :   HttpSession
     *  return   :   List<PaymentDTO>
     ************************************/
    @GetMapping("/my")
    public List<PaymentDTO> myPayments(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return paymentService.getMyPayments(loginUser.getUserId());
    }

    /***********************************
     *  URL      :  "/payments/{paymentId}/cancel"
     *  이름      :   cancelPayment
     *  기능      :   결제 취소(환불) 요청 — 좌석도 함께 반납되어 다시 예매 가능해짐
     *  method   :   POST
     *  param    :   Long, HttpSession, HttpServletRequest
     *  return   :   PaymentDTO
     ************************************/
    @PostMapping("/{paymentId}/cancel")
    public PaymentDTO cancelPayment(@PathVariable Long paymentId,
                                     HttpSession session,
                                     HttpServletRequest servletRequest) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        return paymentService.cancelPayment(paymentId, loginUser.getUserId(), WebUtil.getClientIp(servletRequest));
    }

    /***********************************
     *  URL      :  "/payments/{paymentId}"
     *  이름      :   deletePayment
     *  기능      :   결제 내역 목록에서 삭제 (취소/환불된 건만 가능, 실제 행은 안 지우고 숨김 처리)
     *  method   :   DELETE
     *  param    :   Long, HttpSession, HttpServletRequest
     *  return   :   void
     ************************************/
    @DeleteMapping("/{paymentId}")
    public void deletePayment(@PathVariable Long paymentId,
                               HttpSession session,
                               HttpServletRequest servletRequest) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }
        paymentService.deletePayment(paymentId, loginUser.getUserId(), WebUtil.getClientIp(servletRequest));
    }
}
