package com.xiaozhi.ai.llm.tool;

/**
 * Tipo de ferramenta
 */
public enum ToolType{

    NONE(1, "Após chamar a ferramenta, não realiza nenhuma outra ação"),
    WAIT(2, "Chama a ferramenta e aguarda o retorno da função"),
    CHANGE_SYS_PROMPT(3, "Modifica o System Prompt, alternando a personalidade ou as responsabilidades do papel/role"),
    SYSTEM_CTL(4, "Controle de sistema que afeta o fluxo normal da conversa, como sair, tocar música, etc.; requer a passagem do parâmetro conn"),
    IOT_CTL(5, "Controle de dispositivos IoT; requer a passagem do parâmetro conn"),
    MCP_CLIENT(6, "Cliente MCP");

    private final int code;
    private final String desc;

    ToolType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
