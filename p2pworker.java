public class p2pworker{
  public static void main(String[] args) {
    Peer p = new Peer("1","127.0.0.1" ,args[0], "tracker");
    Peer p2 = new Peer("0","127.0.0.1" ,args[1], "worker");
    Worker w = new Worker(p2);
    w.StartWorker();
    w.JoinNetwork(new Traker(p));
    //t.JoinToNet(new Traker(p));

  }
}
