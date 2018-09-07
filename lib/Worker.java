/*
Clase que construye al Worker, el Worker es un nodo dentro de la red que hace el trabajo de recibir los votos de la Consola
Hacer stream de estos votos a otros nodos, agregar estos votos y construir el próximo bloque si el worker es elegido
y verificar si el bloque publicado por un worker es correcto.
*/
import java.util.*;
import java.net.ServerSocket;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.*;
// Constructor, hereda al nodo porque ES un nodo dentro de la red.
public class Worker extends Node {
  private ArrayList<Traker> trakers_failover = new ArrayList<Traker>(); // No implementado aún, variable que almacenaría trakers auxiliares en caso de que el traker asignado quedara fuera de servicio
  private ArrayList<HashMap<String,Object>> votes_cache = new ArrayList<HashMap<String,Object>>(); //variable que guarda los votos que aún no han sido agregado al bloque
  private Vote voter = new Vote(); // objeto que ayuda a crear y verificar votos.
  private Thread heartbeat; // el thread que audita al tracker.
  private Block blocky = new Block(); //objeto que ayuda a crear y verificar bloques
  private Traker myTraker; // mi tracker, el cual me conecta a la red.

  public Worker(Peer p) {
    super(p);
  }
// funcion que inicia el nodo y el heartbeat, al iniciar el nodo el worker se pone a escuchar peticiones de otros nodos(vía su tarcker)
  public void StartWorker() {
    heartbeat = new Thread() {
      public void run() {
        while (true) {
          if(myTraker != null) HeartBeat(myTraker, "00ID");
          try {
            Thread.sleep(10000); // cada 10 segundos manda un ping al tracker
          } catch (Exception e) {
            System.out.println("Error while waiting:" + e);
          }
        }
      }
    };
    this.Start();
    heartbeat.start();
  }
  // método que pregunta por la cadena actual,
  public void AskForChain(Traker t){
    System.out.println(SendMsg(t,"3e"+getPeer().toString()));
  }
  // método que es llamado cuando el worker es elegido para hacer el siguiente bloque
  public String ChoosenOne(){
    voter.UpdateChain(blk.getChain()); // le pasa al bloque la cadena actual
    if(InputCache.size()>0){ // si hay votos en el cache, los agrega al bloque
      for(HashMap<String,Object> oneinput : InputCache ){
          blk.AddVote(voter.make_vote(oneinput));
      }
      InputCache = new ArrayList<HashMap<String,Object>>(); // luego los votos son limpiados
    }
    blk.NextBlock(); // se crea el bloque
    System.out.println("\n\n chain: \n"+blk.getChain()+"\n\n");

    int last = blk.getChain().size();
    return blk.EncodeBLK(blk.getChain().get(last-1)); // se retorna el bloque serializado
  }
  // metodo que se usa para unir el worker a la red, si se puede conectar al tracker se pregunta por la cadena actual
  public void JoinNetwork(Traker t){
    if(AskToJoin(t)) {
      myTraker = t;
      blk = new Block(blk.DecodeChain(SendMsg(myTraker,"3eVOID")));
      System.out.println("\n"+blk.getChain()+"\n");
      System.out.println("Joined");
    }
    else  System.out.println("Failed");
  }

  // metodo para hacer ping al traker, pregunta tres veces, si las tres fallan se asume que el tracker está inoperativo y se deslogea
  public void HeartBeat(Peer p, String msg) {
    int i = 0;
    boolean wait = true;
    String p_string = p.getPeer().toString();
    while (wait) {
      try {
        String response = SendMsg(p, msg);
        System.out.println(p.getID() + ": " + response);
        if (response.equals("ERROR")) throw new Exception();
        wait = false;
      } catch (Exception e) {
        System.out.println("Unable to comunicate with Peer" + p_string + "\n" + e);
        if (i < 2) i++;
        else {
          System.out.println(SendMsg(this, "13" + p_string));
          wait = false;
        }
      }
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        System.out.println("Error while waiting:" + e);
      }
    }
  }
  //método que verifica el bloque publicado por un worker, si supera la prueba es agregado localmente
  public boolean JoinBlock(String code){
    System.out.println("joinblock code: \n"+blk.SubDecode(code).toString()+" \nand chain is:\n "+blk.getChain().toString());
    return blk.ValidateNextBlock(blk.SubDecode(code));
  }
  // esta función se hereda del nodo y atiende a las peticiones específicas para el worker
  public void URequest(String code, String payload, PrintWriter out) {
    String[] inermsg;
    String[] minermsg;
    if (code.matches("4[0-9a-z]")) { // la codificación 4X pertenece al worker
      switch (code) {
        case "40": // el codigo 40 es para hacer un push de un voto desde una consola a este worker, este codigo solo publica el voto, no lo agrega localmente
          voter.UpdateChain(blk.getChain());
          if(voter.IsAInput(voter.DecodeInput(payload)))
              SendMsg(myTraker,"3c"+payload);
            out.println("DONE");
          break;
        case "41" : // codigo que es para agregar un voto en el cache, independiente si es valido o no
          InputCache.add(voter.DecodeInput(payload));
          //blocky.AddDecodeVote(payload);
          System.out.println(InputCache.toString());
          out.println("RECEIVED");
          break;
        case "42" : // codigo que se sua para agregar un bloque localmente, los traker lo usan si quieren que sus worker verifiquen y agreguen un nuevo bloque publicado
          if(JoinBlock(payload)){
             out.println("ACCEPTED");
             InputCache = new ArrayList<HashMap<String,Object>>();
           }
          else out.println("REFUSED!!!");
          break;
        case "43" :
          out.println(blk.BlocktoEncode()); //codigo que retorna la cadena actual, se usa para pedirle al worker que cadena tiene actualmente
        case "44" : // codigo que designa al worker como el elegido para generar el próximo bloque
          String response =ChoosenOne();
          System.out.println("\n response: \n"+response);
          out.println(response);
        case "45" :
          out.println(blk.BlocktoEncode()); // idem 43, deprecado
      }
    }
  }
}
