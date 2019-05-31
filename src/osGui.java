import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

class osGui extends JFrame implements ActionListener {

    private osAgent myAgent;

    private JTextField[] msg;

    public osGui(osAgent agent) {
        myAgent = agent;

        setTitle("OS Agent - " + myAgent.getLocalName());

        int numLanes = myAgent.getNumLanes();

        setSize(300, (numLanes+1)*100);

        msg = new JTextField[numLanes];

        JPanel base = new JPanel();
        
        base.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        getContentPane().add(base);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 10;
        gbc.ipady = 50;
        gbc.gridx = 0;
        gbc.weighty = 1;

        for (int i = 0; i < numLanes; i++) {
            JPanel p = new JPanel();
            p.setBackground(Color.black);
            p.setLayout(new GridBagLayout());
            gbc.gridy=i;
            base.add(p,gbc);
            msg[i] = new JTextField(Integer.toString(i),3);
            msg[i].setEditable(false);
            msg[i].setBackground(Color.black);
            msg[i].setForeground(Color.white);
            msg[i].setFont(new Font("Arial", Font.BOLD, 30));
            msg[i].setHorizontalAlignment(JTextField.CENTER);
            msg[i].setBorder(BorderFactory.createEmptyBorder());
            p.add(msg[i]);
        }

        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    public void update(String measure, int i) {
        msg[i].setText(measure);
    }
    
}