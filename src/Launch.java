import jade.core.Runtime;

import java.io.File;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class Launch {
    public static void main(String[] args) {
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Create a default profile
        Profile p = new ProfileImpl();
        p.setParameter(Profile.SERVICES, "jade.core.messaging.TopicManagementService;jade.core.event.NotificationService");
        // Create a new non-main container, connecting to the default
        // main container (i.e. on this host, port 1099)
        ContainerController cc = rt.createMainContainer(p);

        try {
            // Initiate RMA (gui)
            AgentController rma = cc.createNewAgent("rma", "jade.tools.rma.rma", null);
            // Fire up GUI
            rma.start();
            // Initiate central
            AgentController central = cc.createNewAgent("central", "agents.centralAgent", null);
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
            configurations[i] = configurations[i].substring(0, configurations[i].length()-4);
        }

        // Create 5 new osAgents
        int numOS = configurations.length;
        AgentController[] OSAgents = new AgentController[numOS];

        for (int i = 0; i < numOS; i++) {
            name = "agent" + Integer.toString(i+1);
            // Concatenate arguments
            Object agentArgs[] = new Object[2];
            agentArgs[0] = lanes;
            agentArgs[1] = configurations[numOS-1-i];
            try {
                // Initiae osAgent
                OSAgents[i] = cc.createNewAgent(name, "agents.osAgent", agentArgs);
                // Fire up the agent
                OSAgents[i].start();
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}