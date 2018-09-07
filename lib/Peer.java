/*
Clase que describe los peer, solo es administrativa y tiene sus id, address, puerto y rol
ademas de una lista de peers vecinos
*/

import java.util.*;
public class Peer{
  public Peer(String id, String addr, String port, String role){
    peer.put("peerID",id);
    peer.put("address",addr);
    peer.put("port",port);
    peer.put("role",role);
    addPeer(this);

  }
  public Peer(String id){
    this(id, "127.0.0.1","9002","miner");

  }
  public Peer( HashMap<String,String> p){
    peer = p;
    addPeer(this);
  }
  public synchronized void PeersIDUpdate(){
    peersid = new ArrayList<String>();
    for(Peer p : peers){
      peersid.add(p.getID());
    }
  }
  public  void addPeers(String s_peers){
    System.out.println(s_peers);
    ArrayList<Peer> peer_aux =this.toPeersList(s_peers);
    for (Peer i : peer_aux){
      System.out.println("adding: "+i.getPeer().toString());
      if(!this.getPeersString().contains(i.getPeer().toString())) peers.add(i);
    }
  }
  public  void removePeer(Peer p){
    String sp = p.getPeer().toString();
    Peer toremove = null;
    for (Peer pepe : this.peers){
      if (pepe.getPeer().toString().equals(sp)) toremove = pepe;
    }
    peers.remove(toremove);

  }
  public  void addRawPeers(ArrayList<Peer> rawps){
    for (Peer i : rawps){
      System.out.println("adding raw: "+i.getPeer().toString());
      if(!this.getPeersString().contains(i.getPeer().toString())) peers.add(i);
    }
  }
  public  void addPeer(Peer p){
    if(!getPeersString().contains(p.getPeer().toString())) peers.add(p);
  }
  public  HashMap<String,String> getPeer(){
    return peer;
  }
  public  String getID(){
    return peer.get("peerID");
  }
  public ArrayList<String> getIDs(){
    return peersid;
  }
  public  String getAddress(){
    return peer.get("address");
  }
  public  String getPort(){
    return peer.get("port");
  }
  public  String getRole(){
    return peer.get("role");
  }
  public  ArrayList<Peer> getPeers(){
    return peers;
  }
  public  String getPeersString(){
    ArrayList<String> s_peers = new ArrayList<String>();
    for(Peer p: peers){
      s_peers.add(p.getPeer().toString());
    }
    return s_peers.toString();
  }
  public void setID(String id){
    peer.put("peerID",id);
  }

  public  void setAddress(String adr){

    peer.put("address",adr);
  }
  public  void setPort(String prt){
    peer.put("port",prt);
  }
  public  void role(String rol){
    peer.put("role",rol);
  }
  public  ArrayList<Peer> toPeersList(String s_peers){
    ArrayList<Peer> array_h2 = new ArrayList<Peer>();
    HashMap<String,String> aux = new HashMap<String,String>();
    String var = s_peers;
    var = var.replace("[","");
    var = var.replace("]","");
    String[] var2 = var.split("}");
    String[] var3 = new String[]{};
    String[] var4 = new String[]{};
    for(String k : var2){
      k = k.replace(", {", "");
      k = k.replace("{","");
      var3 = k.split(", ");
      for(String k2: var3){
        var4 = k2.split("=");
        aux.put(var4[0],var4[1]);
      }
      array_h2.add(new Peer(aux));
      aux = new HashMap<String,String>();

    }
    return array_h2;
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
private HashMap<String,String> peer = new HashMap<String,String>();
private ArrayList<Peer> peers = new ArrayList<Peer>();
private ArrayList<String> peersid = new ArrayList<String>();
}
