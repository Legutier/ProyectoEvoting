/*
Consola de votación
*/

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.*;
import java.nio.*;

public class VoteView {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               MainFrame frame = new MainFrame("127.0.0.1",args[0],"127.0.0.1",args[1]);
               frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
               frame.setLocationRelativeTo(null); // Centrado en la pantalla
               frame.setVisible(true);



            } // run ends
         });
    }
  }

  class MainFrame extends JFrame {

     public MainFrame(String  addr, String port, String p_addr, String p_port) {
        setTitle("Consola de Votacion");
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setLayout(new BorderLayout());
        contentPane = getContentPane();
        make = new MakeVote();
        make.SetAll();
        nodtool = new Node("0", addr, port, "Console"); // crea un nodo como auxiliar, notar que nunca lo inicializa, solo se usa la info del peer
        toPush = new Peer("0", p_addr,p_port, "Worker"); // crea un peer con la info del worker a conectarse, para hacer un push de los votos emitidos en esta consola
        System.out.println(nodtool.SendMsg(toPush,"00ID"));


        RutLog();
        this.repaint();
        this.revalidate();

    }
    // metodo inicial que pregunta el rut del votante
    public void RutLog(){
      contentPane.removeAll();
      login.removeAll();
      Optimus.removeAll();
      Bodoque.removeAll();
      Button.removeAll();
      notloged.removeAll();
      JLabel label = new JLabel("Escriba su RUT:");
      JPasswordField pass = new JPasswordField(10);
      login.add(label);
      login.add(pass);
      contentPane.add(login);
      contentPane.repaint();
      contentPane.revalidate();
      String[] options = new String[]{"OK"};
      int option = JOptionPane.showOptionDialog(null, login, "Acceso a consola de votacion",
                         JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
                         null, options, options[0]);
      // si se ingresa a la consola se muestra los candodatos, en este caso, juancarlos bodoque y optimus prime
      if(option == 0) {
        notloged.removeAll();
        char[] password = pass.getPassword();
        addr = make.DoHash(new String(password));
        contentPane.remove(login);
        JRadioButton option1 = new JRadioButton("Juan Carlos Bodoque");
        JRadioButton option2 = new JRadioButton("Optimus Prime");
        JLabel imagen1 = new JLabel(bodoque);
        JLabel imagen2 = new JLabel(optimus);
        JButton submit = new JButton("Enviar voto");
        ButtonGroup group = new ButtonGroup();
        // votones de opcion, BBBB es JC vodoque y OOOO es optimus prime
        option1.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent event){
           candidate = "BBBB";
         }
       });
        option2.addActionListener(new ActionListener(){
           @Override
           public void actionPerformed(ActionEvent event){
             candidate = "OOOO";
           }
         });
         submit.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent event){
              submit_vote(); // cada vez que se envia un voto se almacena y se llama a este metodo

            }
          });

        GridLayout lay = new GridLayout(3,3);
        group.add(option1);
        group.add(option2);
        Bodoque.add(imagen1);
        Bodoque.add(option1);
        Optimus.add(imagen2);
        Optimus.add(option2);
        Button.add(submit);
        contentPane.setLayout(lay);
        contentPane.add(Optimus);
        contentPane.add(Bodoque);
        contentPane.add(Button);
        contentPane.repaint();
        contentPane.revalidate();
      }
      else{
        // en caso de que escriba un rut no valido o cierre la ventana
        JLabel notlog_label = new JLabel("No entró al sistema, intente denuevo");
        JButton nolog = new JButton("Ok");
        nolog.addActionListener(new ActionListener(){
         @Override
         public void actionPerformed(ActionEvent event){
           RutLog();
         }
       });
       notloged.add(notlog_label);
       notloged.add(nolog);
       contentPane.add(notloged);
       contentPane.repaint();
       contentPane.revalidate();
      }

    }
    public void submit_vote(){
      // agrega el voto y si luego hace un push de el al worker seleccionado anteriormente
      make.pushAVote(candidate,addr);
      String vvotes;
      if(make.getVotes().size()> 0){
        vvotes = make.EncodeInput();
        System.out.println("\n\n"+vvotes+"\n wtf?!");
        String mms = nodtool.SendMsg(toPush,"40"+vvotes);
        System.out.println(mms);
      }
      RutLog();


    }
    Peer toPush;
    Node nodtool ;
    private MakeVote make;
    private Container contentPane;
    public static final int DEFAULT_WIDTH = 600;
    public static final int DEFAULT_HEIGHT = 500;
    public final Icon bodoque = new ImageIcon(getClass().getResource("images/Bodoque.jpg"));
    public final Icon optimus = new ImageIcon(getClass().getResource("images/Optimus.jpg"));
    private static String candidate = "";
    private String addr;
    private JPanel login = new JPanel();
    private JPanel Bodoque = new JPanel();
    private JPanel notloged = new JPanel();
    private JPanel Optimus = new JPanel();
    private JPanel Button = new JPanel();

  }
