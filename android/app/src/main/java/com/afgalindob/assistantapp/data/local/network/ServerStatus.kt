package com.afgalindob.assistantapp.data.local.network

enum class ServerStatus {
    NAME_REQUIRED,      // Falta el nombre del usuario
    UNLINKED,           // Sin configuración previa
    DISCONNECTED,       // VPN/Server no alcanzable
    READY_TO_CONNECT,   // Dominio y Nombre listos para escanear/enviar
    AWAITING_APPROVAL,  // Petición en Redis, esperando al Admin
    ONLINE              // Autorizado y funcional
}