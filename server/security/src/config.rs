use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize)]
pub struct ServerConfig {
    pub domain: String,
    pub port: u16,
    pub name: String,
}

#[derive(Serialize, Deserialize)]
pub struct AuthConfig {
    pub jwt_secret: String,        // ← Ahora guardamos el secreto aquí (temporal)
    pub jwt_expire_days: u32,
    pub admin_password_hash: String,
}

#[derive(Serialize, Deserialize)]
pub struct QrConfig {
    pub expiration_minutes: u32,
}

#[derive(Serialize, Deserialize)]
pub struct Config {
    pub server: ServerConfig,
    pub auth: AuthConfig,
    pub qr: QrConfig,
}