package zombie.network;

import java.util.ArrayList;
import java.util.HashMap;

public class DBResult {
   private HashMap<String, String> values = new HashMap();
   private ArrayList<String> columns = new ArrayList();
   private String type;
   private String tableName;

   public DBResult() {
   }

   public HashMap<String, String> getValues() {
      return this.values;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String var1) {
      this.type = var1;
   }

   public ArrayList<String> getColumns() {
      return this.columns;
   }

   public void setColumns(ArrayList<String> var1) {
      this.columns = var1;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String var1) {
      this.tableName = var1;
   }
}
