package gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import agents.centralAgent;
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
            logWriter = new FileWriter("log.txt");
        } catch (Exception e) {
            //TODO: handle exception
        }

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

    public void updateRef(float location, String[] symbols) {
        Iterator<RefPortal> iterator = refPortalList.iterator();
        while (iterator.hasNext()) {
            RefPortal portal = iterator.next();
            if (portal.getLocation() == location) {
                portal.update(symbols);
            }
        }
    }

    public void updateTime(LocalDateTime dateTime) {
        timeText.setText(dateTime.toString());
    }

    public void updateCongestion(float location, boolean congestion) {
        Iterator<RefPortal> iterator = refPortalList.iterator();
        while (iterator.hasNext()) {
            RefPortal portal = iterator.next();
            if (portal.getLocation() == location) {
                portal.updateCongestion(congestion);
            }
        }
    }

    public void log() {
        Iterator<Portal> portalIterator = portalList.iterator();
        Iterator<RefPortal> refPortalIterator = refPortalList.iterator();
        while (portalIterator.hasNext() && refPortalIterator.hasNext()) {
            Portal portal = portalIterator.next();
            RefPortal refPortal = refPortalIterator.next();
            try {
                logWriter.write(myAgent.getDateTime().toString() + "," + refPortal.getLocation() + "," + refPortal.msg[3].getForeground().getBlue() +
                    "," + refPortal.msg[0].getText() + "," + refPortal.msg[1].getText() + "," + refPortal.msg[2].getText() +
                    "," + refPortal.getLocation() + "," + portal.msg[0].getText() + "," + portal.msg[1].getText() +
                    "," + portal.msg[2].getText() + "\n");
                    logWriter.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}