import java.util.*;
public class p2p{
  public static void main(String[] args) {
    Peer p2 = new Peer("1","127.0.0.1" ,"9001", "Genesis");
    Peer p = new Peer("1","127.0.0.1" ,args[0], "tracker");
    Traker t = new Traker(p);
    Traker g = new Traker(p2);
    t.StartTraker();
    t.JoinToNet(g);
    /*
    while(true){
      try{
      Thread.sleep(10000);
      }
      catch(Exception e){
        System.out.println(e);
      }
      t.AllTrackerStream("3b.-VOID");
    }

*/

    }

}
