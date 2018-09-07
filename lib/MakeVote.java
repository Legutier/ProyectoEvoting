/*
Clase similar a Vote.class, pero esta se encarga de armar los votos según ese formato.
Es ocupada por VoteView (la consola de votación)

*/

import java.security.*;
import java.io.*;
import java.io.File;
import java.nio.file.*;
import java.security.spec.*;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.Base64;
import java.util.Base64.*;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class MakeVote{

  public void MakeVote(){
  }
  // set all es como el constructor pero se deja aparte para elegir en qué momento se inicia, esto es porque quien invoca a
  //MakeVote debe tener el bloque genesis cargado y pasarselo a MakeVote y esto no necesariamente pasa al principio de la invocación
  //puede que esto peuda mejorarse al ser un error de arquitectura invocar un objeto cuando no se está preparado para armarlo
  public void SetAll(){
    try{
      // setAll setea la llave privada con la cual se harán las firmas digitales
      // crea las firmas digitales de la maquina y el hash(address) de la maquina
      byte[] keyBytes = Files.readAllBytes(Paths.get(FILENAME));

      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");

      privateKey= kf.generatePrivate(spec);
      RSAPrivateCrtKey privk = (RSAPrivateCrtKey)privateKey;
      RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      publicKey = keyFactory.generatePublic(publicKeySpec);
      addr = MakeVote.DoHash(new String(publicKey.getEncoded())).getBytes();
      System.out.println(new String(addr));
      Signature sig = Signature.getInstance("SHA256WithRSA");
      sig.initSign(privateKey);
      sig.update(addr);
      sign = sig.sign();
      System.out.println(new String(sign));
    }
      catch(Exception e){
        System.out.println("problem; "+e);
    }
  }

  public String getAddr(){
    return new String(addr);
  }
  public ArrayList<HashMap<String,String>> getVotes(){
    return votes;

  }
  // metodo que agrega un voto en el cache, esto es, un hashmap con la dirección del votante (el hash de su rut), la firma digital y el valor del voto
  public void pushAVote(String candidate, String addr_v){
    try{
      HashMap<String,String> avote = new HashMap<String,String>();
      Signature sigvote = Signature.getInstance("SHA256WithRSA");
      sigvote.initSign(privateKey);
      sigvote.update(addr_v.getBytes());
      byte[] sig_v= sigvote.sign();
      String saddr = Base64.getEncoder().encodeToString(addr_v.getBytes());
      String ssigv = Base64.getEncoder().encodeToString(sig_v);
      String scand = Base64.getEncoder().encodeToString(candidate.getBytes());
      avote.put("addr", saddr);
      avote.put("sign",ssigv);
      avote.put("value",scand);
      votes.add(avote);
    }
    catch (Exception e){
      throw new RuntimeException(e);
      //System.out.println("problem: "+e);

    }
  }
  //serializa un array de bytes de tamaño aleatorio
   public String EncodeToString(byte[] by){
     String precode = Base64.getEncoder().encodeToString(by);
     String nro = completeZero(precode.length()+"",4);
     return nro+precode;
   }
   // serialzia una string de tamaño aleatorio
    public String EncodeToString(String sby){
      //String precode = Base64.getEncoder().encodeToString(by);
      String nro = completeZero(sby.length()+"",4);
      return nro+sby;
    }
  // serializa un int de tamaño aleatorio a uno de "zeros" tamaño, es decir, si el int es 46 y zeros es 4 se retorna 0046
   public String completeZero(String nro, int zeros){
     String newnro = nro+"";
     while(newnro.length() < zeros){
       newnro = "0"+newnro;
     }
     System.out.println(newnro +" "+newnro.length());
     return newnro;
   }
   public String EncodeInput(){
     String code = EncodeToString(addr) +
      EncodeToString(sign) +
      EncodeToString(publicKey.getEncoded()) +
      completeZero(votes.size()+"",4) ;
      System.out.println(votes.toString() +"\n\n");
      for(HashMap<String, String> unvoto: votes){
        code += EncodeToString(unvoto.get("addr"))+
        EncodeToString(unvoto.get("sign"))+
        EncodeToString(unvoto.get("value"));
     }
     votes = new ArrayList<HashMap<String,String>>();
     return code;
   }
   // metodo que haseha con algoritmo SHA-256
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
   private PublicKey publicKey;
   private PrivateKey privateKey ;
   private final String FILENAME = "private.der";
   private byte[] addr = "hola".getBytes();
   private ArrayList<HashMap<String,String>> votes = new ArrayList<HashMap<String,String>>() ;
   private byte[] sign= null;
}
