package com.xiaozhi.device.infrastructure.convert;

import com.xiaozhi.common.model.bo.DeviceBO;
import com.xiaozhi.common.model.bo.VerifyCodeBO;
import com.xiaozhi.device.dal.mysql.dataobject.DeviceDO;
import com.xiaozhi.device.domain.Device;
import com.xiaozhi.device.domain.vo.VerifyCode;
import org.springframework.stereotype.Component;

/**
 * Conversor Device raiz de agregação ↔ DO / BO (entre a camada de domínio e a de infraestrutura).
 * <p>
 * Observação: esta classe é responsável pela conversão DO ↔ raiz de agregação {@link Device},
 * responsabilidade diferente da MapStruct {@code DeviceConvert} (DO ↔ BO); não confundir.
 */
@Component
public class DeviceConverter {

    /** DeviceDO → Device raiz de agregação (reconstrução a partir da camada de persistência) */
    public Device toDomain(DeviceDO d) {
        return new Device(
                d.getDeviceId(),
                d.getDeviceName(),
                d.getUserId(),
                d.getRoleId(),
                d.getMcpList(),
                d.getIp(),
                d.getLocation(),
                d.getWifiName(),
                d.getChipModelName(),
                d.getType(),
                d.getVersion(),
                d.getState(),
                d.getCreateTime(),
                d.getUpdateTime()
        );
    }

    /** Device raiz de agregação → DeviceDO (gravação na camada de persistência) */
    public DeviceDO toDataObject(Device device) {
        DeviceDO d = new DeviceDO();
        d.setDeviceId(device.getDeviceId());
        d.setDeviceName(device.getDeviceName());
        d.setUserId(device.getUserId());
        d.setRoleId(device.getRoleId());
        d.setMcpList(device.getMcpList());
        d.setIp(device.getIp());
        d.setLocation(device.getLocation());
        d.setWifiName(device.getWifiName());
        d.setChipModelName(device.getChipModelName());
        d.setType(device.getType());
        d.setVersion(device.getVersion());
        d.setState(device.getState());
        return d;
    }

    /** Device raiz de agregação → DeviceBO (usado na publicação de eventos; sessionId / roleName não disponíveis) */
    public DeviceBO toBO(Device device) {
        DeviceBO bo = new DeviceBO();
        bo.setDeviceId(device.getDeviceId());
        bo.setDeviceName(device.getDeviceName());
        bo.setUserId(device.getUserId());
        bo.setRoleId(device.getRoleId());
        bo.setMcpList(device.getMcpList());
        bo.setIp(device.getIp());
        bo.setLocation(device.getLocation());
        bo.setWifiName(device.getWifiName());
        bo.setChipModelName(device.getChipModelName());
        bo.setType(device.getType());
        bo.setVersion(device.getVersion());
        bo.setState(device.getState());
        bo.setCreateTime(device.getCreateTime());
        bo.setUpdateTime(device.getUpdateTime());
        return bo;
    }

    /** VerifyCodeBO → objeto de valor VerifyCode */
    public VerifyCode toVerifyCode(VerifyCodeBO bo) {
        return new VerifyCode(
                bo.getCode(),
                bo.getDeviceId(),
                bo.getSessionId(),
                bo.getType(),
                bo.getAudioPath(),
                bo.getCreateTime()
        );
    }
}
