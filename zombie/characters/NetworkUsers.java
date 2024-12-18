package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class NetworkUsers {
   public static NetworkUsers instance = new NetworkUsers();
   public ArrayList<NetworkUser> users = new ArrayList();

   public NetworkUsers() {
   }

   public ArrayList<NetworkUser> getUsers() {
      return this.users;
   }

   public NetworkUser getUser(String var1) {
      Iterator var2 = this.users.iterator();

      NetworkUser var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (NetworkUser)var2.next();
      } while(!var1.equals(var3.username));

      return var3;
   }

   public static void send(ByteBuffer var0, Collection<NetworkUser> var1) {
      var0.putInt(var1.size());
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         NetworkUser var3 = (NetworkUser)var2.next();
         var3.send(var0);
      }

   }

   public void parse(ByteBuffer var1) {
      this.users.clear();
      int var2 = var1.getInt();

      for(int var3 = 0; var3 < var2; ++var3) {
         NetworkUser var4 = new NetworkUser();
         var4.parse(var1);
         this.users.add(var4);
      }

   }
}
