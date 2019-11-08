package environment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private float location;

    public Outstation(String name, Object[] args, ContainerController cc) throws FileNotFoundException, StaleProxyException {
        this.name = name;
        this.agentArgs = args;
        this.cc = cc;
        this.restartDelay = 0;
        this.reader = new BufferedReader(new FileReader("config\\" + agentArgs[1] + ".txt"));
        this.controller = cc.createNewAgent(name, "agents.osAgent", agentArgs);

        // Extract km reading
        Pattern kmPattern = Pattern.compile("(?<=\\s)\\d{1,3}[,\\.]\\d");
        Matcher kmMatcher = kmPattern.matcher((String)agentArgs[1]);
        kmMatcher.find();
        Pattern hmPattern = Pattern.compile("(?<=[+-])\\d{1,3}");
        Matcher hmMatcher = hmPattern.matcher((String)agentArgs[1]);
        String kmDot = null;
        try {
            kmDot = kmMatcher.group().replaceAll(",", ".");
        } catch (Exception e) {
        }
        if (hmMatcher.find()) {
            this.location = Float.parseFloat(kmDot) + Float.parseFloat(hmMatcher.group())/1000;
        } else {
            this.location = Float.parseFloat(kmDot);
        }
    }

    public void start() throws StaleProxyException {
        try {
            controller.start();
        } catch (StaleProxyException e) {
            controller = cc.createNewAgent(name, "agents.osAgent", agentArgs);
            controller.start();
        }
    }

    public void kill(long restartDelay) throws StaleProxyException {
        if (this.restartDelay == 0) {
            controller.kill();
            this.restartDelay = restartDelay;
        }
    }

    public boolean sendCongestion(FileWriter killWriter) throws IOException {
        boolean newCongestion = false;
        String newLine = null;
        newLine = reader.readLine();
        String[] values = newLine.split(",");
        String[] elements = values[1].split(" ");
        // if (Integer.parseInt(values[2])==1) {
        //     try {
        //         System.out.println("kill agent");
        //         controller.kill();
        //         killWriter.write("kill,"+this.name+","+System.currentTimeMillis()+"\n");
        //         killWriter.flush();
        //     } catch (StaleProxyException e) {
        //         //TODO: handle exception
        //     }
        // } else if (Integer.parseInt(values[2])==2) {
        //     try {
        //         System.out.println("start agent");
        //         start();
        //         killWriter.write("start,"+this.name+","+System.currentTimeMillis()+"\n");
        //         killWriter.flush();
        //     } catch (StaleProxyException e) {
        //         //TODO: handle exception
        //     }
        // }
        for (int i = 0; i < elements.length; i++) {
            if (Integer.parseInt(elements[i]) == 1) {
                newCongestion = true;
            }
        }
        try {
            controller.putO2AObject(newCongestion, AgentController.ASYNC);
        } catch (StaleProxyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return newCongestion;
    }

    public void handleDelay(FileWriter killWriter, LocalDateTime dateTime) throws StaleProxyException {
        if (restartDelay > 0) {
            restartDelay -= 1;
        } else {
            start();
            try {
                killWriter.write(dateTime.toString() + ",start," + getLocation()+ "\n");
                killWriter.flush();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    /**
     * @return the location
     */
    public float getLocation() {
        return location;
    }
}