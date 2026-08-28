package com.xiaozhi.utils;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CmsUtils {

    // Cache do endereço IP do servidor - inicializado apenas na primeira chamada de getServerIp
    private String serverIp = null;
    private boolean initializing = false;

    // Abaixo está o código relacionado à detecção de IP

    private static final String[] IP_INFO_SERVICES = {
            "https://www.cip.cc/", // CIP.CC, retorna informações detalhadas
            "https://myip.ipip.net/json", // IPIP.net, retorna informações detalhadas
    };

    // Faixas de endereço IP privado
    private static final String[] PRIVATE_IP_PATTERNS = {
            "^10\\..*", // 10.0.0.0 - 10.255.255.255
            "^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*", // 172.16.0.0 - 172.31.255.255
            "^192\\.168\\..*" // 192.168.0.0 - 192.168.255.255
    };

    // Palavras-chave de operadoras (mantidas em chinês, usadas para casar com o texto retornado pelo serviço de geolocalização de IP)
    private static final String[] ISP_KEYWORDS = {
            "移动", "联通", "电信", "铁通", "网通", "教育网", "有线通", "长城宽带", "广电网",
            "Mobile", "Unicom", "Telecom", "China Telecom", "China Mobile", "China Unicom",
            "Chinanet", "CMCC", "CHINA UNICOM", "CHINA TELECOM"
    };

    // Palavras-chave de provedores de nuvem (mantidas em chinês/inglês original, usadas para casar com o texto retornado pelo serviço de geolocalização de IP)
    private static final String[] CLOUD_KEYWORDS = {
            // Provedores de nuvem nacionais (China)
            "阿里云", "腾讯云", "华为云", "百度云", "金山云", "UCloud", "青云", "七牛云",
            "京东云", "天翼云", "移动云", "联通云", "沃云", "浪潮云", "网易云", "美团云",
            "微众银行", "字节跳动", "火山引擎", "快手云", "小米云", "360云", "新浪云", "盛大云",
            "世纪互联", "光环新网", "数梦工场", "云途腾", "云杉网络", "青云QingCloud", "DaoCloud",
            "数据港", "宝德", "云宏", "中国电信云", "中国移动云", "中国联通云", "中科云", "中兴云",

            // Provedores de nuvem internacionais
            "AWS", "Amazon", "Azure", "Microsoft", "Google", "GCP", "Oracle", "IBM",
            "Salesforce", "SAP", "VMware", "Rackspace", "DigitalOcean", "Linode", "Vultr",
            "OVH", "Hetzner", "Scaleway", "Heroku", "CloudFlare", "Akamai", "Fastly",
            "Alibaba Cloud", "Aliyun", "Tencent Cloud", "Huawei Cloud", "Baidu Cloud",
            "ByteDance", "Bytedance", "TikTok", "Douyin", "Volcano Engine",

            // Palavras-chave genéricas de serviços em nuvem
            "Cloud", "云计算", "云服务", "云平台", "云主机", "云存储", "云数据库", "云网络",
            "IDC", "数据中心", "机房", "服务器集群", "集群", "分布式", "容器云", "Kubernetes",
            "Docker", "虚拟化", "VPS", "ECS", "EC2", "弹性计算", "弹性云服务器", "云服务器",
            "IaaS", "PaaS", "SaaS", "FaaS", "BaaS", "DaaS", "托管云", "混合云", "私有云",
            "公有云", "边缘计算", "CDN", "负载均衡", "高可用", "自动扩展", "弹性伸缩",

            // Palavras-chave de domínios de provedores de nuvem
            "aliyun.com", "alibabacloud.com", "cloud.tencent.com", "huaweicloud.com",
            "bce.baidu.com", "ksyun.com", "ucloud.cn", "qingcloud.com", "qiniu.com",
            "jdcloud.com", "ctyun.cn", "amazonaws.com", "azure.com", "microsoft.com",
            "cloud.google.com", "oracle.com", "ibm.com", "salesforce.com", "sap.com",
            "vmware.com", "rackspace.com", "digitalocean.com", "linode.com", "vultr.com",
            "ovh.com", "hetzner.com", "scaleway.com", "heroku.com", "cloudflare.com",
            "akamai.com", "fastly.com", "volcengine.com", "bytecdn.com", "byted.org",
            "bytedance.com"
    };

    // Nomes de variáveis de ambiente usadas para configurar o IP da máquina hospedeira
    private static final String[] HOST_IP_ENV_VARS = {
            "HOST_IP", "DOCKER_HOST_IP", "HOST_ADDR", "LOCAL_IP", "XIAOZHI_HOST_IP"
    };

    // IPs padrão de gateway do Docker (endereços de gateway Docker comuns)
    private static final String[] DOCKER_DEFAULT_GATEWAYS = {
            "172.17.0.1", "172.18.0.1", "172.19.0.1", "172.20.0.1", "172.21.0.1",
            "192.168.0.1", "192.168.1.1", "10.0.0.1", "10.0.2.2", "10.0.75.1"
    };

    /**
     * Obtém o endereço IP do servidor
     * Determina de forma inteligente o ambiente atual e retorna o endereço IP adequado
     * O resultado é armazenado em cache, calculado apenas uma vez durante o ciclo de vida da aplicação
     *
     * @return endereço IP adequado
     */
    public String getServerIp() {
        // Se o IP já foi inicializado, retorna diretamente
        if (serverIp != null) {
            return serverIp;
        }

        // Se a inicialização estiver em andamento, aguarda sua conclusão
        if (initializing) {
            // Aguarda a conclusão da inicialização, no máximo 5 segundos
            long startTime = System.currentTimeMillis();
            while (initializing && System.currentTimeMillis() - startTime < 5000) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Se após a espera o IP já estiver inicializado, retorna o resultado
            if (serverIp != null) {
                return serverIp;
            }

            // Se a espera expirar, continua com a inicialização
        }

        // Evita que múltiplas threads inicializem ao mesmo tempo
        synchronized (CmsUtils.class) {
            // Verifica novamente se já foi inicializado
            if (serverIp != null) {
                return serverIp;
            }

            initializing = true;
            try {
                long startTime = System.currentTimeMillis();

                // Executa a lógica de detecção do endereço IP
                serverIp = determineServerIp();

                long endTime = System.currentTimeMillis();

                return serverIp;
            } finally {
                initializing = false;
            }
        }
    }

    /**
     * Determina o endereço IP do servidor
     */
    private static String determineServerIp() {
        try {
            boolean isDocker = isRunningInDocker();

            // 1. Primeiro verifica se a variável de ambiente HOST_IP está definida
            String hostIp = getHostIpFromEnv();
            if (hostIp != null) {
                return hostIp;
            }

            // 2. Obtém as informações do IP público
            IPInfo ipInfo = getIPInfo();

            // 3. Se as informações do IP público foram obtidas:
            //    - Em ambiente Docker, usa diretamente o IP público (não é possível obter de forma confiável o IP LAN da máquina hospedeira dentro do container)
            //    - Em ambiente não-Docker, se identificado como ambiente de servidor, usa o IP público
            if (ipInfo != null && !ipInfo.isPrivateIp()) {
                if (isDocker || ipInfo.isServerEnvironment()) {
                    return ipInfo.getIp();
                }
            }

            // 4. Se estiver rodando em ambiente Docker, tenta obter o IP da máquina hospedeira
            if (isDocker) {
                // Tenta obter o IP da máquina hospedeira a partir do gateway Docker
                String dockerHostIp = getDockerHostIp();
                if (dockerHostIp != null) {
                    return dockerHostIp;
                }
            }

            // 5. Se todos os métodos acima falharem, usa o IP local
            String localIp = getLocalIpAddress();

            // 6. Se o IP local for um IP interno de container Docker, tenta usar o gateway padrão
            if (isDocker && isDockerInternalIp(localIp)) {
                for (String gateway : DOCKER_DEFAULT_GATEWAYS) {
                    if (isReachable(gateway)) {
                        return gateway;
                    }
                }
            }

            return localIp;
        } catch (Exception e) {
            log.error("Erro ao determinar o IP do servidor", e);
            return "127.0.0.1"; // Em caso de erro, retorna o endereço de loopback local
        }
    }

    /**
     * Verifica se o IP é um IP interno do Docker
     */
    private static boolean isDockerInternalIp(String ip) {
        return ip != null && (ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("192.168.65.")); // Docker Desktop for Mac VM gateway range
    }

    /**
     * Verifica se o IP é alcançável
     */
    private static boolean isReachable(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isReachable(1000); // Timeout de 1 segundo
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtém o IP da máquina hospedeira a partir das variáveis de ambiente
     */
    private static String getHostIpFromEnv() {
        for (String envVar : HOST_IP_ENV_VARS) {
            String hostIp = System.getenv(envVar);
            if (hostIp != null && !hostIp.trim().isEmpty()) {
                return hostIp.trim();
            }
        }
        return null;
    }

    /**
     * Tenta obter o IP da máquina hospedeira do Docker
     */
    private static String getDockerHostIp() {
        try {
            // Método 1: tenta obter a partir das variáveis de ambiente
            String hostIp = getHostIpFromEnv();
            if (hostIp != null) {
                return hostIp;
            }

            // Método 2: verifica hostnames especiais
            try {
                InetAddress dockerHost = InetAddress.getByName("host.docker.internal");
                return dockerHost.getHostAddress();
            } catch (Exception e) {
                // Ignora o erro, continua tentando outros métodos
            }

            try {
                InetAddress dockerHost = InetAddress.getByName("docker.host.internal");
                return dockerHost.getHostAddress();
            } catch (Exception e) {
                // Ignora o erro, continua tentando outros métodos
            }

            // Método 3: tenta localizar host.docker.internal ou docker.host.internal no arquivo /etc/hosts
            File hostsFile = new File("/etc/hosts");
            if (hostsFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(hostsFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("host.docker.internal") || line.contains("docker.host.internal")) {
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 1) {
                                return parts[0];
                            }
                        }
                    }
                }
            }

            // Método 4: tenta obter o IP do gateway padrão
            String gatewayIp = getDockerGatewayIp();
            if (gatewayIp != null) {
                return gatewayIp;
            }

            // Método 5: tenta obter o IP de uma rede não-Docker via interface de rede
            String nonDockerIp = getNonDockerLocalIp();
            if (nonDockerIp != null) {
                return nonDockerIp;
            }

            // Método 6: tenta o gateway padrão
            for (String gateway : DOCKER_DEFAULT_GATEWAYS) {
                if (isReachable(gateway)) {
                    return gateway;
                }
            }

        } catch (Exception e) {
            log.warn("Falha ao obter o IP da máquina hospedeira do Docker: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Tenta obter o IP do gateway Docker
     */
    private static String getDockerGatewayIp() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("windows")) {
            return getWindowsGatewayIp();
        } else {
            return getLinuxGatewayIp();
        }
    }

    /**
     * Obtém o IP do gateway em ambiente Windows
     */
    private static String getWindowsGatewayIp() {
        try {
            // Usa o comando route do Windows
            Process process = Runtime.getRuntime().exec(new String[]{"route", "print", "0.0.0.0"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Analisa a saída do route print do Windows
                    // Formato semelhante a: 0.0.0.0 0.0.0.0 192.168.1.1 192.168.1.100 1
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 4) {
                        if (parts[0].equals("0.0.0.0") && parts[1].equals("0.0.0.0")) {
                            String gateway = parts[2];
                            if (gateway.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                log.debug("IP do gateway do Windows: {}", gateway);
                                return gateway;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Falha na detecção do gateway do Windows: {}", e.getMessage());
        }

        // Tenta usar o comando ipconfig
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ipconfig"});
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                String currentAdapter = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.contains("适配器") || line.contains("Adapter")) {
                        currentAdapter = line;
                    } else if (line.contains("默认网关") || line.contains("Default Gateway")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            String gateway = parts[1].trim();
                            if (gateway.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                log.debug("Gateway padrão do Windows: {} (adaptador: {})", gateway, currentAdapter);
                                return gateway;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Falha na detecção via ipconfig do Windows: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Obtém o IP do gateway em ambiente Linux
     */
    private static String getLinuxGatewayIp() {
        List<String[]> commands = new ArrayList<>();
        commands.add(new String[]{"ip", "route", "show", "default"});
        commands.add(new String[]{"route", "-n"});
        commands.add(new String[]{"netstat", "-rn"});

        for (String[] cmdArray : commands) {
            try {
                Process process = Runtime.getRuntime().exec(cmdArray);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Tenta extrair o IP do gateway
                        String gatewayIp = extractGatewayIp(line, cmdArray[0]);
                        if (gatewayIp != null) {
                            log.debug("IP do gateway do Linux: {} (comando: {})", gatewayIp, String.join(" ", cmdArray));
                            return gatewayIp;
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        // Tenta ler diretamente a tabela de rotas
        try {
            File routeFile = new File("/proc/net/route");
            if (routeFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(routeFile))) {
                    String line;
                    // Pula a linha de cabeçalho
                    reader.readLine();
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 2 && parts[1].equals("00000000")) {
                            // Encontrou a rota padrão, analisa o endereço do gateway
                            String hex = parts[2];
                            // Converte o hexadecimal em little-endian para um endereço IP
                            if (hex.length() == 8) {
                                int a = Integer.parseInt(hex.substring(6, 8), 16);
                                int b = Integer.parseInt(hex.substring(4, 6), 16);
                                int c = Integer.parseInt(hex.substring(2, 4), 16);
                                int d = Integer.parseInt(hex.substring(0, 2), 16);
                                String gateway = a + "." + b + "." + c + "." + d;
                                log.debug("IP do gateway lido de /proc/net/route: {}", gateway);
                                return gateway;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao ler a tabela de rotas do Linux: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Extrai o IP do gateway a partir da saída de um comando
     */
    private static String extractGatewayIp(String line, String command) {
        try {
            if ("ip".equals(command)) {
                // Analisa uma saída semelhante a "default via 172.17.0.1 dev eth0"
                Pattern pattern = Pattern.compile("default via (\\d+\\.\\d+\\.\\d+\\.\\d+)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } else if ("route".equals(command) || "netstat".equals(command)) {
                // Analisa a saída de route -n ou netstat -rn
                // O formato geralmente é: Destination Gateway Genmask Flags ...
                // 0.0.0.0 192.168.1.1 0.0.0.0 UG ...
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 3) {
                    if (parts[0].equals("0.0.0.0") || parts[0].equals("default")) {
                        // A segunda coluna geralmente é o gateway
                        String gateway = parts[1];
                        if (gateway.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                            return gateway;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao analisar o IP do gateway: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Obtém o IP local de uma rede não-Docker
     */
    private static String getNonDockerLocalIp() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Pula interfaces de rede relacionadas ao Docker
                String name = networkInterface.getName();
                if (name.startsWith("docker") || name.startsWith("br-") ||
                        name.equals("docker0") || name.contains("veth")) {
                    continue;
                }

                // Pula interfaces desabilitadas e de loopback
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // Obtém o endereço IPv4 da interface
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();

                        // Verifica se é um IP privado
                        boolean isPrivate = false;
                        for (String pattern : PRIVATE_IP_PATTERNS) {
                            if (ip.matches(pattern)) {
                                isPrivate = true;
                                break;
                            }
                        }

                        // Se for um IP privado e não for um IP de rede Docker, pode ser o IP da máquina hospedeira
                        if (isPrivate && !isDockerInternalIp(ip)) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao obter o IP de rede não-Docker: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Classe de informações de IP
     */
    public static class IPInfo {
        private String ip;
        private String location;
        private String isp;
        private boolean isCloudProvider;
        private boolean isPrivateIp;

        public IPInfo(String ip, String location, String isp) {
            this.ip = ip;
            this.location = location != null ? location : "";
            this.isp = isp != null ? isp : "";
            this.isCloudProvider = checkIsCloudProvider();
            this.isPrivateIp = checkIsPrivateIp();
        }

        public String getIp() {
            return ip;
        }

        public String getLocation() {
            return location;
        }

        public String getIsp() {
            return isp;
        }

        public boolean isCloudProvider() {
            return isCloudProvider;
        }

        public boolean isPrivateIp() {
            return isPrivateIp;
        }

        /**
         * Determina se é um ambiente de servidor
         * 1. Se for uma faixa de IP de provedor de nuvem, considera ambiente de servidor
         * 2. Se as informações de IP contiverem palavras-chave de provedor de nuvem, considera ambiente de servidor
         * 3. Se não for IP privado e não for IP de operadora comum, pode ser ambiente de servidor
         */
        public boolean isServerEnvironment() {
            // Se for faixa de IP de provedor de nuvem ou as informações de IP contiverem palavras-chave de provedor de nuvem, considera ambiente de servidor
            if (isCloudProvider) {
                return true;
            }

            // Se for IP privado, não é ambiente de servidor
            if (isPrivateIp) {
                return false;
            }

            // Verifica se é um IP de operadora
            boolean isIsp = false;
            for (String keyword : ISP_KEYWORDS) {
                if (isp.contains(keyword)) {
                    isIsp = true;
                    break;
                }
            }

            // Se não for IP de operadora, pode ser ambiente de servidor
            return !isIsp;
        }

        /**
         * Verifica se é um IP de provedor de nuvem (apenas por palavras-chave)
         */
        private boolean checkIsCloudProvider() {
            // Verifica se as informações de IP contêm palavras-chave de provedor de nuvem
            for (String keyword : CLOUD_KEYWORDS) {
                if (location.contains(keyword) || isp.contains(keyword)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Verifica se é um IP privado
         */
        private boolean checkIsPrivateIp() {
            for (String pattern : PRIVATE_IP_PATTERNS) {
                if (ip.matches(pattern)) {
                    return true;
                }
            }

            return ip.startsWith("127.") || ip.equals("0.0.0.0") || ip.equals("localhost");
        }
    }

    /**
     * Obtém informações de geolocalização a partir do endereço IP informado
     */
    public static IPInfo getIPInfoByAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty() || "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return null;
        }

        // Para endereços IP privados, não realiza consulta de geolocalização
        if (isPrivateIp(ipAddress)) {
            return new IPInfo(ipAddress, null, "Rede interna");
        }

        // Primeiro tenta usar os IP_INFO_SERVICES existentes (prioriza os serviços já configurados)
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            String queryUrl = IP_INFO_SERVICES[0];
            // cip.cc suporta adicionar o parâmetro de IP diretamente após a URL
            queryUrl = queryUrl + ipAddress;

            URL url = URI.create(queryUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }

                String content = response.toString();
                IPInfo ipInfo = parseIPInfo(queryUrl, content);
                if (ipInfo != null) {
                    // Força a definição do endereço IP para o IP especificado (pois o serviço pode retornar outro IP)
                    return new IPInfo(ipAddress, ipInfo.getLocation(), ipInfo.getIsp());
                }
            }
        } catch (Exception e) {
            log.debug("Falha ao consultar o IP {}: {}", ipAddress, e.getMessage());
        } finally {
            try {
                if (reader != null) reader.close();
                if (connection != null) connection.disconnect();
            } catch (Exception e) {
                // ignore
            }
        }

        // Se nenhum dos serviços existentes conseguir consultar, retorna informações básicas de IP
        return new IPInfo(ipAddress, "Localização desconhecida", "Operadora desconhecida");
    }

    /**
     * Verifica se é um IP privado
     */
    private static boolean isPrivateIp(String ip) {
        if (ip == null) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // 10.0.0.0/8
            if (first == 10) return true;
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) return true;
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            // 127.0.0.0/8 (loopback)
            if (first == 127) return true;

            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Obtém informações de IP (IP público do servidor)
     */
    private static IPInfo getIPInfo() {
        for (String service : IP_INFO_SERVICES) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            long startTime = System.currentTimeMillis();

            try {
                URL url = URI.create(service).toURL();
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(3000); // Timeout de conexão de 3 segundos
                connection.setReadTimeout(3000); // Timeout de leitura de 3 segundos
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

                // Inicia a conexão
                connection.connect();

                // Verifica se houve timeout
                if (System.currentTimeMillis() - startTime > 3000) {
                    continue;
                }

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;

                    // Define o tempo máximo de leitura
                    long maxReadTime = startTime + 3000;

                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");

                        // Verifica se o tempo máximo de leitura foi excedido
                        if (System.currentTimeMillis() > maxReadTime) {
                            break;
                        }
                    }

                    // Se houve timeout mas já foram lidos dados parciais, continua o processamento
                    if (System.currentTimeMillis() <= maxReadTime || response.length() > 0) {
                        String content = response.toString();

                        // Analisa as informações de IP
                        IPInfo ipInfo = parseIPInfo(service, content);
                        if (ipInfo != null) {
                            return ipInfo;
                        }
                    }
                } else {
                    log.warn("O serviço de informações de IP retornou um status diferente de 200: {} - {}", service, connection.getResponseCode());
                }
            } catch (java.net.SocketTimeoutException e) {
                log.warn("Timeout ao obter informações de IP, trocando para o próximo serviço: {} - {}", service, e.getMessage());
            } catch (Exception e) {
                log.warn("Falha ao obter informações de IP: {} - {}", service, e.getMessage());
            } finally {
                // Fecha os recursos
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                } catch (Exception e) {
                    log.warn("Falha ao fechar os recursos: {}", e.getMessage());
                }
            }

        }

        return null;
    }

    /**
     * Analisa as informações de IP a partir da resposta de diferentes serviços
     */
    private static IPInfo parseIPInfo(String service, String content) {
        try {
            if (service.contains("cip.cc")) {
                // Extrai o IP
                Pattern patternIp = Pattern.compile("IP\\s*:\\s*([\\d.]+)");
                Matcher matcherIp = patternIp.matcher(content);

                if (matcherIp.find()) {
                    String ip = matcherIp.group(1);

                    // Extrai as informações de endereço e operadora - lógica de análise aprimorada
                    String location = "";
                    String isp = "";

                    // Analisa as informações de endereço - procura a linha no formato "地址 : xxx" (Address : xxx, retornado pelo cip.cc em chinês)
                    Pattern patternAddr = Pattern.compile("地址\\s*:\\s*([^\n]+)");
                    Matcher matcherAddr = patternAddr.matcher(content);
                    if (matcherAddr.find()) {
                        location = matcherAddr.group(1).trim();
                        // Mantém apenas as informações básicas de localização, geralmente no formato "País Estado/Província Cidade"
                        if (location.contains(" ")) {
                            String[] parts = location.split("\\s+");
                            // Usa as três primeiras partes como informação de endereço
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < Math.min(parts.length, 3); i++) {
                                if (!parts[i].isEmpty()) {
                                    if (sb.length() > 0) {
                                        sb.append(" ");
                                    }
                                    sb.append(parts[i]);
                                }
                            }
                            location = sb.toString();
                        }
                    }

                    // Analisa as informações de operadora - procura a linha no formato "运营商 : xxx" (Carrier : xxx, retornado pelo cip.cc em chinês)
                    Pattern patternIsp = Pattern.compile("运营商\\s*:\\s*([^\n]+)");
                    Matcher matcherIsp = patternIsp.matcher(content);
                    if (matcherIsp.find()) {
                        isp = matcherIsp.group(1).trim();
                        // Mantém apenas o nome da operadora, removendo possíveis informações extras
                        for (String keyword : ISP_KEYWORDS) {
                            if (isp.contains(keyword)) {
                                isp = keyword;
                                break;
                            }
                        }

                        // Se nenhuma palavra-chave for encontrada, usa a primeira palavra como nome da operadora
                        if (isp.contains(" ")) {
                            isp = isp.split("\\s+")[0];
                        }
                    }

                    // Se não for possível extrair a operadora, tenta extrair a partir do endereço
                    if (isp.isEmpty()) {
                        String originalLocation = matcherAddr.group(1).trim();
                        for (String keyword : ISP_KEYWORDS) {
                            if (originalLocation.contains(keyword)) {
                                isp = keyword;
                                break;
                            }
                        }
                    }

                    return new IPInfo(ip, location, isp);
                }
            } else if (service.contains("ipip.net")) {
                // IPIP.net
                // {"ret":"ok","data":{"ip":"139.226.72.136","location":["中国","上海","上海","","联通"]}}
                Pattern patternIp = Pattern.compile("\"ip\":\"([\\d.]+)\"");
                Pattern patternCountry = Pattern.compile("\\[\"([^\"]*?)\""); // Corresponde ao primeiro elemento do array location (país)
                Pattern patternProvince = Pattern.compile("\\[\"[^\"]*?\",\"([^\"]*?)\""); // Corresponde ao segundo elemento do array location (estado/província)
                Pattern patternCity = Pattern.compile("\\[\"[^\"]*?\",\"[^\"]*?\",\"([^\"]*?)\""); // Corresponde ao terceiro elemento do array location (cidade)
                Pattern patternIsp = Pattern
                        .compile("\\[\"[^\"]*?\",\"[^\"]*?\",\"[^\"]*?\",\"[^\"]*?\",\"([^\"]*?)\""); // Corresponde ao quinto elemento do array location (operadora)
                Matcher matcherIp = patternIp.matcher(content);
                Matcher matcherCountry = patternCountry.matcher(content);
                Matcher matcherProvince = patternProvince.matcher(content);
                Matcher matcherCity = patternCity.matcher(content);
                Matcher matcherIsp = patternIsp.matcher(content);

                if (matcherIp.find()) {
                    String ip = matcherIp.group(1);
                    String country = matcherCountry.find() ? matcherCountry.group(1) : "";
                    String province = matcherProvince.find() ? matcherProvince.group(1) : "";
                    String city = matcherCity.find() ? matcherCity.group(1) : "";
                    String isp = matcherIsp.find() ? matcherIsp.group(1) : "";

                    String location = country + " " + province + " " + city;
                    location = location.trim().replaceAll("\\s+", " ");
                    return new IPInfo(ip, location, isp);
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao analisar as informações de IP: {}", e.getMessage());
            // Exceção na análise, retorna null
        }

        return null;
    }

    /**
     * Limpa o conteúdo HTML
     */
    private static String cleanHtml(String html) {
        if (html == null) {
            return "";
        }

        // Remove todas as tags HTML
        String noHtml = html.replaceAll("<[^>]+>", " ");

        // Remove espaços extras
        noHtml = noHtml.replaceAll("\\s+", " ").trim();

        // Remove JavaScript
        noHtml = noHtml.replaceAll("(?i)\\bjavascript\\b.*?;", "");

        return noHtml;
    }

    /**
     * Obtém o endereço IP local (não-loopback)
     * Em ambiente Docker, considera obter o IP de uma rede não-Docker
     */
    private static String getLocalIpAddress() {
        try {
            boolean isInDocker = isRunningInDocker();

            // Se estiver em ambiente Docker, primeiro tenta obter o IP da máquina hospedeira
            if (isInDocker) {
                String dockerHostIp = getDockerHostIp();
                if (dockerHostIp != null) {
                    return dockerHostIp;
                }
            }

            // Obtém todas as interfaces de rede
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> candidateAddresses = new ArrayList<>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Pula interfaces desabilitadas e de loopback
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // Se estiver rodando no Docker, prioriza interfaces de rede não-Docker
                String name = networkInterface.getName();
                boolean isDockerInterface = name.startsWith("docker") || name.startsWith("br-") ||
                        name.equals("docker0") || name.contains("veth");

                if (isInDocker && isDockerInterface) {
                    // Em ambiente Docker, dá prioridade mais baixa à interface Docker, mas não a exclui totalmente
                    // Processado mais adiante
                } else {
                    // Obtém o endereço IPv4 da interface
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()
                                && !address.isLinkLocalAddress()) {
                            String ip = address.getHostAddress();

                            // Prioriza retornar o IP de interfaces sem fio ou cabeadas
                            if (name.startsWith("wl") || name.startsWith("en") ||
                                    name.startsWith("eth") || name.startsWith("wlan") ||
                                    name.startsWith("wifi")) {
                                return ip;
                            }

                            candidateAddresses.add(address);
                        }
                    }
                }
            }

            // Se nenhuma interface prioritária foi encontrada, mas há outros endereços candidatos, retorna o primeiro
            if (!candidateAddresses.isEmpty()) {
                return candidateAddresses.get(0).getHostAddress();
            }

            // Se nenhuma interface não-Docker foi encontrada, percorre novamente, desta vez incluindo interfaces Docker
            if (isInDocker) {
                networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                        continue;
                    }

                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (address instanceof Inet4Address && !address.isLoopbackAddress()
                                && !address.isLinkLocalAddress()) {
                            return address.getHostAddress();
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Falha ao obter o endereço IP local: {}", e.getMessage(), e);
        }

        return "127.0.0.1"; // Se nenhum IP adequado for encontrado, retorna o endereço de loopback
    }

    /**
     * Detecta se está rodando dentro de um container Docker
     */
    private static boolean isRunningInDocker() {
        try {
            // Método 1: verifica se o arquivo .dockerenv existe
            if (new File("/.dockerenv").exists()) {
                return true;
            }

            // Método 2: verifica variáveis de ambiente (método mais confiável)
            String[] dockerEnvVars = {
                "DOCKER_CONTAINER", 
                "KUBERNETES_SERVICE_HOST", 
                "KUBERNETES_PORT",
                "DOCKER_HOST",
                "COMPOSE_PROJECT_NAME"
            };
            for (String envVar : dockerEnvVars) {
                String value = System.getenv(envVar);
                if (value != null) {
                    log.debug("Variável de ambiente Docker detectada {}: {}", envVar, value);
                    return true;
                }
            }

            // Método 3: verifica se o hostname contém identificadores relacionados ao Docker
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && (hostname.contains("docker") || hostname.contains("container"))) {
                log.debug("Hostname relacionado ao Docker detectado: {}", hostname);
                return true;
            }

            // Método 4: verifica o tipo de sistema operacional, checando o sistema de arquivos /proc apenas em ambiente Linux
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                // Verifica as informações de cgroup
                File cgroupFile = new File("/proc/1/cgroup");
                if (cgroupFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(cgroupFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("docker") || line.contains("kubepods")) {
                                log.debug("Identificador Docker detectado no cgroup: {}", line);
                                return true;
                            }
                        }
                    }
                }

                // Verifica a árvore de processos
                File selfCgroupFile = new File("/proc/self/cgroup");
                if (selfCgroupFile.exists()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(selfCgroupFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("docker") || line.contains("kubepods")) {
                                log.debug("Identificador Docker detectado no self cgroup: {}", line);
                                return true;
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Exceção ao detectar o ambiente Docker: {}", e.getMessage());
            // Ignora a exceção, continua verificando outros métodos
        }

        return false;
    }

}