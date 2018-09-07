import java.util.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

import java.io.*;

public class Vote{

  public void Vote()
  {
  }
  /* La clase tiene la cadena, para este efecto cada vez que se hace una operacion de la cadena, esta debe ser actualziada consola
  la ultima cadena*/
  public void UpdateChain(ArrayList<HashMap<String, Object>> ch)
  {
    chain = ch;
  }
  // Se lleva un registro con los votos que aún no so pusheados a la cadena de bloques para evitar dobles votos.
  // esta funcion limpia este registro
  public void cleanActual(){

    actual = new ArrayList<String>();
  }

  public String[] readnext(char[] bk, int pointer){
    int newpointer = pointer;
    int siz = Integer.parseInt(String.valueOf(bk,newpointer,4));
    int k = 0;
    newpointer += 4;
    String value = String.valueOf(bk,newpointer,siz);
    newpointer += siz;
    return new String[]{value, newpointer+""};
  }

  public HashMap<String,Object> DecodeInput(String code){
    code = code.replaceAll(" ","");
    char[] iter = code.toCharArray();
    String[] aux_m = new String[2];
    HashMap<String,Object> input =  new HashMap<String, Object>();
    //HashMap<String,byte[]> machine = new HashMap<String,byte[]>();
    HashMap<String,String> machine = new HashMap<String,String>();
    int pointer = 0;
    aux_m = readnext(iter,pointer);
    //machine.put("addr",Base64.getMimeDecoder().decode(aux_m[0]));
    machine.put("addr",aux_m[0]);
    pointer = Integer.parseInt(aux_m[1]);
    aux_m = readnext(iter,pointer);
    //machine.put("sign",Base64.getMimeDecoder().decode(aux_m[0]));
    machine.put("sign",aux_m[0]);
    pointer = Integer.parseInt(aux_m[1]);
    aux_m = readnext(iter,pointer);
    //machine.put("pubkey",Base64.getMimeDecoder().decode(aux_m[0]));
    machine.put("pubkey",aux_m[0]);
    pointer = Integer.parseInt(aux_m[1]);
    int votec = Integer.parseInt(String.valueOf(iter,pointer,4));
    pointer +=4;
    //ArrayList<HashMap<String,byte[]>> votes = new ArrayList<HashMap<String,byte[]>>();
    //HashMap<String,byte[]> avote = new HashMap<String, byte[]>();
    ArrayList<HashMap<String,String>> votes = new ArrayList<HashMap<String,String>>();
    HashMap<String,String> avote = new HashMap<String, String>();
    for(int i= 0; i!=votec;i++){
      aux_m=readnext(iter,pointer);
      //avote.put("addr",Base64.getMimeDecoder().decode(aux_m[0]));
      avote.put("addr",aux_m[0]);
      pointer = Integer.parseInt(aux_m[1]);
      aux_m=readnext(iter,pointer);
      //avote.put("sign",Base64.getMimeDecoder().decode(aux_m[0]));
      avote.put("sign",aux_m[0]);
      pointer = Integer.parseInt(aux_m[1]);
      aux_m=readnext(iter,pointer);
      //avote.put("value",Base64.getMimeDecoder().decode(aux_m[0]));
      avote.put("value",aux_m[0]);
      pointer = Integer.parseInt(aux_m[1]);
      votes.add(avote);
    }
    input.put("machine",machine);
    input.put("votes",votes);
    return input;
  }

  //funcion que retorna el formato del voto.
  public HashMap<String,Object> make_vote(HashMap<String,Object>input)
  {
    // revisa si el formato del input(los votos) esta correcto.
    System.out.println("reviso si IsAInput");
    if(!IsAInput(input)) {

      System.out.println("Fallo en IsAInput");
      return new HashMap<String, Object>();
    }
    // revisa que el contenido sea valido, es decir, las firmas son correctas y no hay duplicados
    System.out.println("valido el input");
    if(!validate_input(input)){

      System.out.println("Fallo en validate");
      return new HashMap<String, Object>();
    }
    // si todo esta bien, retorna un input(votos) con la información dada
    HashMap<String,String> machine = (HashMap<String,String>)input.get("machine");
    return make_input(machine);
  }
  // funcion que crea el formato de un input
  public HashMap<String,Object> make_input(HashMap<String,String> machine)
  {
    HashMap<String,Object> input = new HashMap<String,Object>();

    input.put("machine",machine);
    input.put("votes",realvotes);
    realvotes = new ArrayList<HashMap<String,String>> ();
    return input;
  }

  // funcion que verifica que el formato entregado corresponde a un input
  public boolean IsAInput(HashMap<String,Object>input )
  {
    //verifica si tiene una maquina que firma los votos.
    if(input.containsKey("machine")){
      System.out.println("si tiene machine ");
      HashMap<String,String> machine = (HashMap<String,String>)input.get("machine");
      Set mkeys = machine.keySet();
      System.out.println("keys: "+ mkeys);
      // revisa las keys del hashmap de la maquina, que tengan una direccion, una llave publica, y una firma correspondiente a la llave
      //notar que aqui no se verifica la firma ni la direccion.
      if(!mkeys.containsAll(Arrays.asList("addr","pubkey","sign"))){
          System.out.println("algo pasa con las keys");
          return false;
        }
      if(mkeys.size() > 3){
        System.out.println("algo pasa con el tamaño");
        return false;
      }
    }
    else return false;
    // los mismo, pero para los votos.
    if(input.containsKey("votes")){
      System.out.println("Si tiene votos");
      votes = (ArrayList<HashMap<String,String>>)input.get("votes");
      for(HashMap<String,String> vote : votes  ){
        Set keys = vote.keySet();
        System.out.println("keys votos: "+ keys);
        // si un voto no cumple el formato es removido, no se desechan todos los votos validamente formateados.
        if(!keys.containsAll(Arrays.asList("addr","sign","value")))votes.remove(vote);
        if(keys.size() > 3)votes.remove(vote);
        else{
          System.out.println("addr del votante:"+new String(vote.get("addr")));
          }
      }
      System.out.println("los votos son:"+ votes);
      return true;
    }
    else return false;
  }
  // funcion que valida los votos
  public boolean validate_input(HashMap<String,Object> input)
  {
    // rescata el genesis de la cadena para tener la lista de la direccion de los votantes habilitados
    // y de las maquinas habilitadas.

    System.out.println("creando genesis: "+chain.size());
    HashMap<String,Object> genesis=(HashMap<String,Object>) chain.get(0);
    System.out.println("genesis: "+genesis);
    System.out.println("creando machine");
    HashMap<String,String> machine = (HashMap<String,String>)  input.get("machine");
    System.out.println("machine: "+machine);
    // valida individualmente la maquina y los votos.
    if (!validate_machine(machine,genesis))return false;
    System.out.println("maquina validada!!");
    if (!validate_votes(votes, genesis, machine.get("pubkey")))return false;
    return true;
  }
  // funcion que valida la maquina
  public boolean validate_machine(HashMap<String,String> mac, HashMap<String,Object> gen ){

    String addr2 = new String(Base64.getMimeDecoder().decode(mac.get("addr")));
    String addrs = mac.get("addr");
    System.out.println("el addr de la maquina es: "+addr2);
    ArrayList<String> machine_db=new ArrayList<String>();
    // limpiar el hashmap de espacios ninjas
    for(String machine : (ArrayList<String>)gen.get("machines_db"))
        machine_db.add(machine.replaceAll(" ",""));
    // revisa si la direccion de la maquina dada está presente en el bloque genesis
    if(!machine_db.contains(new String(addr2)) ) return false;
    System.out.println("la maquina esta habilitada");
    String sign = mac.get("sign");
    String pubkey = mac.get("pubkey");
    System.out.println("los datos de la maquina son: " + new String(sign)+ new String(pubkey));
    System.out.println("ly soy: "+passInputTest(addrs,pubkey,sign));
    // verifica que la firma corresponde a la direccion y a la pubkey entregada.
    return( passInputTest(addrs,pubkey,sign));
  }
  // funcion que verifica los votos
  // identica a validate_machine pero con los votos, notar que usa la pubkey de la maquina.
  public boolean validate_votes(ArrayList<HashMap<String,String>> vot,HashMap<String,Object> gen, String pubkey ){
    ArrayList<String>vote_db = new ArrayList<String>();
    for (String voter :(ArrayList<String>)gen.get("voters_db"))
        vote_db.add(voter.replaceAll(" ",""));

    for(HashMap<String, String> vote: vot){
      String saddr = new String(Base64.getMimeDecoder().decode(vote.get("addr")));
      System.out.println(vote_db+", y el voto: "+ saddr);
      if (vote_db.contains(saddr)){
        System.out.println("addr del voto: "+saddr);
        // aqui ademas se verifica que el votante está votando por primera vez
        if (passInputTest(vote.get("addr"),pubkey,vote.get("sign"))&&!DoubleVote(saddr)){
            System.out.println("agregando a:"+ saddr+ "porque:"+DoubleVote(saddr));
            actual.add(saddr);
            realvotes.add(vote);
          }
      }
    }
    votes= new ArrayList<HashMap<String,String>> ();
    return true;

  }
  // funcion que verifica firmas
  public boolean passInputTest(String sadr, String skey, String ssig){
    byte[] adr = Base64.getMimeDecoder().decode(sadr);
    byte[] key = Base64.getMimeDecoder().decode(skey);
    byte[] sig = Base64.getMimeDecoder().decode(ssig);
    try{
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(key);
        PublicKey newPublicKey = keyFactory.generatePublic(publicKeySpec);
        Signature sig2 = Signature.getInstance("SHA256WithRSA");
        sig2.initVerify(newPublicKey);
        sig2.update(adr);
        return sig2.verify(sig);

      }
      // estos catch sobran, se deberia poner un unico catch con Exception, pero si funciona, no lo toque
        catch (NoSuchAlgorithmException e){
              System.out.println("problem: "+e);

            }
            catch (InvalidKeySpecException e) {
              System.out.println("problem; "+e);
            }
            catch (InvalidKeyException e){
              System.out.println("problem: "+e);

            }
            catch (SignatureException e) {
              System.out.println("problem; "+e);
            }
          return false;
  }
  // funcion que verifica que el votante esta votando por primera vez.
  public boolean DoubleVote(String addr)
  {
    System.out.println("\n Me llaman");
    // aca se pilla si el votante se repite en la lista de votos que está ingresando
    if(actual.contains(addr)){
      System.out.println("\n Esta repetida");
      return true;

    }
    // aca se pilla si el votante ya fue contabilizado en otro bloque
    System.out.println("chain size: " +chain.size());
    for(int i = 1; i <chain.size();i++ ){
      System.out.println("indice:" + i);
      HashMap<String,Object> onechain =(HashMap<String,Object>)chain.get(i);
      System.out.println("\nonechain: "+ onechain);
      ArrayList<HashMap<String,Object>> tx = (ArrayList<HashMap<String,Object>>) onechain.get("tx");
      System.out.println("tx es: "+tx.size());
      if (tx.size() > 0){
      for (HashMap<String, Object> cursor: tx){
          ArrayList<HashMap<String,String>> votes = (ArrayList<HashMap<String,String>>) cursor.get("votes");
          for(HashMap<String,String> vote : votes ){
            String saddr = new String(Base64.getMimeDecoder().decode(vote.get("addr")));
            System.out.println("\n String in chain:" + saddr.equals(addr));
            return saddr.equals(addr);
          }

      }
    }

    }
    return false;
  }

//funcion que lee un archivo con los votos
public HashMap<String, Object> readAVote(File file){
  try{
    Path path = Paths.get(file.getAbsolutePath());
    byte[] data = Files.readAllBytes(path);
    HashMap<String, Object> newmap = new HashMap<String,Object>();
    ByteArrayInputStream bis = null;
    ObjectInputStream ois = null;
    bis = new ByteArrayInputStream(data);
    ois = new ObjectInputStream(bis);
    newmap = (HashMap<String, Object>) ois.readObject();
    System.out.println("readAVote: " +newmap);
    return newmap;


}catch(Exception e){
  System.out.println("error readvote"+ e);
return null;
}
}
public void printActual(){
  System.out.println("actual: "+actual);
}
//auxiliares
private ArrayList<HashMap<String,String>> votes= new ArrayList<HashMap<String,String>> ();
private ArrayList<HashMap<String,String>> realvotes= new ArrayList<HashMap<String,String>> ();
// caches
private ArrayList<String> actual= new ArrayList<String>();
private ArrayList<HashMap<String,Object>> chain= new ArrayList<HashMap<String,Object>>();
private ArrayList<String> shame = new ArrayList<String>();

}
