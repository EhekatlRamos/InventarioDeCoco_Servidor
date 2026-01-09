/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package server_cocoinv;

import BaseDeDatos.ConexionBD;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import modelos.Alerta;





/**
 *
 * @author shari
 */
public class ServerCocoInv {
    private static final int PUERTO = 1234;
    private static final int PUERTO_ALERTAS = 1235;
    private ServerSocket serverSocket;
    private ServerSocket serverSocketAlertas; 
    private AlertasInventario monitor;
    private Thread hiloMonitor;

    private Set<ConectarClientes> clientesConectados;
    
    private boolean ejecutando = false;
    

    public ServerCocoInv() {
        clientesConectados = Collections.synchronizedSet(new HashSet<>());
    }
    

    public void iniciar() {
        
        
        if (!ConexionBD.probarConexion()) {
            log("Error, no se puede conectar la BD");
            return;
        }
        log("BD conectada");
        log("");
        

        
        try {
            serverSocket = new ServerSocket(PUERTO);
            serverSocketAlertas = new ServerSocket(PUERTO_ALERTAS);
            ejecutando = true;
            log("Servidor iniciado en puerto " + PUERTO);
            log("Esperando conexiones de clientes...");
            log("");
            
        } catch (IOException e) {
            log("ERROR: No se pudo iniciar el servidor en puerto " + PUERTO);
            log("  " + e.getMessage());
            return;
        }
        
        
         new Thread(() -> aceptarClientesAlertas()).start();

        monitor = new AlertasInventario(this);
        hiloMonitor = new Thread(monitor);
        hiloMonitor.start();
        log("Monitor de inventario iniciado");
        log("");
        
        aceptarClientes();
    }
    
 
    private void aceptarClientes() {
        while (ejecutando) {
            try {
                Socket socketCliente = serverSocket.accept();
                log("Cliente conectado");
                ConectarClientes manejador = new ConectarClientes(socketCliente, this);
                Thread hiloCliente = new Thread(manejador);
                hiloCliente.start();
            } catch (IOException ex) {
                System.getLogger(ServerCocoInv.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
    }
    

    public synchronized void agregarCliente(ConectarClientes cliente) {
        clientesConectados.add(cliente);
        log("Clientes conectados: " + clientesConectados.size());
    }
    

    
    public synchronized void removerCliente(ConectarClientes cliente) {
        clientesConectados.remove(cliente);
        log("clientes conectados: " + clientesConectados.size());
    }
    

    
    public void hacerAlerta(Alerta alerta) {
        String mensaje = alerta.toProtocolo();
        
        log("------------------");
        log("Alertando");
        log("Mensaje: " + alerta.getMensaje());
        log("Clientes: " + clientesConectados.size());
        

        
        
        Set<ConectarClientes> copiaClientes;
        synchronized (this) {
            copiaClientes = new HashSet<>(clientesConectados);
        }
        
        int notificados = 0;
        for (ConectarClientes cliente : copiaClientes) {
            try {
                cliente.enviarMensaje(mensaje);
                notificados++;
                log(" Alerta enviada a: " + cliente.getUsuario());
            } catch (Exception e) {
                log("No se pudo alertar a " + cliente.getUsuario() + ": " + e.getMessage());
            }
        }
        
        log("Alertas enviadas: " + notificados + "/" + copiaClientes.size());
        log("-------------------");
        log("");
    }
    

    
    public void log(String mensaje) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        System.out.println("[" + timestamp + "] " + mensaje);
    }
    

    
    public void detener() {
        log("");
        log("Deteniendo servidor...");
        ejecutando = false;
        if (monitor != null) {
            monitor.detener();
            log("✓ Monitor de inventario detenido");
        }
        

        
        
        synchronized (this) {
            log("Cerrando " + clientesConectados.size() + " conexiones con clientes");
            clientesConectados.clear();
        }
        
    
        
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                log("ServerSocket cerrado");
            }
        } catch (IOException e) {
            log("No se pudo cerrar el servidor " + e.getMessage());
        }
        

        
        ConexionBD.desconectarBD();
        log("Servidor detenido");
    }
    
    public int notificarAlertaSuscriptores(Alerta alerta, List<Integer> usuariosSuscritos) {
    String mensaje = alerta.toProtocolo();

    log("Alertando a los conectados");
    log("Producto ID: " + alerta.getProductoId());
    log("Suscriptores totales: " + usuariosSuscritos.size());
    
    Set<ConectarClientes> copiaClientes;
    synchronized (this) {
        copiaClientes = new HashSet<>(clientesConectados);
    }
    
    int notificados = 0;
    for (ConectarClientes cliente : copiaClientes) {
        if (usuariosSuscritos.contains(cliente.getUsuarioId())) {
            try {
                cliente.enviarMensaje(mensaje);
                notificados++;
                log("Alerta enviada a: " + cliente.getUsuario());
            } catch (Exception e) {
                log("No se pudo notificar a " + cliente.getUsuario());
            }
        }
    }
    
    log("Clientes conectados notificados: " + notificados);
    log("--------");
    
    return notificados;
}
    
    
    
    
    
    
    public static void main(String[] args) {
        // TODO code application logic here
    
        ServerCocoInv servidor = new ServerCocoInv();
        
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            servidor.detener();
        }));
        
        servidor.iniciar();
    }

     private void aceptarClientesAlertas() {
        while (ejecutando) {
            try {
                Socket socketAlerta = serverSocketAlertas.accept();
                log("Nuevo receptor de alertas conectado");
            } catch (IOException e) {
                if(ejecutando) log("Error en socket de alertas: " + e.getMessage());
            }
        }
    }
}
    
    
    
    
  