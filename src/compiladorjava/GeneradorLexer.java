/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package compiladorjava;

/**
 *
 * @author carlo
 */
import java.io.File;

public class GeneradorLexer {

    public static void main(String[] args) {
        // 1. Definir la ruta exacta de tu archivo Lexer.flex
        String rutaFlex = "src/compiladorjava/analizadores/Lexer.flex";

        System.out.println("Iniciando generación de Lexer...");

        try {
            // 2. Crear un arreglo de Strings con la ruta (como si fuera un comando)
            String[] opcionesJFlex = { rutaFlex };
            
            // 3. Ejecutar JFlex usando el arreglo de opciones
            jflex.Main.generate(opcionesJFlex);

            System.out.println("Lexer.java generado con éxito.");

        } catch (Exception e) {
            System.err.println("Hubo un error al compilar el archivo .flex");
            e.printStackTrace();
        }
    }
}