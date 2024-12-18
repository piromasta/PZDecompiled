package zombie.network;

import java.nio.ByteBuffer;
import zombie.GameWindow;

public class Userlog {
   private final String username;
   private final String type;
   private final String text;
   private final String issuedBy;
   private final String lastUpdate;
   private int amount;

   public Userlog(String var1, String var2, String var3, String var4, int var5, String var6) {
      this.username = var1;
      this.type = var2;
      this.text = var3;
      this.issuedBy = var4;
      this.amount = var5;
      this.lastUpdate = var6;
   }

   public String getUsername() {
      return this.username;
   }

   public String getType() {
      return this.type;
   }

   public String getText() {
      return this.text;
   }

   public String getIssuedBy() {
      return this.issuedBy;
   }

   public int getAmount() {
      return this.amount;
   }

   public void setAmount(int var1) {
      this.amount = var1;
   }

   public String getLastUpdate() {
      return this.lastUpdate;
   }

   public void write(ByteBuffer var1) {
      GameWindow.WriteStringUTF(var1, this.username);
      GameWindow.WriteStringUTF(var1, this.type);
      GameWindow.WriteStringUTF(var1, this.text);
      GameWindow.WriteStringUTF(var1, this.issuedBy);
      GameWindow.WriteStringUTF(var1, this.lastUpdate);
      var1.putInt(this.amount);
   }

   public Userlog(ByteBuffer var1) {
      this.username = GameWindow.ReadString(var1);
      this.type = GameWindow.ReadString(var1);
      this.text = GameWindow.ReadString(var1);
      this.issuedBy = GameWindow.ReadString(var1);
      this.lastUpdate = GameWindow.ReadString(var1);
      this.amount = var1.getInt();
   }

   public static enum UserlogType {
      AdminLog(0),
      Kicked(1),
      Banned(2),
      DupeItem(3),
      LuaChecksum(4),
      WarningPoint(5),
      UnauthorizedPacket(6),
      SuspiciousActivity(7);

      private final int index;

      private UserlogType(int var3) {
         this.index = var3;
      }

      public int index() {
         return this.index;
      }

      public static UserlogType fromIndex(int var0) {
         return ((UserlogType[])UserlogType.class.getEnumConstants())[var0];
      }

      public static UserlogType FromString(String var0) {
         return valueOf(var0);
      }
   }
}
