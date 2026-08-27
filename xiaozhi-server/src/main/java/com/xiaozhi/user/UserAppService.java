package com.xiaozhi.user;

import com.xiaozhi.authrole.service.AuthRoleService;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.exception.UserPasswordNotMatchException;
import com.xiaozhi.common.exception.UsernameNotFoundException;
import com.xiaozhi.common.model.bo.TemplateBO;
import com.xiaozhi.common.model.bo.UserBO;
import com.xiaozhi.common.model.req.UserPageReq;
import com.xiaozhi.common.model.req.UserRegisterReq;
import com.xiaozhi.common.model.req.UserResetPasswordReq;
import com.xiaozhi.common.model.req.UserUpdateReq;
import com.xiaozhi.common.model.resp.AuthRoleResp;
import com.xiaozhi.common.model.resp.LoginResp;
import com.xiaozhi.common.model.resp.PageResp;
import com.xiaozhi.common.model.resp.PermissionTreeResp;
import com.xiaozhi.common.model.resp.UserResp;
import com.xiaozhi.device.domain.Device;
import com.xiaozhi.device.domain.repository.DeviceRepository;
import com.xiaozhi.device.service.DeviceService;
import com.xiaozhi.permission.service.PermissionService;
import com.xiaozhi.role.service.RoleService;
import com.xiaozhi.security.AuthenticationService;
import com.xiaozhi.template.domain.Template;
import com.xiaozhi.template.domain.repository.TemplateRepository;
import com.xiaozhi.template.service.TemplateService;
import com.xiaozhi.user.convert.UserConvert;
import com.xiaozhi.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de aplicação do domínio de usuários.
 * <p>
 * Responsabilidade: orquestra o fluxo entre o Controller e o Domain Service, incluindo:
 * <ul>
 *   <li>Conversão Req/Resp ↔ BO</li>
 *   <li>Orquestração entre domínios (no cadastro: copia o modelo de papel, cria dispositivo virtual)</li>
 *   <li>Coordenação de autenticação (login, criptografia de senha, validação de código de verificação)</li>
 *   <li>Montagem da resposta de login (Token, permissões, informações do papel)</li>
 * </ul>
 */
@Service
public class UserAppService {

    private static final Integer DEFAULT_AUTH_ROLE_ID = 2;
    private static final Integer ADMIN_TEMPLATE_OWNER_ID = 1;
    private static final int TOKEN_EXPIRE_SECONDS = 2592000;

    @Resource
    private UserService userService;

    @Resource
    private UserConvert userConvert;

    @Resource
    private RoleService roleService;

    @Resource
    private TemplateService templateService;

    @Resource
    private TemplateRepository templateRepository;

    @Resource
    private DeviceService deviceService;

    @Resource
    private DeviceRepository deviceRepository;

    @Resource
    private AuthenticationService authenticationService;

    @Resource
    private AuthRoleService authRoleService;

    @Resource
    private PermissionService permissionService;

    // ==================== Consulta ====================

    public PageResp<UserResp> page(UserPageReq req) {
        UserPageReq r = req == null ? new UserPageReq() : req;
        return userService.page(r.getPageNo(), r.getPageSize(),
            r.getName(), r.getEmail(), r.getTel(), r.getIsAdmin(), r.getAuthRoleId());
    }

    public UserResp get(Integer userId) {
        return userConvert.toResp(userService.getBO(userId));
    }

    // ==================== Cadastro ====================

    @Transactional
    public UserResp register(UserRegisterReq req) {
        String account = StringUtils.hasText(req.getEmail()) ? req.getEmail() : req.getTel();
        if (!StringUtils.hasText(account)) {
            throw new IllegalArgumentException("Informe pelo menos o e-mail ou o número de celular");
        }
        if (!userService.checkCaptcha(account, req.getCode())) {
            throw new IllegalArgumentException("Código de verificação inválido");
        }

        UserBO user = userConvert.toBO(req);
        user.setPassword(authenticationService.encryptPassword(req.getPassword()));
        UserBO created = createUserWithDefaults(user);
        return userConvert.toResp(created);
    }

    /**
     * Cria o usuário e inicializa os recursos padrão (papel, modelo, dispositivo virtual).
     * Compartilhado pelo cadastro, pelo cadastro automático no login por celular e pelo cadastro automático no login com WeChat.
     */
    @Transactional
    public UserBO createUserWithDefaults(UserBO user) {
        UserBO created = userService.create(user);
        Integer userId = created.getUserId();

        Integer defaultRoleId = roleService.copyDefaultRole(ADMIN_TEMPLATE_OWNER_ID, userId);
        List<TemplateBO> templates = templateService.listBO(ADMIN_TEMPLATE_OWNER_ID, null, null);
        for (TemplateBO template : templates) {
            templateRepository.save(Template.newTemplate(userId, template));
        }

        Device virtualDevice = Device.newDevice(
                "user_chat_" + userId, "Chat via navegador", "web", userId, defaultRoleId);
        deviceRepository.save(virtualDevice);

        return created;
    }

    // ==================== Atualização ====================

    @Transactional
    public UserResp update(Integer userId, UserUpdateReq req) {
        UserBO existing = userService.getBO(userId);
        if (existing == null) {
            throw new ResourceNotFoundException("Usuário não encontrado, falha ao atualizar");
        }
        userConvert.updateBO(req, existing);
        if (StringUtils.hasText(req.getPassword())) {
            existing.setPassword(authenticationService.encryptPassword(req.getPassword()));
        }
        existing.setUserId(userId);
        userService.update(existing);
        return userConvert.toResp(userService.getBO(userId));
    }

    // ==================== Redefinição de senha ====================

    @Transactional
    public void resetPassword(UserResetPasswordReq req) {
        if (!userService.checkCaptcha(req.getEmail(), req.getCode())) {
            throw new IllegalArgumentException("Código de verificação incorreto ou expirado");
        }
        UserBO user = userService.getByEmail(req.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("Este e-mail não está cadastrado");
        }

        UserBO updateUser = new UserBO();
        updateUser.setUserId(user.getUserId());
        updateUser.setPassword(authenticationService.encryptPassword(req.getPassword()));
        userService.update(updateUser);
    }

    // ==================== Login ====================

    public UserBO login(String username, String password) {
        UserBO user = userService.getByUsername(username);
        if (user == null) {
            user = userService.getByEmail(username);
        }
        if (user == null) {
            user = userService.getByTel(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException();
        }
        if (!authenticationService.isPasswordValid(password, user.getPassword())) {
            throw new UserPasswordNotMatchException();
        }
        return user;
    }

    public void recordLoginInfo(UserBO user, String loginIp) {
        user.setLoginTime(LocalDateTime.now());
        user.setLoginIp(loginIp);
        userService.update(user);
    }

    public LoginResp buildLoginResp(Integer userId, String token, boolean isNewUser) {
        UserResp user = get(userId);
        if (user == null) {
            return null;
        }

        AuthRoleResp authRoleResp = authRoleService.get(user.getAuthRoleId());
        List<PermissionTreeResp> permissionResp = permissionService.listTreeByUserId(userId);

        return LoginResp.builder()
            .token(token)
            .refreshToken(token)
            .expiresIn(TOKEN_EXPIRE_SECONDS)
            .userId(userId)
            .isNewUser(isNewUser)
            .user(user)
            .authRole(authRoleResp)
            .permissions(permissionResp)
            .build();
    }

    public int getTokenExpireSeconds() {
        return TOKEN_EXPIRE_SECONDS;
    }
}
