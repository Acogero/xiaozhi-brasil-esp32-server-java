package com.xiaozhi.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de resposta unificado
 *
 * @author Joey
 */
@Schema(description = "Resultado de resposta unificado")
public class ApiResponse<T> {

    @Schema(description = "Código de status: 200-sucesso, 500-falha", example = "200")
    private int code;

    @Schema(description = "Mensagem de retorno", example = "Operação realizada com sucesso")
    private String message;

    @Schema(description = "Dados de retorno")
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // -------------------- success --------------------

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResultStatus.SUCCESS, "Operação realizada com sucesso", null);
    }

    public static <T> ApiResponse<T> success(String msg) {
        return new ApiResponse<>(ResultStatus.SUCCESS, msg, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultStatus.SUCCESS, "Operação realizada com sucesso", data);
    }

    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>(ResultStatus.SUCCESS, msg, data);
    }

    // -------------------- semantic helpers --------------------

    public static <T> ApiResponse<T> badRequest(String msg) {
        return new ApiResponse<>(ResultStatus.BAD_REQUEST, msg, null);
    }

    public static <T> ApiResponse<T> unauthorized(String msg) {
        return new ApiResponse<>(ResultStatus.UNAUTHORIZED, msg, null);
    }

    public static <T> ApiResponse<T> forbidden(String msg) {
        return new ApiResponse<>(ResultStatus.FORBIDDEN, msg, null);
    }

    public static <T> ApiResponse<T> notFound(String msg) {
        return new ApiResponse<>(ResultStatus.NOT_FOUND, msg, null);
    }

    public static <T> ApiResponse<T> conflict(String msg) {
        return new ApiResponse<>(ResultStatus.CONFLICT, msg, null);
    }

    public static <T> ApiResponse<T> serverError(String msg) {
        return new ApiResponse<>(ResultStatus.ERROR, msg, null);
    }

    // -------------------- error --------------------

    public static <T> ApiResponse<T> error() {
        return new ApiResponse<>(ResultStatus.ERROR, "Falha na operação", null);
    }

    public static <T> ApiResponse<T> error(String msg) {
        return new ApiResponse<>(ResultStatus.ERROR, msg, null);
    }

    public static <T> ApiResponse<T> error(String msg, T data) {
        return new ApiResponse<>(ResultStatus.ERROR, msg, data);
    }

    public static <T> ApiResponse<T> error(int code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    public static <T> ApiResponse<T> error(int code, String msg, T data) {
        return new ApiResponse<>(code, msg, data);
    }

    // -------------------- getters --------------------

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
