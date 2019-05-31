import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class central extends Agent {
    @Override
    protected void setup() {
        // Print out welcome message
        System.out.println("Hello! Central agent is ready.");

        // Add listener behaviour
        addBehaviour(new Listener());
    }

    @Override
    protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Central terminating.");
    }

    private class Listener extends CyclicBehaviour {
        @Override
        public void action() {

            ACLMessage msg = receive();
            if (msg!= null) {
                System.out.print(msg.getContent());
            }
        }
    }
}