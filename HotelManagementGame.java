import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Random;


public class HotelManagementGame extends JFrame {

   
    static final Color BG1 = new Color(11, 14, 28), BG2 = new Color(24, 29, 56);
    static final Color SURF = new Color(23, 28, 50), SURF2 = new Color(33, 40, 70);
    static final Color LINE = new Color(58, 68, 108);
    static final Color TEXT = new Color(235, 239, 250), MUTED = new Color(138, 147, 178);
    static final Color ACCENT = new Color(108, 123, 255), TEAL = new Color(45, 212, 167);
    static final Color AMBER = new Color(255, 181, 71), ROSE = new Color(255, 92, 122);

    static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

   
    static void smooth(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static String php(int n) {
        return "PHP " + String.format("%,d", n);
    }

    static void ctext(Graphics2D g2, String s, int cx, int y) {
        g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
    }

    static int pill(Graphics2D g2, String s, int x, int y, Color c, Font f) {
        g2.setFont(f);
        int w = g2.getFontMetrics().stringWidth(s) + 14;
        g2.setColor(alpha(c, 50));
        g2.fillRoundRect(x, y - 13, w, 18, 18, 18);
        g2.setColor(c);
        g2.drawString(s, x + 7, y);
        return w;
    }

    
    enum RoomType {
        SINGLE("Single", 1000, TEAL), DOUBLE("Double", 1500, ACCENT), DELUXE("Deluxe", 2500, AMBER);
        final String label;
        final int price;
        final Color color;

        RoomType(String label, int price, Color color) {
            this.label = label;
            this.price = price;
            this.color = color;
        }
    }

    enum State { AVAILABLE, OCCUPIED, DIRTY, LOCKED }

    static class Guest {
        String name, face;
        RoomType wanted;
        int nights, patience, maxPatience;
        boolean vip, served, pitched;
    }

    static class Room {
        int number, daysLeft, unlockCost;
        RoomType type;
        State state;
        Guest guest;
    }

    static final String[] NAMES = {
        "Maria Santos", "Juan Dela Cruz", "Ana Reyes", "Miguel Garcia", "Sofia Lim", "Carlos Tan",
        "Bea Villanueva", "Rico Mendoza", "Lea Navarro", "Paolo Cruz", "Tina Ramos", "Jose Bautista",
        "Grace Aquino", "Dante Flores", "Mika Torres", "Noel Castillo"
    };
    static final String[] FACES = {
        "\uD83E\uDDD1", "\uD83D\uDC69", "\uD83D\uDC68", "\uD83E\uDDD3",
        "\uD83D\uDC75", "\uD83D\uDC74", "\uD83E\uDDD4", "\uD83D\uDC71"
    };
    static final String[] LEVEL_NAMES = {"Tiny Inn", "Cozy Inn", "Nice Hotel", "Luxury Resort", "Grand Palace"};
    static final int[] LEVEL_REQ = {0, 6000, 16000, 32000, 60000};

    static final String[] UPG_NAMES = {"Hire Cleaner", "Marketing", "Luxury Decor", "Concierge Desk"};
    static final String[] UPG_DESC = {
        "Cleans 2 dirty rooms per day (PHP 150/day)", "More guests arrive every day",
        "+10% room prices and +5 reputation", "Guests wait longer in the queue"
    };
    static final int[] UPG_BASE = {1500, 2500, 3000, 2000};
    static final int[] UPG_MAX = {3, 3, 3, 2};
    static final int MAX_QUEUE = 6;

    
    int money = 10000, reputation = 50, level = 1, day = 1, totalGuests = 0, totalEarned = 0;
    int[] upg = new int[4];
    final Random rnd = new Random();
    final ArrayList<Guest> queue = new ArrayList<>();
    final ArrayList<Room> rooms = new ArrayList<>();
    final ArrayList<RoomTile> tiles = new ArrayList<>();
    final ArrayList<PillBar> bars = new ArrayList<>();
    Guest selected = null;
    RoomTile dropTarget = null;
    boolean busy = false;

   
    int goalType, goalTarget, goalProgress, goalMoney, goalRep;
    boolean goalDone;

    
    String summaryText = "";
    int pendingEvent = 0;

   
    double pulse = 0, skyT = 0.05, dispMoney = 10000;
    Timer animTimer;
    final Overlay overlay = new Overlay();

    
    JLabel moneyLbl, dayLbl, levelLbl, guestsLbl, statusLbl, queueTitle, goalLbl, goalReward;
    PillBar repBar, levelBar, goalBar;
    JPanel queuePanel;
    JTextArea logArea;
    SkyPanel sky;
    UpgBtn[] shopBtns = new UpgBtn[4];

    
    
    

    static class GradientPanel extends JPanel {
        GradientPanel(LayoutManager lm) {
            super(lm);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, BG1, getWidth(), getHeight(), BG2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    static class Card extends JPanel {
        final Color fill;

        Card(Color fill) {
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.setColor(alpha(LINE, 150));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class FancyButton extends JButton {
        final Color base;

        FancyButton(String text, Color base) {
            super(text);
            this.base = base;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            Color c = base;
            if (getModel().isPressed()) {
                c = base.darker();
            } else if (getModel().isRollover()) {
                c = mix(base, Color.WHITE, 0.18f);
            }
            g2.setPaint(new GradientPaint(0, 0, c, getWidth(), getHeight(), mix(c, ACCENT, 0.25f)));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    
    static class PillBar extends JComponent {
        final Color c;
        double target, shown;
        String text = "";

        PillBar(Color c, int h) {
            this.c = c;
            setPreferredSize(new Dimension(120, h));
        }

        void set(double f, String t) {
            target = Math.max(0, Math.min(1, f));
            text = t;
            repaint();
        }

        boolean step() {
            double d = target - shown;
            if (Math.abs(d) < 0.002) {
                boolean changed = shown != target;
                shown = target;
                return changed;
            }
            shown += d * 0.18;
            return true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(255, 255, 255, 25));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            int fw = (int) ((w - 1) * shown);
            if (fw > 0) {
                g2.setPaint(new GradientPaint(0, 0, c, w, 0, mix(c, Color.WHITE, 0.35f)));
                g2.fillRoundRect(0, 0, Math.max(fw, h), h - 1, h, h);
            }
            if (!text.isEmpty()) {
                g2.setFont(font(Font.BOLD, Math.min(12, h - 6)));
                g2.setColor(shown > 0.5 ? new Color(20, 22, 40) : TEXT);
                ctext(g2, text, w / 2, h / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
            }
            g2.dispose();
        }
    }

    
    class UpgBtn extends JButton {
        final int idx;

        UpgBtn(int idx) {
            this.idx = idx;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(UPG_DESC[idx]);
            setPreferredSize(new Dimension(270, 56));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            addActionListener(e -> buy(idx));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            boolean maxed = upg[idx] >= UPG_MAX[idx];
            boolean afford = money >= upgCost(idx);
            Color bg = !isEnabled() ? new Color(27, 32, 56)
                    : getModel().isRollover() ? mix(SURF2, ACCENT, 0.25f) : SURF2;
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.setColor(alpha(maxed ? TEAL : afford ? ACCENT : LINE, 160));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(isEnabled() || maxed ? TEXT : MUTED);
            g2.drawString("[" + (idx + 1) + "] " + UPG_NAMES[idx], 12, 22);
            for (int i = 0; i < UPG_MAX[idx]; i++) { // level pips
                g2.setColor(i < upg[idx] ? TEAL : new Color(255, 255, 255, 40));
                g2.fillOval(12 + i * 12, 31, 8, 8);
            }
            g2.setFont(font(Font.PLAIN, 11));
            g2.setColor(MUTED);
            g2.drawString(UPG_DESC[idx].length() > 36 ? UPG_DESC[idx].substring(0, 34) + ".." : UPG_DESC[idx],
                    12 + UPG_MAX[idx] * 12 + 4, 39);

            String s = maxed ? "MAX" : php(upgCost(idx));
            Color pc = maxed ? TEAL : afford ? AMBER : ROSE;
            g2.setFont(font(Font.BOLD, 11));
            int pw = g2.getFontMetrics().stringWidth(s) + 14;
            pill(g2, s, w - pw - 8, 22, pc, font(Font.BOLD, 11));
            g2.dispose();
        }
    }

    
    class SkyPanel extends JPanel {
        final double[][] stars = new double[40][3];

        SkyPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(100, 84));
            for (double[] s : stars) {
                s[0] = rnd.nextDouble();
                s[1] = rnd.nextDouble();
                s[2] = rnd.nextDouble() * 6;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
            double dl = Math.sin(skyT * 2 * Math.PI);
            float t = (float) Math.max(0, Math.min(1, 0.5 + dl * 1.3));
            Color top = mix(new Color(10, 14, 38), new Color(64, 132, 235), t);
            Color bot = mix(new Color(40, 34, 84), new Color(160, 210, 250), t);
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bot));
            g2.fillRect(0, 0, w, h);

            if (t < 0.6f) {
                for (double[] s : stars) {
                    int a = (int) (200 * (1 - t / 0.6) * (0.6 + 0.4 * Math.sin(pulse * 0.5 + s[2])));
                    g2.setColor(alpha(Color.WHITE, a));
                    g2.fillOval((int) (s[0] * w), (int) (s[1] * h * 0.8), 2, 2);
                }
            }
            boolean daytime = skyT < 0.5;
            double p = daytime ? skyT * 2 : (skyT - 0.5) * 2;
            int bx = (int) (p * (w + 60)) - 30;
            int by = (int) (h * 0.95 - Math.sin(p * Math.PI) * h * 0.8);
            if (daytime) {
                g2.setColor(alpha(AMBER, 50));
                g2.fillOval(bx - 26, by - 26, 52, 52);
                g2.setColor(AMBER);
                g2.fillOval(bx - 15, by - 15, 30, 30);
            } else {
                g2.setColor(new Color(232, 236, 250));
                g2.fillOval(bx - 13, by - 13, 26, 26);
                g2.setColor(new Color(200, 206, 226));
                g2.fillOval(bx - 5, by - 6, 7, 7);
                g2.fillOval(bx + 2, by + 2, 5, 5);
            }
            g2.setColor(new Color(255, 255, 255, (int) (60 * t + 18)));
            for (int i = 0; i < 3; i++) {
                int cx = (int) (((pulse * 6 * (0.6 + i * 0.3) + i * w / 3.0) % (w + 160)) - 80);
                int cy = 16 + i * 16;
                g2.fillRoundRect(cx, cy, 70, 14, 14, 14);
                g2.fillRoundRect(cx + 14, cy - 8, 40, 14, 14, 14);
            }
            g2.setFont(font(Font.BOLD, 20));
            g2.setColor(new Color(0, 0, 0, 90));
            g2.drawString("Day " + day + "  \u2022  " + LEVEL_NAMES[level - 1], 19, h - 13);
            g2.setColor(Color.WHITE);
            g2.drawString("Day " + day + "  \u2022  " + LEVEL_NAMES[level - 1], 18, h - 14);
            g2.dispose();
        }
    }

   
   
    

    static class Particle {
        double x, y, vx, vy;
        int life, max, size;
        Color c;
    }

    static class Toast {
        String text;
        Color c;
        int age;

        Toast(String t, Color c) {
            text = t;
            this.c = c;
        }
    }

    class Overlay extends JComponent {
        final ArrayList<Particle> parts = new ArrayList<>();
        final ArrayList<Toast> toasts = new ArrayList<>();
        Guest dragGuest;
        Point dragPt = new Point();
        double fade = -1;
        boolean fired;
        Runnable mid, end;

        Overlay() {
            setOpaque(false);
        }

        @Override
        public boolean contains(int x, int y) {
            return false; 
        }

        void toast(String t, Color c) {
            if (toasts.size() >= 3) {
                toasts.remove(0);
            }
            toasts.add(new Toast(t, c));
        }

        void burst(int x, int y, Color c, int n) {
            for (int i = 0; i < n; i++) {
                Particle p = new Particle();
                double a = rnd.nextDouble() * Math.PI * 2, s = 1.5 + rnd.nextDouble() * 4.5;
                p.x = x;
                p.y = y;
                p.vx = Math.cos(a) * s;
                p.vy = Math.sin(a) * s - 2.5;
                p.max = p.life = 35 + rnd.nextInt(25);
                p.size = 4 + rnd.nextInt(5);
                p.c = c;
                parts.add(p);
            }
        }

        void confetti() {
            Color[] cs = {AMBER, TEAL, ACCENT, ROSE};
            for (Color c : cs) {
                burst(getWidth() / 2, getHeight() / 3, c, 30);
            }
        }

        void startDay(Runnable mid, Runnable end) {
            this.mid = mid;
            this.end = end;
            fade = 0;
            fired = false;
            busy = true;
        }

        void tick() {
            boolean active = !parts.isEmpty() || !toasts.isEmpty() || dragGuest != null || fade >= 0;
            for (int i = parts.size() - 1; i >= 0; i--) {
                Particle p = parts.get(i);
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.22;
                if (--p.life <= 0) {
                    parts.remove(i);
                }
            }
            for (int i = toasts.size() - 1; i >= 0; i--) {
                if (++toasts.get(i).age > 140) {
                    toasts.remove(i);
                }
            }
            if (fade >= 0) {
                fade += 0.03;
                if (fade >= 1 && !fired) {
                    fired = true;
                    mid.run();
                }
                if (fade >= 2) {
                    fade = -1;
                    busy = false;
                    end.run();
                }
            }
            if (active) {
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth();

            for (Particle p : parts) {
                g2.setColor(alpha(p.c, 255 * p.life / p.max));
                g2.fillOval((int) p.x, (int) p.y, p.size, p.size);
            }

            int y = 92;
            for (Toast t : toasts) {
                float in = Math.min(1f, t.age / 8f);
                float out = Math.min(1f, (140 - t.age) / 25f);
                float a = in * out;
                g2.setFont(font(Font.BOLD, 14));
                int tw = g2.getFontMetrics().stringWidth(t.text) + 40;
                int x = (w - tw) / 2, yy = y - (int) ((1 - in) * 12);
                g2.setColor(new Color(14, 18, 38, (int) (235 * a)));
                g2.fillRoundRect(x, yy, tw, 36, 36, 36);
                g2.setColor(alpha(t.c, (int) (255 * a)));
                g2.drawRoundRect(x, yy, tw, 36, 36, 36);
                g2.fillOval(x + 12, yy + 14, 8, 8);
                g2.setColor(alpha(TEXT, (int) (255 * a)));
                g2.drawString(t.text, x + 28, yy + 23);
                y += 44;
            }

            if (dragGuest != null) {
                g2.setColor(new Color(20, 26, 50, 235));
                g2.fillRoundRect(dragPt.x - 20, dragPt.y - 22, 210, 44, 22, 22);
                g2.setColor(AMBER);
                g2.drawRoundRect(dragPt.x - 20, dragPt.y - 22, 210, 44, 22, 22);
                g2.setFont(font(Font.PLAIN, 24));
                g2.setColor(TEXT);
                g2.drawString(dragGuest.face, dragPt.x - 10, dragPt.y + 9);
                g2.setFont(font(Font.BOLD, 13));
                g2.drawString(dragGuest.name, dragPt.x + 24, dragPt.y + 5);
            }

            if (fade >= 0) {
                double a = Math.min(1, (fade < 1 ? fade : 2 - fade) * 1.6);
                g2.setColor(new Color(8, 10, 22, (int) (a * 255)));
                g2.fillRect(0, 0, w, getHeight());
                if (a > 0.5) {
                    int ta = (int) (255 * Math.min(1, (a - 0.5) * 2));
                    g2.setFont(font(Font.BOLD, 46));
                    g2.setColor(alpha(TEXT, ta));
                    ctext(g2, fired ? "Day " + day : "Night falls...", w / 2, getHeight() / 2);
                    g2.setFont(font(Font.PLAIN, 16));
                    g2.setColor(alpha(MUTED, ta));
                    ctext(g2, fired ? summaryText : "Guests are settling in", w / 2, getHeight() / 2 + 34);
                }
            }
            g2.dispose();
        }
    }

    
  
    

    public HotelManagementGame() {
        setTitle("Sunrise Grand Hotel");
        setSize(1380, 820);
        setMinimumSize(new Dimension(1280, 760));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitGame();
            }
        });

        buildRooms();
        buildGUI();
        setGlassPane(overlay);
        overlay.setVisible(true);
        setupShortcuts();

        for (int i = 0; i < 3; i++) {
            queue.add(newGuest());
        }
        newGoal();
        log("Welcome to Sunrise Grand Hotel!");
        flash("Drag a guest onto a vacant room - or click a guest, then a room.", AMBER);
        refreshAll();
        startAnimation();
    }

    void setupShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
        bind(im, am, KeyEvent.VK_ESCAPE, "exit", this::exitGame);
        bind(im, am, KeyEvent.VK_SPACE, "endDay", this::requestNextDay);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            bind(im, am, KeyEvent.VK_1 + i, "upg" + i, () -> {
                if (!busy) {
                    buy(idx);
                }
            });
        }
    }

    void bind(InputMap im, ActionMap am, int key, String name, Runnable r) {
        im.put(KeyStroke.getKeyStroke(key, 0), name);
        am.put(name, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    void startAnimation() {
        animTimer = new Timer(20, e -> {
            pulse += 0.12;
            skyT = (skyT + 0.0004) % 1.0;
            sky.repaint();

            double d = money - dispMoney;
            if (Math.abs(d) > 0.5) {
                dispMoney += d * 0.15;
                if (Math.abs(money - dispMoney) < 1) {
                    dispMoney = money;
                }
                moneyLbl.setText(php((int) Math.round(dispMoney)));
            }
            for (PillBar b : bars) {
                if (b.step()) {
                    b.repaint();
                }
            }
            boolean placing = selected != null || overlay.dragGuest != null;
            for (RoomTile t : tiles) {
                if (t.tickFloat() || placing) {
                    t.repaint();
                }
            }
            overlay.tick();
        });
        animTimer.start();
    }

    void exitGame() {
        int choice = JOptionPane.showOptionDialog(this,
                "Leave the hotel?\n\nDay " + day + "  \u2022  " + php(money) + "  \u2022  "
                        + totalGuests + " guests served\n\nProgress is not saved.",
                "Exit Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[]{"Exit Game", "Keep Playing"}, "Keep Playing");
        if (choice == 0) {
            animTimer.stop();
            dispose();
            System.exit(0);
        }
    }

    void buildRooms() {
        RoomType[] rowTypes = {RoomType.SINGLE, RoomType.DOUBLE, RoomType.DELUXE};
        int[] unlockCosts = {1500, 2500, 4000};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                Room r = new Room();
                r.number = 101 + row * 4 + col;
                r.type = rowTypes[row];
                r.unlockCost = unlockCosts[row];
                r.state = ((row == 0 && col < 2) || col == 0) ? State.AVAILABLE : State.LOCKED;
                rooms.add(r);
            }
        }
    }

    
    
    

    void buildGUI() {
        GradientPanel root = new GradientPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.add(createTopBar(), BorderLayout.NORTH);
        root.add(createQueuePanel(), BorderLayout.WEST);
        root.add(createRoomsPanel(), BorderLayout.CENTER);
        root.add(createSidePanel(), BorderLayout.EAST);
        root.add(createBottomBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    JLabel label(String text, Color c, int style, int size) {
        JLabel l = new JLabel(text);
        l.setForeground(c);
        l.setFont(font(style, size));
        return l;
    }

    PillBar bar(Color c, int h) {
        PillBar b = new PillBar(c, h);
        bars.add(b);
        return b;
    }

    JPanel chip(String title, JLabel value, JComponent extra, Color accent) {
        Card c = new Card(SURF2);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(7, 14, 8, 14));
        JLabel t = label(title, accent, Font.BOLD, 10);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.add(t);
        c.add(value);
        if (extra != null) {
            c.add(Box.createVerticalStrut(3));
            extra.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.add(extra);
        }
        return c;
    }

    JPanel createTopBar() {
        Card top = new Card(SURF);
        top.setLayout(new BorderLayout());
        top.setBorder(new EmptyBorder(10, 20, 10, 16));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        brand.add(label("\uD83C\uDFE8", TEXT, Font.PLAIN, 28));
        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(label("SUNRISE GRAND", TEXT, Font.BOLD, 18));
        names.add(label("Hotel Management", MUTED, Font.PLAIN, 11));
        brand.add(names);
        top.add(brand, BorderLayout.WEST);

        moneyLbl = label("", TEXT, Font.BOLD, 17);
        dayLbl = label("", TEXT, Font.BOLD, 17);
        levelLbl = label("", TEXT, Font.BOLD, 17);
        guestsLbl = label("", TEXT, Font.BOLD, 17);
        levelBar = bar(AMBER, 6);
        repBar = bar(ROSE, 22);
        repBar.setPreferredSize(new Dimension(150, 22));

        FancyButton exit = new FancyButton("\u2716  Exit", ROSE);
        exit.setPreferredSize(new Dimension(92, 44));
        exit.setToolTipText("Exit the game (Esc)");
        exit.addActionListener(e -> exitGame());

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        stats.setOpaque(false);
        stats.add(chip("CASH", moneyLbl, null, TEAL));
        stats.add(chip("DAY", dayLbl, null, ACCENT));
        stats.add(chip("LEVEL", levelLbl, levelBar, AMBER));
        stats.add(chip("GUESTS SERVED", guestsLbl, null, MUTED));
        stats.add(chip("REPUTATION", label(" ", TEXT, Font.PLAIN, 1), repBar, ROSE));
        stats.add(exit);
        top.add(stats, BorderLayout.EAST);
        return top;
    }

    JPanel createQueuePanel() {
        Card left = new Card(SURF);
        left.setLayout(new BorderLayout(0, 10));
        left.setBorder(new EmptyBorder(16, 14, 14, 14));
        left.setPreferredSize(new Dimension(318, 100));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        queueTitle = label("", TEXT, Font.BOLD, 18);
        header.add(queueTitle);
        header.add(Box.createVerticalStrut(4));
        header.add(label("Drag a guest onto a vacant room", MUTED, Font.PLAIN, 12));
        left.add(header, BorderLayout.NORTH);

        queuePanel = new JPanel();
        queuePanel.setOpaque(false);
        queuePanel.setLayout(new BoxLayout(queuePanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(queuePanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        left.add(scroll, BorderLayout.CENTER);
        return left;
    }

    JPanel createRoomsPanel() {
        Card center = new Card(SURF);
        center.setLayout(new BorderLayout(0, 12));
        center.setBorder(new EmptyBorder(14, 14, 14, 14));

        sky = new SkyPanel();
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.setOpaque(false);
        header.add(sky, BorderLayout.CENTER);
        header.add(label("Click an occupied room to upsell: room service or an extra night. Locked rooms can be bought.",
                MUTED, Font.PLAIN, 12), BorderLayout.SOUTH);
        center.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(3, 4, 12, 12));
        grid.setOpaque(false);
        for (Room r : rooms) {
            RoomTile t = new RoomTile(r);
            tiles.add(t);
            grid.add(t);
        }
        center.add(grid, BorderLayout.CENTER);
        return center;
    }

    JPanel createSidePanel() {
        Card right = new Card(SURF);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(new EmptyBorder(16, 14, 14, 14));
        right.setPreferredSize(new Dimension(300, 100));

        JLabel title = label("Upgrades", TEXT, Font.BOLD, 18);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(title);
        right.add(Box.createVerticalStrut(8));
        for (int i = 0; i < 4; i++) {
            shopBtns[i] = new UpgBtn(i);
            right.add(shopBtns[i]);
            right.add(Box.createVerticalStrut(6));
        }

        Card goal = new Card(SURF2);
        goal.setLayout(new BoxLayout(goal, BoxLayout.Y_AXIS));
        goal.setBorder(new EmptyBorder(10, 12, 10, 12));
        goal.setAlignmentX(Component.LEFT_ALIGNMENT);
        goal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        JLabel gt = label("DAILY GOAL", AMBER, Font.BOLD, 10);
        goalLbl = label(" ", TEXT, Font.BOLD, 14);
        goalReward = label(" ", MUTED, Font.PLAIN, 11);
        goalBar = bar(TEAL, 16);
        goalBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        for (JComponent c : new JComponent[]{gt, goalLbl, goalBar, goalReward}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            goal.add(c);
            goal.add(Box.createVerticalStrut(3));
        }
        right.add(goal);
        right.add(Box.createVerticalStrut(10));

        JLabel logTitle = label("Activity Log", TEXT, Font.BOLD, 14);
        logTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(logTitle);
        right.add(Box.createVerticalStrut(6));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(16, 20, 38));
        logArea.setForeground(MUTED);
        logArea.setFont(font(Font.PLAIN, 12));
        logArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createLineBorder(alpha(LINE, 120)));
        sp.setAlignmentX(Component.LEFT_ALIGNMENT);
        right.add(sp);
        return right;
    }

    JPanel createBottomBar() {
        Card bottom = new Card(SURF);
        bottom.setLayout(new BorderLayout(20, 0));
        bottom.setBorder(new EmptyBorder(10, 20, 10, 12));

        statusLbl = label(" ", AMBER, Font.BOLD, 15);
        bottom.add(statusLbl, BorderLayout.CENTER);

        FancyButton next = new FancyButton("END DAY  \u25B6", AMBER);
        next.setForeground(new Color(35, 28, 8));
        next.setFont(font(Font.BOLD, 17));
        next.setPreferredSize(new Dimension(220, 50));
        next.setToolTipText("Advance to the next day (Space)");
        next.addActionListener(e -> requestNextDay());

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        east.setOpaque(false);
        east.add(label("Space: End Day   1-4: Upgrades   Esc: Exit", MUTED, Font.PLAIN, 12));
        east.add(next);
        bottom.add(east, BorderLayout.EAST);
        return bottom;
    }

    
 
    

    class RoomTile extends JPanel {
        final Room r;
        boolean hover = false;
        String floatText = null;
        Color floatColor = TEXT;
        double floatAge = 0;

        RoomTile(Room r) {
            this.r = r;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!busy) {
                        onRoomClick(RoomTile.this.r, RoomTile.this, e.getPoint());
                    }
                }
            });
        }

        void pop(String text, Color c) {
            floatText = text;
            floatColor = c;
            floatAge = 0;
        }

        boolean tickFloat() {
            if (floatText == null) {
                return false;
            }
            floatAge += 0.025;
            if (floatAge >= 1) {
                floatText = null;
            }
            return true;
        }

        void updateTip() {
            switch (r.state) {
                case AVAILABLE:
                    setToolTipText("Room " + r.number + " (" + r.type.label + ") - vacant");
                    break;
                case OCCUPIED:
                    setToolTipText(r.guest.name + " - click for room service or an extra night");
                    break;
                case DIRTY:
                    setToolTipText("Click to clean for PHP 100 (or hire a cleaner)");
                    break;
                default:
                    setToolTipText("Locked - " + php(r.unlockCost) + " to unlock");
                    break;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            int x = 4, y = 4, bw = w - 8, bh = h - 8;
            if (hover) {
                g2.translate(0, -2);
            }

            Color accent, bg1, bg2;
            String st;
            switch (r.state) {
                case AVAILABLE:
                    accent = TEAL; bg1 = new Color(22, 62, 64); bg2 = new Color(18, 42, 54); st = "VACANT";
                    break;
                case OCCUPIED:
                    accent = ACCENT; bg1 = new Color(38, 46, 102); bg2 = new Color(27, 32, 72); st = "OCCUPIED";
                    break;
                case DIRTY:
                    accent = AMBER; bg1 = new Color(74, 54, 30); bg2 = new Color(48, 37, 28); st = "DIRTY";
                    break;
                default:
                    accent = MUTED; bg1 = new Color(34, 38, 58); bg2 = new Color(25, 28, 46); st = "LOCKED";
                    break;
            }

            for (int i = 5; i >= 1; i--) { // soft shadow
                g2.setColor(new Color(0, 0, 0, 14));
                g2.fillRoundRect(x - i + 1, y - i + 5, bw + 2 * i - 2, bh + 2 * i - 2, 22 + i, 22 + i);
            }
            g2.setPaint(new GradientPaint(0, y, bg1, 0, y + bh, bg2));
            g2.fillRoundRect(x, y, bw, bh, 20, 20);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(alpha(accent, hover ? 220 : 110));
            g2.drawRoundRect(x, y, bw, bh, 20, 20);

            Guest active = overlay.dragGuest != null ? overlay.dragGuest : selected;
            boolean canPlace = active != null && r.state == State.AVAILABLE;
            boolean dropHere = dropTarget == this && r.state == State.AVAILABLE;
            if (canPlace || dropHere) {
                g2.setStroke(new BasicStroke(dropHere ? 4f : 3f));
                g2.setColor(alpha(AMBER, dropHere ? 255 : (int) (130 + 110 * Math.sin(pulse))));
                g2.drawRoundRect(x - 1, y - 1, bw + 2, bh + 2, 22, 22);
            }

            g2.setColor(TEXT);
            g2.setFont(font(Font.BOLD, 20));
            g2.drawString(String.valueOf(r.number), x + 14, y + 31);
            pill(g2, r.type.label, x + 14, y + 52, r.type.color, font(Font.BOLD, 10));
            g2.setFont(font(Font.BOLD, 10));
            int pw = g2.getFontMetrics().stringWidth(st) + 14;
            pill(g2, st, x + bw - pw - 10, y + 24, accent, font(Font.BOLD, 10));

            int cx = w / 2;
            int emojiY = y + (int) (bh * 0.70);
            int lineY = y + bh - 14;
            g2.setFont(font(Font.PLAIN, 34));
            g2.setColor(TEXT);
            switch (r.state) {
                case AVAILABLE:
                    ctext(g2, "\uD83D\uDD11", cx, emojiY);
                    break;
                case OCCUPIED:
                    ctext(g2, r.guest.face, cx, emojiY);
                    break;
                case DIRTY:
                    ctext(g2, "\uD83E\uDDF9", cx, emojiY);
                    break;
                default:
                    ctext(g2, "\uD83D\uDD12", cx, emojiY);
                    break;
            }

            g2.setFont(font(Font.BOLD, 11));
            if (canPlace) {
                int v = compare(r, active);
                g2.setColor(v < 0 ? ROSE : v == 0 ? TEAL : AMBER);
                ctext(g2, v == 0 ? "Perfect match" : v > 0 ? "Free upgrade!" : "Downgrade", cx, lineY);
            } else if (r.state == State.OCCUPIED) {
                g2.setColor(TEXT);
                ctext(g2, r.guest.name.split(" ")[0] + (r.guest.vip ? " (VIP)" : "") + "  \u2022  "
                        + r.daysLeft + " night(s)", cx, lineY - 7);
                int bx = x + 16, bwid = bw - 32, by = y + bh - 12;
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(bx, by, bwid, 5, 5, 5);
                g2.setColor(ACCENT);
                g2.fillRoundRect(bx, by, (int) (bwid * r.daysLeft / (double) r.guest.nights), 5, 5, 5);
            } else {
                g2.setColor(MUTED);
                String msg = r.state == State.AVAILABLE ? "Ready for a guest"
                        : r.state == State.DIRTY ? "Click to clean  \u2022  PHP 100"
                        : "Unlock  \u2022  " + php(r.unlockCost);
                ctext(g2, msg, cx, lineY);
            }

            if (floatText != null) {
                int a = (int) (255 * (1 - floatAge));
                int fy = (int) (h * 0.45 - floatAge * 42);
                g2.setFont(font(Font.BOLD, 20));
                g2.setColor(new Color(0, 0, 0, a / 2));
                ctext(g2, floatText, cx, fy + 2);
                g2.setColor(alpha(floatColor, a));
                ctext(g2, floatText, cx, fy);
            }
            g2.dispose();
        }
    }

    
    // GUEST CARD (click to select OR drag onto a room)
    

    class GuestCard extends JPanel {
        final Guest g;
        boolean dragging = false;
        boolean hover = false;

        GuestCard(Guest g) {
            this.g = g;
            setOpaque(false);
            Dimension d = new Dimension(280, 98);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click to select, or drag onto a vacant room");

            MouseAdapter ma = new MouseAdapter() {
                Point press;

                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    press = e.getPoint();
                    dragging = false;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (press == null || busy) {
                        return;
                    }
                    if (!dragging && press.distance(e.getPoint()) > 6) {
                        dragging = true;
                        selected = g;
                        overlay.dragGuest = g;
                        queuePanel.repaint();
                    }
                    if (dragging) {
                        overlay.dragPt = SwingUtilities.convertPoint(GuestCard.this, e.getPoint(), overlay);
                        dropTarget = tileAt(overlay.dragPt);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (dragging) {
                        dragging = false;
                        Point p = SwingUtilities.convertPoint(GuestCard.this, e.getPoint(), overlay);
                        RoomTile t = tileAt(p);
                        overlay.dragGuest = null;
                        dropTarget = null;
                        if (t != null && t.r.state == State.AVAILABLE) {
                            checkIn(t.r, g);
                        } else {
                            flash(t != null ? "That room isn't vacant." : "Drop the guest onto a vacant room.", ROSE);
                            queuePanel.repaint();
                        }
                    } else if (!busy) {
                        selected = (selected == g) ? null : g;
                        if (selected != null) {
                            flash("Now click a vacant room for " + g.name + ".", AMBER);
                        }
                        queuePanel.repaint();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override
        protected void paintComponent(Graphics gr) {
            Graphics2D g2 = (Graphics2D) gr.create();
            smooth(g2);
            int w = getWidth(), h = getHeight();
            boolean sel = (g == selected);
            boolean urgent = g.patience <= 1;
            Color tc = g.wanted.color;

            g2.setColor(sel ? mix(SURF2, AMBER, 0.12f) : hover ? mix(SURF2, ACCENT, 0.15f) : SURF2);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 18, 18);
            g2.setStroke(new BasicStroke(sel || urgent ? 2.5f : 1.2f));
            g2.setColor(sel ? AMBER : urgent ? ROSE : alpha(LINE, 200));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 18, 18);

            g2.setColor(alpha(tc, 55));
            g2.fillOval(12, 14, 46, 46);
            g2.setColor(TEXT);
            g2.setFont(font(Font.PLAIN, 26));
            ctext(g2, g.face, 35, 46);

            g2.setFont(font(Font.BOLD, 14));
            g2.drawString(g.name, 68, 28);
            int px = 68;
            px += pill(g2, g.wanted.label, px, 49, tc, font(Font.BOLD, 10)) + 6;
            g2.setFont(font(Font.PLAIN, 12));
            g2.setColor(MUTED);
            g2.drawString(g.nights + " night" + (g.nights > 1 ? "s" : ""), px, 49);
            if (g.vip) {
                pill(g2, "VIP x1.5", w - 74, 24, AMBER, font(Font.BOLD, 10));
            }
            g2.setFont(font(Font.BOLD, 13));
            g2.setColor(TEAL);
            g2.drawString("\u2248 " + php(quote(g)), 68, 70);

            int sx = 14;
            Color pc = g.patience <= 1 ? ROSE : g.patience == 2 ? AMBER : TEAL;
            for (int i = 0; i < g.maxPatience; i++) {
                g2.setColor(i < g.patience ? pc : new Color(255, 255, 255, 35));
                g2.fillRoundRect(sx, 84, 24, 5, 5, 5);
                sx += 28;
            }
            if (urgent) {
                g2.setFont(font(Font.BOLD, 10));
                g2.setColor(ROSE);
                g2.drawString("ABOUT TO LEAVE", sx + 4, 90);
            }
            g2.dispose();
        }
    }

    RoomTile tileAt(Point pInOverlay) {
        for (RoomTile t : tiles) {
            Rectangle rc = SwingUtilities.convertRectangle(t.getParent(), t.getBounds(), overlay);
            if (rc.contains(pInOverlay)) {
                return t;
            }
        }
        return null;
    }

    
   
    

    void refreshAll() {
        if (Math.abs(dispMoney - money) < 0.5) {
            moneyLbl.setText(php(money));
        }
        moneyLbl.setForeground(money < 0 ? ROSE : TEXT);
        dayLbl.setText("Day " + day);
        levelLbl.setText(LEVEL_NAMES[level - 1] + "  (Lv " + level + ")");
        guestsLbl.setText(String.valueOf(totalGuests));
        repBar.set(reputation / 100.0, reputation + " / 100");

        if (level < LEVEL_REQ.length) {
            int lo = LEVEL_REQ[level - 1], hi = LEVEL_REQ[level];
            levelBar.set((totalEarned - lo) / (double) (hi - lo), "");
            levelLbl.setToolTipText("Earn " + php(hi - totalEarned) + " more to reach the next level");
        } else {
            levelBar.set(1, "");
            levelLbl.setToolTipText("Maximum level reached!");
        }
        rebuildQueue();
        for (UpgBtn b : shopBtns) {
            b.setEnabled(upg[b.idx] < UPG_MAX[b.idx] && money >= upgCost(b.idx));
            b.repaint();
        }
        updateGoal();
        repaintTiles();
    }

    void repaintTiles() {
        for (RoomTile t : tiles) {
            t.updateTip();
            t.repaint();
        }
    }

    void rebuildQueue() {
        queuePanel.removeAll();
        for (Guest g : queue) {
            queuePanel.add(new GuestCard(g));
            queuePanel.add(Box.createVerticalStrut(10));
        }
        if (queue.isEmpty()) {
            JLabel empty = label("<html>No guests waiting.<br>Press END DAY to attract more!</html>", MUTED, Font.BOLD, 13);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            queuePanel.add(Box.createVerticalStrut(20));
            queuePanel.add(empty);
        }
        queueTitle.setText("Guest Queue  " + queue.size() + "/" + MAX_QUEUE);
        queuePanel.revalidate();
        queuePanel.repaint();
    }

    void log(String msg) {
        logArea.append("Day " + day + ": " + msg + "\n");
        if (logArea.getDocument().getLength() > 7000) {
            logArea.replaceRange("", 0, 2500);
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

   
    void flash(String msg, Color c) {
        statusLbl.setText(msg);
        statusLbl.setForeground(c);
    }

    /** Status line + toast. */
    void note(String msg, Color c) {
        flash(msg, c);
        overlay.toast(msg, c);
    }

    void changeRep(int delta) {
        reputation = Math.max(0, Math.min(100, reputation + delta));
    }

    Point tileCenter(Room r) {
        RoomTile t = tiles.get(rooms.indexOf(r));
        return SwingUtilities.convertPoint(t, t.getWidth() / 2, t.getHeight() / 2, overlay);
    }

    void popOn(Room r, String text, Color c) {
        tiles.get(rooms.indexOf(r)).pop(text, c);
    }

    
  
    

    void newGoal() {
        goalType = rnd.nextInt(3);
        goalProgress = 0;
        goalDone = false;
        if (goalType == 0) {
            goalTarget = 2 + rnd.nextInt(2);
            goalMoney = 500;
            goalRep = 3;
        } else if (goalType == 1) {
            goalTarget = 2000 + level * 1000;
            goalMoney = 700;
            goalRep = 2;
        } else {
            goalTarget = 2;
            goalMoney = 400;
            goalRep = 6;
        }
    }

    void addGoal(int n) {
        if (goalDone) {
            return;
        }
        goalProgress += n;
        if (goalProgress >= goalTarget) {
            goalProgress = goalTarget;
            goalDone = true;
            money += goalMoney;
            totalEarned += goalMoney;
            changeRep(goalRep);
            overlay.confetti();
            note("Goal complete! +" + php(goalMoney) + " and +" + goalRep + " reputation", AMBER);
            log("Daily goal completed (+" + php(goalMoney) + ").");
        }
    }

    void updateGoal() {
        String t = goalType == 0 ? "Check in " + goalTarget + " guests"
                : goalType == 1 ? "Earn " + php(goalTarget) + " today"
                : "Make " + goalTarget + " perfect matches";
        goalLbl.setText(t);
        goalReward.setText(goalDone ? "Completed!" : "Reward: " + php(goalMoney) + " + " + goalRep + " reputation");
        goalBar.set(goalProgress / (double) goalTarget,
                goalType == 1 ? "" : goalProgress + " / " + goalTarget);
    }

    
    
    double mult(Guest g) {
        double m = 1.0 + upg[2] * 0.10;
        return g.vip ? m * 1.5 : m;
    }

    int quote(Guest g) {
        return (int) Math.round(g.wanted.price * g.nights * mult(g));
    }

    int compare(Room r, Guest g) {
        return r.type.ordinal() - g.wanted.ordinal();
    }

    int upgCost(int i) {
        return UPG_BASE[i] * (upg[i] + 1);
    }

    Guest newGuest() {
        Guest g = new Guest();
        g.name = NAMES[rnd.nextInt(NAMES.length)];
        g.face = FACES[rnd.nextInt(FACES.length)];
        int roll = rnd.nextInt(10) + (level - 1);
        g.wanted = roll < 5 ? RoomType.SINGLE : roll < 9 ? RoomType.DOUBLE : RoomType.DELUXE;
        g.nights = 1 + rnd.nextInt(4);
        g.maxPatience = 3 + upg[3];
        g.patience = g.maxPatience;
        g.vip = reputation >= 60 && rnd.nextInt(10) == 0;
        return g;
    }

    /** Adds money, tracks goal progress and shows a coin burst on the room. */
    void earn(int amt, Room r, String label) {
        money += amt;
        totalEarned += amt;
        popOn(r, (label.isEmpty() ? "" : label + " ") + "+" + String.format("%,d", amt), AMBER);
        Point c = tileCenter(r);
        overlay.burst(c.x, c.y, AMBER, 16);
        if (goalType == 1) {
            addGoal(amt);
        }
    }

    void onRoomClick(Room r, RoomTile tile, Point p) {
        switch (r.state) {
            case LOCKED:
                if (money >= r.unlockCost) {
                    int ok = JOptionPane.showConfirmDialog(this,
                            "Unlock Room " + r.number + " (" + r.type.label + ") for " + php(r.unlockCost) + "?",
                            "Expand your hotel", JOptionPane.YES_NO_OPTION);
                    if (ok == JOptionPane.YES_OPTION) {
                        money -= r.unlockCost;
                        r.state = State.AVAILABLE;
                        log("Unlocked Room " + r.number + ".");
                        note("Room " + r.number + " unlocked!", TEAL);
                        popOn(r, "Unlocked!", TEAL);
                        Point c = tileCenter(r);
                        overlay.burst(c.x, c.y, TEAL, 24);
                        refreshAll();
                    }
                } else {
                    flash("You need " + php(r.unlockCost) + " to unlock this room.", ROSE);
                }
                break;
            case DIRTY:
                if (money >= 100) {
                    money -= 100;
                    r.state = State.AVAILABLE;
                    flash("Room " + r.number + " cleaned (-PHP 100).", TEAL);
                    popOn(r, "-PHP 100", ROSE);
                    refreshAll();
                } else {
                    flash("Not enough money to clean the room!", ROSE);
                }
                break;
            case OCCUPIED:
                showRoomMenu(r, tile, p);
                break;
            default:
                if (selected == null) {
                    flash("Pick a guest from the queue first - or drag one here!", AMBER);
                } else {
                    checkIn(r, selected);
                }
                break;
        }
    }

    JMenuItem menuItem(String text, boolean enabled, Runnable action) {
        JMenuItem it = new JMenuItem(text);
        it.setEnabled(enabled);
        it.setOpaque(true);
        it.setBackground(SURF2);
        it.setForeground(enabled ? TEXT : MUTED);
        it.setFont(font(Font.PLAIN, 13));
        it.setBorder(new EmptyBorder(7, 12, 7, 12));
        it.addActionListener(e -> action.run());
        return it;
    }

    void showRoomMenu(Room r, RoomTile tile, Point p) {
        Guest g = r.guest;
        JPopupMenu m = new JPopupMenu();
        m.setBackground(SURF2);
        m.setBorder(BorderFactory.createLineBorder(LINE));
        JMenuItem head = menuItem(g.face + "  " + g.name + " - Room " + r.number, false, () -> { });
        head.setForeground(AMBER);
        m.add(head);
        m.add(menuItem("Room service  (-PHP 60, 75% tip chance)", !g.served && money >= 60, () -> roomService(r)));
        m.add(menuItem("Pitch an extra night  (55% accept)", !g.pitched, () -> pitchNight(r)));
        m.show(tile, p.x, p.y);
    }

    void roomService(Room r) {
        Guest g = r.guest;
        g.served = true;
        money -= 60;
        if (rnd.nextInt(100) < 75) {
            int tip = 150 + rnd.nextInt(151);
            earn(tip, r, "Tip");
            changeRep(1);
            note(g.name.split(" ")[0] + " loved the room service! +" + php(tip), TEAL);
            log("Room service in " + r.number + " earned " + php(tip) + ".");
        } else {
            note(g.name.split(" ")[0] + " wasn't impressed. Better luck next time.", MUTED);
        }
        refreshAll();
    }

    void pitchNight(Room r) {
        Guest g = r.guest;
        g.pitched = true;
        if (rnd.nextInt(100) < 55) {
            int pay = (int) Math.round(r.type.price * mult(g) * 0.9);
            r.daysLeft++;
            g.nights++;
            earn(pay, r, "Extra night");
            note(g.name.split(" ")[0] + " extends their stay! +" + php(pay), TEAL);
            log(g.name + " booked an extra night in Room " + r.number + ".");
        } else {
            changeRep(-1);
            note(g.name.split(" ")[0] + " declined the offer (-1 reputation).", ROSE);
        }
        refreshAll();
    }

    void checkIn(Room r, Guest g) {
        int v = compare(r, g);
        int base, repChange;
        String msg;
        if (v == 0) {
            base = r.type.price;
            repChange = 3;
            msg = "Perfect match!";
        } else if (v > 0) {
            base = g.wanted.price;
            repChange = 5;
            msg = "Free upgrade - the guest is thrilled!";
        } else {
            base = (int) (r.type.price * 0.7);
            repChange = -3;
            msg = "Guest wanted a better room and paid less.";
        }
        int pay = (int) Math.round(base * g.nights * mult(g));
        totalGuests++;
        changeRep(repChange);
        r.state = State.OCCUPIED;
        r.guest = g;
        r.daysLeft = g.nights;
        queue.remove(g);
        selected = null;
        dropTarget = null;

        earn(pay, r, "");
        if (goalType == 0 || (goalType == 2 && v == 0)) {
            addGoal(1);
        }
        log(g.name + " checked into Room " + r.number + " (+" + php(pay) + ").");
        note(msg + "  +" + php(pay), v < 0 ? ROSE : TEAL);
        refreshAll();
        checkLevelUp();
    }

    void buy(int i) {
        if (upg[i] >= UPG_MAX[i]) {
            return;
        }
        int cost = upgCost(i);
        if (money < cost) {
            flash("Not enough money for that upgrade!", ROSE);
            return;
        }
        money -= cost;
        upg[i]++;
        if (i == 2) {
            changeRep(5);
        }
        log("Bought: " + UPG_NAMES[i] + " (level " + upg[i] + ").");
        note(UPG_NAMES[i] + " upgraded!", ACCENT);
        refreshAll();
    }

    void requestNextDay() {
        if (busy) {
            return;
        }
        boolean vacant = false;
        for (Room r : rooms) {
            if (r.state == State.AVAILABLE) {
                vacant = true;
                break;
            }
        }
        if (vacant && !queue.isEmpty()) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "You still have guests waiting and vacant rooms.\nEnd the day anyway?",
                    "End Day?", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) {
                return;
            }
        }
        selected = null;
        overlay.startDay(this::nextDayLogic, this::afterDay);
    }

    void nextDayLogic() {
        day++;
        skyT = 0.02;
        int arrivalsTotal = 0, left = 0;

        for (Room r : rooms) {
            if (r.state == State.OCCUPIED && --r.daysLeft <= 0) {
                int tip = rnd.nextInt(3) == 0 ? 0 : reputation * (1 + rnd.nextInt(4));
                money += tip;
                totalEarned += tip;
                log(r.guest.name + " checked out of Room " + r.number + (tip > 0 ? " (tip " + php(tip) + ")." : "."));
                r.state = State.DIRTY;
                r.guest = null;
            }
        }

        int capacity = upg[0] * 2, cleaned = 0;
        for (Room r : rooms) {
            if (capacity > 0 && r.state == State.DIRTY) {
                r.state = State.AVAILABLE;
                capacity--;
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log("Cleaners prepared " + cleaned + " room(s).");
        }

        int open = 0;
        for (Room r : rooms) {
            if (r.state != State.LOCKED) {
                open++;
            }
        }
        int expenses = open * 40 + upg[0] * 150;
        money -= expenses;

        for (int i = queue.size() - 1; i >= 0; i--) {
            Guest g = queue.get(i);
            if (--g.patience <= 0) {
                queue.remove(i);
                changeRep(-4);
                left++;
                log(g.name + " got tired of waiting and left! (-4 reputation)");
            }
        }

        arrivalsTotal += addGuests(1 + rnd.nextInt(2 + upg[1]) + (reputation >= 70 ? 1 : 0));

        int e = rnd.nextInt(100);
        pendingEvent = 0;
        if (e < 8) {
            arrivalsTotal += addGuests(2);
            log("A tour bus arrived with extra guests!");
        } else if (e < 15) {
            changeRep(4);
            log("A guest posted a glowing review! (+4 reputation)");
        } else if (e < 21) {
            money -= 300;
            log("A pipe burst - repairs cost PHP 300.");
        } else if (e < 29) {
            pendingEvent = 1;
        } else if (e < 35) {
            pendingEvent = 2;
        } else if (e < 41) {
            pendingEvent = 3;
        }

        newGoal();
        summaryText = "Expenses -" + php(expenses) + "     New guests +" + arrivalsTotal + "     Left -" + left;
        refreshAll();
        checkLevelUp();
    }

    void afterDay() {
        flash("Day " + day + " begins. Expenses paid.", AMBER);
        if (pendingEvent > 0) {
            runEvent(pendingEvent);
            pendingEvent = 0;
        }
        refreshAll();
        checkGameOver();
    }

    int choice(String title, String msg, String a, String b) {
        return JOptionPane.showOptionDialog(this, msg, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, new String[]{a, b}, a);
    }

    void runEvent(int ev) {
        if (ev == 1) {
            int c = choice("Food Critic", "A famous food critic is staying tonight!\nServe a gourmet breakfast for PHP 500?",
                    "Serve it (PHP 500)", "Decline");
            if (c == 0 && money >= 500) {
                money -= 500;
                if (rnd.nextInt(100) < 65) {
                    changeRep(10);
                    note("The critic raves about your hotel! +10 reputation", TEAL);
                } else {
                    changeRep(-5);
                    note("The critic was not impressed. -5 reputation", ROSE);
                }
            } else if (c == 0) {
                note("You can't afford the gourmet breakfast.", ROSE);
            }
        } else if (ev == 2) {
            int c = choice("Wedding Party", "A wedding party wants two big rooms!\nThey pay 1.5x but have little patience.",
                    "Accept the booking", "Decline");
            if (c == 0) {
                for (int i = 0; i < 2 && queue.size() < MAX_QUEUE; i++) {
                    Guest g = newGuest();
                    g.vip = true;
                    g.wanted = rnd.nextBoolean() ? RoomType.DOUBLE : RoomType.DELUXE;
                    g.nights = 3;
                    g.maxPatience = 2;
                    g.patience = 2;
                    queue.add(g);
                }
                note("Two VIP wedding guests are waiting at the desk!", AMBER);
            }
        } else {
            int c = choice("Water Leak", "A pipe is leaking in the east wing.\nCall a plumber now (PHP 400) or patch it yourself?",
                    "Call plumber (PHP 400)", "Patch it myself");
            if (c == 0) {
                money -= 400;
                note("The plumber fixed it quickly.", TEAL);
            } else if (rnd.nextBoolean()) {
                note("Your patch job held up. Lucky!", TEAL);
            } else {
                changeRep(-6);
                note("The patch failed and guests complained. -6 reputation", ROSE);
            }
        }
    }

    int addGuests(int count) {
        int added = 0;
        for (int i = 0; i < count; i++) {
            if (queue.size() < MAX_QUEUE) {
                queue.add(newGuest());
                added++;
            } else {
                changeRep(-1);
                log("The queue is full - a guest walked away. (-1 reputation)");
            }
        }
        return added;
    }

    void checkLevelUp() {
        int newLevel = 1;
        for (int i = 0; i < LEVEL_REQ.length; i++) {
            if (totalEarned >= LEVEL_REQ[i]) {
                newLevel = i + 1;
            }
        }
        if (newLevel > level) {
            level = newLevel;
            changeRep(5);
            log("Level up! Your hotel is now a " + LEVEL_NAMES[level - 1] + ".");
            overlay.confetti();
            note("LEVEL UP!  You are now a " + LEVEL_NAMES[level - 1], AMBER);
            refreshAll();
        }
    }

    void checkGameOver() {
        if (money >= -1000) {
            if (money < 0) {
                flash("You are in debt! Fill your rooms before it is too late.", ROSE);
            }
            return;
        }
        int c = JOptionPane.showOptionDialog(this,
                "Your hotel went bankrupt on day " + day + "!\n\nGuests served: " + totalGuests
                        + "\nTotal earned: " + php(totalEarned),
                "Game Over", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                null, new String[]{"Play Again", "Quit"}, "Play Again");
        if (c == 0) {
            animTimer.stop();
            dispose();
            SwingUtilities.invokeLater(() -> new HotelManagementGame().setVisible(true));
        } else {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
          
        }
        SwingUtilities.invokeLater(() -> new HotelManagementGame().setVisible(true));
    }
}
