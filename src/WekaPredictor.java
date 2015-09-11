import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.*;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.SerializationHelper;


/**
 * A socket server on top of Weka.jar enabling efficient 
 * classification of test data from any programming language 
 * other than Java.
 * 
 * Socket server copyrights goes to: 
 * http://cs.lmu.edu/~ray/notes/javanetexamples/
 */
public class WekaPredictor {

    /**
     * Application method to run the server runs in an infinite loop
     * listening on port 9898.  When a connection is requested, it
     * spawns a new thread to do the servicing and immediately returns
     * to listening.  The server keeps a unique client number for each
     * client that connects just to show interesting logging
     * messages.  It is certainly not necessary to do this.
     */
    public static void main(String[] args) throws Exception {
    	
    	int port = 9100;
    	if (args.length > 0) {
    	    try {
    	    	port = Integer.parseInt(args[0]);
    	    	System.out.println("Using port: " + Integer.toString(port));
    	    } catch (NumberFormatException e) {
    	        System.err.println("Port " + args[0] + " must be an integer.");
    	        System.exit(1);
    	    }
    	} else {
    		System.err.println("Using default port: " + Integer.toString(port));
    	}
    	
        System.out.println("Weka server is running ...");
        int clientNumber = 0;
        ServerSocket listener = new ServerSocket(port);
        try {
            while (true) {
                new WekaConnector(listener.accept(), clientNumber++).start();
            }
        } finally {
            listener.close();
        }
    }
    


    /**
     * A private thread to handle requests on a particular
     * socket.  The client terminates the dialogue by sending a single line
     * containing only a period.
     */
    private static class WekaConnector extends Thread {
        private Socket socket;
        private int clientNumber;

        public WekaConnector(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            log("New connection with client# " + clientNumber + " at " + socket);
        }
        
        // Weka prediction results
        public class WekaResult{
        	public double index;
        	public double dist[];
        	public String label;
        	
        	public double getIndex(){
        		return index;
        	}
        	public double[] getDist(){
        		return dist;
        	}   
        	public String getLabel(){
        		return label;
        	}    
        }
        
        // Write error message to the socket
        public void replyError(String message){
            PrintWriter out;
			try {
				System.out.println(message);
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println("JAVA ERROR: " + message);
			} catch (IOException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
        }

        /**
         * Services this thread's client by first sending the
         * client a welcome message then repeatedly reading strings
         * and sending back the capitalized version of the string.
         */
        public void run() {
            try {

                // Decorate the streams so we can send characters
                // and not just bytes.  Ensure output is flushed
                // after every newline.
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                
                // First message should contain the pre-trained model file path
                String modelFilePath = in.readLine();
                if (modelFilePath == null || modelFilePath.equals(".")) {
                	throw new FileNotFoundException(".model file path not specified");
                }
                //load model
                Classifier cls = (Classifier) SerializationHelper.read(modelFilePath);
                
                // Send a welcome message to the client.
                out.println("Connected to Weka Server as client#" + clientNumber + ".");

                // Get messages from the client, line by line; return them
                // capitalized
                while (true) {
                    String input = in.readLine();
                    if (input == null || input.equals(".")) {
                        break;
                    }
                    //log(input);
                    //out.println(input.toUpperCase());
                                        
                    Instance inst= new DenseInstance(14);
                    JSONObject obj = new JSONObject(input);
                    JSONArray attr = obj.getJSONArray("attributes");
                    for (int i = 0; i < attr.length(); i++)
                    {
                        inst.setValue(i, attr.getDouble(i));
                    }

                    WekaResult wekaResult = new WekaResult();
                    // perform the prediction
                    wekaResult.index =cls.classifyInstance(inst);
                    // get the distributions
                    wekaResult.dist = cls.distributionForInstance(inst);
                    //get the name of the class index
                    wekaResult.label = inst.classAttribute().value((int)wekaResult.index);
                    
                    //System.out.println("Prediction: " + wekaResult.label); 
                    
                    // Marshal to json
                    JSONObject jsonObject = new JSONObject(wekaResult);
                    String json  =jsonObject.toString();
                    
                    // Send the reply
                    out.println(json);
                }
            } catch (FileNotFoundException e) {
            	replyError(e.getMessage());
            } catch (IOException e) {
                log("Error handling client# " + clientNumber + ": " + e);
            } catch (Exception e) {
            	log(e.getMessage());
				e.printStackTrace();
			} finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log("Couldn't close a socket, what's going on?");
                }
                log("Connection with client# " + clientNumber + " closed");
            }
        }

        /**
         * Logs a simple message.  In this case we just write the
         * message to the server applications standard output.
         */
        private void log(String message) {
            System.out.println(message);
        }
    }
}