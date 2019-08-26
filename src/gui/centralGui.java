package gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

import agents.centralAgent;
import agents.osAgent;
import config.Configuration;
import config.RefConfiguration;
import gui.CentralBackground;
import jade.core.AID;

public class centralGui extends JFrame {

    private static final long serialVersionUID = 1L;

    private centralAgent myAgent;

    public int canvasWidth = 1500;
    public int canvasHeight = 600;
    public int margin = 10;
    public int roadLength = canvasWidth - 2 * margin;

    private JPanel road;
    private JPanel refRoad;
    private JTextField timeText;

    private ArrayList<Portal> portalList = new ArrayList<Portal>();
    private ArrayList<RefConfiguration> refConfigList = new ArrayList<RefConfiguration>();
    private ArrayList<RefPortal> refPortalList = new ArrayList<RefPortal>();

    private BufferedReader msiReplay;

    private FileWriter logWriter;

    public centralGui(centralAgent agent) {
        myAgent = agent;
        setSize(canvasWidth, canvasHeight);
        getContentPane().setLayout(null);

        CentralBackground background = new CentralBackground(this);
        getContentPane().add(background);
        background.setBounds(0, 0, canvasWidth, canvasHeight);

        road = new JPanel();
        getContentPane().add(road, 0);
        road.setBounds(margin, margin, roadLength, 200);
        road.setOpaque(false);
        road.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        refRoad = new JPanel();
        getContentPane().add(refRoad, 0);
        refRoad.setBounds(margin, margin + 2 * 150, roadLength, 200);
        refRoad.setOpaque(false);
        refRoad.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

        timeText = new JTextField(Long.toString(System.currentTimeMillis()));
        getContentPane().add(timeText, 0);
        timeText.setBounds(margin, 2 * margin + 200, 150, 50);

        try {
            FileReader reader = new FileReader("180223BEELDEN.csv");
            msiReplay = new BufferedReader(reader);
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            logWriter = new FileWriter("log.txt");
        } catch (Exception e) {
            //TODO: handle exception
        }

        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (agent.getTime() != null) {
                    boolean done = false;
                    while(!done) {
                        try {
                            timeText.setText(agent.getTime().toString());
                            String line = null;
                            line = msiReplay.readLine().replaceAll(",", ".");
                            String[] values = line.split(";");
                            LocalTime lineTime = LocalTime.parse(values[1]);
                            if (lineTime.compareTo(agent.getTime().plusMinutes(1)) > -1) {
                                msiReplay.reset();
                                done = true;
                            } else {
                                msiReplay.mark(1000);
                                updateRef(new AID(values[4] + " " + values[5],AID.ISLOCALNAME),Arrays.copyOfRange(values,6,8+1));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            done = true;
                        }
                    }
                    updateRefCongestion();
                    Iterator<Portal> portalIterator = portalList.iterator();
                    Iterator<RefPortal> refPortalIterator = refPortalList.iterator();
                    while (portalIterator.hasNext() && refPortalIterator.hasNext()) {
                        Portal portal = portalIterator.next();
                        RefPortal refPortal = refPortalIterator.next();
                        try {
                            logWriter.write(agent.getTime().toString() + "," + refPortal.getLocation() + "," + refPortal.msg[3].getForeground().getBlue() +
                                "," + refPortal.msg[0].getText() + "," + refPortal.msg[1].getText() + "," + refPortal.msg[2].getText() +
                                "," + refPortal.getLocation() + "," + portal.msg[0].getText() + "," + portal.msg[1].getText() +
                                "," + portal.msg[2].getText() + "\n");
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        new Timer((int)osAgent.minute, taskPerformer).start();

        File folder = new File("config");
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {

            Pattern kmPattern = Pattern.compile("(?<=\\s)\\d{1,3}[,\\.]\\d");
            Matcher kmMatcher = kmPattern.matcher(listOfFiles[i].getName());
            kmMatcher.find();
            Pattern hmPattern = Pattern.compile("(?<=[+-])\\d{1,3}");
            Matcher hmMatcher = hmPattern.matcher(listOfFiles[i].getName());
            String kmDot;
            try {
                kmDot = kmMatcher.group().replaceAll(",", ".");
            } catch (Exception e) {
                kmDot = kmMatcher.group();
            }
            String location;
            if (hmMatcher.find()) {
                location = kmDot + hmMatcher.group();
            } else {
                location = kmDot + "00";
            }

            RefConfiguration config = new RefConfiguration();
            config.getAID = new AID("A13R "+location,AID.ISLOCALNAME);
            config.location = Float.parseFloat(location);
            config.fileName = "config\\" + listOfFiles[i].getName();
            refConfigList.add(config);
        }
        addRefPortal();

        setTitle(myAgent.getLocalName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    

    public void addPortal() {
        ArrayList<Configuration> OsList = myAgent.getOS();
        OsList.sort(Configuration.kmCompare);
        
        for (int i = 0; i < OsList.size(); i++) {
            try {
                if (!portalList.get(i).getName().equals(OsList.get(i).getAID)) {
                    JPanel portal = new JPanel();
                    road.add(portal, i);
                    portal.setLayout(new GridBagLayout());
                    portal.setOpaque(false);
                    portal.setPreferredSize(new Dimension(50,200));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridx = 0;
                    gbc.weighty = 3;
                    portalList.add(i, new Portal(OsList.get(i).getAID, OsList.get(i).location,portal,gbc));
                }
            } catch (IndexOutOfBoundsException e) {
                JPanel portal = new JPanel();
                road.add(portal,i);
                portal.setLayout(new GridBagLayout());
                portal.setOpaque(false);
                portal.setPreferredSize(new Dimension(50,200));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 0;
                gbc.weighty = 3;
                portalList.add(i, new Portal(OsList.get(i).getAID, OsList.get(i).location,portal,gbc));
            }
            revalidate();
            repaint();
        }
    }

    public void addRefPortal() {
        refConfigList.sort(Configuration.kmCompare);
        
        for (int i = 0; i < refConfigList.size(); i++) {
            try {
                if (!refPortalList.get(i).getName().equals(refConfigList.get(i).getAID)) {
                    JPanel portal = new JPanel();
                    refRoad.add(portal, i);
                    portal.setLayout(new GridBagLayout());
                    portal.setOpaque(false);
                    portal.setPreferredSize(new Dimension(50,200));
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.gridx = 0;
                    gbc.weighty = 3;
                    refPortalList.add(i, new RefPortal(refConfigList.get(i).getAID, refConfigList.get(i).location,portal,gbc,refConfigList.get(i).fileName));
                }
            } catch (IndexOutOfBoundsException e) {
                JPanel portal = new JPanel();
                refRoad.add(portal,i);
                portal.setLayout(new GridBagLayout());
                portal.setOpaque(false);
                portal.setPreferredSize(new Dimension(50,200));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 0;
                gbc.weighty = 3;
                refPortalList.add(i, new RefPortal(refConfigList.get(i).getAID, refConfigList.get(i).location,portal,gbc,refConfigList.get(i).fileName));
            }
        }
    }

    public void update(AID id, int[] symbols) {
        Iterator<Portal> iterator = portalList.iterator();
        while (iterator.hasNext()) {
            Portal portal = iterator.next();
            if (portal.getName().equals(id)) {
                portal.update(symbols);
            }
        }
    }

    public void updateRef(AID id, String[] symbols) {
        Iterator<RefPortal> iterator = refPortalList.iterator();
        while (iterator.hasNext()) {
            RefPortal portal = iterator.next();
            if (portal.getName().equals(id)) {
                portal.update(symbols);
            }
        }
    }

    public void updateRefCongestion() {
        Iterator<RefPortal> iterator = refPortalList.iterator();
        while (iterator.hasNext()) {
            RefPortal portal = iterator.next();
            portal.updateCongestion();
        }
    }
}