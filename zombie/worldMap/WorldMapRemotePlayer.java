package zombie.worldMap;

import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.network.GameClient;
import zombie.network.ServerOptions;

public final class WorldMapRemotePlayer {
   private short changeCount = 0;
   private final short OnlineID;
   private String username = "???";
   private String forename = "???";
   private String surname = "???";
   private String accessLevel = "None";
   private float x;
   private float y;
   private boolean invisible = false;
   private boolean disguised = false;
   private boolean bHasFullData = false;

   public WorldMapRemotePlayer(short var1) {
      this.OnlineID = var1;
   }

   public void setPlayer(IsoPlayer var1) {
      boolean var2 = false;
      if (!this.username.equals(var1.username)) {
         this.username = var1.username;
         var2 = true;
      }

      if (!this.forename.equals(var1.getDescriptor().getForename())) {
         this.forename = var1.getDescriptor().getForename();
         var2 = true;
      }

      if (!this.surname.equals(var1.getDescriptor().getSurname())) {
         this.surname = var1.getDescriptor().getSurname();
         var2 = true;
      }

      if (!this.accessLevel.equals(var1.getRole().getName())) {
         this.accessLevel = var1.getRole().getName();
         var2 = true;
      }

      this.x = var1.getX();
      this.y = var1.getY();
      if (this.invisible != var1.isInvisible()) {
         this.invisible = var1.isInvisible();
         var2 = true;
      }

      if (this.disguised != var1.usernameDisguised) {
         this.disguised = var1.usernameDisguised;
         var2 = true;
      }

      if (var2) {
         ++this.changeCount;
      }

   }

   public void setFullData(short var1, String var2, String var3, String var4, String var5, float var6, float var7, boolean var8, boolean var9) {
      this.changeCount = var1;
      this.username = var2;
      this.forename = var3;
      this.surname = var4;
      this.accessLevel = var5;
      this.x = var6;
      this.y = var7;
      this.invisible = var8;
      this.disguised = var9;
      this.bHasFullData = true;
   }

   public void setPosition(float var1, float var2) {
      this.x = var1;
      this.y = var2;
   }

   public short getOnlineID() {
      return this.OnlineID;
   }

   public String getForename() {
      return this.forename;
   }

   public String getSurname() {
      return this.surname;
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public short getChangeCount() {
      return this.changeCount;
   }

   public boolean isInvisible() {
      return this.invisible;
   }

   public boolean isDisguised() {
      return this.disguised;
   }

   public boolean hasFullData() {
      return this.bHasFullData;
   }

   public String getUsername(Boolean var1) {
      String var2 = this.username;
      if (this.disguised) {
         var2 = Translator.getText("IGUI_Disguised_Player_Name");
      } else if (var1 && GameClient.bClient && ServerOptions.instance.ShowFirstAndLastName.getValue() && this.isAccessLevel("None")) {
         var2 = this.forename + " " + this.surname;
         if (ServerOptions.instance.DisplayUserName.getValue()) {
            var2 = var2 + " (" + this.username + ")";
         }
      }

      return var2;
   }

   public String getUsername() {
      return this.getUsername(false);
   }

   public String getAccessLevel() {
      String var10000;
      switch (this.accessLevel) {
         case "admin":
            var10000 = "Admin";
            break;
         case "moderator":
            var10000 = "Moderator";
            break;
         case "overseer":
            var10000 = "Overseer";
            break;
         case "gm":
            var10000 = "GM";
            break;
         case "observer":
            var10000 = "Observer";
            break;
         default:
            var10000 = "None";
      }

      return var10000;
   }

   public boolean isAccessLevel(String var1) {
      return this.getAccessLevel().equalsIgnoreCase(var1);
   }
}
