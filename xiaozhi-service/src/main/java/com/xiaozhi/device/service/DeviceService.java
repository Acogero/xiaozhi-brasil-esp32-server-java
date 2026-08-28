package com.xiaozhi.device.service;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.VerifyCodeBO;
import com.xiaozhi.common.model.resp.DeviceResp;
import com.xiaozhi.common.model.resp.PageResp;

import java.util.List;

public interface DeviceService {

    /** Nome do cache de dispositivos (usado por DeviceServiceImpl para leitura e por DeviceRepositoryImpl para invalidação após escrita) */
    String CACHE_NAME = "XiaoZhi:Device";

    // ===================== Operações de consulta =====================

    PageResp<DeviceResp> page(int pageNo, int pageSize, String deviceId, String deviceName,
                              String roleName, String state, Integer roleId, Integer userId);

    DeviceBO getBO(String deviceId);

    List<DeviceBO> listByStateAndType(String state, String type);

    DeviceResp get(String deviceId);

    // ===================== Operações de código de verificação (tabela independente, fora do agregado Device) =====================

    VerifyCodeBO generateCode(String deviceId, String sessionId, String type);

    int updateCodeAudioPath(String deviceId, String sessionId, String code, String audioPath);

}
