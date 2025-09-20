package view;

import controller.Game;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import model.board.Position;
import model.pieces.Pawn;
import model.pieces.Piece;

public class ChessGUI extends JFrame {
    private static final long serialVersionUID = 1L;

    // cores
    private Color lightSquareColor = new Color(240, 217, 181);
    private Color darkSquareColor = new Color(181, 136, 99);
    private static final Color HILITE_SELECTED = new Color(50, 120, 220);
    private static final Color HILITE_LEGAL = new Color(20, 140, 60);
    private static final Color HILITE_LASTMOVE = new Color(220, 170, 30);

    private static final Border BORDER_SELECTED = new MatteBorder(3, 3, 3, 3, HILITE_SELECTED);
    private static final Border BORDER_LEGAL = new MatteBorder(3, 3, 3, 3, HILITE_LEGAL);
    private static final Border BORDER_LASTMOVE = new MatteBorder(3, 3, 3, 3, HILITE_LASTMOVE);

    private final Game game;
    private final JPanel boardPanel;
    private final JButton[][] squares = new JButton[8][8];

    private final JLabel status;
    private final JTextArea history;
    private final JScrollPane historyScroll;

    private JCheckBoxMenuItem pcAsBlack;
    private JSpinner depthSpinner;
    private JMenuItem newGameItem, quitItem;

    private Position selected = null;
    private List<Position> legalForSelected = new ArrayList<>();
    private Position lastFrom = null, lastTo = null;

    private boolean aiThinking = false;
    private final Random rnd = new Random();

    // relógio
    private JSpinner timeMinutesSpinner;
    private JSpinner incrementSecondsSpinner;
    private JLabel whiteClockLabel;
    private JLabel blackClockLabel;
    private JButton startPauseClockBtn;
    private JButton resetClockBtn;
    private JCheckBox enableClockCheck;
    private long whiteMillis = 0L;
    private long blackMillis = 0L;
    private boolean clockRunning = false;
    private boolean whiteClockActive = true;
    private javax.swing.Timer clockTimer;
    private long lastTimerTick = 0L;

    // --------------------- Construtor ---------------------
    public ChessGUI() {
        super("Chess Game");

        this.game = new Game();

        // monta tabuleiro
        boardPanel = new JPanel(new GridLayout(8, 8, 0, 0)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                int side = Math.min(size.width, size.height);
                return new Dimension(side, side);
            }
        };

        // criar botões do tabuleiro
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton btn = new JButton();
                final int row = r, col = c;
                btn.addActionListener(e -> handleClick(new Position(row, col)));
                squares[r][c] = btn;
                boardPanel.add(btn);
            }
        }

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        setJMenuBar(buildMenuBar());
        add(buildSideControls(), BorderLayout.SOUTH);

        status = new JLabel("Pronto");
        add(status, BorderLayout.NORTH);

        history = new JTextArea(10, 20);
        history.setEditable(false);
        historyScroll = new JScrollPane(history);
        add(historyScroll, BorderLayout.EAST);

      //  setupAccelerators();
        initClockTimer();
        doNewGame();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // --------------------- Menus ---------------------
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu gameMenu = new JMenu("Jogo");

        newGameItem = new JMenuItem("Novo Jogo");
        newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newGameItem.addActionListener(e -> doNewGame());

        pcAsBlack = new JCheckBoxMenuItem("PC joga com as Pretas");
        pcAsBlack.setSelected(false);

        JMenu depthMenu = new JMenu("Profundidade IA");
        depthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1));
        depthSpinner.setToolTipText("Profundidade efetiva da IA (heurística não-minimax)");
        depthMenu.add(depthSpinner);

        quitItem = new JMenuItem("Sair");
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        quitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));

        JMenuItem colorItem = new JMenuItem("Mudar cores do tabuleiro");
        colorItem.addActionListener(e -> escolherCoresTabuleiro());

        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(pcAsBlack);
        gameMenu.add(depthMenu);
        gameMenu.addSeparator();
        gameMenu.add(colorItem);
        gameMenu.addSeparator();
        gameMenu.add(quitItem);

        mb.add(gameMenu);
        return mb;
    }

    // --------------------- Controles laterais ---------------------
    private JPanel buildSideControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        JButton btnNew = new JButton("Novo Jogo");
        btnNew.addActionListener(e -> doNewGame());
        panel.add(btnNew);

        JCheckBox cb = new JCheckBox("PC (Pretas)");
        cb.setSelected(pcAsBlack.isSelected());
        cb.addActionListener(e -> pcAsBlack.setSelected(cb.isSelected()));
        panel.add(cb);

        panel.add(new JLabel("Prof. IA:"));
        int curDepth = (Integer) depthSpinner.getValue();
        JSpinner sp = new JSpinner(new SpinnerNumberModel(curDepth, 1, 4, 1));
        sp.addChangeListener(e -> depthSpinner.setValue(sp.getValue()));
        panel.add(sp);

        // relógio
        panel.add(new JLabel("Tempo (min):"));
        timeMinutesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 180, 1));
        panel.add(timeMinutesSpinner);

        panel.add(new JLabel("Inc (s):"));
        incrementSecondsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
        panel.add(incrementSecondsSpinner);

        startPauseClockBtn = new JButton("Pausar");
        startPauseClockBtn.addActionListener(e -> toggleClockRunning());
        panel.add(startPauseClockBtn);

        resetClockBtn = new JButton("Reset relógio");
        resetClockBtn.addActionListener(e -> initClocksForNewGame());
        panel.add(resetClockBtn);

        whiteClockLabel = new JLabel("Brancas: 00:00");
        whiteClockLabel.setFont(whiteClockLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(whiteClockLabel);

        blackClockLabel = new JLabel("Pretas: 00:00");
        blackClockLabel.setFont(blackClockLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(blackClockLabel);

        enableClockCheck = new JCheckBox("Relógio ativo");
        enableClockCheck.setSelected(true);
        panel.add(enableClockCheck);

        return panel;
    }

    // --------------------- Novo Jogo ---------------------
    private void doNewGame() {
        selected = null;
        legalForSelected.clear();
        lastFrom = lastTo = null;
        aiThinking = false;
        game.newGame();
        initClocksForNewGame();
        setMinimumSize(new Dimension(1100, 750));
        refresh();
        maybeTriggerAI();
    }

    // --------------------- Interação com o tabuleiro ---------------------
    private void handleClick(Position clicked) {
        if (game.isGameOver() || aiThinking) return;
        if (pcAsBlack.isSelected() && !game.whiteToMove()) return;

        Piece p = game.board().get(clicked);

        if (selected == null) {
            if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            }
        } else {
            List<Position> legals = game.legalMovesFrom(selected);
            if (legals.contains(clicked)) {
                Character promo = null;
                Piece moving = game.board().get(selected);
                if (moving instanceof Pawn && game.isPromotion(selected, clicked)) {
                    promo = askPromotion();
                }
                lastFrom = selected;
                lastTo = clicked;
                game.move(selected, clicked, promo);


                onMoveProcessed();
                selected = null;
                legalForSelected.clear();
                refresh();
                maybeAnnounceEnd();
                maybeTriggerAI();
                return;
            } else if (p != null && p.isWhite() == game.whiteToMove()) {
                selected = clicked;
                legalForSelected = game.legalMovesFrom(selected);
            } else {
                selected = null;
                legalForSelected.clear();
            }
        }
        refresh();
    }

    private Character askPromotion() {
        String[] opts = {"Rainha", "Torre", "Bispo", "Cavalo"};
        int ch = JOptionPane.showOptionDialog(this, "Escolha a peça para promoção:",
                "Promoção", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opts, opts[0]);
        return switch (ch) {
            case 1 -> 'R';
            case 2 -> 'B';
            case 3 -> 'N';
            default -> 'Q';
        };
    }

    // ----------------- IA (não bloqueante) -----------------

    private void maybeTriggerAI() {
        if (game.isGameOver()) return;
        if (!pcAsBlack.isSelected()) return;
        if (game.whiteToMove()) return; // PC joga de pretas

        aiThinking = true;
        status.setText("Vez: Pretas — PC pensando...");
        final int depth = (Integer) depthSpinner.getValue();

        new SwingWorker<Void, Void>() {
            Position aiFrom, aiTo;
            @Override
            protected Void doInBackground() {
                // Heurística simples: escolher melhor captura disponível; senão, um lance aleatório "ok".
                var allMoves = collectAllLegalMovesForSide(false); // pretas
                if (allMoves.isEmpty()) return null;

                int bestScore = Integer.MIN_VALUE;
                List<Move> bestList = new ArrayList<>();

                for (Move mv : allMoves) {
                    int score = 0;

                    Piece target = game.board().get(mv.to);
                    if (target != null) {
                        score += pieceValue(target); // capturas valem
                    }
                    score += centerBonus(mv.to);
                    score += (depth - 1) * 2;

                    if (score > bestScore) {
                        bestScore = score;
                        bestList.clear();
                        bestList.add(mv);
                    } else if (score == bestScore) {
                        bestList.add(mv);
                    }
                }
                Move chosen = bestList.get(rnd.nextInt(bestList.size()));
                aiFrom = chosen.from;
                aiTo   = chosen.to;
                return null;
            }

            @Override
            protected void done() {
                try { get(); } catch (Exception ignored) {}

                if (aiFrom != null && aiTo != null && !game.isGameOver() && !game.whiteToMove()) {
                    lastFrom = aiFrom;
                    lastTo   = aiTo;
                    Character promo = null;
                    Piece moving = game.board().get(aiFrom);
                    if (moving instanceof Pawn && game.isPromotion(aiFrom, aiTo)) {
                        promo = 'Q';
                    }
                    game.move(aiFrom, aiTo, promo);

                    // --- relógio: processa troca de turno e incrementos (IA acabou de mover) ---
                    onMoveProcessed();
                }
                aiThinking = false;
                refresh();
                maybeAnnounceEnd();
            }
        }.execute();
    }

    private static class Move {
        final Position from, to;
        Move(Position f, Position t) { this.from = f; this.to = t; }
    }

    private List<Move> collectAllLegalMovesForSide(boolean whiteSide) {
        List<Move> moves = new ArrayList<>();
        if (whiteSide != game.whiteToMove()) return moves;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position from = new Position(r, c);
                Piece piece = game.board().get(from);
                if (piece != null && piece.isWhite() == whiteSide) {
                    for (Position to : game.legalMovesFrom(from)) {
                        moves.add(new Move(from, to));
                    }
                }
            }
        }
        return moves;
    }

    private int pieceValue(Piece p) {
        if (p == null) return 0;
        switch (p.getSymbol()) {
            case "P": return 100;
            case "N":
            case "B": return 300;
            case "R": return 500;
            case "Q": return 900;
            case "K": return 20000;
        }
        return 0;
    }

    private int centerBonus(Position pos) {
        int r = pos.getRow(), c = pos.getColumn();
        if ((r==3 || r==4) && (c==3 || c==4)) return 10;
        if ((r>=2 && r<=5) && (c>=2 && c<=5)) return 4;
        return 0;
    }

    // ----------------- Atualização de UI -----------------

    private void refresh() {
        // 1) Cores base e limpa bordas
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                boolean light = (r + c) % 2 == 0;
                Color base = light ? lightSquareColor : darkSquareColor;
                JButton b = squares[r][c];
                b.setBackground(base);
                b.setBorder(null);
                b.setToolTipText(null);
            }
        }

        // 2) Realce último lance
        if (lastFrom != null) squares[lastFrom.getRow()][lastFrom.getColumn()].setBorder(BORDER_LASTMOVE);
        if (lastTo   != null) squares[lastTo.getRow()][lastTo.getColumn()].setBorder(BORDER_LASTMOVE);

        // 3) Realce seleção e movimentos legais
        if (selected != null) {
            squares[selected.getRow()][selected.getColumn()].setBorder(BORDER_SELECTED);
            for (Position d : legalForSelected) {
                squares[d.getRow()][d.getColumn()].setBorder(BORDER_LEGAL);
            }
        }

        // 4) Ícones das peças (ou Unicode como fallback)
        int iconSize = computeSquareIconSize();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = game.board().get(new Position(r, c));
                JButton b = squares[r][c];

                if (p == null) {
                    b.setIcon(null);
                    b.setText("");
                    continue;
                }

                char sym = p.getSymbol().charAt(0);
                ImageIcon icon = ImageUtil.getPieceIcon(p.isWhite(), sym, iconSize);
                if (icon != null) {
                    b.setIcon(icon);
                    b.setText("");
                } else {
                    b.setIcon(null);
                    b.setText(toUnicode(p.getSymbol(), p.isWhite()));
                }
            }
        }

        // 5) Status e histórico
        String side = game.whiteToMove() ? "Brancas" : "Pretas";
        String chk = game.inCheck(game.whiteToMove()) ? " — Xeque!" : "";
        if (aiThinking) chk = " — PC pensando...";
        status.setText("Vez: " + side + chk);

        StringBuilder sb = new StringBuilder();
        var hist = game.history();
        for (int i = 0; i < hist.size(); i++) {
            if (i % 2 == 0) sb.append((i / 2) + 1).append('.').append(' ');
            sb.append(hist.get(i)).append(' ');
            if (i % 2 == 1) sb.append('\n');
        }
        history.setText(sb.toString());
        history.setCaretPosition(history.getDocument().getLength());

        // atualiza labels do relógio
        updateClockLabels();
    }

    private void maybeAnnounceEnd() {
        if (!game.isGameOver()) return;
        String msg;
        // se a última linha do histórico indicar timeout, mostra mensagem apropriada
        List<String> h = game.history();
        if (!h.isEmpty()) {
            String last = h.get(h.size()-1);
            if (last.startsWith("Timeout")) {
                msg = "Tempo esgotado — " + (last.contains("White") ? "Brancas perdem por tempo." : "Pretas perdem por tempo.");
                JOptionPane.showMessageDialog(this, msg, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        if (game.inCheck(game.whiteToMove())) {
            msg = "Xeque-mate! " + (game.whiteToMove() ? "Brancas" : "Pretas") + " estão em mate.";
        } else {
            msg = "Empate por afogamento (stalemate).";
        }
        JOptionPane.showMessageDialog(this, msg, "Fim de Jogo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String toUnicode(String sym, boolean white) {
        return switch (sym) {
            case "K" -> white ? "\u2654" : "\u265A";
            case "Q" -> white ? "\u2655" : "\u265B";
            case "R" -> white ? "\u2656" : "\u265C";
            case "B" -> white ? "\u2657" : "\u265D";
            case "N" -> white ? "\u2658" : "\u265E";
            case "P" -> white ? "\u2659" : "\u265F";
            default -> "";
        };
    }

    private int computeSquareIconSize() {
        JButton b = squares[0][0];
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        int side = Math.min(w, h);
        if (side <= 1) return 64;
        return Math.max(24, side - 8);
    }

    private void escolherCoresTabuleiro() {
        Color novaClara = JColorChooser.showDialog(this, "Escolha a cor das casas claras", lightSquareColor);
        if (novaClara != null) {
            lightSquareColor = novaClara;
        }

        Color novaEscura = JColorChooser.showDialog(this, "Escolha a cor das casas escuras", darkSquareColor);
        if (novaEscura != null) {
            darkSquareColor = novaEscura;
        }

        refresh(); // redesenha o tabuleiro com as novas cores
    }

    // ----------------------------- Relógio: implementação -----------------------------

    private void initClockTimer() {
        // timer de UI que "marca" com base em System.currentTimeMillis() (preciso)
        clockTimer = new javax.swing.Timer(200, e -> {
            if (!clockRunning || game.isGameOver() || !enableClockCheck.isSelected()) return;

            long now = System.currentTimeMillis();
            long delta = (lastTimerTick == 0L) ? 0L : (now - lastTimerTick);
            lastTimerTick = now;

            if (delta <= 0) return;

            if (whiteClockActive) {
                whiteMillis -= delta;
                if (whiteMillis <= 0) {
                    whiteMillis = 0;
                    clockRunning = false;
                    // Brancas perderam por tempo
                    game.flagTimeout(true);
                    JOptionPane.showMessageDialog(this, "Tempo esgotado: Brancas perdem por tempo.", "Tempo", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    return;
                }
            } else {
                blackMillis -= delta;
                if (blackMillis <= 0) {
                    blackMillis = 0;
                    clockRunning = false;
                    // Pretas perderam por tempo
                    game.flagTimeout(false);
                    JOptionPane.showMessageDialog(this, "Tempo esgotado: Pretas perdem por tempo.", "Tempo", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    return;
                }
            }
            updateClockLabels();
        });
        clockTimer.setRepeats(true);
        clockTimer.start();
    }

    private void initClocksForNewGame() {
        int minutes = ((Integer) timeMinutesSpinner.getValue()).intValue();
        int inc = ((Integer) incrementSecondsSpinner.getValue()).intValue();
        whiteMillis = minutes * 60_000L;
        blackMillis = minutes * 60_000L;
        whiteClockActive = game.whiteToMove();
        // só inicia o relógio se checkbox estiver ativo
        clockRunning = enableClockCheck != null && enableClockCheck.isSelected() && !game.isGameOver();
        lastTimerTick = clockRunning ? System.currentTimeMillis() : 0L;
        startPauseClockBtn.setText(clockRunning ? "Pausar" : "Start");
        updateClockLabels();
    }

    private void toggleClockRunning() {
        clockRunning = !clockRunning;
        if (clockRunning) {
            lastTimerTick = System.currentTimeMillis();
            startPauseClockBtn.setText("Pausar");
        } else {
            startPauseClockBtn.setText("Start");
        }
    }

    private void updateClockLabels() {
        // mostra mm:ss
        whiteClockLabel.setText("Brancas: " + formatTime(whiteMillis) + (whiteClockActive ? " ◀" : ""));
        blackClockLabel.setText("Pretas: " + formatTime(blackMillis) + (!whiteClockActive ? " ◀" : ""));

        // muda cor quando abaixo de 10s
        if (whiteMillis <= 10_000) whiteClockLabel.setForeground(Color.RED); else whiteClockLabel.setForeground(Color.BLACK);
        if (blackMillis <= 10_000) blackClockLabel.setForeground(Color.RED); else blackClockLabel.setForeground(Color.BLACK);
    }

    private String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    /**
     * Deve ser chamado sempre depois de um game.move(...) (humano ou IA).
     * Cuida de subtrair o delta até o momento do movimento, aplicar incremento ao que acabou de mover e
     * alternar o relógio para o próximo jogador.
     */
    private void onMoveProcessed() {
        if (enableClockCheck == null || !enableClockCheck.isSelected()) return;

        long now = System.currentTimeMillis();
        if (lastTimerTick == 0L) lastTimerTick = now;
        long delta = now - lastTimerTick;
        lastTimerTick = now;

        // subtrai o tempo que passou para o relógio ativo até o momento do movimento
        if (clockRunning) {
            if (whiteClockActive) {
                whiteMillis -= delta;
                if (whiteMillis <= 0) {
                    whiteMillis = 0;
                    clockRunning = false;
                    game.flagTimeout(true);
                    JOptionPane.showMessageDialog(this, "Tempo esgotado: Brancas perdem por tempo.", "Tempo", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    return;
                }
            } else {
                blackMillis -= delta;
                if (blackMillis <= 0) {
                    blackMillis = 0;
                    clockRunning = false;
                    game.flagTimeout(false);
                    JOptionPane.showMessageDialog(this, "Tempo esgotado: Pretas perdem por tempo.", "Tempo", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                    return;
                }
            }
        }

        // Aplica incremento ao jogador que acabou de mover:
        boolean movedByWhite = !game.whiteToMove(); // after move, whiteToMove changed -> the mover is the opposite
        long incMs = ((Integer) incrementSecondsSpinner.getValue()).intValue() * 1000L;
        if (movedByWhite) whiteMillis += incMs; else blackMillis += incMs;

        // alterna relógio para o lado que tem vez agora
        whiteClockActive = game.whiteToMove();

        // reinicia contagem (se permitido)
        if (!game.isGameOver() && enableClockCheck.isSelected()) {
            clockRunning = true;
            lastTimerTick = System.currentTimeMillis();
        } else {
            clockRunning = false;
        }

        updateClockLabels();
    }

    // ----------------------------- fim relógio -----------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessGUI::new);
    }
}

