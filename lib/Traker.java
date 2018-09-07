/*
Tracker, es la clase que describe al nodo que genera el anillo interno  en la red, son quienes conectan a los worker a la red
y permiten la publicación de votos, bloques y cadenas en la red. Todos los trakers se conocen y se auditan.

Los Trakers hace votaciones entre si para elegir el traker responsable de entregar el próximo bloque. Una vez que un
traker es elegido este elige uno de sus workers para armar el bloque, el worker a su vez lo arma y lo entrega al traker
electo y este lo publica a toda la red para luego reiniciar las votaciones para elegir el próximo traker responsable
*/

import java.util.*;
import java.net.ServerSocket;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.*;
 //import java.text.*;
public class Traker extends Node{
  Block Blocky = new Block(); //objeto que tiene el bloque interno del tracker
  ArrayList<Traker> trakers = new ArrayList<Traker>(); // lista de trakers en la red
  ArrayList<String> trakersid = new ArrayList<String>(); // lista de las id de los trakers vecinos
  Thread heartbeat; // rutina para auditar a lso trakers
  Thread waitblock;
  HashMap<String,Integer> list = new HashMap<String,Integer>();
  int listsize = 0;
  int wearevoting = 0;
  boolean notenteryet = true;
  HashMap<String,Integer> votes = new HashMap<String,Integer>();
  int winner=0;
  int consensus = 0;
  public Traker(Peer p){
    super(p);
  }
  // inicia la rutina que recibe mensajes del nodo, el heartbeat a los workers y a los trakers
  public void StartTraker(){
    addTraker(this);
    heartbeat = new Thread(){
      public void run(){
        while(true){
          HeartBeatRutine();
          try {

            Thread.sleep(10000);
          }
          catch(Exception e){
            System.out.println("Error while waiting:"+ e);
          }
        }
      }
    };
    waitblock = new Thread(){
      public void run(){
        try {
          Thread.sleep(15000);
        }
        catch(Exception e){
          System.out.println("Error while waiting:"+ e);
        }
        System.out.println(SendMsg(new Peer(getPeer()),"3bVOID"));
      }
    };
    this.Start();
    heartbeat.start();

  }
  public Block getBlock(){
    return Blocky;
  }
  public void setBlock(Block bk){
    System.out.println("\n\n"+bk.getChain().toString());
    Blocky = bk;
  }
  // metodo que pregunta por el bloque actual en la red
  public void AskForChain(Peer p){
    String nblock = SendMsg(p,"3eVOID");
    System.out.println("\n"+nblock+"\n");
    ArrayList<HashMap<String,Object>> nchain = Blocky.DecodeChain(nblock);
    setBlock(new Block(nchain));
    //System.out.println(Blocky.getChain().toString());
  }
  // retorna una lista de los peers en formato string de los trakers
  public  ArrayList<String> TrakersToString(ArrayList<Traker> trks){
    ArrayList<String> aux = new ArrayList<String>();
    for (Traker a : trks){
      aux.add(a.getPeer().toString());
    }
    return aux;
  }
  // saca un traker de la lista de trakers
  public void removeTraker(Traker t){
    System.out.println("removing :)");
    String removing = t.getPeer().toString();
    Traker rip = null;
    for(Traker tata : trakers )
      if(tata.getPeer().toString().equals(removing)){
        rip = tata;
        break;
    }
    trakers.remove(rip);
  }
  // agrega un traker a la lista si no existe
  public  void addTraker(Traker t){
    if(!TrakersToString(trakers).contains(t.getPeer().toString()))trakers.add(t);
    System.out.println("total trakers: "+TrakersToString(trakers));
  }
  // heartbeat que diferencia entre traker o worker
  public synchronized  void HeartBeatRutine(){
    ArrayList<Peer> pirs = new ArrayList<Peer>(getPeers());
    ArrayList<Traker> auxlist = new ArrayList<Traker>();
    auxlist.addAll(trakers);
    if(pirs.size() > 1){
      for(Peer p : pirs){
        HeartBeat(p,"00ID");
      }
    }
    else System.out.println("No peers founded");
    if(trakers.size() > 1){
      for(Peer t : auxlist){
        if(!t.getPeer().toString().equals(this.getPeer().toString()))
            HeartBeat(t,"37"+getID()+"._"+getPeers().size());
      }
    }
    else System.out.println("No trakers founded");
  }
  // metodo que describe la rutina heartbeat y pregunta por workers y trakers
  public void HeartBeat(Peer p, String msg){
    int i = 0;
    boolean wait = true;
    String p_string = p.getPeer().toString();
    while (wait) {
      try {
        String response = SendMsg(p,msg);
        System.out.println(p.getID()+": " +response);
        if(response.equals("ERROR")) throw new Exception();
        wait=false;
      }
      catch(Exception e){
        System.out.println("Unable to comunicate with Peer"+p_string+"\n"+e);
        if(i < 2) i++;
        else{
          if(!p.getRole().equals("tracker"))SendMsg(this,"13"+p_string);
          else {
            System.out.println(SendMsg(this,"35"+p_string));

            }
          wait = false;
          }
      }
      try {
        Thread.sleep(2000);
      }
      catch(Exception e){
        System.out.println("Error while waiting:"+ e);
      }
      }
  }
  // pide agregarse a una red de trakers o al traker genesis
  public void JoinToNet(Traker t){
    String[] response = SendMsg(t,"36"+this.getPeer().toString()).split("._");
    System.out.println(SendMsg(t,"3e"+this.getPeer().toString()));
    AskForChain(t);

    removeTraker(this);
    for(Traker tt: toTrakerList(response[0]))
        addTraker(tt);
    setID(response[1]);

  }

  public void sizeUpdate(){
    listsize= 0;
    for( int i : list.values()) listsize += i;
  }

  public  boolean TrackerSearch(String msg){
    for(Traker t: trakers){
      if(new String(SendMsg(t,msg)).equals("OK")) return true;
    }
    return false;
  }
  // este metodo genera el voto de traker para el próximo traker responsable y lo publica
  public void makeMyVote(){
    trakersIDUpdate();
    Random votex = new Random();
    int vote = votex.nextInt(trakersid.size());

    String  msg = "38"+getID()+"._"+trakersid.get(vote);
    votes.put(getID()+"",Integer.parseInt(trakersid.get(vote)));
    try{Thread.sleep(2000);}
    catch(Exception e){System.out.println(e);}
    TrackerStream(msg);

  }
  // actualzia la lista de id de los trakers
  public synchronized void trakersIDUpdate(){
    trakersid = new ArrayList<String>();
    for(Traker t : trakers){
      trakersid.add(t.getID());
    }
  }
  // publica un mensaje a todos los trakers incluyendose a si mismo
  public void AllTrackerStream(String msg){
    System.out.println("cantidad de trakers: "+trakers.size());
    for(Traker t: trakers){
        System.out.println("from trakerstream: "+SendMsg(t,msg));
    }
  }
  // publica un mensaje a todos los trakers excepto a si mismo
  public  void TrackerStream(String msg){
    for(Traker t: trakers){
      if(!t.getPeer().toString().equals(this.getPeer().toString()))
        System.out.println(SendMsg(t,msg));
    }
  }
  // metodo que recibe al ganador, si es el mismo la habla a un worker random para armar el bloque y luego lo publica
  // si no es el mismo espera a que el ganador publique el bloque, tiene un timeout.
  public void letsChoise(){
    System.out.println("the winner in choise is: "+winner+"\n");
    Random votex = new Random();
    String nexblock;
    long startTime = System.currentTimeMillis();
    int i = 0;
    if(getID().equals(""+winner) && getPeers().size() > 1){
      System.out.println("I'm the winner :)");
      int chs = votex.nextInt(getPeers().size());
      if(chs == 0)chs++;
      nexblock = SendMsg(getPeers().get(chs), "44VOID");
      try{Thread.sleep(3000);}
      catch(Exception e){System.out.println(e);}
      System.out.println(SendMsg(this,"3f"+Blocky.completeZero(getID(),4)+nexblock));
      try{Thread.sleep(6000);}
      catch(Exception e){System.out.println(e);}
      AllTrackerStream("3bYOLOENVIO"+getID());
    }

    else{ System.out.println("I'm not the winner");
      waitblock.run();
      //System.out.println("The Traker run out of time!! ");
    }
    //AskForChain(getPeers().get(1));
  }
  // metodo para adquirir y modificar si propia ID por una disponible en la red
  public String GetAnID(){
    ArrayList<Integer> aux = new ArrayList<Integer>();
    trakersIDUpdate();
    System.out.println("trakers id: "+trakersid);
    for (String t : trakersid){
      aux.add(Integer.parseInt(t));
    }
    Collections.sort(aux);
    int u = 100;
    for (int i : aux){
      if(u != i){

        return u+"";
        }
      u++;
  }

  return (aux.size()+100)+"";
}
  public ArrayList<String> getTrakersString(){
    ArrayList<String> aux = new ArrayList<String>();
    for(Traker tt : trakers){
      aux.add(tt.getPeer().toString());
    }
    return aux;
  }
  public  ArrayList<Traker> toTrakerList(String s_peers){
    ArrayList<Traker> array_h2 = new ArrayList<Traker>();
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
      array_h2.add(new Traker(new Peer(aux)));
      aux = new HashMap<String,String>();

    }
    return array_h2;
  }

  public int HowManyTrakers(){
    return trakers.size();
  }
  /* rutina que procesa los mensajes especiales del traker
  el codigo 3X le pertenece al traker
  el 30 y 31 fueron deprecados
  el 32  agrega un traker a la  lista de trakers local
  el 33 y 34 mandan mensajes a un worker en especifico, estos codigos no se usan ya que no hay un uso practico para enviarle un mensaje a un worker en especifico
  el 35 deslogea un traker
  el 36 y 37 junto con el 32 se usan para agregar un traker a la red
  el 38 al 3b se usa para el sistema de votaciones expuesto en la documentación
  los codigos restantes son para recibir y publicar votos, bloques y cadenas
  */
  @Override
  public void URequest(String code, String payload, PrintWriter out){
    String[] inermsg;
    String[] minermsg;

    if(code.matches("3[0-9a-z]")){
       switch(code){

        case "32" :
          Traker candidate = new Traker(StringToPeer(payload));
          String role = this.SendMsg(candidate , "00ROLE");
          if(role.equals("tracker")) addTraker(candidate) ;
          out.println("traker: "+candidate.getPeer().toString()+" added");
          break;
        case "33" :
          if(TrackerSearch("34"+payload))
              out.println("OK");
          else out.println("ERROR");
          break;
        case "34" :
          inermsg = payload.split("._");
          for(Peer p : this.getPeers()){
            if(inermsg[0].equals(p.getPeer().toString())){
              minermsg=inermsg[1].split("*-");
              if(!SendMsg(p,minermsg[0]+""+minermsg[1]).equals("ERROR")) out.println("OK");
              else out.println("ERROR");
            }
            else out.println("ERROR");
          }
          break;
        case "35" :
          System.out.println("removing :)");
          Peer pepe = StringToPeer(payload);
          Traker victim = new Traker(pepe);
          list.remove(victim.getID());
          removeTraker(victim);
          sizeUpdate();
          out.println("remove: "+victim.getPeer().toString());
          break;
        case "36" :
          Traker newtraker = new Traker(StringToPeer(payload));

          String newid = GetAnID();
          newtraker.setID(newid);
          String nt_string = newtraker.getPeer().toString();
          System.out.println("new peer: "+nt_string);
          System.out.println(SendMsg(this,"32"+nt_string));
          TrackerStream("32"+nt_string);
          out.println(getTrakersString()+"._"+newid);
          break;
        case "37" :
          inermsg = payload.split("._");
          list.put(inermsg[0],Integer.parseInt(inermsg[1]));
          sizeUpdate();
          out.println("added");
          break;
        case "38" :
          try{Thread.sleep(200);}
          catch(Exception e){out.println(e);}

          inermsg = payload.split("._");
          trakersIDUpdate();
          System.out.println(votes.size()+" trakers08: "+ (trakers.size()*0.8));
          out.println("vote recived");
          votes.put(inermsg[0],Integer.parseInt(inermsg[1]));
          if(votes.size() > (trakers.size()*0.5)){
            AllTrackerStream("39"+votes.toString());
          }/*
          if(trakersid.contains(inermsg[0])){
          }
          else out.println("you're not a tracker: "+inermsg[0]+" "+trakersid.toString()); */
          break;
        case "39" :
        try{Thread.sleep(1000);}
        catch(Exception e){out.println(e);}
          String[] aux;
          int sum = 0;
          if(payload.equals(votes.toString()) && notenteryet){
              notenteryet=false;
              consensus++;
              System.out.println("Consensus: "+ consensus+ "and trakers: "+ (trakers.size()*0.5));
              if(consensus >= (trakers.size()*0.5)){
                out.println("election!, consensus: "+consensus);
                aux = payload.toString().replaceAll("([^0-9,])","").split(",");
                for(String i : aux)
                  sum+=Integer.parseInt(i);
                sum = (sum % votes.size())+100;
                trakersIDUpdate();
                while(!trakersid.contains(sum+""))sum++;
                winner =sum;
                consensus = 0;
                System.out.println("the winner: "+winner);
                letsChoise();

                //AllTrackerStream("3a.-"+winner);
              }
              else out.println("not consensus yet: \n");

          }
          else out.println("Something fishy: "+payload+" votes: "+votes.toString());
          break;
        case "3a" :
          try{Thread.sleep(200);}
          catch(Exception e){out.println(e);}
          out.println("tha winner: "+winner);
          if(payload.equals(winner+"") && wearevoting != 0){
            wearevoting = 0;
            letsChoise();
            }
          break;
        case "3b" :
          try{Thread.sleep(500);}
          catch(Exception e){out.println(e);}
          votes= new HashMap<String,Integer>();
          out.println("lets vote!");
          notenteryet=true;
          makeMyVote();
          break;
        case "3c" :
        try{Thread.sleep(50);}
        catch(Exception e){out.println(e);}
          AllTrackerStream("3d"+payload);
          out.println("OK");
          break;
        case "3d" :
        try{Thread.sleep(50);}
        catch(Exception e){out.println(e);}
          StreamMsg("41"+payload);
          out.println("RECEIVED");
          break;
        case "3e" :
        try{Thread.sleep(50);}
        catch(Exception e){out.println(e);}
          out.println(Blocky.BlocktoEncode());
          break;
        case "3f" :
        try{Thread.sleep(500);}
        catch(Exception e){out.println(e);}
          AllTrackerStream("3g"+payload);
          out.println("OK");
          break;
        case "3g" :
        try{Thread.sleep(500);}
        catch(Exception e){out.println(e);}
          try{
            waitblock.interrupt();
            waitblock.join();
          }
          catch(Exception e){System.out.println("Error parando waitblock: "+e);}
          if(Integer.parseInt(payload.substring(0,4)) == winner){
              StreamMsg("42"+payload.substring(4));
              HashMap<String,Object> newbk = Blocky.SubDecode(payload.substring(4));
              System.out.println("nextbk!!:\n"+newbk.toString()+"\n");
              if(Blocky.ValidateNextBlock(newbk)){
                System.out.println("New block added!");
                InputCache = new ArrayList<HashMap<String,Object>>();
              }
              out.println("RECEIVED");

              }
          else out.println(getID()+" "+Integer.parseInt(payload.substring(0,4))+", "+ winner+": REFUSED");
          break;
      }
    }
  }
}
