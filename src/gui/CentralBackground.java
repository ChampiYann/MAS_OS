package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JPanel;

import gui.centralGui;

public class CentralBackground extends JPanel {

    private static final long serialVersionUID = 1L;
    
    private centralGui myGui;

    public CentralBackground(centralGui outer) {
        setBackground(new Color(50,200,50));
        myGui = outer;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // g2d.setBackground(new Color(50,200,50));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(myGui.margin, myGui.margin, myGui.roadLength, 150);
        g2d.fillRect(myGui.margin, myGui.margin + 150*2, myGui.roadLength, 150);
        g2d.setColor(Color.WHITE);
        g2d.drawLine(myGui.margin, myGui.margin, myGui.canvasWidth-myGui.margin, myGui.margin);
        g2d.drawLine(myGui.margin, myGui.margin + 150, myGui.canvasWidth-myGui.margin, myGui.margin + 150);
        g2d.drawLine(myGui.margin, myGui.margin + 150*2, myGui.canvasWidth-myGui.margin, myGui.margin + 150*2);
        g2d.drawLine(myGui.margin, myGui.margin + 150 + 150*2, myGui.canvasWidth-myGui.margin, myGui.margin + 150 + 150*2);
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.drawLine(myGui.margin, myGui.margin + 50, myGui.canvasWidth-myGui.margin, myGui.margin + 50);
        g2d.drawLine(myGui.margin, myGui.margin + 100, myGui.canvasWidth-myGui.margin, myGui.margin + 100);
        g2d.drawLine(myGui.margin, myGui.margin + 50 + 150*2, myGui.canvasWidth-myGui.margin, myGui.margin + 50 + 150*2);
        g2d.drawLine(myGui.margin, myGui.margin + 100 + 150*2, myGui.canvasWidth-myGui.margin, myGui.margin + 100 + 150*2);
    }
}