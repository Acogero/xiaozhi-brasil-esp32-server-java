package com.xiaozhi.common.web;

/**
 * Código de status de retorno
 * 
 * @author Joey
 */
public class ResultStatus {
    /**
     * Operação realizada com sucesso
     */
    public static final int SUCCESS = 200;

    /**
     * Objeto criado com sucesso
     */
    public static final int CREATED = 201;

    /**
     * Requisição já foi aceita
     */
    public static final int ACCEPTED = 202;

    /**
     * Operação executada com sucesso, mas sem dados de retorno
     */
    public static final int NO_CONTENT = 204;

    /**
     * Recurso foi removido
     */
    public static final int MOVED_PERM = 301;

    /**
     * Redirecionamento
     */
    public static final int SEE_OTHER = 303;

    /**
     * Recurso não foi modificado
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * Lista de parâmetros incorreta (ausente, formato incompatível)
     */
    public static final int BAD_REQUEST = 400;

    /**
     * Não autorizado
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * Acesso restrito, autorização expirada
     */
    public static final int FORBIDDEN = 403;

    /**
     * Recurso ou serviço não encontrado
     */
    public static final int NOT_FOUND = 404;

    /**
     * Método http não permitido
     */
    public static final int BAD_METHOD = 405;

    /**
     * Conflito de recurso, ou recurso bloqueado
     */
    public static final int CONFLICT = 409;

    /**
     * Dados ou tipo de mídia não suportado
     */
    public static final int UNSUPPORTED_TYPE = 415;

    /**
     * Erro interno do sistema
     */
    public static final int ERROR = 500;

    /**
     * Interface não implementada
     */
    public static final int NOT_IMPLEMENTED = 501;
}
