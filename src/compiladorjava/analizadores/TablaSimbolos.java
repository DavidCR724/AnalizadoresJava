/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package compiladorjava.analizadores;

/**
 *
 * @author carlo
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class TablaSimbolos {
    
    // Una pila de diccionarios. El tope de la pila es el ámbito actual (local).
    private Stack<Map<String, Simbolo>> pilaAmbitos;

    public TablaSimbolos() {
        pilaAmbitos = new Stack<>();
        entrarAmbito(); // Ámbito global por defecto
    }

    // Se llama cuando el parser lee una llave abierta '{'
    public void entrarAmbito() {
        pilaAmbitos.push(new HashMap<>());
    }

    // Se llama cuando el parser lee una llave cerrada '}'
    public void salirAmbito() {
        if (!pilaAmbitos.isEmpty()) {
            pilaAmbitos.pop();
        }
    }

    public void insertarVariable(String nombre, String tipo, int linea, int col) {
        Map<String, Simbolo> ambitoActual = pilaAmbitos.peek();
        
        if (ambitoActual.containsKey(nombre)) {
            throw new RuntimeException("Error Semántico: La variable '" + nombre + "' ya está declarada en este ámbito. [Línea " + linea + "]");
        }
        ambitoActual.put(nombre, new Simbolo(nombre, tipo, "VARIABLE"));
    }

    // Busca la variable desde el ámbito más local hasta el global
    public String obtenerTipoVariable(String nombre, int linea, int col) {
        for (int i = pilaAmbitos.size() - 1; i >= 0; i--) {
            Map<String, Simbolo> ambito = pilaAmbitos.get(i);
            if (ambito.containsKey(nombre)) {
                return ambito.get(nombre).tipo;
            }
        }
        throw new RuntimeException("Error Semántico: La variable '" + nombre + "' no ha sido declarada. [Línea " + linea + "]");
    }
}

// Clase de apoyo
class Simbolo {
    String nombre;
    String tipo;
    String categoria; // "VARIABLE", "METODO", etc.

    public Simbolo(String nombre, String tipo, String categoria) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
    }
}
