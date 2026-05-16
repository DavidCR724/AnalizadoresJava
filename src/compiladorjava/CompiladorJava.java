package compiladorjava;

import compiladorjava.analizadores.Lexer;
import compiladorjava.analizadores.Parser;
import compiladorjava.analizadores.sym;
import java_cup.runtime.Symbol;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalizadorJava.java
 *
 * Vista de analizador léxico/sintáctico para el compilador Java reducido.
 * Usa el Lexer (JFlex), Parser (CUP) y sym generados automáticamente.
 *
 * Pestañas:
 *   1. Tokens      → tabla léxica con lexema, patrón, categoría y línea
 *   2. Resultado   → traza de ejecución (variables declaradas con sus valores)
 *   3. Símbolos    → tabla de símbolos (nombre | tipo | valor | línea)
 *   4. Excepciones → errores léxicos y sintácticos
 */
public class CompiladorJava extends JFrame {

    // =========================================================
    //  PALETA DE COLORES
    // =========================================================
    private static final Color BG_MAIN      = new Color(245, 245, 245);
    private static final Color BG_PANEL     = new Color(255, 255, 255);
    private static final Color COLOR_ACCENT = new Color(30,  100, 200);   // azul Java
    private static final Color COLOR_BORDER = new Color(215, 215, 215);
    private static final Color TEXT_LIGHT   = new Color(30,   30,  30);
    private static final Color TEXT_MUTED   = new Color(110, 110, 110);
    private static final Color PILL_BG      = new Color(225, 235, 255);
    private static final Color PILL_TEXT    = new Color(20,   70, 180);

    private static final Color COLOR_OK     = new Color(20,  130,  60);
    private static final Color COLOR_ERR    = new Color(190,  30,  30);
    private static final Color COLOR_INFO   = new Color(30,   90, 200);
    private static final Color COLOR_VAL    = new Color(130,   0, 160);
    private static final Color COLOR_KW     = new Color(150,  60,   0);

    private static final Font FUENTE_MONO  = new Font("Consolas",  Font.PLAIN, 14);
    private static final Font FUENTE_UI    = new Font("Segoe UI",  Font.PLAIN, 13);
    private static final Font FUENTE_UI_B  = new Font("Segoe UI",  Font.BOLD,  13);
    private static final Font FUENTE_SMALL = new Font("Segoe UI",  Font.BOLD,  11);

    // =========================================================
    //  COMPONENTES
    // =========================================================
    private JTextPane editorCodigo;

    private DefaultTableModel modeloTokens;
    private JTable            tablaTokens;

    private JTextPane salidaResultado;

    private DefaultTableModel modeloSimbolos;
    private JTable            tablaSimbolos;

    private JTextPane salidaExcepciones;

    private JButton btnAnalizar;
    private JButton btnLimpiar;
    private JButton btnEjemplo;

    // Captura los errores que el Lexer/Parser imprimen a System.err
    private final StringBuilder bufferErrores = new StringBuilder();

    // =========================================================
    //  CONSTRUCTOR
    // =========================================================
    public CompiladorJava() {
        super("Analizador Java — Compilador");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setPreferredSize(new Dimension(1300, 800));
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        UIManager.put("Panel.background",            BG_MAIN);
        UIManager.put("OptionPane.background",        BG_MAIN);
        UIManager.put("OptionPane.messageForeground", TEXT_LIGHT);

        add(crearBarraSuperior(), BorderLayout.NORTH);
        add(crearCuerpo(),        BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        agregarCodigoEjemplo();
    }

    // =========================================================
    //  BARRA SUPERIOR
    // =========================================================
    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(BG_MAIN);
        barra.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(12, 20, 12, 20)));

        // Título
        JPanel panelTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelTitulo.setOpaque(false);
        JLabel punto = new JLabel("◆");
        punto.setForeground(COLOR_ACCENT);
        punto.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel titulo = new JLabel("Analizador Java");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(TEXT_LIGHT);
        panelTitulo.add(punto);
        panelTitulo.add(titulo);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        panelBotones.setOpaque(false);

        btnLimpiar  = crearBotonPill("Limpiar",    false);
        btnEjemplo  = crearBotonPill("Ejemplo",    false);
        btnAnalizar = crearBotonPill("▶ Analizar", true);

        btnAnalizar.addActionListener(e -> ejecutarAnalisis());
        btnLimpiar .addActionListener(e -> limpiarTodo());
        btnEjemplo .addActionListener(e -> agregarCodigoEjemplo());

        panelBotones.add(btnLimpiar);
        panelBotones.add(btnEjemplo);
        panelBotones.add(btnAnalizar);

        barra.add(panelTitulo,  BorderLayout.WEST);
        barra.add(panelBotones, BorderLayout.EAST);
        return barra;
    }

    private JButton crearBotonPill(String texto, boolean isPrimary) {
        JButton btn = new JButton(texto) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if      (getModel().isPressed())  g2.setColor(new Color(210, 220, 240));
                else if (getModel().isRollover()) g2.setColor(new Color(230, 238, 255));
                else                              g2.setColor(BG_MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(isPrimary ? COLOR_ACCENT : COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FUENTE_UI_B);
        btn.setForeground(isPrimary ? COLOR_ACCENT : TEXT_LIGHT);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(isPrimary ? new Dimension(120, 32) : new Dimension(100, 32));
        return btn;
    }

    // =========================================================
    //  CUERPO PRINCIPAL
    // =========================================================
    private Component crearCuerpo() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                crearPanelEditor(),
                crearPanelDerecho());
        split.setResizeWeight(0.35);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(BG_MAIN);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    public void setBorder(Border b) {}
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(COLOR_BORDER);
                        g.fillRect(0, 0, getSize().width, getSize().height);
                    }
                };
            }
        });
        return split;
    }

    // =========================================================
    //  PANEL EDITOR (izquierda)
    // =========================================================
    private JPanel crearPanelEditor() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);

        JLabel lbl = new JLabel("  EDITOR DE CÓDIGO — Java reducido");
        lbl.setFont(FUENTE_SMALL);
        lbl.setForeground(TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(100, 35));
        lbl.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

        editorCodigo = new JTextPane();
        editorCodigo.setFont(FUENTE_MONO);
        editorCodigo.setBackground(BG_PANEL);
        editorCodigo.setForeground(TEXT_LIGHT);
        editorCodigo.setCaretColor(TEXT_LIGHT);
        editorCodigo.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scroll = new JScrollPane(editorCodigo);
        scroll.setBorder(new MatteBorder(0, 0, 0, 1, COLOR_BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(lbl,    BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    //  PANEL DERECHO  (Tabs + Referencia de tokens)
    // =========================================================
    private JSplitPane crearPanelDerecho() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                crearPanelTabs(),
                crearPanelReferencia());
        split.setResizeWeight(0.72);
        split.setDividerSize(2);
        split.setBorder(null);
        split.setBackground(BG_MAIN);
        return split;
    }

    // =========================================================
    //  PESTAÑAS
    // =========================================================
    private JPanel crearPanelTabs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FUENTE_UI_B);
        tabs.setBackground(BG_MAIN);
        tabs.setForeground(TEXT_MUTED);
        tabs.setFocusable(false);
        tabs.setBorder(new MatteBorder(0, 0, 0, 1, COLOR_BORDER));

        // ── Tab 1: Tokens ─────────────────────────────────────
        String[] colsTokens = {"LEXEMA", "PATRÓN", "CATEGORÍA", "LÍNEA"};
        modeloTokens = new DefaultTableModel(colsTokens, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaTokens = new JTable(modeloTokens);
        configurarEstiloTablaTokens(tablaTokens);
        JScrollPane scrollTok = new JScrollPane(tablaTokens);
        scrollTok.setBorder(null);
        scrollTok.getViewport().setBackground(BG_MAIN);

        // ── Tab 2: Resultado (traza) ──────────────────────────
        salidaResultado = crearAreaTexto();

        // ── Tab 3: Tabla de Símbolos ──────────────────────────
        String[] colsSim = {"IDENTIFICADOR", "TIPO", "VALOR", "LÍNEA"};
        modeloSimbolos = new DefaultTableModel(colsSim, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaSimbolos = new JTable(modeloSimbolos);
        configurarEstiloTablaSimbolos(tablaSimbolos);
        JScrollPane scrollSim = new JScrollPane(tablaSimbolos);
        scrollSim.setBorder(null);
        scrollSim.getViewport().setBackground(BG_MAIN);

        // ── Tab 4: Excepciones ────────────────────────────────
        salidaExcepciones = crearAreaTexto();

        tabs.addTab("Tokens",      scrollTok);
        tabs.addTab("Resultado",   new JScrollPane(salidaResultado));
        tabs.addTab("Símbolos",    scrollSim);
        tabs.addTab("Excepciones", new JScrollPane(salidaExcepciones));

        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    //  ESTILOS TABLA TOKENS
    // =========================================================
    private void configurarEstiloTablaTokens(JTable tabla) {
        tabla.setBackground(BG_MAIN);
        tabla.setForeground(TEXT_LIGHT);
        tabla.setFont(FUENTE_UI);
        tabla.setRowHeight(35);
        tabla.setShowGrid(true);
        tabla.setGridColor(COLOR_BORDER);
        tabla.setBorder(null);

        JTableHeader h = tabla.getTableHeader();
        h.setBackground(BG_MAIN);
        h.setForeground(TEXT_MUTED);
        h.setFont(FUENTE_SMALL);
        h.setPreferredSize(new Dimension(100, 35));
        h.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

        // Columna CATEGORÍA → píldora
        tabla.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
                p.setBackground(sel ? t.getSelectionBackground() : BG_MAIN);
                p.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

                String categ = value != null ? value.toString() : "";
                Color[] cols = colorParaCategoria(categ);

                JLabel pill = new JLabel(" " + categ + " ") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(cols[0]);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                pill.setFont(FUENTE_SMALL);
                pill.setForeground(cols[1]);
                p.add(pill);
                return p;
            }
        });

        DefaultTableCellRenderer base = baseRenderer();
        tabla.getColumnModel().getColumn(0).setCellRenderer(base);
        tabla.getColumnModel().getColumn(1).setCellRenderer(base);
        tabla.getColumnModel().getColumn(3).setCellRenderer(base);
    }

    private Color[] colorParaCategoria(String cat) {
        switch (cat) {
            case "RESERVADA":
                return new Color[]{new Color(255, 245, 210), new Color(150, 80, 0)};
            case "OPERADOR":
                return new Color[]{new Color(240, 220, 255), new Color(100, 0, 160)};
            case "LITERAL":
                return new Color[]{new Color(220, 250, 230), new Color(0, 110, 50)};
            case "SEPARADOR":
                return new Color[]{new Color(235, 235, 235), new Color(80, 80, 80)};
            case "IDENTIFICADOR":
                return new Color[]{PILL_BG, PILL_TEXT};
            default:
                return new Color[]{COLOR_BORDER, TEXT_MUTED};
        }
    }

    // =========================================================
    //  ESTILOS TABLA SÍMBOLOS
    // =========================================================
    private void configurarEstiloTablaSimbolos(JTable tabla) {
        tabla.setBackground(BG_MAIN);
        tabla.setForeground(TEXT_LIGHT);
        tabla.setFont(FUENTE_UI);
        tabla.setRowHeight(35);
        tabla.setShowGrid(true);
        tabla.setGridColor(COLOR_BORDER);
        tabla.setBorder(null);

        JTableHeader h = tabla.getTableHeader();
        h.setBackground(BG_MAIN);
        h.setForeground(TEXT_MUTED);
        h.setFont(FUENTE_SMALL);
        h.setPreferredSize(new Dimension(100, 35));
        h.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

        // Columna TIPO → píldora con color por tipo
        tabla.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
                p.setBackground(sel ? t.getSelectionBackground() : BG_MAIN);
                p.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

                String tipo = value != null ? value.toString() : "";
                Color bg, fg;
                switch (tipo) {
                    case "int":
                        bg = new Color(220, 240, 255); fg = new Color(0, 80, 160);    break;
                    case "boolean":
                        bg = new Color(255, 235, 235); fg = new Color(160, 0, 0);     break;
                    case "void":
                        bg = new Color(240, 240, 240); fg = new Color(80, 80, 80);    break;
                    default:
                        bg = PILL_BG; fg = PILL_TEXT; break;
                }

                JLabel pill = new JLabel(" " + tipo + " ") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(bg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                pill.setFont(FUENTE_SMALL);
                pill.setForeground(fg);
                p.add(pill);
                return p;
            }
        });

        DefaultTableCellRenderer base = baseRenderer();
        tabla.getColumnModel().getColumn(0).setCellRenderer(base);
        tabla.getColumnModel().getColumn(2).setCellRenderer(base);
        tabla.getColumnModel().getColumn(3).setCellRenderer(base);
    }

    private DefaultTableCellRenderer baseRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                ((JComponent) c).setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, COLOR_BORDER),
                        new EmptyBorder(0, 10, 0, 0)));
                return c;
            }
        };
    }

    // =========================================================
    //  ANÁLISIS PRINCIPAL
    // =========================================================
    private void ejecutarAnalisis() {
        String codigo = editorCodigo.getText().trim();
        limpiarResultados();
        bufferErrores.setLength(0);

        if (codigo.isEmpty()) {
            appendTexto(salidaExcepciones, "Sin código para analizar.\n", COLOR_ERR, false);
            return;
        }

        // Redirigir System.err para capturar mensajes del Lexer/Parser
        java.io.PrintStream errOriginal = System.err;
        java.io.ByteArrayOutputStream errBuf = new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(errBuf));

        // ── FASE 1: ANÁLISIS LÉXICO ────────────────────────────
        List<Symbol> tokens = new ArrayList<>();
        try {
            Lexer lexer = new Lexer(new StringReader(codigo));
            Symbol tok;
            while ((tok = lexer.next_token()).sym != sym.EOF) {
                tokens.add(tok);
            }
        } catch (Exception ex) {
            System.setErr(errOriginal);
            appendTexto(salidaExcepciones,
                    " Error Léxico interno\n\n" + ex.getMessage() + "\n", COLOR_ERR, false);
            return;
        }

        // Mostrar tokens en la tabla
        mostrarTokensEnTabla(tokens);

        // ── FASE 2: ANÁLISIS SINTÁCTICO ────────────────────────
        boolean sinErrores = true;
        try {
            java_cup.runtime.DefaultSymbolFactory sf =
                    new java_cup.runtime.DefaultSymbolFactory();
            Lexer lexer2 = new Lexer(new StringReader(codigo));
            Parser parser = new Parser(lexer2, sf);
            parser.parse();
        } catch (Exception ex) {
            sinErrores = false;
            System.setErr(errOriginal);
            String errCapturado = errBuf.toString().trim();
            appendTexto(salidaExcepciones,
                    " Error Sintáctico\n\n", COLOR_ERR, true);
            if (!errCapturado.isEmpty()) {
                appendTexto(salidaExcepciones, errCapturado + "\n", COLOR_ERR, false);
            } else {
                String msg = ex.getMessage() != null ? ex.getMessage()
                                                     : ex.getClass().getSimpleName();
                appendTexto(salidaExcepciones, msg + "\n", COLOR_ERR, false);
            }
        }

        System.setErr(errOriginal);

        // Mostrar errores léxicos que el Lexer haya impreso a stderr
        String errLex = errBuf.toString().trim();
        if (!errLex.isEmpty()) {
            appendTexto(salidaExcepciones, "\n  Advertencias Léxicas\n\n", COLOR_ERR, true);
            appendTexto(salidaExcepciones, errLex + "\n", COLOR_ERR, false);
            sinErrores = false;
        }

        if (sinErrores) {
            appendTexto(salidaResultado, "  Análisis completado sin errores.\n\n",
                    COLOR_OK, true);
            appendTexto(salidaExcepciones, "0 excepciones detectadas.", TEXT_MUTED, false);
            construirTrazaYSimbolos(tokens);
        } else {
            appendTexto(salidaResultado,
                    "  El código contiene errores. Revisa la pestaña Excepciones.\n",
                    COLOR_ERR, false);
            // Aun así mostramos los símbolos que pudimos detectar
            construirTrazaYSimbolos(tokens);
        }
    }

    // =========================================================
    //  TRAZA DE EJECUCIÓN + TABLA DE SÍMBOLOS
    //
    //  Recorre la lista de tokens (emitida por el Lexer) e
    //  interpreta sentencias de declaración de variables:
    //
    //    [modificador]  TIPO  ID  ASIGNACION  expresion  PUNTO_COMA
    //    [modificador]  TIPO  ID  PUNTO_COMA
    //
    //  Los tokens de modificador (PUBLIC / PRIVATE) se ignoran en
    //  la traza pero se registran.
    //  Las expresiones se evalúan recursivamente.
    // =========================================================
    private void construirTrazaYSimbolos(List<Symbol> tokens) {

        // tablas locales de símbolos
        Map<String, Object>  tablaValores = new LinkedHashMap<>();
        Map<String, String>  tablaTipos   = new LinkedHashMap<>();
        Map<String, Integer> tablaLineas  = new LinkedHashMap<>();

        appendTexto(salidaResultado,
                "── Traza de declaraciones de variables ─────────────\n\n",
                TEXT_MUTED, true);

        int i = 0;
        while (i < tokens.size()) {
            Symbol t = tokens.get(i);

            // Saltar modificadores de acceso
            if (t.sym == sym.PUBLIC || t.sym == sym.PRIVATE) {
                i++;
                continue;
            }

            // Detectar inicio de declaración: INT, BOOLEAN o VOID
            boolean esTipoDecl = (t.sym == sym.INT
                               || t.sym == sym.BOOLEAN
                               || t.sym == sym.VOID);

            if (!esTipoDecl) {
                i++;
                continue;
            }

            String tipoDecl = nombreTipo(t.sym);
            int lineaDecl   = t.left;   // JFlex ya reporta 1-based (yyline + 1)
            i++;

            // Siguiente debe ser un ID
            if (i >= tokens.size() || tokens.get(i).sym != sym.ID) {
                i++;
                continue;
            }
            String nombreVar = tokenValor(tokens.get(i));
            i++;

            // Verificar si hay ASIGNACION o PUNTO_COMA
            if (i >= tokens.size()) break;

            Object valor = null;

            if (tokens.get(i).sym == sym.ASIGNACION) {
                i++; // consumir =

                // Recolectar tokens de la expresión hasta PUNTO_COMA o fin
                List<Symbol> exprToks = new ArrayList<>();
                while (i < tokens.size() && tokens.get(i).sym != sym.PUNTO_COMA) {
                    exprToks.add(tokens.get(i));
                    i++;
                }
                if (i < tokens.size()) i++; // consumir ;

                valor = evaluarExpresion(exprToks, tablaValores);

            } else if (tokens.get(i).sym == sym.PUNTO_COMA) {
                i++; // declaración sin valor inicial
                valor = valorDefault(tipoDecl);
            } else if (tokens.get(i).sym == sym.COMA || tokens.get(i).sym == sym.PARENTESIS_C) {
                i++; 
                valor = "Parámetro"; 
            } else {
                // Estructura desconocida: avanzar
                i++;
                continue;
            }

            // Guardar en tablas locales
            tablaValores.put(nombreVar, valor);
            tablaTipos  .put(nombreVar, tipoDecl);
            tablaLineas .put(nombreVar, lineaDecl);

            // ── Escribir línea de traza ───────────────────────
            String prefijo = String.format("  L%-3d  ", lineaDecl);
            appendTexto(salidaResultado, prefijo, TEXT_MUTED, false);
            appendTexto(salidaResultado, tipoDecl + " ", COLOR_KW, true);
            appendTexto(salidaResultado, nombreVar, COLOR_INFO, true);
            appendTexto(salidaResultado, "  =  ", TEXT_LIGHT, false);
            appendTexto(salidaResultado,
                    formatearValor(valor, tipoDecl) + "\n",
                    COLOR_VAL, false);
        }

        appendTexto(salidaResultado,
                "\n── Fin de traza ─────────────────────────────────────\n",
                TEXT_MUTED, true);

        // ── Poblar tabla de símbolos ──────────────────────────
        modeloSimbolos.setRowCount(0);
        for (Map.Entry<String, Object> entry : tablaValores.entrySet()) {
            String nom  = entry.getKey();
            String tipo = tablaTipos .getOrDefault(nom, "?");
            String val  = formatearValor(entry.getValue(), tipo);
            int    lin  = tablaLineas.getOrDefault(nom, 0);
            modeloSimbolos.addRow(new Object[]{nom, tipo, val, "L" + lin});
        }
    }

    // =========================================================
    //  EVALUADOR DE EXPRESIONES ARITMÉTICAS
    //
    //  Soporta:  LITERAL_ENTERO | LITERAL_FLOTANTE | LITERAL_STRING
    //            | ID (variable ya declarada)
    //            | expr OP expr   (SUMA, RESTA, MULTIPLICACION, DIVISION)
    //
    //  Precedencia: primero se resuelven +/-, luego */÷
    //  (búsqueda de derecha a izquierda para respetar asociatividad izq.)
    // =========================================================
    private Object evaluarExpresion(List<Symbol> toks, Map<String, Object> tabla) {
        if (toks.isEmpty()) return null;

        // Nivel 1: + y -
        int idx = buscarOperador(toks, sym.SUMA, sym.RESTA);
        if (idx > 0) {
            Object izq = evaluarExpresion(toks.subList(0, idx),          tabla);
            Object der = evaluarExpresion(toks.subList(idx + 1, toks.size()), tabla);
            return aplicarOp(toks.get(idx).sym, izq, der);
        }

        // Nivel 2: * y /
        idx = buscarOperador(toks, sym.MULTIPLICACION, sym.DIVISION);
        if (idx > 0) {
            Object izq = evaluarExpresion(toks.subList(0, idx),          tabla);
            Object der = evaluarExpresion(toks.subList(idx + 1, toks.size()), tabla);
            return aplicarOp(toks.get(idx).sym, izq, der);
        }

        // Expresión atómica (token único)
        if (toks.size() == 1) {
            Symbol s = toks.get(0);
            if (s.sym == sym.LITERAL_ENTERO)  return s.value;           // ya es Integer
            if (s.sym == sym.LITERAL_FLOTANTE) return s.value;          // ya es Double
            if (s.sym == sym.LITERAL_STRING) {
                String raw = tokenValor(s);
                // Quitar comillas si el valor las incluye
                if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2)
                    return raw.substring(1, raw.length() - 1);
                return raw;
            }
            if (s.sym == sym.TRUE)  return Boolean.TRUE;
            if (s.sym == sym.FALSE) return Boolean.FALSE;
            if (s.sym == sym.ID)    return tabla.getOrDefault(tokenValor(s), "?");
        }

        return null;
    }

    /** Busca el operador más a la derecha con sym == op1 ó sym == op2. */
    private int buscarOperador(List<Symbol> toks, int op1, int op2) {
        for (int i = toks.size() - 1; i >= 0; i--) {
            int s = toks.get(i).sym;
            if (s == op1 || s == op2) return i;
        }
        return -1;
    }

    private Object aplicarOp(int op, Object a, Object b) {
        if (a == null || b == null) return "Parámetro";
        if ("Parámetro".equals(a) || "Parámetro".equals(b)) {
            return "null [no asignado]";
        }
        try {
            if (a instanceof Boolean || b instanceof Boolean) return a; // no operable
            if (a instanceof String  || b instanceof String)
                return String.valueOf(a) + String.valueOf(b);

            double da = toDouble(a);
            double db = toDouble(b);
            double res;
            switch (op) {
                case sym.SUMA:           res = da + db; break;
                case sym.RESTA:          res = da - db; break;
                case sym.MULTIPLICACION: res = da * db; break;
                case sym.DIVISION:       res = db != 0 ? da / db : Double.NaN; break;
                default: return null;
            }
            if (a instanceof Integer && b instanceof Integer
                    && op != sym.DIVISION && res == (int) res) {
                return (int) res;
            }
            return res;
        } catch (Exception e) {
            return null;
        }
    }

    private double toDouble(Object o) {
        if (o instanceof Integer) return ((Integer) o).doubleValue();
        if (o instanceof Double)  return (Double) o;
        return 0;
    }

    // =========================================================
    //  TABLA DE TOKENS
    // =========================================================
    private void mostrarTokensEnTabla(List<Symbol> tokens) {
        modeloTokens.setRowCount(0);
        for (Symbol s : tokens) {
            String lexema = s.value != null ? s.value.toString() : nombreTokenSym(s.sym);
            String patron = determinarPatron(s.sym, lexema);
            String categ  = categoriaToken(s.sym);
            String linea  = "L" + s.left;
            modeloTokens.addRow(new Object[]{lexema, patron, categ, linea});
        }
    }

    // =========================================================
    //  MAPEOS / UTILIDADES DE TOKENS
    // =========================================================
    private String nombreTokenSym(int id) {
        if (id >= 0 && id < sym.terminalNames.length) return sym.terminalNames[id];
        return "DESCONOCIDO";
    }

    private String tokenValor(Symbol s) {
        return s.value != null ? s.value.toString() : nombreTokenSym(s.sym);
    }

    private String nombreTipo(int symId) {
        switch (symId) {
            case sym.INT:     return "int";
            case sym.BOOLEAN: return "boolean";
            case sym.VOID:    return "void";
            default:          return "desconocido";
        }
    }

    private Object valorDefault(String tipo) {
        switch (tipo) {
            case "int":     return 0;
            case "boolean": return Boolean.FALSE;
            default:        return null;
        }
    }

    private String formatearValor(Object val, String tipo) {
        if (val == null) return "null";
        if (val instanceof Double) {
            double d = (Double) val;
            if (Double.isNaN(d)) return "NaN (división por 0)";
            if (d == (long) d) return (long) d + ".0";
        }
        return String.valueOf(val);
    }

    private String categoriaToken(int id) {
        switch (id) {
            case sym.PUBLIC:
            case sym.PRIVATE:
            case sym.CLASS:
            case sym.IF:
            case sym.ELSE:
            case sym.WHILE:
            case sym.INT:
            case sym.BOOLEAN:
            case sym.VOID:
            case sym.TRUE:
            case sym.FALSE:
                return "RESERVADA";
            case sym.SUMA:
            case sym.RESTA:
            case sym.MULTIPLICACION:
            case sym.DIVISION:
            case sym.ASIGNACION:
            case sym.IGUAL_QUE:
            case sym.MENOR_QUE:
            case sym.MAYOR_QUE:
                return "OPERADOR";
            case sym.LITERAL_ENTERO:
            case sym.LITERAL_FLOTANTE:
            case sym.LITERAL_STRING:
                return "LITERAL";
            case sym.PUNTO_COMA:
            case sym.LLAVE_A:
            case sym.LLAVE_C:
            case sym.PARENTESIS_A:
            case sym.PARENTESIS_C:
                return "SEPARADOR";
            case sym.ID:
                return "IDENTIFICADOR";
            default:
                return "DESCONOCIDO";
        }
    }

    private String determinarPatron(int id, String lexema) {
        switch (id) {
            case sym.ID:               return "[a-zA-Z_][a-zA-Z0-9_]*";
            case sym.LITERAL_ENTERO:   return "[0-9]+";
            case sym.LITERAL_FLOTANTE: return "[0-9]+\\.[0-9]+";
            case sym.LITERAL_STRING:   return "\".*\"";
            case sym.SUMA:             return "+";
            case sym.RESTA:            return "-";
            case sym.MULTIPLICACION:   return "*";
            case sym.DIVISION:         return "/";
            case sym.ASIGNACION:       return "=";
            case sym.IGUAL_QUE:        return "==";
            case sym.MENOR_QUE:        return "<";
            case sym.MAYOR_QUE:        return ">";
            case sym.PUNTO_COMA:       return ";";
            case sym.LLAVE_A:          return "{";
            case sym.LLAVE_C:          return "}";
            case sym.PARENTESIS_A:     return "(";
            case sym.PARENTESIS_C:     return ")";
            default:                   return lexema;
        }
    }

    // =========================================================
    //  ÁREA DE TEXTO ESTILIZADA
    // =========================================================
    private JTextPane crearAreaTexto() {
        JTextPane area = new JTextPane();
        area.setFont(FUENTE_MONO);
        area.setEditable(false);
        area.setBackground(BG_MAIN);
        area.setForeground(TEXT_LIGHT);
        area.setBorder(new EmptyBorder(15, 15, 15, 15));
        return area;
    }

    private void appendTexto(JTextPane area, String texto, Color color, boolean bold) {
        StyledDocument doc = area.getStyledDocument();
        Style style = new javax.swing.text.StyleContext().addStyle(null, null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setBold(style, bold);
        StyleConstants.setFontFamily(style, "Consolas");
        StyleConstants.setFontSize(style, 13);
        try { doc.insertString(doc.getLength(), texto, style); }
        catch (BadLocationException ignored) {}
    }

    // =========================================================
    //  LIMPIEZA
    // =========================================================
    private void limpiarResultados() {
        modeloTokens.setRowCount(0);
        salidaResultado.setText("");
        modeloSimbolos.setRowCount(0);
        salidaExcepciones.setText("");
    }

    private void limpiarTodo() {
        editorCodigo.setText("");
        limpiarResultados();
    }

    // =========================================================
    //  PANEL DE REFERENCIA DE TOKENS (reemplaza al diccionario)
    // =========================================================
    private JPanel crearPanelReferencia() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);

        JLabel lblHeader = new JLabel("  REFERENCIA");
        lblHeader.setFont(FUENTE_SMALL);
        lblHeader.setForeground(COLOR_ACCENT);
        lblHeader.setPreferredSize(new Dimension(100, 35));
        lblHeader.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_PANEL);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(crearSeccion("TIPOS"));
        content.add(crearFila("int",     "entero"));
        content.add(crearFila("boolean", "lógico"));
        content.add(crearFila("void",    "sin retorno"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        content.add(crearSeccion("MODIFICADORES"));
        content.add(crearFila("public",  "acceso público"));
        content.add(crearFila("private", "acceso privado"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        content.add(crearSeccion("OPERADORES"));
        content.add(crearFila("+",  "SUMA"));
        content.add(crearFila("-",  "RESTA"));
        content.add(crearFila("*",  "MULTIPLICACION"));
        content.add(crearFila("/",  "DIVISION"));
        content.add(crearFila("=",  "ASIGNACION"));
        content.add(crearFila("==", "IGUAL_QUE"));
        content.add(crearFila("<",  "MENOR_QUE"));
        content.add(crearFila(">",  "MAYOR_QUE"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        content.add(crearSeccion("CONTROL"));
        content.add(crearFila("if",    "condicional"));
        content.add(crearFila("else",  "alternativa"));
        content.add(crearFila("while", "bucle"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));

        content.add(crearSeccion("LITERALES"));
        content.add(crearFila("true / false", "BOOLEAN"));
        content.add(crearFila("123",          "ENTERO"));
        content.add(crearFila("3.14",         "FLOTANTE"));
        content.add(crearFila("\"hola\"",     "STRING"));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(lblHeader, BorderLayout.NORTH);
        panel.add(scroll,    BorderLayout.CENTER);
        return panel;
    }

    private JLabel crearSeccion(String titulo) {
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(FUENTE_SMALL);
        lbl.setForeground(TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(10, 0, 5, 0));
        return lbl;
    }

    private JPanel crearFila(String token, String desc) {
        JPanel fila = new JPanel(new BorderLayout());
        fila.setBackground(BG_PANEL);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        fila.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));

        JLabel lblToken = new JLabel(token);
        lblToken.setFont(FUENTE_UI_B);
        lblToken.setForeground(TEXT_LIGHT);

        JPanel pillCont = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        pillCont.setOpaque(false);
        JLabel pill = new JLabel(" " + desc + " ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PILL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setFont(FUENTE_SMALL);
        pill.setForeground(PILL_TEXT);
        pillCont.add(pill);

        fila.add(lblToken, BorderLayout.WEST);
        fila.add(pillCont, BorderLayout.EAST);
        return fila;
    }

    // =========================================================
    //  CÓDIGO DE EJEMPLO
    //  (válido para la gramática del Parser CUP)
    // =========================================================
    private void agregarCodigoEjemplo() {
        editorCodigo.setText(
            "public class Calculadora {\n" +
            "\n" +
            "    public int suma(int a, int b) {\n" +
            "        int resultado = a + b;\n" +
            "        return resultado;\n" +
            "    }\n" +
            "\n" +
            "    public void demo() {\n" +
            "        int x = 10;\n" +
            "        int y = 20;\n" +
            "        int total = x + y;\n" +
            "\n" +
            "        if (total > 25) {\n" +
            "            int bonus = total * 2;\n" +
            "        }\n" +
            "\n" +
            "        int contador = 0;\n" +
            "        while (contador < 5) {\n" +
            "            contador = contador + 1;\n" +
            "        }\n" +
            "\n" +
            "        boolean activo = true;\n" +
            "    }\n" +
            "}\n"
        );
    }

    // =========================================================
    //  MAIN
    // =========================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new CompiladorJava().setVisible(true);
        });
    }
}