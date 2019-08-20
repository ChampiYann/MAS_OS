import jade.core.Runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

import org.apache.commons.math3.distribution.WeibullDistribution;

import agents.osAgent;
import environment.Outstation;

public class Launch {

    public static final long minute = 400;

    public static void main(String[] args) {
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Create a default profile
        Profile p = new ProfileImpl();
        p.setParameter(Profile.SERVICES, "jade.core.messaging.TopicManagementService;jade.core.event.NotificationService");
        // Create a new non-main container, connecting to the default
        // main container (i.e. on this host, port 1099)
        ContainerController cc = rt.createMainContainer(p);

        Object centralArgs[] = new Object[1];
        centralArgs[0] = (long) (Math.ceil(System.currentTimeMillis() / 10000.0) * 10000 + 10000);

        try {
            // Initiate RMA (gui)
            AgentController rma = cc.createNewAgent("rma", "jade.tools.rma.rma", null);
            // Fire up GUI
            rma.start();
            // Initiate central
            AgentController central = cc.createNewAgent("central", "agents.centralAgent", centralArgs);
            // Fire up central
            central.start();
        } catch (StaleProxyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Set arguments
        // All OS will have 3 lanes
        String lanes = "3";
        String name;

        File folder = new File("config");
        File[] listOfFiles = folder.listFiles();
        String[] configurations = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            configurations[i] = listOfFiles[i].getName();
            configurations[i] = configurations[i].substring(0, configurations[i].length() - 4);
        }

        // Create 5 new osAgents
        int numOS = configurations.length;
        Vector<Outstation> outstations = new Vector<Outstation>(numOS);

        for (int i = 0; i < numOS; i++) {
            name = "agent" + Integer.toString(i+1);
            // Concatenate arguments
            Object agentArgs[] = new Object[3];
            agentArgs[0] = lanes;
            agentArgs[1] = configurations[numOS-1-i];
            agentArgs[2] = centralArgs[0];
            try {
                // Initiae osAgent
                try {
                    outstations.add(new Outstation(name, agentArgs, cc));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // Fire up the agent
                outstations.get(i).start();
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        WeibullDistribution distribution = new WeibullDistribution(0.4360,792.0608);
        Random rand = new Random();

        Timer timer = new Timer();
        TimerTask task = new TimerTask(){
        
            @Override
            public void run() {
                Iterator<Outstation> outstationIterator = outstations.iterator();
                while (outstationIterator.hasNext()) {
                    Outstation nextOutstation = outstationIterator.next();
                    try {
                        nextOutstation.handleDelay();
                    } catch (StaleProxyException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    try {
                        nextOutstation.sendCongestion();
                    } catch (StaleProxyException | IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    if (rand.nextInt(1000) <= 0) {
                        long restartDelaySeconds = Math.round(distribution.sample()+1);
                        // System.out.println(Math.round(distribution.sample()/60)+1);
                        long restartDelayMinutes = restartDelaySeconds/60;
                        try {
                            nextOutstation.kill(restartDelayMinutes);
                        } catch (StaleProxyException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        long delay = (long)centralArgs[0] - System.currentTimeMillis();
        timer.scheduleAtFixedRate(task, delay, osAgent.minute);
    }
}