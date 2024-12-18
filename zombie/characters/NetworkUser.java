package zombie.characters;

import java.nio.ByteBuffer;
import zombie.GameWindow;

public class NetworkUser {
   public boolean inWhitelist;
   public String world;
   public String username;
   public String lastConnection;
   public Role role;
   public AuthType authType;
   public String steamid;
   public String displayName;
   public boolean online;
   public int warningPoints;
   public int suspicionPoints;
   public int kicks;

   public NetworkUser() {
   }

   public NetworkUser(String var1, String var2, String var3, String var4, int var5, String var6, String var7, boolean var8) {
      this.inWhitelist = false;
      this.world = var1;
      this.username = var2;
      this.lastConnection = var3;
      this.role = Roles.getRole(var4);
      switch (var5) {
         case 1:
            this.authType = NetworkUser.AuthType.password;
            break;
         case 2:
            this.authType = NetworkUser.AuthType.google_auth;
            break;
         case 3:
            this.authType = NetworkUser.AuthType.two_factor;
      }

      this.steamid = var6;
      this.displayName = var7;
      this.online = var8;
      this.warningPoints = 0;
      this.suspicionPoints = 0;
      this.kicks = 0;
   }

   public String getWorld() {
      return this.world;
   }

   public String getUsername() {
      return this.username;
   }

   public String getLastConnection() {
      return this.lastConnection;
   }

   public Role getRole() {
      return this.role;
   }

   public AuthType getAuthType() {
      return this.authType;
   }

   public String getAuthTypeName() {
      return this.authType == null ? "-" : this.authType.name();
   }

   public String getSteamid() {
      return this.steamid;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isOnline() {
      return this.online;
   }

   public void setWarningPoints(int var1) {
      this.warningPoints = var1;
   }

   public int getWarningPoints() {
      return this.warningPoints;
   }

   public void setSuspicionPoints(int var1) {
      this.suspicionPoints = var1;
   }

   public int getSuspicionPoints() {
      return this.suspicionPoints;
   }

   public void setKicks(int var1) {
      this.kicks = var1;
   }

   public int getKicks() {
      return this.kicks;
   }

   public void setInWhitelist(boolean var1) {
      this.inWhitelist = var1;
   }

   public boolean isInWhitelist() {
      return this.inWhitelist;
   }

   public void send(ByteBuffer var1) {
      GameWindow.WriteStringUTF(var1, this.world);
      GameWindow.WriteStringUTF(var1, this.username);
      GameWindow.WriteStringUTF(var1, this.lastConnection);
      GameWindow.WriteStringUTF(var1, this.role.getName());
      var1.put((byte)this.authType.ordinal());
      GameWindow.WriteStringUTF(var1, this.steamid);
      GameWindow.WriteStringUTF(var1, this.displayName);
      var1.put((byte)(this.online ? 1 : 0));
      var1.putInt(this.warningPoints);
      var1.putInt(this.suspicionPoints);
      var1.putInt(this.kicks);
      var1.put((byte)(this.inWhitelist ? 1 : 0));
   }

   public void parse(ByteBuffer var1) {
      this.world = GameWindow.ReadString(var1);
      this.username = GameWindow.ReadString(var1);
      this.lastConnection = GameWindow.ReadString(var1);
      String var2 = GameWindow.ReadString(var1);
      this.role = Roles.getRole(var2);
      this.authType = NetworkUser.AuthType.values()[var1.get()];
      this.steamid = GameWindow.ReadString(var1);
      this.displayName = GameWindow.ReadString(var1);
      this.online = var1.get() > 0;
      this.warningPoints = var1.getInt();
      this.suspicionPoints = var1.getInt();
      this.kicks = var1.getInt();
      this.inWhitelist = var1.get() > 0;
   }

   public static enum AuthType {
      password,
      google_auth,
      two_factor;

      private AuthType() {
      }
   }
}
