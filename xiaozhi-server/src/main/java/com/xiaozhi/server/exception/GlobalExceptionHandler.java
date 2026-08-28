package com.xiaozhi.server.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.exception.UnauthorizedException;
import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.common.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;
/**
 * Manipulador global de exceções
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UsernameNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleUsernameNotFoundException(UsernameNotFoundException e, WebRequest request) {
        log.warn("Exceção de nome de usuário não encontrado: {}", e.getMessage(), e);
        return ApiResponse.badRequest("Nome de usuário não encontrado");
    }

    @ExceptionHandler(UserPasswordNotMatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleUserPasswordNotMatchException(UserPasswordNotMatchException e, WebRequest request) {
        log.warn("Exceção de senha incorreta: {}", e.getMessage(), e);
        return ApiResponse.badRequest("Senha incorreta");
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleUnauthorizedException(UnauthorizedException e, WebRequest request) {
        log.warn("Permissão insuficiente: {}", e.getMessage());
        return ApiResponse.forbidden(e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleNotLoginException(NotLoginException e, WebRequest request) {
        return ApiResponse.unauthorized("Login expirado, faça login novamente");
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleNotPermissionException(NotPermissionException e, WebRequest request) {
        log.warn("Permissão insuficiente: {}", e.getMessage());
        return ApiResponse.forbidden("Permissão insuficiente");
    }

    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleNotRoleException(NotRoleException e, WebRequest request) {
        log.warn("Permissão do papel insuficiente: {}", e.getMessage());
        return ApiResponse.forbidden("Permissão do papel insuficiente");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleResourceNotFoundException(ResourceNotFoundException e, WebRequest request) {
        log.warn("Recurso não encontrado: {}", e.getMessage());
        return ApiResponse.notFound(e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNoResourceFoundException(NoResourceFoundException e, WebRequest request) {
        log.warn("Recurso estático não encontrado: {}", e.getResourcePath());
        return ApiResponse.notFound("O recurso solicitado não existe");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("Caminho da requisição não encontrado: {} {}", e.getHttpMethod(), e.getRequestURL());
        return ApiResponse.notFound("O endpoint solicitado não existe");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<?> handleHttpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException e,
        HttpServletRequest request
    ) {
        log.warn("Método de requisição não suportado: {} {}, métodos suportados: {}", e.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "Método de requisição não suportado");
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    public ApiResponse<?> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e, WebRequest request) {
        log.warn("Tempo limite da solicitação assíncrona: {}", request.getDescription(false));
        return ApiResponse.error(HttpStatus.REQUEST_TIMEOUT.value(), "Tempo limite da requisição excedido, tente novamente mais tarde");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBindException(Exception e) {
        BindingResult bindingResult = e instanceof MethodArgumentNotValidException methodArgumentNotValidException
            ? methodArgumentNotValidException.getBindingResult()
            : ((BindException) e).getBindingResult();
        String message = extractBindingMessage(bindingResult, "Parâmetros da requisição inválidos");
        log.warn("Falha na validação dos parâmetros da requisição: {}", message);
        return ApiResponse.badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
            .map(violation -> violation.getMessage())
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse("Parâmetros da requisição inválidos");
        log.warn("Falha na validação de restrição: {}", message);
        return ApiResponse.badRequest(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("Parâmetro ausente na requisição: {}", e.getParameterName());
        return ApiResponse.badRequest("Parâmetro obrigatório ausente: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("Tipo de parâmetro incompatível: {}", e.getName(), e);
        return ApiResponse.badRequest("Tipo de parâmetro inválido: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Falha ao analisar o corpo da requisição: {}", e.getMessage());
        return ApiResponse.badRequest("Formato do corpo da requisição inválido");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleIllegalArgumentException(IllegalArgumentException e, WebRequest request) {
        log.warn("Parâmetro inválido: {}", e.getMessage(), e);
        return ApiResponse.badRequest(defaultMessage(e.getMessage(), "Parâmetros da requisição inválidos"));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleIllegalStateException(IllegalStateException e, WebRequest request) {
        log.warn("Conflito de estado de negócio: {}", e.getMessage(), e);
        return ApiResponse.conflict(defaultMessage(e.getMessage(), "O estado atual não permite esta operação"));
    }

    @ExceptionHandler(OperationFailedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleOperationFailedException(OperationFailedException e, WebRequest request) {
        log.error("Falha na operação de negócio: {}", e.getMessage(), e);
        return ApiResponse.serverError(defaultMessage(e.getMessage(), "Falha na operação, tente novamente mais tarde"));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleRuntimeException(RuntimeException e, WebRequest request) {
        log.error("Exceção de negócio: {}", e.getMessage(), e);
        return ApiResponse.serverError("Erro no servidor, entre em contato com o administrador");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e, WebRequest request) {
        log.error("Exceção do sistema: {}", e.getMessage(), e);
        return ApiResponse.serverError("Erro no servidor, entre em contato com o administrador");
    }

    private String extractBindingMessage(BindingResult bindingResult, String fallback) {
        if (bindingResult == null) {
            return fallback;
        }
        FieldError fieldError = bindingResult.getFieldError();
        if (fieldError != null && StringUtils.hasText(fieldError.getDefaultMessage())) {
            return fieldError.getDefaultMessage();
        }
        return fallback;
    }

    private String defaultMessage(String message, String fallback) {
        return StringUtils.hasText(message) ? message : fallback;
    }
}
