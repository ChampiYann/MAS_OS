import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.WeibullDistribution;

import agents.osAgent;
import behaviour.InputHandlerBehaviour;
import environment.Outstation;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Launch {

    // public static final long minute = 400;
    static volatile AgentController central = null;
    // static volatile LocalTime time = LocalTime.of(1, 0, 0);
    // static volatile LocalDate date = LocalDate.of(2018, 3, 1);
    static volatile LocalDateTime dateTime = LocalDateTime.of(2018, 3, 1, 1, 0);

    public static void main(String[] args) {
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Create a default profile
        Profile p = new ProfileImpl();
        p.setParameter(Profile.SERVICES,
                "jade.core.messaging.TopicManagementService;jade.core.event.NotificationService");
        // Create a new non-main container, connecting to the default
        // main container (i.e. on this host, port 1099)
        ContainerController cc = rt.createMainContainer(p);

        Object centralArgs[] = new Object[1];
        centralArgs[0] = dateTime;

        try {
            // Initiate RMA (gui)
            AgentController rma = cc.createNewAgent("rma", "jade.tools.rma.rma", null);
            // Fire up GUI
            rma.start();
            // Initiate central
            central = cc.createNewAgent("central", "agents.centralAgent", centralArgs);
            // Fire up central
            central.start();
        } catch (StaleProxyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
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

        // Create new osAgents
        int numOS = configurations.length;
        Vector<Outstation> outstations = new Vector<Outstation>(numOS);

        List<Integer> range = IntStream.rangeClosed(0, numOS-1)
            .boxed().collect(Collectors.toList());
        Collections.shuffle(range);

        for (int i = 0; i < numOS; i++) {
            name = "agent" + String.format("%02d", i+1);
            // Concatenate arguments
            Object agentArgs[] = new Object[3];
            agentArgs[0] = lanes;
            agentArgs[1] = configurations[numOS-1-i];
            try {
                // Initiae osAgent
                try {
                    outstations.add(new Outstation(name, agentArgs, cc));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        for (int i = 0; i < numOS; i++) {
            // Fire up the agent
            try {
                outstations.get(range.get(i)).start();
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // AgentController sniff;

        // try {
        //     sniff = cc.createNewAgent("sniff", "jade.tools.sniffer.Sniffer", null);
        //     sniff.start(); 
        // } catch (StaleProxyException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }

        FileWriter killWriter;
        try {
            dateTime.minusMinutes(1);

            killWriter = new FileWriter("kill_log.txt");

            BufferedReader msiReplay;
            FileReader reader = new FileReader("BEELD1803.csv");
            msiReplay = new BufferedReader(reader);

            WeibullDistribution distribution = new WeibullDistribution(0.4360,792.0608);
            Random rand = new Random();
            Timer timer = new Timer();
            TimerTask task = new TimerTask(){
            
                @Override
                public void run() {

                    Vector<Object> packet2 = new Vector<Object>();
                    packet2.add(dateTime);
                    packet2.add(InputHandlerBehaviour.LOG);
                    try {
                        central.putO2AObject(packet2, AgentController.ASYNC);
                    } catch (StaleProxyException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    dateTime = dateTime.plusMinutes(1);

                    boolean done = false;
                    while(!done) {
                        try {
                            String line = null;
                            line = msiReplay.readLine().replaceAll(",", ".");
                            String[] values = line.split(";");
                            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d-M-yyyy");
                            LocalDate lineDate = LocalDate.parse(values[0],dateFormatter);
                            LocalTime lineTime = LocalTime.parse(values[1]);
                            LocalDateTime lineDateTime = lineDate.atTime(lineTime);
                            if (lineDateTime.compareTo(dateTime.plusMinutes(1)) > -1) {
                                msiReplay.reset();
                                done = true;
                            } else {
                                msiReplay.mark(1000);
                                Vector<Object> packet = new Vector<Object>();
                                packet.add(dateTime);
                                packet.add(InputHandlerBehaviour.MSI);
                                packet.add(Float.parseFloat(values[5]));
                                packet.add(Arrays.copyOfRange(values,6,8+1));
                                central.putO2AObject(packet, AgentController.ASYNC);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            done = true;
                        } catch (StaleProxyException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    
                    Iterator<Outstation> outstationIterator = outstations.iterator();
                    while (outstationIterator.hasNext()) {
                        Outstation nextOutstation = outstationIterator.next();
                        // try {
                        //     nextOutstation.handleDelay(killWriter, dateTime);
                        // } catch (StaleProxyException e) {
                        //     // TODO Auto-generated catch block
                        //     e.printStackTrace();
                        // }
                        try {
                            boolean congestion = nextOutstation.sendCongestion(killWriter);
                            Vector<Object> packet = new Vector<Object>();
                            packet.add(dateTime);
                            packet.add(InputHandlerBehaviour.CONGESTION);
                            packet.add(nextOutstation.getLocation());
                            packet.add(congestion);
                            central.putO2AObject(packet, AgentController.ASYNC);
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            // e1.printStackTrace();
                        } catch (StaleProxyException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        // if (rand.nextInt(16117524) < 169) {
                        //     long restartDelaySeconds = Math.round(distribution.sample()+1);
                        //     // System.out.println(Math.round(distribution.sample()/60)+1);
                        //     long restartDelayMinutes = restartDelaySeconds/60;
                        //     try {
                        //         nextOutstation.kill(restartDelayMinutes);
                        //         try {
                        //             killWriter.write(dateTime.toString() + ",kill," + nextOutstation.getLocation()+ "\n");
                        //             killWriter.flush();
                        //         } catch (IOException e) {
                        //             // TODO Auto-generated catch block
                        //             e.printStackTrace();
                        //         }
                        //     } catch (StaleProxyException e) {
                        //         // TODO Auto-generated catch block
                        //         e.printStackTrace();
                        //     }
                        // }
                    }

                    // Vector<Object> packet = new Vector<Object>();
                    // packet.add(dateTime);
                    // packet.add(InputHandlerBehaviour.LOG);
                    // try {
                    //     central.putO2AObject(packet, AgentController.ASYNC);
                    // } catch (StaleProxyException e) {
                    //     // TODO Auto-generated catch block
                    //     e.printStackTrace();
                    // }

                    // dateTime = dateTime.plusMinutes(1);

                    if (dateTime.isAfter(LocalDateTime.of(2018, 4, 1, 1, 0))) {
                        outstations.stream().forEach(n -> {
                            try {
                                n.kill(0);
                            } catch (StaleProxyException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        });
                        try {
                            central.kill();
                        } catch (StaleProxyException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                        this.cancel();
                        // try {
                        //     cc.kill();
                        // } catch (StaleProxyException e) {
                        //     // TODO Auto-generated catch block
                        //     e.printStackTrace();
                        // }
                    }
                }
            };

            // long simStartTime = (long) (Math.ceil(System.currentTimeMillis() / 10000.0) * 10000 + 10000);
            // long delay = simStartTime - System.currentTimeMillis();
            long delay = 5000;
            timer.scheduleAtFixedRate(task, delay, osAgent.minute);

        } catch (IOException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
    }
}