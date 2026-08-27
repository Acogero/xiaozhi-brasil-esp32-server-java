package com.xiaozhi.user;

import com.xiaozhi.server.web.BaseController;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.xiaozhi.common.annotation.AuditLog;
import com.xiaozhi.common.annotation.CheckOwner;
import com.xiaozhi.common.exception.OperationFailedException;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.google.gson.Gson;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.common.model.req.UserCheckReq;
import com.xiaozhi.common.model.req.UserLoginReq;
import com.xiaozhi.common.model.req.UserPageReq;
import com.xiaozhi.common.model.req.UserRegisterReq;
import com.xiaozhi.common.model.req.UserResetPasswordReq;
import com.xiaozhi.common.model.req.UserSendCaptchaReq;
import com.xiaozhi.common.model.req.UserTelLoginReq;
import com.xiaozhi.common.model.req.UserUpdateReq;
import com.xiaozhi.common.model.req.UserWechatLoginReq;
import com.xiaozhi.common.model.resp.LoginResp;
import com.xiaozhi.common.web.ApiResponse;
import com.xiaozhi.security.AuthenticationService;
import com.xiaozhi.user.service.UserService;
import com.xiaozhi.user.service.WxLoginService;
import com.xiaozhi.common.model.bo.UserAuthBO;
import com.xiaozhi.userauth.service.UserAuthService;
import com.xiaozhi.utils.CaptchaUtils;
import com.xiaozhi.utils.RequestContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "Gerenciamento de usuários", description = "Operações relacionadas a usuários")
public class UserController extends BaseController {

    @Resource
    private UserAppService userAppService;

    @Resource
    private UserService userService;

    @Resource
    private AuthenticationService authenticationService;

    @Resource
    private WxLoginService wxLoginService;

    @Resource
    private UserAuthService userAuthService;

    @Resource
    private CaptchaUtils captchaUtils;

    @GetMapping("/check-token")
    @Operation(summary = "Verificar validade do Token", description = "Verifica se o Token atual é válido; se for, retorna as informações do usuário")
    public ApiResponse<?> checkToken() {
        if (!StpUtil.isLogin()) {
            return ApiResponse.unauthorized("Token inválido ou expirado");
        }
        Integer userId = StpUtil.getLoginIdAsInt();
        LoginResp response = userAppService.buildLoginResp(userId, StpUtil.getTokenValue(), false);
        if (response == null) {
            return ApiResponse.unauthorized("Usuário não encontrado");
        }
        return ApiResponse.success(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Renovar Token", description = "Renova a validade do Token e retorna um novo Token")
    public ApiResponse<?> refreshToken() {
        if (!StpUtil.isLogin()) {
            return ApiResponse.unauthorized("Usuário não encontrado");
        }
        Integer userId = StpUtil.getLoginIdAsInt();
        if (userService.getBO(userId) == null) {
            return ApiResponse.unauthorized("Usuário não encontrado");
        }

        int expireSeconds = userAppService.getTokenExpireSeconds();
        StpUtil.logout();
        StpUtil.login(userId, expireSeconds);
        LoginResp response = userAppService.buildLoginResp(userId, StpUtil.getTokenValue(), false);
        return response == null
            ? ApiResponse.unauthorized("Falha ao renovar o Token, faça login novamente")
            : ApiResponse.success(response);
    }

    @SaIgnore
    @PostMapping("/login")
    @AuditLog(module = "Gerenciamento de usuários", operation = "Login de usuário")
    @Operation(summary = "Login com usuário e senha", description = "Login com nome de usuário/e-mail/celular e senha")
    public ApiResponse<?> login(@Valid @RequestBody UserLoginReq req, HttpServletRequest request) {
        UserBO user = userAppService.login(req.getUsername(), req.getPassword());
        userAppService.recordLoginInfo(user, RequestContextUtils.getClientIp(request));

        int expireSeconds = userAppService.getTokenExpireSeconds();
        StpUtil.login(user.getUserId(), expireSeconds);
        return ApiResponse.success(requireLoginResp(user.getUserId(), false));
    }

    @SaIgnore
    @PostMapping("/tel-login")
    @AuditLog(module = "Gerenciamento de usuários", operation = "Login por celular")
    @Operation(summary = "Login por celular com código de verificação", description = "Login com número de celular e código de verificação; se não houver cadastro, ele é criado automaticamente")
    public ApiResponse<?> telLogin(@Valid @RequestBody UserTelLoginReq req, HttpServletRequest request) {
        if (!userService.checkCaptcha(req.getTel(), req.getCode())) {
            throw new IllegalArgumentException("Código de verificação incorreto ou expirado");
        }

        UserBO user = userService.getByTel(req.getTel());
        if (user == null) {
            String suffix = req.getTel().length() >= 4 ? req.getTel().substring(req.getTel().length() - 4) : req.getTel();
            UserBO createUser = new UserBO();
            createUser.setUsername("tel_" + suffix + "_" + System.currentTimeMillis() % 1000);
            createUser.setPassword(authenticationService.encryptPassword(UUID.randomUUID().toString()));
            createUser.setName("Usuario" + suffix);
            createUser.setTel(req.getTel());
            user = userAppService.createUserWithDefaults(createUser);
        }

        userAppService.recordLoginInfo(user, RequestContextUtils.getClientIp(request));

        int expireSeconds = userAppService.getTokenExpireSeconds();
        StpUtil.login(user.getUserId(), expireSeconds);
        return ApiResponse.success(requireLoginResp(user.getUserId(), false));
    }

    @SaIgnore
    @PostMapping("/wx-login")
    @ResponseBody
    @AuditLog(module = "Gerenciamento de usuários", operation = "Login com WeChat")
    @Operation(summary = "Login com WeChat", description = "Login com o code do WeChat; se não houver cadastro, ele é criado automaticamente")
    public ApiResponse<?> wxLogin(@Valid @RequestBody UserWechatLoginReq req, HttpServletRequest request) {
        Map<String, String> wxLoginInfo = wxLoginService.getWxLoginInfo(req.getCode());
        String openId = wxLoginInfo.get("openid");
        String unionId = wxLoginInfo.get("unionid");
        if (!StringUtils.hasText(openId)) {
            throw new IllegalStateException("Falha ao obter o openid do WeChat");
        }

        UserAuthBO userAuth = userAuthService.getByOpenIdAndPlatform(openId, "wechat");
        UserBO user;
        boolean isNewUser = false;
        if (userAuth == null) {
            UserBO createUser = new UserBO();
            createUser.setUsername("wx_" + openId.substring(0, Math.min(10, openId.length())));
            createUser.setPassword(authenticationService.encryptPassword(UUID.randomUUID().toString()));
            createUser.setName("UsuarioWeChat" + System.currentTimeMillis() % 10000);
            user = userAppService.createUserWithDefaults(createUser);
            isNewUser = true;

            userAuth = new UserAuthBO();
            userAuth.setUserId(user.getUserId());
            userAuth.setOpenId(openId);
            userAuth.setUnionId(unionId);
            userAuth.setPlatform("wechat");
            userAuth.setProfile(new Gson().toJson(wxLoginInfo));
            userAuthService.create(userAuth);
        } else {
            user = userService.getBO(userAuth.getUserId());
            if (user == null) {
                throw new ResourceNotFoundException("Usuário não encontrado");
            }
        }

        userAppService.recordLoginInfo(user, RequestContextUtils.getClientIp(request));

        int expireSeconds = userAppService.getTokenExpireSeconds();
        StpUtil.login(user.getUserId(), expireSeconds);
        return ApiResponse.success(requireLoginResp(user.getUserId(), isNewUser));
    }

    @SaIgnore
    @PostMapping("")
    @AuditLog(module = "Gerenciamento de usuários", operation = "Cadastro de usuário")
    @Operation(summary = "Cadastro de usuário", description = "Cadastro de novo usuário")
    public ApiResponse<?> create(@Valid @RequestBody UserRegisterReq req) {
        return ApiResponse.success(userAppService.register(req));
    }

    @GetMapping("")
    @ResponseBody
    @SaCheckPermission("system:user:api:list")
    @Operation(summary = "Consulta a lista de usuários de acordo com os filtros", description = "Retorna a lista de usuários")
    public ApiResponse<?> queryUsers(@Valid UserPageReq req) {
        return ApiResponse.success(userAppService.page(req));
    }

    @PutMapping("/{userId}")
    @SaCheckPermission("system:setting:account:api:update")
    @CheckOwner(resource = "user", id = "#userId")
    @AuditLog(module = "Gerenciamento de usuários", operation = "Atualizar informações do usuário")
    @Operation(summary = "Altera as informações do usuário", description = "Atualiza as informações pessoais do usuário")
    public ApiResponse<?> update(@PathVariable Integer userId, @Valid @RequestBody UserUpdateReq req) {
        return ApiResponse.success(userAppService.update(userId, req));
    }

    @SaIgnore
    @PostMapping("/resetPassword")
    @AuditLog(module = "Gerenciamento de usuários", operation = "Redefinir senha")
    @Operation(summary = "Redefinir senha", description = "Redefine a senha usando o código de verificação enviado por e-mail")
    public ApiResponse<?> resetPassword(@Valid @RequestBody UserResetPasswordReq req) {
        userAppService.resetPassword(req);
        return ApiResponse.success("Senha redefinida com sucesso");
    }

    @SaIgnore
    @PostMapping("/sendEmailCaptcha")
    @Operation(summary = "Enviar código de verificação por e-mail", description = "Envia o código de verificação para o e-mail informado")
    public ApiResponse<?> sendEmailCaptcha(@Valid @RequestBody UserSendCaptchaReq req) {
        if ("forget".equals(req.getType()) && userService.getByEmail(req.getEmail()) == null) {
            throw new IllegalArgumentException("Este e-mail não está cadastrado");
        }

        String code = userService.generateCaptcha(req.getEmail());
        CaptchaUtils.CaptchaResult result = captchaUtils.sendEmailCaptcha(req.getEmail(), code);
        if (!result.isSuccess()) {
            throw new OperationFailedException(result.getMessage());
        }
        return ApiResponse.success();
    }

    @SaIgnore
    @PostMapping("/sendSmsCaptcha")
    @Operation(summary = "Enviar código de verificação por SMS", description = "Envia o código de verificação para o número de celular informado")
    public ApiResponse<?> sendSmsCaptcha(@Valid @RequestBody UserSendCaptchaReq req) {
        if ("forget".equals(req.getType()) && userService.getByTel(req.getTel()) == null) {
            throw new IllegalArgumentException("Este número de celular não está cadastrado");
        }

        String code = userService.generateCaptcha(req.getTel());
        CaptchaUtils.CaptchaResult result = captchaUtils.sendSmsCaptcha(req.getTel(), code);
        if (!result.isSuccess()) {
            throw new OperationFailedException(result.getMessage());
        }
        return ApiResponse.success();
    }

    @SaIgnore
    @GetMapping("/checkUser")
    @ResponseBody
    @Operation(summary = "Verifica se o nome de usuário e o número de celular já existem", description = "Retorna o resultado da verificação")
    public ApiResponse<?> checkUser(@Valid UserCheckReq req) {
        if (StringUtils.hasText(req.getTel()) && userService.getByTel(req.getTel()) != null) {
            throw new IllegalStateException("Este número já está cadastrado");
        }
        if (StringUtils.hasText(req.getEmail()) && userService.getByEmail(req.getEmail()) != null) {
            throw new IllegalStateException("Este e-mail já está cadastrado");
        }
        if (StringUtils.hasText(req.getUsername()) && userService.getByUsername(req.getUsername()) != null) {
            throw new IllegalStateException("Nome de usuário já existe");
        }
        return ApiResponse.success();
    }

    private LoginResp requireLoginResp(Integer userId, boolean isNewUser) {
        LoginResp response = userAppService.buildLoginResp(userId, StpUtil.getTokenValue(), isNewUser);
        if (response == null) {
            throw new IllegalStateException("Login realizado, mas falha ao carregar as informações do usuário");
        }
        return response;
    }
}
