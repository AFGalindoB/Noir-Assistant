use std::io::{self, Write};

pub fn required_input(prompt: &str) -> String {
    loop {
        print!("{}", prompt);
        io::stdout().flush().unwrap();

        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let trimmed = input.trim().to_string();

        if !trimmed.is_empty() {
            return trimmed;
        }
        println!("Este campo es obligatorio. Inténtalo de nuevo.");
    }
}

pub fn required_u16(prompt: &str) -> u16 {
    loop {
        print!("{}", prompt);
        io::stdout().flush().unwrap();

        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let trimmed = input.trim();

        if trimmed.is_empty() {
            println!("Este campo es obligatorio.");
            continue;
        }

        match trimmed.parse::<u16>() {
            Ok(n) if n > 0 => return n,
            _ => println!("Por favor ingresa un número de puerto válido (1-65535)."),
        }
    }
}

pub fn required_u32(prompt: &str) -> u32 {
    loop {
        print!("{}", prompt);
        io::stdout().flush().unwrap();

        let mut input = String::new();
        io::stdin().read_line(&mut input).unwrap();
        let trimmed = input.trim();

        if trimmed.is_empty() {
            println!("Este campo es obligatorio.");
            continue;
        }

        match trimmed.parse::<u32>() {
            Ok(n) if n > 0 => return n,
            _ => println!("Por favor ingresa un número válido mayor a 0 y menor a 4.294.967.296."),
        }
    }
}