import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;



public class Read{

  public static void main(String[] args) {
    String FILENAME = "./example.tx";
    int c;
    StringBuilder item = new StringBuilder();
    ArrayList<String> fil = new ArrayList<String>();
    BufferedReader br = null;
		FileReader fr = null;
    try{
      fr = new FileReader(FILENAME);
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
    String machine = fil.get(0);
    String[] part = machine.split(":");
    String key = part[0];
    String value = part[1];
    String[] part_mac = value.split(",");
    System.out.println(part_mac[1]);
  }
}
