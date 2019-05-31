import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class Launch {
    public static void main(String[] args) {
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Create a default profile
        Profile p = new ProfileImpl();
        // Create a new non-main container, connecting to the default
        // main container (i.e. on this host, port 1099)
        ContainerController cc = rt.createMainContainer(p);

        try {
            // Initiae RMA (gui)
            AgentController rma = cc.createNewAgent("rma", "jade.tools.rma.rma", null);
            // Fre up GUI
            rma.start();
        } catch (StaleProxyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Create a new agent, a DummyAgent
        // and pass it a reference to an Object
        int numOS = 5;
        AgentController[] OSAgents = new AgentController[numOS];

        String lanes = "3";
        String upstream;
        String downstream;
        String name;
        for (int i = 0; i < numOS; i++) {
            upstream = "agent"+ Integer.toString(i+2);
            downstream = "agent" + Integer.toString(i);
            name = "agent" + Integer.toString(i+1);
            Object agentArgs[] = new Object[3];
            agentArgs[0] = lanes;
            agentArgs[1] = upstream;
            agentArgs[2] = downstream;
            try {
                // Initiae osAgent
                OSAgents[i] = cc.createNewAgent(name, "osAgent", agentArgs);
                // Fire up the agent
                OSAgents[i].start();
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}