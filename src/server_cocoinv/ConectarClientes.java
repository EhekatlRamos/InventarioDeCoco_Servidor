/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_cocoinv;

import BaseDeDatos.ConexionBD;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.io.IOException;
import java.sql.SQLException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import modelos.Producto;

/**
 *
 * @author shari
 */
public class ConectarClientes implements Runnable {
    
    private Socket socketCliente;
    private ServerCocoInv servidor;
    private DataInputStream entrada;      // ✅ CAMBIO: Usar DataInputStream
    private DataOutputStream salida;      // ✅ CAMBIO: Usar DataOutputStream
    private String usuarioActual;
    private int usuarioId = -1;
    private boolean autenticado = false;
    
    public ConectarClientes(Socket socketCliente, ServerCocoInv servidor) {
        this.socketCliente = socketCliente;
        this.servidor = servidor;
    }
    
    @Override
    public void run() {
        try {
            // ✅ CAMBIO: Inicializar streams como el cliente espera
            entrada = new DataInputStream(socketCliente.getInputStream());
            salida = new DataOutputStream(socketCliente.getOutputStream());
            
            while (true) {
                // ✅ CAMBIO: Leer con readUTF() como el cliente envía
                String msg = entrada.readUTF();
                servidor.log("► Mensaje recibido: " + msg);
                
                // LOGIN
                if (msg.startsWith("LOGIN:")) {
                    String[] partes = msg.substring(6).split("\\|");
                    if (partes.length == 2) {
                        String usuario = partes[0];
                        String password = partes[1];
                        
                        int id = validarLogin(usuario, password);
                        if (id > 0) {
                            this.usuarioActual = usuario;
                            this.usuarioId = id;
                            this.autenticado = true;
                            
                            // ✅ CAMBIO: Responder con boolean como el cliente espera
                            salida.writeBoolean(true);
                            salida.flush();
                            
                            servidor.agregarCliente(this);
                            servidor.log("✓ Usuario autenticado: " + usuario + " (ID: " + id + ")");
                            
                            enviarAlertasPendientes();
                        } else {
                            // ✅ CAMBIO: Enviar false
                            salida.writeBoolean(false);
                            salida.flush();
                            servidor.log("✗ Login fallido para: " + usuario);
                        }
                    }
                    continue;
                }
                
                if (!autenticado) {
                    continue;
                }
                
                // ✅ CAMBIO: El cliente envía "GET_LISTADO"
                if (msg.equals("GET_LISTADO")) {
                    mostrarProductos();
                }
                else if (msg.startsWith("MOSTRAR_TODOS:")) {
                    mostrarTodosProductos();
                }
                else if (msg.startsWith("ACTUALIZAR_UMBRAL:")) {
                    String[] partes = msg.substring(18).split("\\|");
                    if (partes.length == 2) {
                        int id = Integer.parseInt(partes[0]);
                        int nuevoUmbral = Integer.parseInt(partes[1]);
                        actualizarUmbral(id, nuevoUmbral);
                    }
                }
                else if (msg.startsWith("ACTUALIZAR_CANTIDAD:")) {
                    String[] partes = msg.substring(20).split("\\|");
                    if (partes.length == 2) {
                        int id = Integer.parseInt(partes[0]);
                        int nuevaCantidad = Integer.parseInt(partes[1]);
                        actualizarCantidad(id, nuevaCantidad);
                    }
                }
                else if (msg.startsWith("AGREGAR_PRODUCTO:")) {
                    String[] partes = msg.substring(17).split("\\|");
                    if (partes.length == 5) {
                        agregarProducto(partes[0], partes[1], 
                                       Integer.parseInt(partes[2]),
                                       Integer.parseInt(partes[3]),
                                       Double.parseDouble(partes[4]));
                    }
                }
                else if (msg.startsWith("DESACTIVAR_PRODUCTO:")) {
                    int id = Integer.parseInt(msg.substring(20));
                    desactivarProducto(id);
                }
                else if (msg.startsWith("ACTIVAR_PRODUCTO:")) {
                    int id = Integer.parseInt(msg.substring(17));
                    activarProducto(id);
                }
                else if (msg.startsWith("SUSCRIBIR:")) {
                    int productoId = Integer.parseInt(msg.substring(10));
                    suscribirseAProducto(productoId);
                }
                else if (msg.startsWith("DESUSCRIBIR:")) {
                    int productoId = Integer.parseInt(msg.substring(12));
                    desuscribirseDeProducto(productoId);
                }
                else if (msg.startsWith("MIS_SUSCRIPCIONES:")) {
                    mostrarSuscripciones();
                }
            }
            
        } catch (EOFException e) {
            servidor.log("Cliente desconectado: " + usuarioActual);
        } catch (IOException ex) {
            servidor.log("Error con cliente " + usuarioActual + ": " + ex.getMessage());
        } finally {
            cerrarConexion();
        }
    }
    
    private int validarLogin(String usuario, String password) {
        String query = "SELECT id FROM usuarios WHERE usuario = ? AND password = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, usuario);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
            
        } catch (SQLException e) {
            servidor.log("✗ Error al validar login: " + e.getMessage());
        }
        return -1;
    }
    
    private void enviarAlertasPendientes() {
        String query = "SELECT DISTINCT a.producto_id " +
                      "FROM alertas a " +
                      "INNER JOIN alertas_pendientes ap ON a.id = ap.alerta_id " +
                      "WHERE ap.usuario_id = ? AND ap.notificada = 0";
                
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            
            int contador = 0;
            while (rs.next()) {
                int productoId = rs.getInt("producto_id");
                // ✅ Enviar alerta con writeUTF
                salida.writeUTF("ALERTA:" + productoId);
                contador++;
            }
            
            if (contador > 0) {
                servidor.log("Enviadas " + contador + " alertas pendientes a " + usuarioActual);
                
                String updateQuery = "UPDATE alertas_pendientes " +
                                    "SET notificada = 1, fecha_notificacion = NOW() " +
                                    "WHERE usuario_id = ? AND notificada = 0";
                
                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setInt(1, usuarioId);
                    updateStmt.executeUpdate();
                }
            }
            
        } catch (SQLException | IOException e) {
            servidor.log("Error al enviar alertas pendientes: " + e.getMessage());
        }
    }
    

    private void mostrarProductos() {
        String query = "SELECT * FROM productos WHERE vigencia = 1 ORDER BY nombre";
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = ConexionBD.conectarBD();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Producto p = new Producto(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("descripcion"),
                    rs.getInt("cantidad_actual"),
                    rs.getInt("umbral_minimo"),
                    rs.getDouble("precio"),
                    rs.getBoolean("vigencia")
                );
                productos.add(p);
            }
            

            salida.writeUTF("LISTA_INICIO:");
            
            for (Producto p : productos) {
                salida.writeUTF(p.toMensaje());
            }
            
            salida.writeUTF("LISTA_FIN:");
            salida.flush();
            
            servidor.log("Lista de productos enviada a " + usuarioActual + " (" + productos.size() + " productos)");
            
        } catch (SQLException | IOException e) {
            servidor.log("Error al mostrar: " + e.getMessage());
        }
    }
    
    private void mostrarTodosProductos() {
        String query = "SELECT * FROM productos ORDER BY vigencia DESC, nombre";
        mostrarProductosConQuery(query, "todos");
    }
    
    private void mostrarProductosConQuery(String query, String tipo) {
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = ConexionBD.conectarBD();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Producto p = new Producto(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("descripcion"),
                    rs.getInt("cantidad_actual"),
                    rs.getInt("umbral_minimo"),
                    rs.getDouble("precio"),
                    rs.getBoolean("vigencia")
                );
                productos.add(p);
            }
            
            salida.writeUTF("LISTA_INICIO:");
            for (Producto p : productos) {
                salida.writeUTF(p.toMensaje());
            }
            salida.writeUTF("LISTA_FIN:");
            salida.flush();
            
            servidor.log("Lista de productos " + tipo + " enviada a " + usuarioActual);
            
        } catch (SQLException | IOException e) {
            servidor.log("Error al mostrar: " + e.getMessage());
        }
    }
    
    private void actualizarUmbral(int id, int nuevoUmbral) {
        String query = "UPDATE productos SET umbral_minimo = ? WHERE id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nuevoUmbral);
            stmt.setInt(2, id);
            
            stmt.executeUpdate();
            servidor.log("Umbral actualizado: Producto " + id + " -> " + nuevoUmbral);
            
        } catch (SQLException e) {
            servidor.log("Error al actualizar umbral: " + e.getMessage());
        }
    }
    
    private void actualizarCantidad(int id, int nuevaCantidad) {
        String query = "UPDATE productos SET cantidad_actual = ? WHERE id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nuevaCantidad);
            stmt.setInt(2, id);
            
            stmt.executeUpdate();
            servidor.log("Cantidad actualizada: Producto " + id + " -> " + nuevaCantidad);
            
        } catch (SQLException e) {
            servidor.log("Error al actualizar cantidad: " + e.getMessage());
        }
    }
    
    private void agregarProducto(String nombre, String descripcion, 
                                  int cantidad, int umbral, double precio) {
        String query = "INSERT INTO productos (nombre, descripcion, cantidad_actual, " +
                      "umbral_minimo, precio, vigencia) VALUES (?, ?, ?, ?, ?, 1)";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, nombre);
            stmt.setString(2, descripcion);
            stmt.setInt(3, cantidad);
            stmt.setInt(4, umbral);
            stmt.setDouble(5, precio);
            
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int nuevoId = rs.getInt(1);
                servidor.log("Producto agregado: " + nombre + " (ID: " + nuevoId + ")");
            }
            
        } catch (SQLException e) {
            servidor.log("Error al agregar producto: " + e.getMessage());
        }
    }
    
    private void desactivarProducto(int id) {
        String query = "UPDATE productos SET vigencia = 0 WHERE id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
            servidor.log("✓ Producto desactivado: ID " + id);
            
        } catch (SQLException e) {
            servidor.log("✗ Error al desactivar producto: " + e.getMessage());
        }
    }
    
    private void activarProducto(int id) {
        String query = "UPDATE productos SET vigencia = 1 WHERE id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
            servidor.log("Producto activado: ID " + id);
            
        } catch (SQLException e) {
            servidor.log("Error al activar producto: " + e.getMessage());
        }
    }
    
    private void suscribirseAProducto(int productoId) {
        String query = "INSERT INTO suscripciones (usuario_id, producto_id) VALUES (?, ?)";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, usuarioId);
            stmt.setInt(2, productoId);
            stmt.executeUpdate();
            servidor.log("✓ " + usuarioActual + " suscrito a producto " + productoId);
            
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate")) {
                servidor.log("✗ Error al suscribir: " + e.getMessage());
            }
        }
    }
    
    private void desuscribirseDeProducto(int productoId) {
        String query = "DELETE FROM suscripciones WHERE usuario_id = ? AND producto_id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, usuarioId);
            stmt.setInt(2, productoId);
            stmt.executeUpdate();
            servidor.log("✓ " + usuarioActual + " desuscrito de producto " + productoId);
            
        } catch (SQLException e) {
            servidor.log("Error al desuscribirse: " + e.getMessage());
        }
    }
    
    private void mostrarSuscripciones() {
        String query = "SELECT p.* FROM productos p " +
                      "INNER JOIN suscripciones s ON p.id = s.producto_id " +
                      "WHERE s.usuario_id = ? ORDER BY p.nombre";
        
        List<Producto> productos = new ArrayList<>();
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Producto p = new Producto(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("descripcion"),
                    rs.getInt("cantidad_actual"),
                    rs.getInt("umbral_minimo"),
                    rs.getDouble("precio"),
                    rs.getBoolean("vigencia")
                );
                productos.add(p);
            }
            
            salida.writeUTF("SUSCRIPCIONES_INICIO:");
            for (Producto p : productos) {
                salida.writeUTF(p.toMensaje());
            }
            salida.writeUTF("SUSCRIPCIONES_FIN:");
            salida.flush();
            
            servidor.log("Suscripciones enviadas: " + usuarioActual);
            
        } catch (SQLException | IOException e) {
            servidor.log("Error al mostrar suscripciones: " + e.getMessage());
        }
    }
    
    // ✅ CAMBIO: Enviar alertas con writeUTF
    public void enviarMensaje(String mensaje) {
        if (socketCliente != null && !socketCliente.isClosed()) {
            try {
                salida.writeUTF(mensaje);
                salida.flush();
            } catch (IOException e) {
                servidor.log("Error al enviar alerta a " + usuarioActual);
            }
        }
    }
    
    private void cerrarConexion() {
        try {
            servidor.removerCliente(this);
            
            if (socketCliente != null && !socketCliente.isClosed()) {
                socketCliente.close();
            }
            
            servidor.log("Cliente desconectado: " + usuarioActual);
            
        } catch (IOException e) {
            servidor.log("No se pudo desconectar");
        }
    }
    
    public String getUsuario() {
        return usuarioActual;
    }
    
    public int getUsuarioId() {
        return usuarioId;
    }
}