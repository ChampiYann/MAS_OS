package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import jade.core.AID;
import measure.MSI;

public class Portal {
    private AID name;
    private float location;
    protected JTextField[] msg;

    public Portal(AID id, float km, JPanel portal, GridBagConstraints gbc) {
        name = id;
        location = km;
        msg = new JTextField[4];

        for (int i = 0; i < 4; i++) {
            JPanel msi = new JPanel();
            GridBagConstraints gbc2 = new GridBagConstraints();
            gbc2.fill = GridBagConstraints.CENTER;
            msi.setLayout(new GridBagLayout());
            msi.setOpaque(false);
            msi.setBorder(new LineBorder(Color.WHITE, 1));
            msi.setPreferredSize(new Dimension(45, 45));
            gbc.gridy = i;
            portal.add(msi, gbc);
            msg[i] = new JTextField(" ", 3);
            msg[i].setEditable(false);
            msg[i].setOpaque(false);
            msg[i].setForeground(Color.white);
            msg[i].setFont(new Font("Arial", Font.BOLD, 16));
            msg[i].setHorizontalAlignment(JTextField.CENTER);
            msg[i].setBorder(BorderFactory.createEmptyBorder());
            msi.add(msg[i], gbc2);
            portal.revalidate();
            portal.repaint();
        }
        msg[3].setText(Float.toString(location));
    }

    public void update(int[] symbols) {
        for (int i = 0; i < 3; i++) {
            msg[i].setText(MSI.getSymbol(symbols[i]));
        }
    }

    /**
     * @return the name
     */
    public AID getName() {
        return name;
    }

    /**
     * @return the location
     */
    public float getLocation() {
        return location;
    }
}