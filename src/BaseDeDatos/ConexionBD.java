/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package BaseDeDatos;
import java.sql.*;


/**
 *
 * @author shari
 */
public class ConexionBD {
    
    private static Connection conexion = null;
    

    public static Connection conectarBD() {
        try {
            if (conexion == null || conexion.isClosed()) {
             
                conexion = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/cocoinventario", "root", "");
                
                System.out.println("Se conecto la BD");
            }
            return conexion;
            
        } catch (SQLException e) {
            System.err.println("No se pudo conectar la bd");
            
            e.printStackTrace();
            return null;
        }
    }
    
    public static void desconectarBD() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("BD desconectada ");
            }
        } catch (SQLException e) {
            System.err.println("Error al desconectar");
        }
    }
    
    public static boolean probarConexion() {
        try {        
            Connection conn = conectarBD();
            
            if (conn != null && !conn.isClosed()) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                
                if (rs.next()) {
                    System.out.println("Bd fuc\n");
                    return true;
                }
            }
            
            System.err.println("La conexión falló\n");
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error en prueba: " + e.getMessage() + "\n");
            return false;
        }
    }
}