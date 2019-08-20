package environment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Outstation {

    private ContainerController cc;
    private AgentController controller;
    private BufferedReader reader;
    private String name;
    private Object agentArgs[];
    private long restartDelay;

    public Outstation(String name, Object[] args, ContainerController cc) throws FileNotFoundException, StaleProxyException {
        this.name = name;
        this.agentArgs = args;
        this.cc = cc;
        this.restartDelay = 0;
        this.reader = new BufferedReader(new FileReader("config\\" + agentArgs[1] + ".txt"));
        this.controller = cc.createNewAgent(name, "agents.osAgent", agentArgs);
    }

    public void start() throws StaleProxyException {
        try {
            controller.start();
        } catch (StaleProxyException e) {
            controller = cc.createNewAgent(name, "agents.osAgent", agentArgs);
        }
    }

    public void kill(long restartDelay) throws StaleProxyException {
        if (restartDelay == 0) {
            controller.kill();
            this.restartDelay = restartDelay;
        }
    }

    public void sendCongestion() throws IOException, StaleProxyException {
        boolean newCongestion = false;
        String newLine = null;
        newLine = reader.readLine();
        String[] values = newLine.split(",");
        String[] elements = values[1].split(" ");
        for (int i = 0; i < elements.length; i++) {
            if (Integer.parseInt(elements[i]) == 1) {
                newCongestion = true;
            }
        }
        controller.putO2AObject(newCongestion, AgentController.ASYNC);
    }

    public void handleDelay() throws StaleProxyException {
        if (restartDelay > 0) {
            restartDelay -= 1;
        } else {
            start();
        }
    }
}