/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author HP G4
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//cmd line arg[0] should be port number for this machine, the rest would be port numbers of other machines (4 in total)
public class ChatProgram implements Runnable{
    static DatagramSocket socket = null;
    static String getMessage = null;
    static String sendMessage = null;
    static byte[] getData = new byte[2048];
    static byte[] sendData = new byte[2048];
    static BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));    
    static ArrayList<Integer> portList = null;
    static ArrayList<String> nameList = null;
    static ArrayList<InetAddress> IPList = null;    
    static ArrayList<InetAddress> blockedIP = null;
    static Scanner fileInput = null;
    static DatagramPacket getPacket = null;
    static DatagramPacket sendPacket = null;
    
    public static void main(String args[])
    {
        //try to open file to get peer's names and IP addresses
        try {
            fileInput = new Scanner(new File("client.txt"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //initialize lists to hold info
        nameList = new ArrayList<String>();
        IPList = new ArrayList<InetAddress>();
        portList = new ArrayList<Integer>();        
        
        //read data in file into memory
        while(fileInput.hasNext()){
            //read peer name
            nameList.add(fileInput.next()); 
            //read peer IP address            
            try {
                IPList.add(InetAddress.getByName(fileInput.next()));
            } catch (UnknownHostException ex) {
                Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
            }                                    
        }        
        
        //if command line args do not match number of peers, terminate
        if((args.length - 1) != IPList.size()){
            System.out.println("CLA count do not match peer count.Terminated");
            System.exit(1);
        }
        
        try {
            //get port number for this machine
            socket = new DatagramSocket(Integer.parseInt(args[0]));
        } catch (SocketException ex) {
            Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //get port numbers of peer machines
        for(int i = 1; i < args.length; ++i){
            portList.add(Integer.parseInt(args[i]));
        }       
        
        //display list of peers
        for(int i = 0; i < IPList.size(); ++i){
            int peerNum = i + 1;
            System.out.println("Peer " + peerNum + ", " + nameList.get(i) + "<" + IPList.get(i) + ">");
        }
 
        
        //now that all the required information are gathered, start chat
        //start a thread to get incoming messages
        new Thread(new ChatProgram()).start();
        
        //the main thread will be responsible of sending out messages
        while(true){
            //get input from user
            try {
                getMessage = inReader.readLine();
            } catch (IOException ex) {
                Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
            }
            //clear the data buffer
            sendData = null;
            //convert the string to bytes
            sendData = getMessage.getBytes();
            
            //loop will send the same message to every peer, starting with peer one
            for(int i = 0; i < nameList.size(); ++i){
                //wrap the data in a packet
                sendPacket = new DatagramPacket(sendData, sendData.length, IPList.get(i), portList.get(i));
                try {
                    //send the packet out to the ith peer
                    socket.send(sendPacket);
                } catch (IOException ex) {
                    Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
                     
    }

    //thread that handles incoming messages
    @Override
    public void run() {
        while(true){
            getData = null;
            getData = new byte[2048];            
            getPacket = new DatagramPacket(getData, getData.length);
            
            
            try {
                socket.receive(getPacket);
            } catch (IOException ex) {
                Logger.getLogger(ChatProgram.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //check if received packet is from authorized IP
            if(checkIP(getPacket) == true){
                System.out.println(getMessage);
            }
            else{
                for(int i = 0; i < blockedIP.size(); ++i){
                    if(getPacket.getAddress().equals(blockedIP.get(i)) == false){
                        System.out.println("Unauthorized chat request from <" + getPacket.getAddress() + ">");
                        blockedIP.add(getPacket.getAddress());
                    }
                }
            }
       }
       
}
    
    public boolean checkIP(DatagramPacket getPacket)
    {
         for(int i = 0; i < IPList.size(); ++i){                     
                if(getPacket.getAddress().equals(IPList.get(i))){                        
                    getMessage = nameList.get(i) + "<" + getPacket.getAddress() + "> " + new String(getPacket.getData()).trim();  
                    return true;
                }                
         }
        return false;
    }                  
    
}
