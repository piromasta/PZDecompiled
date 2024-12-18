package zombie.entity.components.spriteconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.world.ScriptsDictionary;

@DebugClassFields
public class SpriteConfig extends Component {
   private SpriteConfigScript configScript;
   private boolean wasLoadedAsMaster = false;
   private SpriteConfigManager.TileInfo tileInfo;
   private SpriteConfigManager.FaceInfo faceInfo;
   private SpriteConfigManager.ObjectInfo objectInfo;

   private SpriteConfig() {
      super(ComponentType.SpriteConfig);
   }

   protected void readFromScript(ComponentScript var1) {
      super.readFromScript(var1);
      this.configScript = (SpriteConfigScript)var1;
   }

   protected void onAddedToOwner() {
      this.initObjectInfo();
   }

   protected void onRemovedFromOwner() {
      this.resetObjectInfo();
   }

   private void initObjectInfo() {
      this.resetObjectInfo();
      if (this.getOwner() != null && this.getOwner() instanceof IsoObject) {
         if (this.configScript != null) {
            this.objectInfo = SpriteConfigManager.GetObjectInfo(this.configScript.getName());
            if (this.objectInfo != null) {
               IsoSprite var1 = ((IsoObject)this.getOwner()).sprite;
               if (var1 != null) {
                  this.faceInfo = this.objectInfo.getFaceForSprite(var1.name);
                  if (this.faceInfo != null) {
                     this.tileInfo = this.faceInfo.getTileInfoForSprite(var1.name);
                  }
               }
            }
         }

         if (!this.isValid()) {
            this.resetObjectInfo();
            String var10001 = this.objectInfo != null ? this.objectInfo.getName() : "null";
            DebugLog.General.warn("Invalid SpriteConfig object! scripted object = " + var10001);
         }

      }
   }

   private void resetObjectInfo() {
      this.objectInfo = null;
      this.faceInfo = null;
      this.tileInfo = null;
   }

   protected void reset() {
      super.reset();
      this.resetObjectInfo();
      this.wasLoadedAsMaster = false;
   }

   public SpriteConfigManager.TileInfo getTileInfo() {
      return this.tileInfo;
   }

   public SpriteConfigManager.FaceInfo getFaceInfo() {
      return this.faceInfo;
   }

   public SpriteConfigManager.ObjectInfo getObjectInfo() {
      return this.objectInfo;
   }

   public boolean isValid() {
      if (super.isValid() && this.objectInfo != null && this.faceInfo != null && this.tileInfo != null) {
         return this.getOwner() != null && this.getOwner() instanceof IsoObject;
      } else {
         return false;
      }
   }

   public boolean isCanRotate() {
      return this.isValid() ? this.objectInfo.canRotate() : false;
   }

   public boolean isValidMultiSquare() {
      return this.isValid() && this.faceInfo.isMultiSquare();
   }

   public boolean isMultiSquareMaster() {
      return this.isValid() && this.tileInfo.isMaster();
   }

   public boolean isMultiSquareSlave() {
      return this.isValid() && !this.tileInfo.isMaster();
   }

   public int getMasterOffsetX() {
      return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetX() : 0;
   }

   public int getMasterOffsetY() {
      return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetY() : 0;
   }

   public int getMasterOffsetZ() {
      return this.isValidMultiSquare() ? this.tileInfo.getMasterOffsetZ() : 0;
   }

   public IsoObject getMultiSquareMaster() {
      if (!this.isValid()) {
         return null;
      } else if (this.faceInfo.isMultiSquare() && !this.tileInfo.isMaster()) {
         IsoObject var7 = (IsoObject)this.getOwner();
         IsoGridSquare var8 = IsoWorld.instance.CurrentCell.getGridSquare((double)(var7.getX() + (float)this.tileInfo.getMasterOffsetX()), (double)(var7.getY() + (float)this.tileInfo.getMasterOffsetY()), (double)(var7.getZ() + (float)this.tileInfo.getMasterOffsetZ()));
         SpriteConfigManager.TileInfo var3 = this.faceInfo.getMasterTileInfo();
         if (var8 != null) {
            for(int var4 = 0; var4 < var8.getObjects().size(); ++var4) {
               IsoObject var5 = (IsoObject)var8.getObjects().get(var4);
               SpriteConfig var6 = var5.getSpriteConfig();
               if (var6 != null && var6.isMultiSquareMaster() && var3.verifyObject(var5)) {
                  return var5;
               }
            }
         }

         return null;
      } else {
         SpriteConfigManager.TileInfo var1 = this.faceInfo.getMasterTileInfo();
         IsoObject var2 = (IsoObject)this.getOwner();
         return var1.verifyObject(var2) ? var2 : null;
      }
   }

   public boolean isMultiSquareFullyLoaded() {
      return !this.isValidMultiSquare() ? false : this.findAllMultiSquareObjects((ArrayList)null);
   }

   public boolean getAllMultiSquareObjects(ArrayList<IsoObject> var1) {
      if (!this.isValid()) {
         return false;
      } else if (!this.faceInfo.isMultiSquare()) {
         if (var1 != null) {
            var1.add((IsoObject)this.getOwner());
         }

         return true;
      } else {
         return this.findAllMultiSquareObjects(var1);
      }
   }

   private boolean findAllMultiSquareObjects(ArrayList<IsoObject> var1) {
      if (!this.isValid()) {
         return false;
      } else if (!this.faceInfo.isMultiSquare()) {
         if (var1 != null) {
            var1.add((IsoObject)this.getOwner());
         }

         return true;
      } else {
         IsoObject var2 = (IsoObject)this.getOwner();
         int var3 = var2.getSquare().getX() - this.tileInfo.getX();
         int var4 = var2.getSquare().getY() - this.tileInfo.getY();
         int var5 = var2.getSquare().getZ() - this.tileInfo.getZ();
         int var8 = var5;

         for(int var9 = 0; var8 < var5 + this.faceInfo.getzLayers(); ++var9) {
            int var10 = var3;

            for(int var11 = 0; var10 < var3 + this.faceInfo.getWidth(); ++var11) {
               int var12 = var4;

               for(int var13 = 0; var12 < var4 + this.faceInfo.getHeight(); ++var13) {
                  SpriteConfigManager.TileInfo var6 = this.faceInfo.getTileInfo(var11, var13, var9);
                  if (!var6.isEmpty()) {
                     IsoGridSquare var7 = IsoWorld.instance.CurrentCell.getGridSquare(var10, var12, var8);
                     if (var7 == null) {
                        return false;
                     }

                     boolean var14 = false;

                     for(int var15 = 0; var15 < var7.getObjects().size(); ++var15) {
                        IsoObject var16 = (IsoObject)var7.getObjects().get(var15);
                        if (var6.verifyObject(var16) && var16.getSpriteConfig() != null) {
                           if (var1 != null) {
                              var1.add(var16);
                           }

                           var14 = true;
                        }
                     }

                     if (!var14) {
                        return false;
                     }
                  }

                  ++var12;
               }

               ++var10;
            }

            ++var8;
         }

         return true;
      }
   }

   public boolean isWasLoadedAsMaster() {
      return this.wasLoadedAsMaster;
   }

   protected boolean onReceivePacket(ByteBuffer var1, EntityPacketType var2, UdpConnection var3) throws IOException {
      switch (var2) {
         default:
            return false;
      }
   }

   protected void saveSyncData(ByteBuffer var1) throws IOException {
   }

   protected void loadSyncData(ByteBuffer var1) throws IOException {
   }

   protected void save(ByteBuffer var1) throws IOException {
      var1.put((byte)(this.configScript != null ? 1 : 0));
      if (this.configScript != null) {
         ScriptsDictionary.spriteConfigs.saveScript(var1, this.configScript);
         var1.putLong(this.configScript.getScriptVersion());
         var1.put((byte)(this.isMultiSquareMaster() ? 1 : 0));
      }

   }

   protected void load(ByteBuffer var1, int var2) throws IOException {
      if (var1.get() == 0) {
         DebugLog.General.error("Sprite config has no script saved.");
      } else {
         SpriteConfigScript var3 = (SpriteConfigScript)ScriptsDictionary.spriteConfigs.loadScript(var1, var2);
         long var4 = var1.getLong();
         this.wasLoadedAsMaster = var1.get() == 1;
         if (var3 != null) {
            this.readFromScript(var3);
            if (var3.getScriptVersion() != var4) {
            }
         } else {
            DebugLog.General.error("Could not load script for sprite config.");
         }

      }
   }
}
