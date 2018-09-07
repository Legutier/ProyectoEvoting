/*
Clase auxiliar que contiene los métodos para conectarse, mandar mensaje,
ecibir mensajes y recibir un nuevo nodo en la red

*/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.nio.*;
import java.io.*;
import java.util.*;
public class ConectionHandler{
  // se construye con el peer que lo llama
  public ConectionHandler(Peer p){
    peer = p;

  }
  public void UpdatePeer(Peer p){
    peer = p;
  }
  // método que se usa para enviar un mensaje a cierto peer, estos mensaje deben ser del formato CODIGOMENSAJE, eg 00ID, para ser entendidods por el nodo
  public String MakeAskConection(Peer p, String q){
    int timeout = 0;
    Exception ee = null;
    BufferedReader in;
    PrintWriter out;
    while(timeout < 2){
      try{
      Socket sock = new Socket(p.getAddress(), Integer.parseInt(p.getPort())); // se crea un socket con la información entregada por el peer
      sock.setSoTimeout(4000);
      sock.setReceiveBufferSize(21550);
      in = new BufferedReader(
              new InputStreamReader(sock.getInputStream()));
      out = new PrintWriter(sock.getOutputStream(), true);
      //byte[] data = q.getBytes();
      out.println(q);
      //out.close();
      String response ="";
      try{Thread.sleep(50);} // se simula el ping
      catch(Exception e){out.println(e);}
      //while(!(str=in.readLine()).equals("****END****") && str.length()!= 0) response += str;
      response = in.readLine();
      sock.close();
      return response;
      }
      catch(Exception e){
        timeout++;
        ee=e;
        System.out.println("Error al hacer una pregunta: "+e+"\n");
      }
    }
    return "ERROR";
  }

  // funcion para hacer un stream a todos los peer internos, los traker querran usar este metodo para habalrle a todos sus workers
  public void inerStream(String msg){
    System.out.println("mis iner peers: "+ peer.getPeersString());
    for(Peer p: peer.getPeers()){
      String sp = p.getPeer().toString();
      String fmsg = msg.substring(3);

      if(!sp.equals(peer.getPeer().toString()) && !sp.equals(fmsg)){
        System.out.println("Stremeando a: "+p.getPeer()+"\n");
        System.out.println(MakeAskConection(p,msg));


      }
    }
  }
  // metodo para unirse a una red local, worker querran usar este metodo para unirse a un traker
  public String JoinRequest(Peer ask_p, Peer self_p){
        return MakeAskConection(ask_p, "11"+self_p.getPeer().toString());
    }

  public void RequestHandler(Socket sck){
    Handler h = new Handler(sck, peer, this);
  }
  ////////////////////////// Metodos reemplazados en Node ///////////////
  ///////////////////////////////////////////////////////////////////////
  public void addPeerAtJoin(Peer p){
    return;
  }
  public void removePeerAtJoin(Peer p){
    return;
  }
  public void UnhandledRequest(String code, String payload, PrintWriter out){
    return;
  }
  public String GetAnID(){
    return "";
  }

  private Peer peer;
  /*
  private BufferedReader in;
  private PrintWriter out;
  */
  //private static String FILE_TO_RECEIVED = "/home/madiaz/POO/p2p/files/recived.txt";

/////////// Rutina de recepcion de mensajes //////////////

/* esta rutina procesa los mensajes para el nodo, hay ciertos codigos que son generales para todos
Por ejemplo los 00 son para pedir información del peer, su ID, su rol, etc.
El 1X para unir, pedir unirse, deslogear y pedir deslogeo de un peer.
El 2X, para cargar, pedir, votos, bloques o cadenas entera a los nodos.

*/
  private class Handler implements Runnable {
    private Socket sock;
    private Thread t;
    private Peer peer;
    private ConectionHandler inerHandler;;
    Handler(Socket skt, Peer p, ConectionHandler chand){
      this.sock = skt;
      this.peer = p;
      this.t = new Thread(this);
      this.inerHandler = chand;
      t.start();
    }

      public void run(){
        try {

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(sock.getInputStream()));
            PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
            String msg ="";
            //String str;
            //while(!(str=in.readLine()).equals("****END****")&& str.length()!= 0) msg += str;
            msg = in.readLine();
            if(msg.length()<2){
              System.out.println("Error en el mensaje: "+msg);
              return;
            }
            String msg1 = msg.substring(0,2);
            String msg2 = msg.substring(2);
            System.out.println("msg1: "+msg1+" msg2: "+msg2);
            if(msg1.equals("00")) {
              switch(msg2){
                case "PEERS" :
                  out.println(peer.getPeersString());
                  break;
                case "ID" :
                  out.println(peer.getID());
                  break;
                case "ROLE" :
                  out.println(peer.getRole());
                  break;
                case "PEER" :
                  out.println(peer.getPeer().toString());
                  break;
                default :
                  out.println("Unhandled request");
                }
            }
            else if (msg1.matches("1[0-4]")) {
              switch(msg1){
                case "10" :
                  out.println("OK");
                  System.out.println("OK");
                  break;
                case "11" :
                  tryPeer(msg2,out);
                  System.out.println("\n El peer a stremear es: "+msg2+"\n");
                  inerHandler.inerStream("12"+msg2);
                  break;
                case "12" :
                  System.out.println("adding peer: "+ msg2);
                  tryPeer(msg2,out);
                  break;
                case "13" :
                  System.out.println("Unlogin the peer: "+msg2);
                  Unlogin(msg2,out);
                  inerHandler.inerStream("14"+msg2);
                  break;
                case "14" :
                  System.out.println("Unlogin the peer: "+msg2);
                  Unlogin(msg2,out);
                  break;
              }
            }
            else if(msg1.matches("2[0-1]")){
              switch(msg1){
                case "20" :
                  out.println("DEPRECATED");
                  break;
              }
            }
            else ConectionHandler.this.UnhandledRequest(msg1,msg2,out);

            out.close();
            t.join();
          }
          catch(Exception e){
            System.out.println("Error del thread: "+ e);
          }
        }

        public void tryPeer(String request, PrintWriter out){
          Peer test = MakePeer(request);
          if (test != null){
              System.out.println(peer.getPeersString());
              out.println(peer.getPeersString()+".-"+GetAnID());
              peer.addPeer(test);
              ConectionHandler.this.addPeerAtJoin(test);

          }
          else System.out.println("Wrong format of peer");

        }

        public void Unlogin(String request, PrintWriter out){
          Peer test = StringToPeer(request);
          peer.removePeer(test);
          ConectionHandler.this.removePeerAtJoin(test);
          out.println("peer "+request+" removed");
        }

        public Peer StringToPeer(String s_peer){
          String sp = s_peer.replace("{","");
          sp = sp.replace("}","");
          HashMap<String,String> p_map= new HashMap<String,String>();
          String[] prepeer = sp.split(", ");
          for(String i: prepeer){
            p_map.put(i.split("=")[0],i.split("=")[1]);
          }
          return new Peer(p_map);
        }

        public Peer MakePeer(String s_peer){
          Peer p_candidate = StringToPeer(s_peer);
          ConectionHandler p_hand = new ConectionHandler(peer);
          if (p_hand.MakeAskConection(p_candidate,"00ID").equals(p_candidate.getID())) {
            return p_candidate;
        }
          else return null;
        }

    }
}
/*
0088YzY5ZWJkYmM1ZTBiYThiYTY0YjkzNTI5ZjhiNDVhYjg3YWEwYjQyY2JkMWM2OTUxOTA0ODNhZDc2ZTYwN2JhZg==0172h3w2Vp7GijmPZ+z8YbxK6JesUltd54VA0YuLu0L8T8V2OoZS/xA1Vk0mjQhxlBW8cFgGjDQJU5a0ZJEnXZhjrOpPJMwoUOz1WRIYtdQ7f7lj4vjOsrSPKwGYSKvtn3lal4GAgjQbiBt/Xsg+V02/fIMnamA6wMD9gW6pxco1PfA=0216MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCvbV3nRhuCV74w4vFb92ZBf628GZcsz6z93p8w/fZVmVYHhiH5rqqH664OrEfZY5To6p41elOtZGLNLtfQWPY96PnlWKd0q23Z1DtPBJifejvgHeN/GwTJzLeWBY3Bq71AuZ6EzhDzb9+m+ZkEJ0ldixBMDydz0lLeEKpQDSRrrwIDAQAB00020088MzFhYzM1ZmUxY2JiNjU2N2I2ZWIxMGY3ZTY0NGQzMzQ3OTgzMTc1YzU2MGJiYWRjNTI5MmRiZjE0ZGIzMmRkNw==0172e4M++aC5N6sRi56LrCIdHAQsrzHmWsR4hVoX6iff+zzRGy6ASTKcUbT8B4lT51bfQlswVAtHDXrSzMBJcZxk+0XIph0XgLaef2N6GtMXn2/ncQbG9bhBQGvabYEcXDaMjDDFoJKH9qOt0iA5Zi8E9kFTfHi+7n8C9nrmWmcDdao=0008QkJCQg==0088MDhkZDE5ZWJlMzMyYWViNjdiY2EzMDU4OWU1NGU2MjcyMzI5NDljNzM3M2QzZTM1M2Y5NGMyMWY2OWE5ZDljNQ==0172DRVV9FbJRpWmAfhd56yO8jzq+/GSyqsiRvf/VDxlEV6x8OMnHcIzgk24sXHcbXfUM+JvXS3IDejiRpEdG3KkDbbTaXLNCI9XHNNC7Re7g8c+XI7M1IoWkjgg52t/ReYGPcFg0V4rFgq0MCroyR3osfqe/cdGB92+ELWILbkapAs=0008T09PTw==


*/
