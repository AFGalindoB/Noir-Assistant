import tkinter as tk
from tkinter import ttk, messagebox, simpledialog, Toplevel
import qrcode
from PIL import ImageTk
import json
import data.database as db
import data.redis_manager as rm
from noir_core import noir_security
from path_manager import PathManager

class AdminApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Noir Assistant - Admin Panel")
        self.root.geometry("600x700")

        config_path = PathManager.get_config_dir()
        self.server_domain = noir_security.get_server_domain(config_path)
        self.server_port = noir_security.get_server_port(config_path)
        db.init_db()

        self.header_qr = "noir-app-v2.0.0"

        tab_control = ttk.Notebook(root)
        self.tab_request_access_qr = ttk.Frame(tab_control)
        self.tab_approve = ttk.Frame(tab_control)
        self.tab_aprroved_devices = ttk.Frame(tab_control)

        tab_control.add(self.tab_approve, text='1. Aprobar Dispositivos')
        tab_control.add(self.tab_aprroved_devices, text='2. Dispositivos Autorizados')
        tab_control.pack(expand=1, fill="both")

        self.setup_approve_tab()
        self.setup_approved_devices_tab()

    # --- PESTAÑA APROBACIÓN ---
    def setup_approve_tab(self):
        """Lista de dispositivos esperando aprobación con Redis."""
        ttk.Label(self.tab_approve, text="Dispositivos esperando autorización", font=("Arial", 10, "bold")).pack(pady=10)

        columns = ("device_id", "device_name", "time")
        self.tree = ttk.Treeview(self.tab_approve, columns=columns, show='headings', height=15)
        self.tree.heading("device_id", text="ID del Hardware")
        self.tree.heading("device_name", text="Nombre Dispositivo")
        self.tree.heading("time", text="Fecha/Hora")
        
        self.tree.column("device_id", width=150)
        self.tree.column("device_name", width=200)
        
        self.tree.pack(fill="both", expand=True, padx=10, pady=5)

        top_title = "Generar QR de Autorización"
        top_description = "Escanea este QR con el dispositivo para solicitar autorización."

        ttk.Button(self.tab_approve, text="Mostrar QR de Autorización", command=lambda: 
                        self.show_token_qr_window(
                            top_title, 
                            top_description,
                            {
                                "header": self.header_qr,
                                "base_url": f"http://{self.server_domain}:{self.server_port}",
                                "sid": rm.create_qr_session(ttl=300),
                                "endpoint": "/auth/request_autorization",
                                "qr_type": "authorization"
                            } 
                        )
                    ).pack(pady=5)
        ttk.Button(self.tab_approve, text="⟲ Actualizar Lista", command=self.refresh_pending).pack(pady=5)
        ttk.Button(self.tab_approve, text="X Rechazar Seleccionado", command=self.reject_action).pack(pady=5)
        ttk.Button(self.tab_approve, text="✓ Aprobar Seleccionado", command=self.approve_action).pack(pady=5)

    # --- PESTAÑA DISPOSITIVOS AUTORIZADOS ---
    def setup_approved_devices_tab(self):
        """Muestra los dispositivos autorizados desde SQLite."""
        ttk.Label(self.tab_aprroved_devices, text="Dispositivos Autorizados", font=("Arial", 10, "bold")).pack(pady=10)

        columns = ("device_id", "device_name", "username", "created_at", "is_active", "banned", "expires_at")
        self.approved_tree = ttk.Treeview(self.tab_aprroved_devices, columns=columns, show='headings', height=15)
        self.approved_tree.heading("device_id", text="ID del Hardware")
        self.approved_tree.heading("device_name", text="Nombre Dispositivo")
        self.approved_tree.heading("username", text="Usuario Asignado")
        self.approved_tree.heading("created_at", text="Fecha de Aprobación")
        self.approved_tree.heading("is_active", text="Activo")
        self.approved_tree.heading("banned", text="Baneado")
        self.approved_tree.heading("expires_at", text="Expira En")

        self.approved_tree.column("device_id", width=150)
        self.approved_tree.column("device_name", width=200)
        self.approved_tree.column("username", width=150)
        
        self.approved_tree.pack(fill="both", expand=True, padx=10, pady=5)

        token_payload = {
            "header": self.header_qr,
            "endpoint": "/auth/claim_token",
            "qr_type": "claim_token"
        }
        top_title = "Generar QR de Reclamo de Token"
        top_description = "Escanea este QR con el dispositivo aprobado para reclamar su token de acceso."

        ttk.Button(self.tab_aprroved_devices, text="Mostrar QR de Vinculación", command=lambda: self.show_token_qr_window(top_title, top_description, token_payload)).pack(pady=5)
        ttk.Button(self.tab_aprroved_devices, text="⟲ Actualizar Lista de Autorizados", command=self.refresh_approved).pack(pady=5)
        ttk.Button(self.tab_aprroved_devices, text="Desactivar/Activar Dispositivo", command=self.toggle_device_status).pack(pady=5)
        ttk.Button(self.tab_aprroved_devices, text="Banear/Desbanear Dispositivo", command=self.toggle_ban_device).pack(pady=5)

    def toggle_device_status(self):
        selected = self.approved_tree.selection()
        if not selected:
            messagebox.showwarning("Atención", "Selecciona un dispositivo de la lista.")
            return
        
        item = self.approved_tree.item(selected[0])['values']
        device_id = str(item[0])
        
        confirm = messagebox.askyesno("Confirmar", f"¿Deseas cambiar el estado del dispositivo '{item[1]}'?")

        if confirm:
            success, msg = db.toggle_device_status(device_id)
            if success:
                messagebox.showinfo("Éxito", "Estado del dispositivo actualizado.")
                self.refresh_approved()
            else:
                messagebox.showerror("Error", msg)

    def refresh_approved(self):
        for i in self.approved_tree.get_children(): self.approved_tree.delete(i)
        for dev in db.get_all_authorized_devices():
            self.approved_tree.insert("", "end", values=(
                dev["device_id"],
                dev["device_name"], 
                dev["username"], 
                dev["created_at"], 
                "Sí" if dev["is_active"] else "No",
                "Sí" if dev["banned"] else "No",
                dev["expires_at"]
            ))

    def refresh_pending(self):
        for i in self.tree.get_children(): self.tree.delete(i)
        for dev in rm.get_all_pending():
            self.tree.insert("", "end", values=(
                dev["device_id"], 
                dev["device_name"], 
                dev["timestamp"]
            ))

    def approve_action(self):
        selected = self.tree.selection()
        if not selected:
            messagebox.showwarning("Atención", "Selecciona un dispositivo de la lista.")
            return
        
        item = self.tree.item(selected[0])['values']
        device_id = str(item[0])
        device_name = str(item[1])

        raw_username = simpledialog.askstring(
            "Asignar Usuario", 
            f"¿A quién pertenece '{device_name}'?\n(Nota: Distingue mayúsculas y minúsculas)"
        )
        
        if raw_username is None:
            return

        username = raw_username.strip()

        if not username:
            messagebox.showerror("Error", "El nombre de usuario no puede estar vacío o contener solo espacios.")
            return
        
        token_payload = {
            "header": self.header_qr,
            "endpoint": "/auth/claim_token",
            "qr_type": "claim_token"
        }
        top_title = "Generar QR de Reclamo de Token"
        top_description = "Escanea este QR con el dispositivo aprobado para reclamar su token de acceso."

        if username is not None:
            success = db.approve_device(
                device_id=device_id,
                device_name=device_name,
                username=username
            )

            if success:
                self.refresh_pending()
                self.show_token_qr_window(top_title, top_description, token_payload)
            else:
                messagebox.showerror("Error", "No se pudo procesar la aprobación. Revisa la consola para más detalles.")

    def reject_action(self):
        selected = self.tree.selection()
        if not selected:
            messagebox.showwarning("Atención", "Selecciona un dispositivo de la lista.")
            return
        
        item = self.tree.item(selected[0])['values']
        device_id = str(item[0])
        device_name = str(item[1])

        confirm = messagebox.askyesno("Confirmar Rechazo", f"¿Deseas rechazar el dispositivo '{device_name}'?")

        if confirm:
            _, msg = rm.delete_pending_request(device_id)
            messagebox.showinfo("Rechazo Procesado", msg)
            self.refresh_pending()

    def toggle_ban_device(self):
        selected = self.approved_tree.selection()
        if not selected:
            messagebox.showwarning("Atención", "Selecciona un dispositivo de la lista.")
            return
        
        item = self.approved_tree.item(selected[0])['values']
        device_id = str(item[0])
        device_name = str(item[1])

        confirm = messagebox.askyesno("Confirmar Baneo", f"¿Deseas banear el dispositivo '{device_name}'? Esto lo bloqueará permanentemente.")

        if confirm:
            success, msg = db.toggle_device_ban(device_id)
            if success:
                messagebox.showinfo("Dispositivo Baneado", "El dispositivo ha sido baneado exitosamente.")
                self.refresh_approved()
            else:
                messagebox.showerror("Error", msg)

    def show_token_qr_window(self, title:str,  description:str, qr_payload=None):
        """Crea una ventana emergente con el QR final."""
        top = Toplevel(self.root)
        top.title(title)
        top.geometry("400x500")
        top.grab_set()

        desc_label = ttk.Label(top, text=description, wraplength=350, justify="center")
        desc_label.pack(pady=10)
        qr_label_top = ttk.Label(top)
        qr_label_top.pack(pady=20)

        self.display_qr(qr_payload, qr_label_top)
        ttk.Button(top, text="Cerrar", command=top.destroy).pack(pady=10)

    def display_qr(self, data, label_widget):
        """Renderiza cualquier payload en el widget seleccionado."""
        img = qrcode.make(json.dumps(data, separators=(',', ':')))
        img = img.resize((250, 250))
        photo = ImageTk.PhotoImage(img)
        label_widget.config(image=photo)
        label_widget.image = photo
