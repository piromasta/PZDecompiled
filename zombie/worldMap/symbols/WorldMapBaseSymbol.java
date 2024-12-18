package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Faction;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;

public abstract class WorldMapBaseSymbol {
   public static float DEFAULT_SCALE = 0.666F;
   WorldMapSymbols m_owner;
   WorldMapSymbolNetworkInfo m_networkInfo;
   float m_x;
   float m_y;
   float m_width;
   float m_height;
   float m_anchorX = 0.0F;
   float m_anchorY = 0.0F;
   float m_scale;
   float m_r;
   float m_g;
   float m_b;
   float m_a;
   boolean m_collide;
   boolean m_collided;
   float m_layoutX;
   float m_layoutY;
   boolean m_visible;
   protected static final ColorInfo s_tempColorInfo = new ColorInfo();

   public WorldMapBaseSymbol(WorldMapSymbols var1) {
      this.m_scale = DEFAULT_SCALE;
      this.m_collide = false;
      this.m_collided = false;
      this.m_visible = true;
      this.m_owner = var1;
   }

   public abstract WorldMapSymbols.WorldMapSymbolType getType();

   public void setNetworkInfo(WorldMapSymbolNetworkInfo var1) {
      this.m_networkInfo = var1;
   }

   public WorldMapSymbolNetworkInfo getNetworkInfo() {
      return this.m_networkInfo;
   }

   public void setPrivate() {
      this.m_networkInfo = null;
   }

   public boolean isShared() {
      return this.getNetworkInfo() != null;
   }

   public boolean isPrivate() {
      return this.getNetworkInfo() == null;
   }

   public boolean isAuthorLocalPlayer() {
      if (!GameServer.bServer && GameClient.bClient) {
         return !this.isShared() ? false : StringUtils.equals(GameClient.username, this.getNetworkInfo().getAuthor());
      } else {
         throw new IllegalStateException("not client");
      }
   }

   public void setAnchor(float var1, float var2) {
      this.m_anchorX = PZMath.clamp(var1, 0.0F, 1.0F);
      this.m_anchorY = PZMath.clamp(var2, 0.0F, 1.0F);
   }

   public void setPosition(float var1, float var2) {
      this.m_x = var1;
      this.m_y = var2;
   }

   public void setCollide(boolean var1) {
      this.m_collide = var1;
   }

   public void setRGBA(float var1, float var2, float var3, float var4) {
      this.m_r = PZMath.clamp_01(var1);
      this.m_g = PZMath.clamp_01(var2);
      this.m_b = PZMath.clamp_01(var3);
      this.m_a = PZMath.clamp_01(var4);
   }

   public void setScale(float var1) {
      this.m_scale = var1;
   }

   public float getDisplayScale(UIWorldMap var1) {
      if (this.m_scale <= 0.0F) {
         return this.m_scale;
      } else {
         return this.m_owner.getMiniMapSymbols() ? PZMath.min(this.m_owner.getLayoutWorldScale(), 1.0F) : this.m_owner.getLayoutWorldScale() * this.m_scale;
      }
   }

   public void layout(UIWorldMap var1, WorldMapSymbolCollisions var2, float var3, float var4) {
      float var5 = var1.getAPI().worldToUIX(this.m_x, this.m_y) - var3;
      float var6 = var1.getAPI().worldToUIY(this.m_x, this.m_y) - var4;
      this.m_layoutX = var5 - this.widthScaled(var1) * this.m_anchorX;
      this.m_layoutY = var6 - this.heightScaled(var1) * this.m_anchorY;
      this.m_collided = var2.addBox(this.m_layoutX, this.m_layoutY, this.widthScaled(var1), this.heightScaled(var1), this.m_collide);
      if (this.m_collided) {
      }

   }

   public float widthScaled(UIWorldMap var1) {
      return this.m_scale <= 0.0F ? this.m_width : this.m_width * this.getDisplayScale(var1);
   }

   public float heightScaled(UIWorldMap var1) {
      return this.m_scale <= 0.0F ? this.m_height : this.m_height * this.getDisplayScale(var1);
   }

   public void setVisible(boolean var1) {
      this.m_visible = var1;
   }

   public boolean isVisible() {
      if (this.isShared() && !this.getNetworkInfo().isVisibleToEveryone() && !this.isAuthorLocalPlayer()) {
         boolean var1 = false;
         if (this.getNetworkInfo().isVisibleToFaction() && Faction.getPlayerFaction(GameClient.username) != null && Faction.getPlayerFaction(GameClient.username) == Faction.getPlayerFaction(this.getNetworkInfo().getAuthor())) {
            var1 = true;
         }

         if (this.getNetworkInfo().isVisibleToSafehouse() && SafeHouse.hasSafehouse(GameClient.username) != null && SafeHouse.hasSafehouse(GameClient.username) == SafeHouse.hasSafehouse(this.getNetworkInfo().getAuthor())) {
            var1 = true;
         }

         if (this.getNetworkInfo().hasPlayer(GameClient.username)) {
            var1 = true;
         }

         if (!var1) {
            return false;
         }
      }

      return this.m_visible;
   }

   public void save(ByteBuffer var1) throws IOException {
      var1.putFloat(this.m_x);
      var1.putFloat(this.m_y);
      var1.putFloat(this.m_anchorX);
      var1.putFloat(this.m_anchorY);
      var1.putFloat(this.m_scale);
      var1.put((byte)((int)(this.m_r * 255.0F)));
      var1.put((byte)((int)(this.m_g * 255.0F)));
      var1.put((byte)((int)(this.m_b * 255.0F)));
      var1.put((byte)((int)(this.m_a * 255.0F)));
      var1.put((byte)(this.m_collide ? 1 : 0));
   }

   public void load(ByteBuffer var1, int var2, int var3) throws IOException {
      this.m_x = var1.getFloat();
      this.m_y = var1.getFloat();
      this.m_anchorX = var1.getFloat();
      this.m_anchorY = var1.getFloat();
      this.m_scale = var1.getFloat();
      this.m_r = (float)(var1.get() & 255) / 255.0F;
      this.m_g = (float)(var1.get() & 255) / 255.0F;
      this.m_b = (float)(var1.get() & 255) / 255.0F;
      this.m_a = (float)(var1.get() & 255) / 255.0F;
      this.m_collide = var1.get() == 1;
   }

   public abstract void render(UIWorldMap var1, float var2, float var3);

   void renderCollided(UIWorldMap var1, float var2, float var3) {
      float var4 = var2 + this.m_layoutX + this.widthScaled(var1) / 2.0F;
      float var5 = var3 + this.m_layoutY + this.heightScaled(var1) / 2.0F;
      var1.DrawTextureScaledCol((Texture)null, (double)(var4 - 3.0F), (double)(var5 - 3.0F), 6.0, 6.0, (double)this.m_r, (double)this.m_g, (double)this.m_b, (double)this.m_a);
   }

   ColorInfo getColor(UIWorldMap var1, ColorInfo var2) {
      return this.isPrivate() && var1.getAPIv2().isDimUnsharedSymbols() ? var2.set(0.25F, 0.25F, 0.25F, 0.5F) : var2.set(this.m_r, this.m_g, this.m_b, this.m_a);
   }

   public abstract void release();
}
