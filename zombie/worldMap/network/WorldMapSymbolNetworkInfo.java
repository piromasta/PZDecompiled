package zombie.worldMap.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public final class WorldMapSymbolNetworkInfo {
   private int m_id;
   private String m_author;
   private boolean m_bEveryone = false;
   private boolean m_bFaction = false;
   private boolean m_bSafehouse;
   private ArrayList<String> m_players = null;

   public WorldMapSymbolNetworkInfo() {
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else {
         WorldMapSymbolNetworkInfo var2 = (WorldMapSymbolNetworkInfo)Type.tryCastTo(var1, WorldMapSymbolNetworkInfo.class);
         if (var2 == null) {
            return false;
         } else if (!this.m_author.equals(var2.m_author)) {
            return false;
         } else {
            return this.m_bEveryone == var2.m_bEveryone && this.m_bFaction == var2.m_bFaction && this.m_bSafehouse == var2.m_bSafehouse ? Objects.equals(this.m_players, var2.m_players) : false;
         }
      }
   }

   public void setID(int var1) {
      this.m_id = var1;
   }

   public int getID() {
      return this.m_id;
   }

   public String getAuthor() {
      return this.m_author;
   }

   public void setAuthor(String var1) {
      if (StringUtils.isNullOrWhitespace(var1)) {
         throw new IllegalStateException("username can't be null or empty");
      } else {
         this.m_author = var1;
      }
   }

   public boolean isVisibleToEveryone() {
      return this.m_bEveryone;
   }

   public void setVisibleToEveryone(boolean var1) {
      this.m_bEveryone = var1;
   }

   public boolean isVisibleToFaction() {
      return this.m_bFaction;
   }

   public void setVisibleToFaction(boolean var1) {
      this.m_bFaction = var1;
   }

   public boolean isVisibleToSafehouse() {
      return this.m_bSafehouse;
   }

   public void setVisibleToSafehouse(boolean var1) {
      this.m_bSafehouse = var1;
   }

   public void addPlayer(String var1) {
      if (!StringUtils.isNullOrWhitespace(var1)) {
         if (!this.hasPlayer(var1)) {
            if (this.m_players == null) {
               this.m_players = new ArrayList();
            }

            this.m_players.add(var1);
         }
      }
   }

   public int getPlayerCount() {
      return this.m_players == null ? 0 : this.m_players.size();
   }

   public String getPlayerByIndex(int var1) {
      return (String)this.m_players.get(var1);
   }

   public boolean hasPlayer(String var1) {
      return this.m_players == null ? false : this.m_players.contains(var1);
   }

   public void clearPlayers() {
      if (this.m_players != null) {
         this.m_players.clear();
      }
   }

   public void save(ByteBuffer var1) throws IOException {
      var1.putInt(this.m_id);
      GameWindow.WriteStringUTF(var1, this.m_author);
      BitHeaderWrite var2 = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, var1);
      if (this.m_bEveryone) {
         var2.addFlags(1);
      }

      if (this.m_bFaction) {
         var2.addFlags(2);
      }

      if (this.m_bSafehouse) {
         var2.addFlags(4);
      }

      if (this.getPlayerCount() > 0) {
         var2.addFlags(8);
         var1.put((byte)this.getPlayerCount());

         for(int var3 = 0; var3 < this.m_players.size(); ++var3) {
            GameWindow.WriteStringUTF(var1, (String)this.m_players.get(var3));
         }
      }

      var2.write();
      var2.release();
   }

   public void load(ByteBuffer var1, int var2, int var3) throws IOException {
      this.m_id = var1.getInt();
      this.m_author = GameWindow.ReadStringUTF(var1);
      BitHeaderRead var4 = BitHeader.allocRead(BitHeader.HeaderSize.Byte, var1);
      this.m_bEveryone = var4.hasFlags(1);
      this.m_bFaction = var4.hasFlags(2);
      this.m_bSafehouse = var4.hasFlags(4);
      this.clearPlayers();
      if (var4.hasFlags(8)) {
         byte var5 = var1.get();

         for(int var6 = 0; var6 < var5; ++var6) {
            this.addPlayer(GameWindow.ReadStringUTF(var1));
         }
      }

      var4.release();
   }
}
