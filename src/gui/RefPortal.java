package gui;

import java.awt.Color;
import java.awt.GridBagConstraints;

import javax.swing.JPanel;

import jade.core.AID;

public class RefPortal extends Portal {

    public RefPortal(AID id, float km, JPanel portal, GridBagConstraints gbc, String fn) {
        super(id, km, portal, gbc);
    }
    
    public void update(String[] symbols) {
        for (int i = 0; i < 3; i++) {
            if (symbols[i].equals("BL")) {
                msg[i].setText(" ");
            } else { 
                msg[i].setText(symbols[i]);
            }
        }
    }

    public void updateCongestion(boolean congestion) {
        if (congestion == true) {
            msg[3].setForeground(Color.RED);
        } else {
            msg[3].setForeground(Color.white);
        }
    }
}