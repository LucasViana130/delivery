package br.com.fiap.delivery.payment.exceptions;

public class PaymentFailureException extends RuntimeException {

    public PaymentFailureException(String message) {
        super(message);
    }
}
