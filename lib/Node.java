/*
Metodo que describe los nodos de la red p2p,
tiene 2 rutinas principales, uan que escucha los mensajes y otra que los publica
los mensaje tienen el formato CODIGOMENSAJE, eg 00ID, de tal forma de distribuirlos mejor según el codigo

*/
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
public class Node extends Peer {

  private ConectionHandler p_handler;
  private Thread plistener;
  protected Vote voter = new Vote();
  protected Block blk = new Block();
  protected ArrayList<HashMap<String,Object>>  InputCache = new ArrayList<HashMap<String,Object>>();
  public Node(Peer p){
    super(p.getPeer());
    p_handler = new InnerNodeHandler(this); //objeto auxiliar que se ocupa para enviar un recibir mensjaes mediante la apertura de sockets

  }

  public Node(String id, String addr, String port, String role){
    this(new Peer(id, addr, port, role));
  }
  public Node(HashMap<String, String> hashmp){
    this(new Peer(hashmp));
  }
  // rutina que pone al nodo a escuchar mensajes en el puerto del peer
  public void Start(){
    try{
    ServerSocket listener = new ServerSocket(Integer.parseInt(this.getPort()),250);
    plistener = new Thread(){
      public void run(){
        System.out.println("Escuchando");
        try{
          while(true){
          p_handler.RequestHandler(listener.accept());

        }
        }
        catch(Exception e){
          System.out.println("Error en thread main: "+e);
        }
      }
    };

  }
  catch(Exception e){
    System.out.println("Error en el thread: "+e);
  }
    plistener.start();

  }

 //para la rutina que escucha mensajes
  public void Stop(){
    if (plistener.isAlive()) plistener.suspend();

  }
  // ocupa a conection handler para enviar un mensaje al peer solicitado
  public String SendMsg( Peer to_p,String msg){
    return p_handler.MakeAskConection(to_p, msg);
  }

  // publica un mensaje a todos los peers enlistados
  public void StreamMsg(String msg){
    ArrayList<Peer>maPeers = this.getPeers();
    for (Peer p : maPeers ){
      if(!p.getPeer().toString().equals(this.getPeer().toString()))
        System.out.println("msg: "+SendMsg(p, msg));
    }
  }
  // metodo que se ocupa para unirse a una red
  public boolean AskToJoin(Peer p){
    Peer old = new Peer(getPeer());
    String[] response = p_handler.JoinRequest(p, this).split(".-");
    if(response[0].equals("ERROR")) return false;
    addPeers(response[0]);
    setID(response[1]);
    removePeer(old);
    addPeer(this);
    return true;

  }
  // se ocupa para modificar el id propio con uno único en la red
  public String GetAvailableID(){
    PeersIDUpdate();
    ArrayList<String> ids = getIDs();
    ArrayList<Integer> aux = new ArrayList<Integer>();
    Collections.sort(ids);
    int nexid = 1;
    for(String i : ids){
      if(nexid != Integer.parseInt(i)) return nexid+"";
      nexid++;
    }
    return (ids.size()+1)+"";

  }
  public void Logout(Peer p){
    System.out.println(p_handler.MakeAskConection(p,"13"+getPeer().toString()));
  }

 // este metodo es sobreescrito dependiendo del rol del nodo, y corresponde a la rutina que entiende los codigos enviados en los mensajes
  public void URequest(String code, String payload, PrintWriter out){
    return;
  }

  private class InnerNodeHandler extends ConectionHandler{

    public InnerNodeHandler(Peer p){
      super(p);
    }
    @Override
    public void addPeerAtJoin(Peer p){
      Node.this.addPeer(p);
    }
    @Override
    public void removePeerAtJoin(Peer p){
      Node.this.removePeer(p);
    }
    @Override
    public void UnhandledRequest(String code, String payload, PrintWriter out){
      Node.this.URequest(code,payload,out);
    }
    @Override
    public String GetAnID(){
      return Node.this.GetAvailableID();
    }


  }
}
