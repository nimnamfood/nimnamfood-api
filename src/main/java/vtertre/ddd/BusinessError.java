package vtertre.ddd;

public class BusinessError extends RuntimeException {
    public BusinessError(String code) {
        super(code);
    }
}
