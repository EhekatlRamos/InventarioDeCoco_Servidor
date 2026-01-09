/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelos;

/**
 *
 * @author shari
 */
public class Producto {
    private int id;
    private String nombre;
    private String descripcion;
    private int cantidadActual;
    private int umbralMinimo;
    private double precio;
    private boolean vigencia; // true = activo, false = inactivo
    

    //llenar los datos después
    public Producto() {
    }
    

    // leer desde la BD
public Producto(int id, String nombre, String descripcion, 
                    int cantidadActual, int umbralMinimo, double precio, boolean vigencia) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadActual = cantidadActual;
        this.umbralMinimo = umbralMinimo;
        this.precio = precio;
        this.vigencia = vigencia;  
    }


    // insertar un producto en la BD
    public Producto(String nombre, String descripcion, int cantidadActual, int umbralMinimo, double precio) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadActual = cantidadActual;
        this.umbralMinimo = umbralMinimo;
        this.precio = precio;
        this.vigencia = true; 
    }
    

    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public int getCantidadActual() {
        return cantidadActual;
    }
    
    public void setCantidadActual(int cantidadActual) {
        this.cantidadActual = cantidadActual;
    }
    
    public int getUmbralMinimo() {
        return umbralMinimo;
    }
    
    public void setUmbralMinimo(int umbralMinimo) {
        this.umbralMinimo = umbralMinimo;
    }
    
    public double getPrecio() {
        return precio;
    }
    
    public void setPrecio(double precio) {
        this.precio = precio;
    }
    
    public boolean isVigencia() {
        return vigencia;
    }
    
    public void setVigencia(boolean vigencia) {
        this.vigencia = vigencia;
    }

    
    
    
    
    public boolean necesitaAlerta() {
        return cantidadActual <= umbralMinimo;
    }
    

    
    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", cantidadActual=" + cantidadActual +
                ", umbralMinimo=" + umbralMinimo +
                ", precio=" + precio +
                '}';
    }

    public String toMensaje() {
        return "PRODUCTO:" + id + "|" + nombre + "|" + descripcion + "|" + 
               cantidadActual + "|" + umbralMinimo + "|" + precio + "|" + 
               (vigencia ? "1" : "0");
    }
}
