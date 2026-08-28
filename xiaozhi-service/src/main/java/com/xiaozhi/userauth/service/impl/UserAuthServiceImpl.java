package com.xiaozhi.userauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaozhi.common.exception.ResourceNotFoundException;
import com.xiaozhi.common.model.bo.UserAuthBO;
import com.xiaozhi.userauth.convert.UserAuthConvert;
import com.xiaozhi.userauth.dal.mysql.dataobject.UserAuthDO;
import com.xiaozhi.userauth.dal.mysql.mapper.UserAuthMapper;
import com.xiaozhi.userauth.service.UserAuthService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserAuthServiceImpl implements UserAuthService {

    @Resource
    private UserAuthMapper userAuthMapper;

    @Resource
    private UserAuthConvert userAuthConvert;

    @Override
    public UserAuthBO getByOpenIdAndPlatform(String openId, String platform) {
        if (!StringUtils.hasText(openId) || !StringUtils.hasText(platform)) {
            return null;
        }
        UserAuthDO d = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuthDO>()
            .eq(UserAuthDO::getOpenId, openId)
            .eq(UserAuthDO::getPlatform, platform)
            .last("LIMIT 1"));
        return userAuthConvert.toBO(d);
    }

    @Override
    public UserAuthBO getByUserIdAndPlatform(Integer userId, String platform) {
        if (userId == null || !StringUtils.hasText(platform)) {
            return null;
        }
        UserAuthDO d = userAuthMapper.selectOne(new LambdaQueryWrapper<UserAuthDO>()
            .eq(UserAuthDO::getUserId, userId)
            .eq(UserAuthDO::getPlatform, platform)
            .last("LIMIT 1"));
        return userAuthConvert.toBO(d);
    }

    @Override
    @Transactional
    public UserAuthBO create(UserAuthBO userAuth) {
        if (userAuth == null || userAuth.getUserId() == null || !StringUtils.hasText(userAuth.getOpenId())
            || !StringUtils.hasText(userAuth.getPlatform())) {
            throw new IllegalArgumentException("Informações de autorização do usuário incompletas");
        }
        UserAuthDO d = userAuthConvert.toDO(userAuth);
        d.setId(null);
        if (userAuthMapper.insert(d) <= 0) {
            throw new IllegalStateException("Falha ao criar autorização do usuário");
        }
        UserAuthDO result = userAuthMapper.selectById(d.getId());
        if (result == null) {
            throw new IllegalStateException("Falha ao criar autorização do usuário");
        }
        return userAuthConvert.toBO(result);
    }

    @Override
    @Transactional
    public void update(UserAuthBO userAuth) {
        if (userAuth == null || userAuth.getId() == null) {
            throw new IllegalArgumentException("Informações de autorização do usuário incompletas");
        }
        if (userAuthMapper.selectById(userAuth.getId()) == null) {
            throw new ResourceNotFoundException("Autorização do usuário não encontrada");
        }
        if (userAuthMapper.updateById(userAuthConvert.toDO(userAuth)) <= 0) {
            throw new IllegalStateException("Falha ao atualizar autorização do usuário");
        }
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("O ID da autorização não pode estar vazio");
        }
        if (userAuthMapper.selectById(id) == null) {
            throw new ResourceNotFoundException("Autorização do usuário não encontrada");
        }
        if (userAuthMapper.deleteById(id) <= 0) {
            throw new IllegalStateException("Falha ao excluir autorização do usuário");
        }
    }

}
