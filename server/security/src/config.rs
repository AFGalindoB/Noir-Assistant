use serde::{Serialize, Deserialize};
use std::sync::OnceLock;
use zeroize::Zeroize;

#[derive(Serialize, Deserialize, Clone)]
pub struct ServerConfig {
    pub domain: String,
    pub port: u16,
    pub name: String,
}

#[derive(Serialize, Deserialize, Clone, Zeroize)]
#[zeroize(drop)]
pub struct AuthConfig {
    pub jwt_secret_path: String,
    pub jwt_expire_days: u32,
    pub admin_password_hash: String,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct QrConfig {
    pub expiration_minutes: u32,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct Config {
    pub server: ServerConfig,
    pub auth: AuthConfig,
    pub qr: QrConfig,
}

#[derive(Zeroize)]
#[zeroize(drop)]
pub struct InMemorySecret {
    pub content: String,
}

pub static CONFIG_CONTAINER: OnceLock<Config> = OnceLock::new();
pub static JWT_SECRET_CONTAINER: OnceLock<InMemorySecret> = OnceLock::new();

pub fn get_config_from_ram() -> Result<&'static Config, anyhow::Error> {
    CONFIG_CONTAINER.get()
        .ok_or_else(|| anyhow::anyhow!("Fallo crítico: El config.json no ha sido cargado en RAM."))
}

pub fn get_jwt_secret_from_ram() -> Result<&'static str, anyhow::Error> {
    JWT_SECRET_CONTAINER.get()
        .map(|storage| storage.content.as_str())
        .ok_or_else(|| anyhow::anyhow!("Fallo crítico: El secreto criptográfico no ha sido cargado en RAM."))
}