import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Stroke;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class centralGui extends JFrame {
    private centralAgent myAgent;

    private int canvasWidth = 1000;
    private int canvasHeight = 600;
    private int margin = 100;
    private int roadLength = canvasWidth - 2 * margin;

    private JTextField msg;
    private JPanel road;

    public centralGui (centralAgent agent) {
        myAgent = agent;
        setSize(canvasWidth, canvasHeight);
        getContentPane().setLayout(null);

        Painter drawing = new Painter();
        getContentPane().add(drawing);
        drawing.setBounds(0,0,canvasWidth,canvasHeight);

        road = new JPanel();
        getContentPane().add(road,0);
        road.setBounds(margin, margin, roadLength, 150);
        road.setOpaque(false);
        road.setLayout(new FlowLayout(FlowLayout.CENTER,5,0));

        setTitle(myAgent.getLocalName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private class Painter extends JPanel {

        public Painter() {
            setBackground(new Color(50,200,50));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;

            // g2d.setBackground(new Color(50,200,50));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(margin, margin, roadLength, 150);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(margin, margin, canvasWidth-margin, margin);
            g2d.drawLine(margin, margin + 150, canvasWidth-margin, margin + 150);
            Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
            g2d.setStroke(dashed);
            g2d.drawLine(margin, margin + 50, canvasWidth-margin, margin + 50);
            g2d.drawLine(margin, margin + 100, canvasWidth-margin, margin + 100);
        }
    }

    public void addPortal() {
        JPanel portal = new JPanel();
        road.add(portal);
        portal.setLayout(new GridBagLayout());
        portal.setOpaque(false);
        portal.setPreferredSize(new Dimension(50,150));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weighty = 3;
        for (int i = 0; i < 3; i++) {
            JPanel msi = new JPanel();
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.fill = GridBagConstraints.CENTER;
            msi.setLayout(new GridBagLayout());
            msi.setOpaque(false);
            msi.setBorder(new LineBorder(Color.WHITE, 1));
            msi.setPreferredSize(new Dimension(45,45));
            gbc.gridy=i;
            portal.add(msi,gbc);
            msg = new JTextField("*70*",3);
            msg.setEditable(false);
            msg.setOpaque(false);
            msg.setForeground(Color.white);
            msg.setFont(new Font("Arial", Font.BOLD, 16));
            msg.setHorizontalAlignment(JTextField.CENTER);
            msg.setBorder(BorderFactory.createEmptyBorder());
            msi.add(msg,gbc2);
            revalidate();
            repaint();
        }
    }
}