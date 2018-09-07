/*
Clase que describe la estructura de la cadena de bloques, inicializa la cadena con los datos de la votacion:
los candidatos, las maquinas y personas habilitadas para emitir votos y la metadata del primer bloque.
Cada bloque tiene headers y payload.
El header es el hash del bloque anterior, el nonce del bloque anterior, su propio nonce y hash.
Los bloques son guardados en un ArrayList que sería la cadena, por lo cual son guardados por posición
y esta posición sería su key (su "ID").
El payload por su parte son las transacciones, a excepción del primer bloque, el bloque genesis.
Las transacciones son la colección de votos y estos son descritos en otra clase.

Esta clase también tiene los metodos para verificar la cadena de bloques.
La cadena es un ArrayList de HashMap, como los votos son HashMap tambien el HashMap guarda objectos como valor con String como keys (HashMap<String,Object>)
por lo tanto tiene una forma como:
{0:                                     <-- indice del ArrayList, esto es sólo representativo, la estructura de dato no contiene esta key explisitamente.
                                            Este es el bloque genesis, id 0.
  {"voters_db": a3f49s..., kk42o9d... ,  <--- las direcciones(hash del las publicKey) de los votantes habilitados
    "machines_db": 44f45s...,9s9sa... ,  <---las direcciones(hash de las publickey) delas maquinas que firman los votos.
    "candidates" : pedrito, juanito   ,  <--- nombre de los candidatos
    "prev_hash" : "0",
    "prev_nonce" "0",                     <--- al ser el bloque genesis estos datos son ignorados, SOLO para este bloque
    "hash":  00as7s7a89a...               <--- hash del bloque actual (genesis)
    "nonce": 2993                         <--- nonce del bloque actual (genesis)
  },
 1:                                        <--- primer bloque de verdad
  {"prev_hash": 00as7s7a89a...             <--- hash del bloque previo, notar que es el mismo que el hash del bloque genesis
   "prev_nonce": 2993                      <--- idem para el nonce.
   "tx": {
            _VOTO_                         <--- aqui va el payload, que en este caso son los votos descritos en otra clase
         }
   "hash" : 009021ds09...
   "nonce" : 187                           <--- hash y nonce del bloque(notar que todos los hashes empeizan con 00).
  }
  .
  .
  .
}
Notas:
 -Para que este modelo funcione todos los peer(o la mayoría) deben tener la misma cadena.
 -Para añadir un bloque nuevo se debe verificar toda la cadena primero. Esto puede ser un problema si la cadena es muy grande.
 -El payload puede ser lo que sea, esta clase no se preocupa de verificar los votos, pero si llama a un metodo verificador
 de otra clase. Esto puede servir si se le quiere dar otro propósito a la cadena de bloques.
*/

import java.util.*;
import java.util.regex.*;
import java.security.*;
import java.io.*;
import java.nio.*;
public class Block {
  //Constructor
  public Block( ArrayList<String> machines, ArrayList<String> voters, ArrayList<String> candidates){
    HashMap<String,Object> genesis = new HashMap<String,Object>(); //inicia la cadena con el bloque de genesis
    genesis.put("voters_db",voters);
    genesis.put("machines_db",machines); //las direcciones de maquinas que pueden firmar los votos
    genesis.put("candidates", candidates); //los candidatos validos

    ArrayList<String> hashes = hashinger(genesis); // generar el par hash-nonce del bloque genesis
    genesis.put("hash",hashes.get(0));
    genesis.put("nonce",hashes.get(1));
    prev_hash = hashes.get(0);
    prev_nonce= hashes.get(1);
    chain.add(0,genesis); // poner el bloque genesis como el primer bloque de la cadena
    voter = new Vote();
    voter.UpdateChain(chain); // agregar la cadena al sistema de votos

    //voting = new Vote(chain);
  }
  // segundo constructor, recibe una cadena hecha
  public Block(ArrayList<HashMap<String,Object>> ch){
    if (!Validate(ch))return;
    chain = new ArrayList<HashMap<String,Object>>(); // si la cadena es valdia la agrega
    chain.addAll(ch);
    int index =chain.size()-1;
    prev_hash = (String)((HashMap<String,Object>)chain.get(index)).get("hash"); //
    prev_nonce = (String)((HashMap<String,Object>)chain.get(index)).get("nonce");
    voter = new Vote();
    voter.UpdateChain(chain); // agregar la cadena al sistema de votos

  }
  // constructor dummy para ocupar funciones
  public Block(){
    System.out.println("dummy");
    chain = new ArrayList<HashMap<String,Object>>();
    voter = new Vote();
    voter.UpdateChain(chain); // agregar la cadena al sistema de votos
  }
  // funcion que agrega un bloque a la cadena
  public boolean ValidateNextBlock(HashMap<String,Object> nxtblk){
    ArrayList<HashMap<String,Object>> aux = new ArrayList<HashMap<String,Object>>();
    aux.addAll(chain);
    aux.add(nxtblk);

    if(!Validate(aux)){
      return false;
    }
    chain.add(nxtblk);
    prev_hash = (String)nxtblk.get("hash");
    prev_nonce = (String)nxtblk.get("nonce");
    return true;

  }
  public void NextBlock(){

    HashMap<String,Object> block = blockfy(current,prev_hash,prev_nonce);
    ArrayList<String> block_hashing = hashinger(block);
    String block_hash = block_hashing.get(0);
    String block_nonce = block_hashing.get(1);
    block.put("hash", block_hash);
    block.put("nonce", block_nonce);

    prev_hash = block_hash;
    prev_nonce = block_nonce;
    chain.add(block); // agrega el bloque a la cadena
    voter.printActual();
    voter.cleanActual();
    voter.printActual();
    current = new ArrayList<HashMap<String,Object>>();
  }
  // funcion que crea el formato dle bloque
  private HashMap<String,Object> blockfy(ArrayList<HashMap<String,Object>> cur, String prev_hash, String prev_nonce){

    HashMap<String,Object> jblock = new HashMap<String,Object>();
    jblock.put("tx", cur);
    jblock.put("prev_hash", prev_hash);
    jblock.put("prev_nonce", prev_nonce);


    return jblock;
  }
  // agrega un voto a la blockchain
  public void AddVote( HashMap<String,Object> input){
    current.add(input );

  }

  public void AddDecodeVote(String inputhash){
    HashMap<String,Object> input = voter.DecodeInput(inputhash);

    current.add(input );
  }
  //funciones que validan la cadena entera

  private boolean Validate(){
    return Validate(chain);
  }

  public boolean Validate(ArrayList<HashMap<String,Object>> ch){
    if(ch.size() < 2) return true;
    int c_index = 2; //parte del primer bloque, ignora el genesis
    HashMap<String,Object> l_block = new HashMap<String,Object>();
    HashMap<String,Object> a_block = new HashMap<String,Object>();

     //rescata el primer bloque
    while (c_index < ch.size()) {
      l_block = ch.get(c_index-1);
      a_block = ch.get(c_index);
      ArrayList<HashMap<String,Object>> btx = (ArrayList<HashMap<String,Object>>)l_block.get("tx"); //obtiene la transaccion del bloque anterior rescatado
      String bprev_hash = (String)l_block.get("prev_hash"); //su hash previo
      String bprev_nonce = (String)l_block.get("prev_nonce"); //y su nonce previo
      // recrea el nonce y el hash del bloque anterior al rescatado
      HashMap<String,Object> prev_block = blockfy(btx,bprev_hash,bprev_nonce);
      ArrayList<String> l_hashing = hashinger(prev_block);
      String l_hash = l_hashing.get(0);
      String l_nonce = l_hashing.get(1);
      //los compara con los escritos en el bloque rescatado
      //si uno falla, la funcion retorna falso, diciendo que la cadena está alterada
      System.out.println("1:"+a_block.get("prev_hash")+", "+l_hash);
      if ( !a_block.get("prev_hash").equals(l_hash)) return false ;
      System.out.println("2:"+a_block.get("prev_nonce")+", "+l_nonce);
      if ( !a_block.get("prev_nonce").equals(l_nonce)) return false ;
      System.out.println("3");
      //si no sigue con el siguiente bloque*/
      l_block = new HashMap<String,Object>();;
      c_index++;


    }
  // si todo está bien, retorna verdadero
  return true;
  }
  // funcion que calcula el par nonce y hash para un bloque
  public ArrayList<String> hashinger(HashMap<String,Object> blk ){
    String blkString = blk.toString();
    int nonce = 0;
    String hashString = addString(blkString,nonce);
    String hash = DoHash(hashString);
    ArrayList<String> validDuo= new ArrayList<String>();
    System.out.println("\n\nblk:"+blk.toString()+"\n\n"+"chain size: "+chain.size());

    while (!IsValid(hash)) {

      nonce++;
      hashString = addString(blkString,nonce);
      hash = DoHash(hashString);
   }
   validDuo.add(hash);
   validDuo.add(nonce+"");
   return validDuo;
 }
 // funcion que crea un hash para cualquier string con SHA-256
  public static  String DoHash(String base){

      try{
          MessageDigest digest = MessageDigest.getInstance("SHA-256");
          byte[] hash = digest.digest(base.getBytes("UTF-8"));

          StringBuffer hexString = new StringBuffer();

          for (int i = 0; i < hash.length; i++) {
              String hex = Integer.toHexString(0xff & hash[i]);
              if(hex.length() == 1) hexString.append('0');
              hexString.append(hex);
          }

          return hexString.toString();
      } catch(Exception ex){
         throw new RuntimeException(ex);
      }
  }
  // esta funcion sobra
  private String addString(String str, int nonc){
    String added = str + nonc;
    return added;
  }
  // funcion que verifica un hash valido, el hash valido debe ser SHA-256 y empezar con 00
  public  boolean IsValid(String arg){
    String patternStr = "(^00)(.*)" ;
    Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(arg);
    boolean matches = matcher.matches();
    if (matches) return true;
    else return false;
    }

  public ArrayList<HashMap<String,Object>> getChain(){
    return chain;
  }
  public ArrayList<HashMap<String,Object>> getCurrent(){
    return current;
  }



  //////////////////////////////////////////////////////////////
  // metodos para codificar la cadena //////////////////////*****************************************
  //////////////////////////////////////////////////////////////*****************************************
  //////////////////////////////////////////////////////////////


  public String completeZero(String nro, int zeros){
    String newnro = nro;
    while(newnro.length() < zeros){
      newnro = "0"+newnro;
    }

    return newnro;
  }

  public String EncodeToString(byte[] by){
    String precode = Base64.getEncoder().encodeToString(by);
    String nro = completeZero(precode.length()+"",4);
    return nro+precode;
  }
  public String EncodeToString(String sby){
    String nro = completeZero(sby.length()+"",4);
    return nro+sby;
  }
  public String EncodeInput(HashMap<String,Object> inp){
     HashMap<String,String> mach = (HashMap<String,String>)inp.get("machine");
     ArrayList<HashMap<String,String>> vots = (ArrayList<HashMap<String,String>>)inp.get("votes");
     String code = EncodeToString(mach.get("addr"))+EncodeToString(mach.get("sign"))+EncodeToString(mach.get("pubkey"))
     +completeZero(vots.size()+"",4);
     for(HashMap<String, String> unvoto: vots){
       code += EncodeToString(unvoto.get("addr"))+
       EncodeToString(unvoto.get("sign"))+
       EncodeToString(unvoto.get("value"));
    }
    return completeZero(code.length()+"",8)+code;
  }
  public String BlocktoEncode(){
    String encode = EncodeGenesis()+"%_%_%";
    for(int i=1 ; i<chain.size(); i++){
      HashMap<String,Object> blk = chain.get(i);
      encode +=EncodeBLK(blk) ;
    }
    return encode;
  }
  public String EncodeBLK(HashMap<String,Object> blk){
    String encode="";

    ArrayList<HashMap<String,Object>> txx= (ArrayList<HashMap<String,Object>>)blk.get("tx");
    encode +=completeZero(txx.size()+"",4);
    if(txx.size()>0){
      for(HashMap<String,Object> inp : txx ){
        encode += EncodeInput(inp);
      }
    }
    encode += completeZero((String)blk.get("prev_nonce"),8)+(String)blk.get("prev_hash")
    +completeZero((String)blk.get("nonce"),8)+(String)blk.get("hash")+"%_%_%";

    return encode;
  }

  public String EncodeGenesis(){
    HashMap<String,Object> gen = chain.get(0);
    String code = EncodeArray((ArrayList<String>)gen.get("voters_db"))
      +EncodeArray((ArrayList<String>)gen.get("machines_db"))
      +EncodeArray((ArrayList<String>)gen.get("candidates"))
      +completeZero((String)gen.get("nonce"),8)
      +(String)gen.get("hash");
    return code;
  }

  public String EncodeArray(ArrayList<String> arr){
    String encode = completeZero(arr.size()+"",8);
    for(String i: arr){
      encode += i;
    }
    return encode;

  }
  //////////////////////////////////////////////////////////////
  ///// Metodos para decodificar la cadena /////////////////////
  //////////////////////////////////////////////////////////////
  public static String[] readnext(char[] bk, int pointer){
    int newpointer = pointer;
    int siz = Integer.parseInt(String.valueOf(bk,newpointer,4));
    int k = 0;
    newpointer += 4;
    String value = String.valueOf(bk,newpointer,siz);
    newpointer += siz;
    return new String[]{value, newpointer+""};
  }

  public HashMap<String,Object> GenesisDecode( String pregencode){
      int pointer = 0;
      String gencode = pregencode.replaceAll(" ","");
      ArrayList<String> voters = new ArrayList<String>();
      ArrayList<String> mach = new ArrayList<String>();
      ArrayList<String> cand = new ArrayList<String>();


      int voterscount = Integer.parseInt(gencode.substring(pointer,pointer+8));
      pointer +=8;
      for(int i=0; i < voterscount;i++){
        voters.add(gencode.substring(pointer,pointer+64));
        pointer +=64;

      }

      int machinecount = Integer.parseInt(gencode.substring(pointer,pointer+8));
      pointer +=8;
      for(int i=0; i < machinecount; i++){
        mach.add(gencode.substring(pointer,pointer+64));
        pointer+= 64;
      }
      int candcount = Integer.parseInt(gencode.substring(pointer,pointer+8));
      pointer +=8;
      for(int i=0; i<candcount; i++){
        cand.add(gencode.substring(pointer,pointer+4));
        pointer +=4;
      }
      String nonce = Integer.parseInt(gencode.substring(pointer,pointer+8))+"";
      pointer += 8;
      String hash = gencode.substring(pointer,pointer+64);
      return new HashMap<String,Object>(){{
        put("voters_db",voters);
        put("machines_db", mach);
        put("candidates", cand);
        put("nonce",nonce);
        put("hash",hash);
      }};
  }
  public ArrayList<Object> DecodeInput(String code){
    code = code.replaceAll(" ","");
    char[] iter = code.toCharArray();
    String[] aux_m = new String[2];
    HashMap<String,Object> input =  new HashMap<String, Object>();
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
    ArrayList<Object> arr = new ArrayList<Object>();
    arr.add(input);
    arr.add(pointer);
    return new ArrayList<Object>(arr);
  }
  public HashMap<String,Object> SubDecode(String subcode){
    int pointer = 0;
    int inlength;
    int txcount = Integer.parseInt(subcode.substring(pointer,pointer+4));
    ArrayList<HashMap<String,Object>> txx = new ArrayList<HashMap<String,Object>>();
    ArrayList<Object>  colector = new ArrayList<Object>();
    pointer += 4;

    if(txcount > 0){
      for(int i =0; i < txcount; i++){
        inlength = Integer.parseInt(subcode.substring(pointer,pointer+8));
        pointer +=8;
        colector = DecodeInput(subcode.substring(pointer,pointer+inlength));
        txx.add((HashMap<String,Object>)colector.get(0));
        pointer += (int)colector.get(1);
      }
    }
    String prev_nonce = Integer.parseInt(subcode.substring(pointer,pointer+8))+"";
    pointer+=8;
    String prev_hash = subcode.substring(pointer,pointer+64);
    pointer+=64;
    String nonce = Integer.parseInt(subcode.substring(pointer,pointer+8))+"";
    pointer+=8;
    String hash = subcode.substring(pointer,pointer+64);
    return new HashMap<String,Object>(){{
      put("tx",txx);
      put("prev_nonce",prev_nonce);
      put("prev_hash",prev_hash);
      put("nonce",nonce);
      put("hash",hash);
    }};
  }

  public ArrayList<HashMap<String,Object>> DecodeChain( String code){
    ArrayList<HashMap<String,Object>> thechain = new ArrayList<HashMap<String,Object>>();
    String[] codechain = code.split("%_%_%");
    thechain.add(GenesisDecode(codechain[0]));
    for(int i = 1 ; i < codechain.length ; i++){
      thechain.add(SubDecode(codechain[i]));
    }
    return thechain;
  }

  private Vote voter;
  private ArrayList<HashMap<String,Object>> chain = new ArrayList<HashMap<String,Object>>();
  private ArrayList<HashMap<String,Object>> current = new  ArrayList<HashMap<String,Object>>();
  private String prev_hash = "0";
  private String prev_nonce = "0";
  private String toVote=null;
  //private Vote voting;
}
