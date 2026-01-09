/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server_cocoinv;

import BaseDeDatos.ConexionBD;
import java.util.ArrayList;
import java.util.List;
import modelos.Producto;
import java.sql.Connection; 
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import modelos.Alerta;


/**
 *
 * @author shari
 */
public class AlertasInventario implements Runnable {

 private ServerCocoInv servidor;   
    private boolean ejecutando = true;
    private static final long CICLO_ALERTA = 5000;
    
    
    
    
    public AlertasInventario(ServerCocoInv servidor) {
        this.servidor = servidor;
    }
    

    @Override
    public void run() {
        servidor.log("Monitor de inventario iniciado (verificando cada " + (CICLO_ALERTA/1000) + " segundos)");
        
        while (ejecutando) {
            try {
              
                Thread.sleep(CICLO_ALERTA);
                

                List<Producto> productos = obtenerProductos();
                

                for (Producto producto : productos) {
                    if (producto.necesitaAlerta()) {
                        procesarAlerta(producto);
                    }
                }
                
            } catch (InterruptedException e) {
                servidor.log("Ciclo alertas se interrumpio");
                break;
            } catch (Exception e) {
                servidor.log("Error en el ciclo de alertas: " + e.getMessage());
            }
        }
        
        servidor.log("Paro el ciclo de alertas");
    }
    

    private List<Producto> obtenerProductos() {
        List<Producto> productos = new ArrayList<>();
        String query = "SELECT * FROM productos WHERE vigencia = 1";
        
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
            
        } catch (SQLException e) {
            servidor.log("Error al obtener productos para monitoreo: " + e.getMessage());
        }
        
        return productos;
    }
    
    
    private void procesarAlerta(Producto producto) {
        if (existeAlertaReciente(producto.getId())) {
            return; 
        }
        
        
        String mensaje = "¡ALERTA! El producto '" + producto.getNombre() + "' tiene stock bajo. Cantidad actual: " + producto.getCantidadActual() + " | Umbral mínimo: " + producto.getUmbralMinimo();
        
        
        int alertaId = insertarAlerta(producto.getId(), mensaje);
        
        if (alertaId > 0) {
            
            Alerta alerta = new Alerta(alertaId, producto.getId(), mensaje, false);
            
            List<Integer> usuariosSuscritos = obtenerUsuariosSuscritos(producto.getId());
            
            
            if (!usuariosSuscritos.isEmpty()) {
                int notificados = servidor.notificarAlertaSuscriptores(alerta, usuariosSuscritos);
                crearAlertasPendientes(alertaId, usuariosSuscritos);
                servidor.log("ALERTA GENERADA: Producto " + producto.getId() +  " | Suscritos: " + usuariosSuscritos.size() + " | Conectados notificados: " + notificados);
                
                
            } else {
                servidor.log("ALERTA GENERADA: Producto " + producto.getId() +  " | Sin suscriptores");
            }
            
        }
    }
    
    
    private boolean existeAlertaReciente(int productoId) {
        String query = "SELECT COUNT(*) as total FROM alertas " + "WHERE producto_id = ? ";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, productoId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total") > 0;
            }
            
        } catch (SQLException e) {
            servidor.log("Error al verificar alertas recientes: " + e.getMessage());
        }
        
        return false;
    }
    

    
    private List<Integer> obtenerUsuariosSuscritos(int productoId) {
        List<Integer> usuarios = new ArrayList<>();
        String query = "SELECT usuario_id FROM suscripciones WHERE producto_id = ?";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, productoId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                usuarios.add(rs.getInt("usuario_id"));
            }
            
        } catch (SQLException e) {
            servidor.log("Error al obtener suscriptores: " + e.getMessage());
        }
        
        return usuarios;
    }

    
    
    
    private void crearAlertasPendientes(int alertaId, List<Integer> usuariosSuscritos) {
        String query = "INSERT INTO alertas_pendientes (alerta_id, usuario_id, notificada) " + "VALUES (?, ?, 0) " + "ON DUPLICATE KEY UPDATE notificada = notificada"; 
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            for (int usuarioId : usuariosSuscritos) {
                stmt.setInt(1, alertaId);
                stmt.setInt(2, usuarioId);
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            
        } catch (SQLException e) {
            servidor.log("Error al crear alertas pendientes: " + e.getMessage());
        }
    }

    
    
    
    private int insertarAlerta(int productoId, String mensaje) {
        String query = "INSERT INTO alertas (producto_id, mensaje) VALUES (?, ?)";
        
        try (Connection conn = ConexionBD.conectarBD();
             PreparedStatement stmt = conn.prepareStatement(query,  Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, productoId);
            stmt.setString(2, mensaje);
            
            int filasAfectadas = stmt.executeUpdate();
            
            
            
            if (filasAfectadas > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1); // Regresa id
                }
            }
            
        } catch (SQLException e) {
            servidor.log("Error al insertar alerta: " + e.getMessage());
        }
        
        return -1;
    }
    
    
    
    
    public void detener() {
        ejecutando = false;
    }
}