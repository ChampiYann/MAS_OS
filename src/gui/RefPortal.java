package gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JPanel;

import jade.core.AID;

public class RefPortal extends Portal {
    private BufferedReader congestionReader;

    public RefPortal(AID id, float km, JPanel portal, GridBagConstraints gbc, String fn) {
        super(id, km, portal, gbc);
        try {
            congestionReader = new BufferedReader(new FileReader(fn));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    public void updateCongestion() {
        String newLine;
        try {
            newLine = congestionReader.readLine();
            String[] values = newLine.split(",");
            String[] elements = values[1].split(" ");
            for (int i = 0; i < elements.length; i++) {
                if (Integer.parseInt(elements[i]) == 1) {
                    msg[3].setForeground(Color.RED);
                } else {
                    msg[3].setForeground(Color.white);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}