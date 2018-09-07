import java.security.*;
import java.io.*;
import java.security.spec.EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.*;
import javax.crypto.spec.*;
public class MakeSign{
  public static void main(String[] args) {
        try{
          /*
          KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");


          keyGen.initialize(512);
          KeyPair kp = keyGen.generateKeyPair();
          Key pub = kp.getPublic();
          Key priv = kp.getPrivate();

*/
          KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
	        keyPairGenerator.initialize(1024);
	        KeyPair keyPair = keyPairGenerator.genKeyPair(); // la idea es generar un par con lo del codigo QR
	        PrivateKey privateKey = keyPair.getPrivate();
	        PublicKey publicKey = keyPair.getPublic();

          System.out.println(publicKey);



	       byte[] privateKeyBytes = privateKey.getEncoded();
	       byte[] publicKeyBytes = publicKey.getEncoded(); // necesito esta wea
         System.out.println(publicKeyBytes.toString());
         String pubString = new String(publicKeyBytes);
         String addr = DoHash2(pubString); // necesito esta wea
         System.out.println("The address: "+addr); //

	       EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
	       EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
         byte[] data = "holi".getBytes("UTF8"); //en vez de holi, se tiene que usar la addr generada

         Signature sig = Signature.getInstance("SHA256WithRSA");
         sig.initSign(privateKey);
         sig.update(data);
         byte[] signatureBytes = sig.sign(); // necesito esta wea

         KeyFactory keyFactory = KeyFactory.getInstance("RSA");

	       PrivateKey newPrivateKey = keyFactory.generatePrivate(privateKeySpec);

	       PublicKey newPublicKey = keyFactory.generatePublic(publicKeySpec);
         System.out.println(newPublicKey);
         Signature sig2 = Signature.getInstance("SHA256WithRSA");
         sig2.initVerify(newPublicKey);
         sig2.update(data);
         System.out.println(signatureBytes);
         System.out.println("se logro: "+sig2.verify(signatureBytes));
          //System.out.println(pub.getDecoded());


        }
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
    catch (UnsupportedEncodingException e) {
      System.out.println("problem; "+e);
}








  }
  private static  String DoHash2(String base){

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

  }

//  public void MakeSign(){


          /*byte[] publicKey = keyGen.genKeyPair().getPublic().getEncoded();
          StringBuffer retString = new StringBuffer();
          for (int i = 0; i < publicKey.length; ++i) {
              retString.append(Integer.toHexString(0x0100 + (publicKey[i] & 0x00FF)).substring(1));
          }
          System.out.println(retString);
          */
      //}
