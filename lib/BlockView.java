/*
Clase que describa la consola de administración, en definitiva es sólo un Tracker que tiene el bloque génesis
el bloque génesis es donde está toda la información inicial necesaria para llevar a cabo la votación: Los
votantes inscritos, las máquinas que peuden emitir votos y los candidatos.
*/

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class BlockView {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(new Runnable() {   // implementación Swing recomendada
            public void run() {
               MainFrame frame = new MainFrame(args[0],args[1]);
               frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
               frame.setLocationRelativeTo(null); // Centrado en la pantalla
               frame.setVisible(true);
            } // run ends
         });
    }
  }

class MainFrame extends JFrame {
// frame principal con un menu
   public MainFrame(String addr, String port) {
      setTitle("Consola de administracion");
      setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
      setLayout(new BorderLayout());
      setJMenuBar(new MainMenuBar(this));
      MainMenuBar menu = (MainMenuBar) this.getJMenuBar();
      contentPane = getContentPane();
      trk = new Traker(new Peer("100",addr,port,"Genesis")); // se crea el tracker con el puerto y la dirección asiganda por el programa
      blk = trk.getBlock(); // se habilita un bloque
      menu.setFrame(this);

      FirstPane(0); // se parte con el panel que muestra la cadena

      this.repaint();
      this.revalidate();
      trk.StartTraker(); // se da comienzo al tracker, este empieza a recibir peticiones y hacer heartbeat, como es el primer nodo de la red este heartbeat no muestra mucho hasta que se comienzan a unir nodos en la red.
  }
// funcion que setea el bloque del tracker
    public void setBLK(Block b){
      if(b.getChain().size()> trk.getBlock().getChain().size())
        trk.setBlock(b);
    }
//panel que se muestra, depende de que menu se elija, puede mostrar la cadena de bloques en su forma de diccionario o en su forma serializada
   public void FirstPane(int idx){
     // siempre que es llamado remueve todo
     contentPane.remove(stat);
     contentPane.remove(buttons2);
     first.removeAll();
     buttons.removeAll();
     JButton bchain = new JButton("Recargar");
     JTextArea text= new JTextArea();
     text.setEditable(false);

     JScrollPane scrollPane = new JScrollPane(text,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
     scrollPane.setPreferredSize(new Dimension(800, 800));

     bchain.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent event){ // cuando es apretado el boton de recargar siempre se pregunta por la cadena actual
        //System.out.println(chain);
        blk = trk.getBlock();
        if(blk.getChain().size() != 0){ // si hay una cadena cargada muestra los menos, si no manda a un panel pidiendo la cadena
          if(idx == 0){ // muestra la cadena en su forma de diccionario
            chain = blk.getChain();

            //if(!chain.toString().equals(schain)){

            schain = chain.toString();
            //text.setText("What?");
            text.setText(schain.replaceAll("(\\w+?)=","\n$1=").replaceAll("}","\n\n}"));

            //}
          }
          else if(idx == 1){ // muestra la cadena en su forma serializada
            String sblk = blk.BlocktoEncode();
            String str="";
            int last=0;
            for(int i=0; i<sblk.length()-100;i+=100){
              str+=sblk.substring(i,i+99)+"\n";
              last =i;
            }
            str+=sblk.substring(last+99);
            text.setText(str);
            }
          else
            text.setText("Developing :)");
        }
        else text.setText("Carga una Cadena!");

      }
    });

    bchain.getActionListeners()[0].actionPerformed(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,null));
    buttons.add(bchain);
    first.add(scrollPane);
    contentPane.add(first, BorderLayout.CENTER);
    contentPane.add(buttons, BorderLayout.SOUTH);
    contentPane.repaint();
    contentPane.revalidate();
   }
   // cuenta los votos de la cadena para ser mostrados en las estadísticas
   public ArrayList<Integer> countVotes(){
     System.out.println("contando votos");
     // las siguientes lineas obtienen los candidatos desde el bloque Genesis
     //esto puede ser mejorado teniendo una variable que almacene a lso candidatos desde el principio ya que estos no cambian en el tiempo
     HashMap<String,Object> genesis = (HashMap<String,Object>)chain.get(0);
     ArrayList<String> candidates= new ArrayList<String>();
     for(String cand: (ArrayList<String>) genesis.get("candidates"))
          candidates.add(cand.replaceAll(" ",""));
     System.out.println("los candidatos son:"+candidates);
     ArrayList<Integer> count = new ArrayList<Integer>(3);
     count.add(0);
     count.add(0);
     HashMap<String,Object> entry = new HashMap<String,Object>();
     ArrayList<HashMap<String,Object>> tx = new ArrayList<HashMap<String,Object>>();
     //las siguientes lienas revisan los votos para contarlos ssi la cadena no está vacía
     if (chain.size()>1){
       for(int i=1; i < chain.size();i++){

          entry = (HashMap<String,Object>)chain.get(i);
          System.out.println("revisando votos con indcie: "+i);
          tx = (ArrayList<HashMap<String,Object>>)entry.get("tx");
          if(tx.size()>0){
          System.out.println("\n counting vote in:"+tx);
          for(HashMap<String,Object> map: tx ){
          System.out.println("\n el mapa es:"+map);
          ArrayList<HashMap<String,String>> votes = (ArrayList<HashMap<String,String>>)map.get("votes");
          System.out.println("\n los votos son:"+votes);
            for(HashMap<String,String> vote: votes ){
              String dvote = new String(Base64.getMimeDecoder().decode(vote.get("value")));
              System.out.println("\n el voto es:" +dvote.equals(candidates.get(0)));
              System.out.println("\n el voto es:" +dvote.equals(candidates.get(1)));
              if(dvote.equals(candidates.get(0))) count.set(0,count.get(0)+1);
              if(dvote.equals(candidates.get(1))) count.set(1,count.get(1)+1);
            }
          }
        }
      }

   }
   return count;
  }
  //panel que muestra el status de los votos, cuenta los votos, los trakers y el tamaño de la cadena
   public void Second(){
     contentPane.remove(first);
     contentPane.remove(buttons);
     if(blk.getChain().size() > 0){
       stat.removeAll();
       buttons2.removeAll();

       state ="second";
       HashMap<String,Object> genesis = (HashMap<String,Object>)chain.get(0);
       ArrayList<String> candidates= (ArrayList<String>) genesis.get("candidates");
       ArrayList<Integer> count = countVotes();
       String canA;
       String canB;
       if(candidates.get(0).equals("OOOO")){canA = "Optimus Prime"; canB = "Juan C. Bodoque";}
       else{canB = "Optimus Prime"; canA = "Juan C. Bodoque";}
       String votecount = new String("Conteo de Votos: \n\n"+canA+": "+count.get(0)+"\n"+canB+": "+count.get(1));
       JTextArea votes = new JTextArea(30,30);
       JTextArea nodes = new JTextArea(30,30);
       JButton refresh = new JButton("recargar");
       JButton init = new JButton("iniciar");
       votes.setEditable(false);
       votes.setText(votecount);
       nodes.setEditable(false);
       nodes.setText("Trakers en la red: \n"+trk.HowManyTrakers()+"\n Cantidad de bloques en la cadena \n: "+chain.size());

       refresh.addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent event){
          HashMap<String,Object> genesis = (HashMap<String,Object>)chain.get(0);
          ArrayList<String> candidates= (ArrayList<String>) genesis.get("candidates");
          String canA; String canB;
          ArrayList<Integer> count = countVotes();
          if(candidates.get(0).equals("OOOO")){canA = "Optimus Prime"; canB = "Juan C. Bodoque";}
          else{canB = "Optimus Prime"; canA = "Juan C. Bodoque";}
          String votecount = new String("Conteo de Votos: \n\n"+canA+": "+count.get(0)+"\n"+canB+": "+count.get(1));
          votes.setText(votecount);
          nodes.setText("Trakers en la red: \n"+trk.HowManyTrakers()+"\n Cantidad de bloques en la cadena \n: "+chain.size());
          stat.repaint();
          stat.revalidate();


        }
      });
      init.addActionListener(new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent event){
          if(trk.getBlock().getChain().size() < 2 && trk.getTrakersString().size() > 1 )
            trk.AllTrackerStream("3bVOID");
          else System.out.println("No se puede iniciar votación");
        }
      });
      stat.add(votes);
      stat.add(nodes);
      buttons2.add(refresh);
      buttons2.add(init);
      contentPane.add(stat, BorderLayout.CENTER);
      contentPane.add(buttons2, BorderLayout.SOUTH);
      contentPane.repaint();
      contentPane.revalidate();
   }
   else{
     state = "failed";
     JLabel alert = new JLabel("carga un bloque!");
     stat.add(alert);
     contentPane.add(stat, BorderLayout.CENTER);
     contentPane.repaint();
     contentPane.revalidate();

   }

   }
   public void SeeRaw(){

   }
   private String state="";
   private ArrayList<HashMap<String,Object>> chain=null;
   private Block blk = null;
   private Traker trk;
   private String schain ="";
   private Container contentPane;
   public static final int DEFAULT_WIDTH = 900;
   public static final int DEFAULT_HEIGHT = 900;
   private JPanel first = new JPanel();
   private JPanel buttons = new JPanel();
   private JPanel stat = new JPanel();
   private JPanel buttons2 = new JPanel();
 }
 // menu que permite seleccionar lso paneles anteriormente descritos y cargar el bloque genesis desde un archivo con cierto formato
class MainMenuBar extends JMenuBar implements ActionListener{

  JMenuItem item = new JMenuItem("Abrir");
  JMenuItem item2 = new JMenuItem("Administrar");
  JMenuItem item3 = new JMenuItem("Ver Bloque");
  JMenuItem item4 = new JMenuItem("Ver Raw");

  private JFileChooser fc;
  private JFileChooser fc2;
  private Container parent;
  private Block blocky=null;
  private MainFrame mainFrame= null;
   public MainMenuBar(Container p)  {
       parent = p;
      JMenu menu = new JMenu("File");


      item.addActionListener(this);
      item2.addActionListener(this);
      item3.addActionListener(this);
      item4.addActionListener(this);

      menu.add(item);
      menu.add(item2);
      menu.add(item3);
      menu.add(item4);

      add(menu);
     }

   public void setFrame(MainFrame fr){
     mainFrame = fr;
   }
   public void actionPerformed(ActionEvent event){
     if(event.getSource() == item) {
        System.out.println("Open");

        fc = new JFileChooser(System.getProperty("user.dir"));
        //fc.setFileFilter(new FileNameExtensionFilter("PBM file", "pbm"));
        int returnVal = fc.showOpenDialog(parent);
        if(returnVal == JFileChooser.APPROVE_OPTION){
            System.out.println("Abriendo el archivo " + fc.getSelectedFile().getName());
            try{
                File file = fc.getSelectedFile();
                ArrayList<String> dbs = readFile(file);
                ArrayList<String> machine_db =  new ArrayList<String>(Arrays.asList(dbs.get(0).split(",")));
                ArrayList<String> voters =  new ArrayList<String>(Arrays.asList(dbs.get(1).split(",")));
                ArrayList<String> candidates = new ArrayList<String>(Arrays.asList(dbs.get(2).split(",")));
                blocky = new Block(machine_db, voters,candidates);
                mainFrame.setBLK(blocky);
                System.out.println(blocky.getChain());
              }
            catch (Exception e) {
                System.out.println(e);
              }
      }
    }
    if(event.getSource()== item2){
      mainFrame.Second();
    }
    if(event.getSource() == item3){
      mainFrame.FirstPane(0);
    }
    if(event.getSource() == item4){
      mainFrame.FirstPane(1);
    }


  }
    public ArrayList<String> readFile(File theFile){
      int c;
      StringBuilder item = new StringBuilder();
      ArrayList<String> fil = new ArrayList<String>();
      BufferedReader br = null;
  		FileReader fr = null;
      try{
        fr = new FileReader(theFile);
    		br = new BufferedReader(fr);

        while ((c = br.read()) != -1) {
            if ((char)c == ';') {
              fil.add(new String(item));
              item = new StringBuilder();
            }
            else item.append((char)c);
          }

      } catch (IOException e) {
  			e.printStackTrace();

  		} finally {
  			try {
  				if (br != null)
  					br.close();
  				if (fr != null)
  					fr.close();
  			} catch (IOException ex) {
  				ex.printStackTrace();
  			}
  		}
      System.out.println(fil);
      String[] part = null;
      String machine_db = "";
      String voters = "";
      String candidates ="";
      for (int i=0; i<fil.size() ; i++){
        part = fil.get(i).split(":");
        if( i == 0) machine_db = part[1];
        if(i == 1 ) voters = part[1];
        if( i == 2 ) candidates = part[1];
      }

      ArrayList<String> dbs = new ArrayList<String>();
      dbs.add(machine_db);
      dbs.add(voters);
      dbs.add(candidates);
      return dbs;
    }
    public Block getBlock(){
      return blocky;
    }
}
