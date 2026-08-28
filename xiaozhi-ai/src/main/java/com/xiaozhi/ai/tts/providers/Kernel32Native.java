package com.xiaozhi.ai.tts.providers;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.WString;

/**
 * Acesso mínimo à API Kernel32 do Windows, usado somente para ajustar a ordem
 * de busca de DLLs nativas (SetDllDirectory) antes de carregar o sherpa-onnx.
 * Não é referenciado em nenhuma plataforma que não seja Windows.
 */
interface Kernel32Native extends Library {
    Kernel32Native INSTANCE = Native.load("kernel32", Kernel32Native.class);

    boolean SetDllDirectoryW(WString lpPathName);
}
