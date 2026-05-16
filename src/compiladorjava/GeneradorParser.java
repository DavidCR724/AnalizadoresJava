/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package compiladorjava;

/**
 *
 * @author carlo
 */

public class GeneradorParser {
    
    public static void main(String[] args) {
        // 1. Definir la ruta exacta donde está tu archivo Parser.cup
        // Nota: En NetBeans, la ruta suele empezar desde la raíz del proyecto, pasando por "src"
        String rutaBase = "src/compiladorjava/analizadores/";
        String rutaCup = rutaBase + "Parser.cup";
        
        System.out.println("Iniciando generación de Parser...");
        
        try {
            // 2. Parámetros que le enviaríamos a la consola
            String[] opcionesCUP = {
                "-destdir", rutaBase,     // Dónde guardar el Parser.java y sym.java
                "-parser",  "Parser",     // Nombre del archivo .java generado
                "-symbols", "sym",        // Nombre del archivo de símbolos
                rutaCup                   // El archivo origen que vamos a leer
            };
            
            // 3. Ejecutar la herramienta CUP directamente desde Java
            java_cup.Main.main(opcionesCUP);
            
            System.out.println("Parser.java y sym.java generados con éxito en: " + rutaBase);
            
        } catch (Exception e) {
            System.err.println("Hubo un error al compilar el archivo .cup");
            e.printStackTrace();
        }
    }
}
