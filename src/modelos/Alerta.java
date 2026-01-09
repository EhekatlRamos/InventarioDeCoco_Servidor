/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelos;

/**
 *
 * @author shari
 */
public class Alerta {
    private int id;
    private int productoId;
    private String mensaje;
    private boolean notificada;
    
    public Alerta() {
    }
    
    
    public Alerta(int id, int productoId, String mensaje, boolean notificada) {
        this.id = id;
        this.productoId = productoId;
        this.mensaje = mensaje;
        this.notificada = notificada;
    }
    

    public Alerta(int productoId, String mensaje) {
        this.productoId = productoId;
        this.mensaje = mensaje;
        this.notificada = false; 
    }
    
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getProductoId() {
        return productoId;
    }
    
    public void setProductoId(int productoId) {
        this.productoId = productoId;
    }
    
    public String getMensaje() {
        return mensaje;
    }
    
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
    
    public boolean isNotificada() {
        return notificada;
    }
    
    public void setNotificada(boolean notificada) {
        this.notificada = notificada;
    }
    

    @Override
    public String toString() {
        return "Alerta{" +
                "id=" + id +
                ", productoId=" + productoId +
                ", mensaje='" + mensaje + '\'' +
                ", notificada=" + notificada +
                '}';
    }
    

    public String toProtocolo() {
        return "ALERTA:" + productoId;
    }
}
