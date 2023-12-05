package zombie.iso;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.debug.LineDrawer;
import zombie.network.GameServer;
import zombie.util.Type;

public final class IsoMarkers {
   public static final IsoMarkers instance = new IsoMarkers();
   private static int NextIsoMarkerID = 0;
   private final List<IsoMarker> markers = new ArrayList();
   private final List<CircleIsoMarker> circlemarkers = new ArrayList();
   private static int NextCircleIsoMarkerID = 0;

   private IsoMarkers() {
   }

   public void init() {
   }

   public void reset() {
      this.markers.clear();
      this.circlemarkers.clear();
   }

   public void update() {
      if (!GameServer.bServer) {
         this.updateIsoMarkers();
         this.updateCircleIsoMarkers();
      }
   }

   private void updateIsoMarkers() {
      if (IsoCamera.frameState.playerIndex == 0) {
         if (this.markers.size() != 0) {
            int var1;
            for(var1 = this.markers.size() - 1; var1 >= 0; --var1) {
               if (((IsoMarker)this.markers.get(var1)).isRemoved()) {
                  if (((IsoMarker)this.markers.get(var1)).hasTempSquareObject()) {
                     ((IsoMarker)this.markers.get(var1)).removeTempSquareObjects();
                  }

                  this.markers.remove(var1);
               }
            }

            for(var1 = 0; var1 < this.markers.size(); ++var1) {
               IsoMarker var2 = (IsoMarker)this.markers.get(var1);
               if (var2.alphaInc) {
                  var2.alpha += GameTime.getInstance().getMultiplier() * var2.fadeSpeed;
                  if (var2.alpha > var2.alphaMax) {
                     var2.alphaInc = false;
                     var2.alpha = var2.alphaMax;
                  }
               } else {
                  var2.alpha -= GameTime.getInstance().getMultiplier() * var2.fadeSpeed;
                  if (var2.alpha < var2.alphaMin) {
                     var2.alphaInc = true;
                     var2.alpha = 0.3F;
                  }
               }
            }

         }
      }
   }

   public boolean removeIsoMarker(IsoMarker var1) {
      return this.removeIsoMarker(var1.getID());
   }

   public boolean removeIsoMarker(int var1) {
      for(int var2 = this.markers.size() - 1; var2 >= 0; --var2) {
         if (((IsoMarker)this.markers.get(var2)).getID() == var1) {
            ((IsoMarker)this.markers.get(var2)).remove();
            this.markers.remove(var2);
            return true;
         }
      }

      return false;
   }

   public IsoMarker getIsoMarker(int var1) {
      for(int var2 = 0; var2 < this.markers.size(); ++var2) {
         if (((IsoMarker)this.markers.get(var2)).getID() == var1) {
            return (IsoMarker)this.markers.get(var2);
         }
      }

      return null;
   }

   public IsoMarker addIsoMarker(String var1, IsoGridSquare var2, float var3, float var4, float var5, boolean var6, boolean var7) {
      if (GameServer.bServer) {
         return null;
      } else {
         IsoMarker var8 = new IsoMarker();
         var8.setSquare(var2);
         var8.init(var1, var2.x, var2.y, var2.z, var2, var7);
         var8.setR(var3);
         var8.setG(var4);
         var8.setB(var5);
         var8.setA(1.0F);
         var8.setDoAlpha(var6);
         var8.setFadeSpeed(0.006F);
         var8.setAlpha(1.0F);
         var8.setAlphaMin(0.3F);
         var8.setAlphaMax(1.0F);
         this.markers.add(var8);
         return var8;
      }
   }

   public IsoMarker addIsoMarker(KahluaTable var1, KahluaTable var2, IsoGridSquare var3, float var4, float var5, float var6, boolean var7, boolean var8) {
      return this.addIsoMarker(var1, var2, var3, var4, var5, var6, var7, var8, 0.006F, 0.3F, 1.0F);
   }

   public IsoMarker addIsoMarker(KahluaTable var1, KahluaTable var2, IsoGridSquare var3, float var4, float var5, float var6, boolean var7, boolean var8, float var9, float var10, float var11) {
      if (GameServer.bServer) {
         return null;
      } else {
         IsoMarker var12 = new IsoMarker();
         var12.init(var1, var2, var3.x, var3.y, var3.z, var3, var8);
         var12.setSquare(var3);
         var12.setR(var4);
         var12.setG(var5);
         var12.setB(var6);
         var12.setA(1.0F);
         var12.setDoAlpha(var7);
         var12.setFadeSpeed(var9);
         var12.setAlpha(0.0F);
         var12.setAlphaMin(var10);
         var12.setAlphaMax(var11);
         this.markers.add(var12);
         return var12;
      }
   }

   public void renderIsoMarkers(IsoCell.PerPlayerRender var1, int var2, int var3) {
      if (!GameServer.bServer && this.markers.size() != 0) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null) {
            for(int var5 = 0; var5 < this.markers.size(); ++var5) {
               IsoMarker var6 = (IsoMarker)this.markers.get(var5);
               if (var6.z == (float)var2 && var6.z == var4.getZ() && var6.active) {
                  for(int var7 = 0; var7 < var6.textures.size(); ++var7) {
                     Texture var8 = (Texture)var6.textures.get(var7);
                     float var9 = IsoUtils.XToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffX() - (float)var8.getWidth() / 2.0F;
                     float var10 = IsoUtils.YToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffY() - (float)var8.getHeight();
                     SpriteRenderer.instance.render(var8, var9, var10, (float)var8.getWidth(), (float)var8.getHeight(), var6.r, var6.g, var6.b, var6.alpha, (Consumer)null);
                  }
               }
            }

         }
      }
   }

   public void renderIsoMarkersDeferred(IsoCell.PerPlayerRender var1, int var2, int var3) {
      if (!GameServer.bServer && this.markers.size() != 0) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null) {
            for(int var5 = 0; var5 < this.markers.size(); ++var5) {
               IsoMarker var6 = (IsoMarker)this.markers.get(var5);
               if (var6.z == (float)var2 && var6.z == var4.getZ() && var6.active) {
                  for(int var7 = 0; var7 < var6.overlayTextures.size(); ++var7) {
                     Texture var8 = (Texture)var6.overlayTextures.get(var7);
                     float var9 = IsoUtils.XToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffX() - (float)var8.getWidth() / 2.0F;
                     float var10 = IsoUtils.YToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffY() - (float)var8.getHeight();
                     SpriteRenderer.instance.render(var8, var9, var10, (float)var8.getWidth(), (float)var8.getHeight(), var6.r, var6.g, var6.b, var6.alpha, (Consumer)null);
                  }
               }
            }

         }
      }
   }

   public void renderIsoMarkersOnSquare(IsoCell.PerPlayerRender var1, int var2, int var3) {
      if (!GameServer.bServer && this.markers.size() != 0) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null) {
            for(int var5 = 0; var5 < this.markers.size(); ++var5) {
               IsoMarker var6 = (IsoMarker)this.markers.get(var5);
               if (var6.z == (float)var2 && var6.z == var4.getZ() && var6.active) {
                  for(int var7 = 0; var7 < var6.overlayTextures.size(); ++var7) {
                     Texture var8 = (Texture)var6.overlayTextures.get(var7);
                     float var9 = IsoUtils.XToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffX() - (float)var8.getWidth() / 2.0F;
                     float var10 = IsoUtils.YToScreen(var6.x, var6.y, var6.z, 0) - IsoCamera.cameras[var3].getOffY() - (float)var8.getHeight();
                     SpriteRenderer.instance.render(var8, var9, var10, (float)var8.getWidth(), (float)var8.getHeight(), var6.r, var6.g, var6.b, var6.alpha, (Consumer)null);
                  }
               }
            }

         }
      }
   }

   private void updateCircleIsoMarkers() {
      if (IsoCamera.frameState.playerIndex == 0) {
         if (this.circlemarkers.size() != 0) {
            int var1;
            for(var1 = this.circlemarkers.size() - 1; var1 >= 0; --var1) {
               if (((CircleIsoMarker)this.circlemarkers.get(var1)).isRemoved()) {
                  this.circlemarkers.remove(var1);
               }
            }

            for(var1 = 0; var1 < this.circlemarkers.size(); ++var1) {
               CircleIsoMarker var2 = (CircleIsoMarker)this.circlemarkers.get(var1);
               if (var2.alphaInc) {
                  var2.alpha += GameTime.getInstance().getMultiplier() * var2.fadeSpeed;
                  if (var2.alpha > var2.alphaMax) {
                     var2.alphaInc = false;
                     var2.alpha = var2.alphaMax;
                  }
               } else {
                  var2.alpha -= GameTime.getInstance().getMultiplier() * var2.fadeSpeed;
                  if (var2.alpha < var2.alphaMin) {
                     var2.alphaInc = true;
                     var2.alpha = 0.3F;
                  }
               }
            }

         }
      }
   }

   public boolean removeCircleIsoMarker(CircleIsoMarker var1) {
      return this.removeCircleIsoMarker(var1.getID());
   }

   public boolean removeCircleIsoMarker(int var1) {
      for(int var2 = this.circlemarkers.size() - 1; var2 >= 0; --var2) {
         if (((CircleIsoMarker)this.circlemarkers.get(var2)).getID() == var1) {
            ((CircleIsoMarker)this.circlemarkers.get(var2)).remove();
            this.circlemarkers.remove(var2);
            return true;
         }
      }

      return false;
   }

   public CircleIsoMarker getCircleIsoMarker(int var1) {
      for(int var2 = 0; var2 < this.circlemarkers.size(); ++var2) {
         if (((CircleIsoMarker)this.circlemarkers.get(var2)).getID() == var1) {
            return (CircleIsoMarker)this.circlemarkers.get(var2);
         }
      }

      return null;
   }

   public CircleIsoMarker addCircleIsoMarker(IsoGridSquare var1, float var2, float var3, float var4, float var5) {
      if (GameServer.bServer) {
         return null;
      } else {
         CircleIsoMarker var6 = new CircleIsoMarker();
         var6.init(var1.x, var1.y, var1.z, var1);
         var6.setSquare(var1);
         var6.setR(var2);
         var6.setG(var3);
         var6.setB(var4);
         var6.setA(var5);
         var6.setDoAlpha(false);
         var6.setFadeSpeed(0.006F);
         var6.setAlpha(1.0F);
         var6.setAlphaMin(1.0F);
         var6.setAlphaMax(1.0F);
         this.circlemarkers.add(var6);
         return var6;
      }
   }

   public void renderCircleIsoMarkers(IsoCell.PerPlayerRender var1, int var2, int var3) {
      if (!GameServer.bServer && this.circlemarkers.size() != 0) {
         IsoPlayer var4 = IsoPlayer.players[var3];
         if (var4 != null) {
            for(int var5 = 0; var5 < this.circlemarkers.size(); ++var5) {
               CircleIsoMarker var6 = (CircleIsoMarker)this.circlemarkers.get(var5);
               if (var6.z == (float)var2 && var6.z == var4.getZ() && var6.active) {
                  LineDrawer.DrawIsoCircle(var6.x, var6.y, var6.z, var6.size, 32, var6.r, var6.g, var6.b, var6.a);
               }
            }

         }
      }
   }

   public void render() {
      this.update();
   }

   public static final class IsoMarker {
      private int ID;
      private ArrayList<Texture> textures = new ArrayList();
      private ArrayList<Texture> overlayTextures = new ArrayList();
      private ArrayList<IsoObject> tempObjects = new ArrayList();
      private IsoGridSquare square;
      private float x;
      private float y;
      private float z;
      private float r;
      private float g;
      private float b;
      private float a;
      private boolean doAlpha;
      private float fadeSpeed = 0.006F;
      private float alpha = 0.0F;
      private float alphaMax = 1.0F;
      private float alphaMin = 0.3F;
      private boolean alphaInc = true;
      private boolean active = true;
      private boolean isRemoved = false;

      public IsoMarker() {
         this.ID = IsoMarkers.NextIsoMarkerID++;
      }

      public int getID() {
         return this.ID;
      }

      public void remove() {
         this.isRemoved = true;
      }

      public boolean isRemoved() {
         return this.isRemoved;
      }

      public void init(KahluaTable var1, KahluaTable var2, int var3, int var4, int var5, IsoGridSquare var6) {
         this.square = var6;
         int var7;
         int var8;
         String var9;
         Texture var10;
         if (var1 != null) {
            var7 = var1.len();

            for(var8 = 1; var8 <= var7; ++var8) {
               var9 = (String)Type.tryCastTo(var1.rawget(var8), String.class);
               var10 = Texture.trygetTexture(var9);
               if (var10 != null) {
                  this.textures.add(var10);
                  this.setPos(var3, var4, var5);
               }
            }
         }

         if (var2 != null) {
            var7 = var2.len();

            for(var8 = 1; var8 <= var7; ++var8) {
               var9 = (String)Type.tryCastTo(var2.rawget(var8), String.class);
               var10 = Texture.trygetTexture(var9);
               if (var10 != null) {
                  this.overlayTextures.add(var10);
                  this.setPos(var3, var4, var5);
               }
            }
         }

      }

      public void init(KahluaTable var1, KahluaTable var2, int var3, int var4, int var5, IsoGridSquare var6, boolean var7) {
         this.square = var6;
         if (var7) {
            if (var1 != null) {
               int var8 = var1.len();

               for(int var9 = 1; var9 <= var8; ++var9) {
                  String var10 = (String)Type.tryCastTo(var1.rawget(var9), String.class);
                  Texture var11 = Texture.trygetTexture(var10);
                  if (var11 != null) {
                     IsoObject var12 = new IsoObject(var6.getCell(), var6, var11.getName());
                     this.tempObjects.add(var12);
                     this.addTempSquareObject(var12);
                     this.setPos(var3, var4, var5);
                  }
               }
            }
         } else {
            this.init(var1, var2, var3, var4, var5, var6);
         }

      }

      public void init(String var1, int var2, int var3, int var4, IsoGridSquare var5, boolean var6) {
         this.square = var5;
         if (var6 && var1 != null) {
            IsoObject var7 = IsoObject.getNew(var5, var1, var1, false);
            this.tempObjects.add(var7);
            this.addTempSquareObject(var7);
            this.setPos(var2, var3, var4);
         }

      }

      public boolean hasTempSquareObject() {
         return this.tempObjects.size() > 0;
      }

      public void addTempSquareObject(IsoObject var1) {
         this.square.localTemporaryObjects.add(var1);
      }

      public void removeTempSquareObjects() {
         this.square.localTemporaryObjects.clear();
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }

      public float getZ() {
         return this.z;
      }

      public float getR() {
         return this.r;
      }

      public float getG() {
         return this.g;
      }

      public float getB() {
         return this.b;
      }

      public float getA() {
         return this.a;
      }

      public void setR(float var1) {
         this.r = var1;
      }

      public void setG(float var1) {
         this.g = var1;
      }

      public void setB(float var1) {
         this.b = var1;
      }

      public void setA(float var1) {
         this.a = var1;
      }

      public float getAlpha() {
         return this.alpha;
      }

      public void setAlpha(float var1) {
         this.alpha = var1;
      }

      public float getAlphaMax() {
         return this.alphaMax;
      }

      public void setAlphaMax(float var1) {
         this.alphaMax = var1;
      }

      public float getAlphaMin() {
         return this.alphaMin;
      }

      public void setAlphaMin(float var1) {
         this.alphaMin = var1;
      }

      public boolean isDoAlpha() {
         return this.doAlpha;
      }

      public void setDoAlpha(boolean var1) {
         this.doAlpha = var1;
      }

      public float getFadeSpeed() {
         return this.fadeSpeed;
      }

      public void setFadeSpeed(float var1) {
         this.fadeSpeed = var1;
      }

      public IsoGridSquare getSquare() {
         return this.square;
      }

      public void setSquare(IsoGridSquare var1) {
         this.square = var1;
      }

      public void setPos(int var1, int var2, int var3) {
         this.x = (float)var1 + 0.5F;
         this.y = (float)var2 + 0.5F;
         this.z = (float)var3;
      }

      public boolean isActive() {
         return this.active;
      }

      public void setActive(boolean var1) {
         this.active = var1;
      }
   }

   public static final class CircleIsoMarker {
      private int ID;
      private IsoGridSquare square;
      private float x;
      private float y;
      private float z;
      private float r;
      private float g;
      private float b;
      private float a;
      private float size;
      private boolean doAlpha;
      private float fadeSpeed = 0.006F;
      private float alpha = 0.0F;
      private float alphaMax = 1.0F;
      private float alphaMin = 0.3F;
      private boolean alphaInc = true;
      private boolean active = true;
      private boolean isRemoved = false;

      public CircleIsoMarker() {
         this.ID = IsoMarkers.NextCircleIsoMarkerID++;
      }

      public int getID() {
         return this.ID;
      }

      public void remove() {
         this.isRemoved = true;
      }

      public boolean isRemoved() {
         return this.isRemoved;
      }

      public void init(int var1, int var2, int var3, IsoGridSquare var4) {
         this.square = var4;
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }

      public float getZ() {
         return this.z;
      }

      public float getR() {
         return this.r;
      }

      public float getG() {
         return this.g;
      }

      public float getB() {
         return this.b;
      }

      public float getA() {
         return this.a;
      }

      public void setR(float var1) {
         this.r = var1;
      }

      public void setG(float var1) {
         this.g = var1;
      }

      public void setB(float var1) {
         this.b = var1;
      }

      public void setA(float var1) {
         this.a = var1;
      }

      public float getSize() {
         return this.size;
      }

      public void setSize(float var1) {
         this.size = var1;
      }

      public float getAlpha() {
         return this.alpha;
      }

      public void setAlpha(float var1) {
         this.alpha = var1;
      }

      public float getAlphaMax() {
         return this.alphaMax;
      }

      public void setAlphaMax(float var1) {
         this.alphaMax = var1;
      }

      public float getAlphaMin() {
         return this.alphaMin;
      }

      public void setAlphaMin(float var1) {
         this.alphaMin = var1;
      }

      public boolean isDoAlpha() {
         return this.doAlpha;
      }

      public void setDoAlpha(boolean var1) {
         this.doAlpha = var1;
      }

      public float getFadeSpeed() {
         return this.fadeSpeed;
      }

      public void setFadeSpeed(float var1) {
         this.fadeSpeed = var1;
      }

      public IsoGridSquare getSquare() {
         return this.square;
      }

      public void setSquare(IsoGridSquare var1) {
         this.square = var1;
      }

      public void setPos(int var1, int var2, int var3) {
         this.x = (float)var1;
         this.y = (float)var2;
         this.z = (float)var3;
      }

      public boolean isActive() {
         return this.active;
      }

      public void setActive(boolean var1) {
         this.active = var1;
      }
   }
}
