package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.objects.interfaces.Thumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class IsoWindowFrame extends IsoObject implements BarricadeAble {
   private boolean north = false;

   public IsoWindowFrame(IsoCell var1) {
      super(var1);
   }

   public IsoWindowFrame(IsoCell var1, IsoGridSquare var2, IsoSprite var3, boolean var4) {
      super(var1, var2, var3);
      this.north = var4;
   }

   public String getObjectName() {
      return "IsoWindowFrame";
   }

   public boolean haveSheetRope() {
      return IsoWindow.isTopOfSheetRopeHere(this.getSquare(), this.getNorth());
   }

   public int countAddSheetRope() {
      return countAddSheetRope(this);
   }

   public boolean canAddSheetRope() {
      return !this.canClimbThrough((IsoGameCharacter)null) ? false : canAddSheetRope(this);
   }

   public boolean addSheetRope(IsoPlayer var1, String var2) {
      return addSheetRope(this, var1, var2);
   }

   public boolean removeSheetRope(IsoPlayer var1) {
      return removeSheetRope(this, var1);
   }

   public Thumpable getThumpableFor(IsoGameCharacter var1) {
      IsoWindow var2 = this.getWindow();
      if (var2 != null) {
         return var2.getThumpableFor(var1);
      } else {
         IsoBarricade var3 = this.getBarricadeForCharacter(var1);
         if (var3 != null) {
            return var3;
         } else {
            var3 = this.getBarricadeOppositeCharacter(var1);
            return var3 != null ? var3 : null;
         }
      }
   }

   public boolean isBarricaded() {
      IsoBarricade var1 = this.getBarricadeOnSameSquare();
      if (var1 == null) {
         var1 = this.getBarricadeOnOppositeSquare();
      }

      return var1 != null;
   }

   public boolean isBarricadeAllowed() {
      return this.getWindow() == null;
   }

   public IsoBarricade getBarricadeOnSameSquare() {
      return this.hasWindow() ? null : IsoBarricade.GetBarricadeOnSquare(this.square, this.getNorth() ? IsoDirections.N : IsoDirections.W);
   }

   public IsoBarricade getBarricadeOnOppositeSquare() {
      return this.hasWindow() ? null : IsoBarricade.GetBarricadeOnSquare(this.getOppositeSquare(), this.getNorth() ? IsoDirections.S : IsoDirections.E);
   }

   public IsoBarricade getBarricadeForCharacter(IsoGameCharacter var1) {
      return this.hasWindow() ? null : IsoBarricade.GetBarricadeForCharacter(this, var1);
   }

   public IsoBarricade getBarricadeOppositeCharacter(IsoGameCharacter var1) {
      return this.hasWindow() ? null : IsoBarricade.GetBarricadeOppositeCharacter(this, var1);
   }

   public IsoGridSquare getOppositeSquare() {
      return this.getSquare() == null ? null : this.getSquare().getAdjacentSquare(this.getNorth() ? IsoDirections.N : IsoDirections.W);
   }

   public boolean getNorth() {
      return this.north;
   }

   public void save(ByteBuffer var1, boolean var2) throws IOException {
      super.save(var1, var2);
      var1.put((byte)(this.north ? 1 : 0));
   }

   public void load(ByteBuffer var1, int var2, boolean var3) throws IOException {
      super.load(var1, var2, var3);
      this.north = var1.get() == 1;
   }

   public IsoWindow getWindow() {
      return this.getSquare() == null ? null : this.getSquare().getWindow(this.getNorth());
   }

   public boolean hasWindow() {
      return this.getWindow() != null;
   }

   public boolean canClimbThrough(IsoGameCharacter var1) {
      return canClimbThrough(this, var1);
   }

   public IsoCurtain getCurtain() {
      return getCurtain(this);
   }

   public IsoCurtain HasCurtains() {
      return this.getCurtain();
   }

   public IsoGridSquare getAddSheetSquare(IsoGameCharacter var1) {
      return getAddSheetSquare(this, var1);
   }

   public void addSheet(IsoGameCharacter var1) {
      addSheet(this, var1);
   }

   private static Direction getDirection(IsoObject var0) {
      if (var0 instanceof IsoWindowFrame var1) {
         return var1.getNorth() ? IsoWindowFrame.Direction.NORTH : IsoWindowFrame.Direction.WEST;
      } else if (!(var0 instanceof IsoWindow) && !(var0 instanceof IsoThumpable)) {
         if (var0 != null && var0.getProperties() != null && var0.getObjectIndex() != -1) {
            if (var0.getProperties().Is(IsoFlagType.WindowN)) {
               return IsoWindowFrame.Direction.NORTH;
            } else {
               return var0.getProperties().Is(IsoFlagType.WindowW) ? IsoWindowFrame.Direction.WEST : IsoWindowFrame.Direction.INVALID;
            }
         } else {
            return IsoWindowFrame.Direction.INVALID;
         }
      } else {
         return IsoWindowFrame.Direction.INVALID;
      }
   }

   public static boolean isWindowFrame(IsoObject var0) {
      return getDirection(var0).isValid();
   }

   public static boolean isWindowFrame(IsoObject var0, boolean var1) {
      Direction var2 = getDirection(var0);
      return var1 && var2 == IsoWindowFrame.Direction.NORTH || !var1 && var2 == IsoWindowFrame.Direction.WEST;
   }

   public static int countAddSheetRope(IsoObject var0) {
      Direction var1 = getDirection(var0);
      return var1.isValid() ? IsoWindow.countAddSheetRope(var0.getSquare(), var1 == IsoWindowFrame.Direction.NORTH) : 0;
   }

   public static boolean canAddSheetRope(IsoObject var0) {
      Direction var1 = getDirection(var0);
      return var1.isValid() && IsoWindow.canAddSheetRope(var0.getSquare(), var1 == IsoWindowFrame.Direction.NORTH);
   }

   public static boolean haveSheetRope(IsoObject var0) {
      Direction var1 = getDirection(var0);
      return var1.isValid() && IsoWindow.isTopOfSheetRopeHere(var0.getSquare(), var1 == IsoWindowFrame.Direction.NORTH);
   }

   public static boolean addSheetRope(IsoObject var0, IsoPlayer var1, String var2) {
      return !canAddSheetRope(var0) ? false : IsoWindow.addSheetRope(var1, var0.getSquare(), getDirection(var0) == IsoWindowFrame.Direction.NORTH, var2);
   }

   public static boolean removeSheetRope(IsoObject var0, IsoPlayer var1) {
      return !haveSheetRope(var0) ? false : IsoWindow.removeSheetRope(var1, var0.getSquare(), getDirection(var0) == IsoWindowFrame.Direction.NORTH);
   }

   public static IsoGridSquare getOppositeSquare(IsoObject var0) {
      Direction var1 = getDirection(var0);
      if (!var1.isValid()) {
         return null;
      } else {
         boolean var2 = var1 == IsoWindowFrame.Direction.NORTH;
         return var0.getSquare().getAdjacentSquare(var2 ? IsoDirections.N : IsoDirections.W);
      }
   }

   public static IsoGridSquare getIndoorSquare(IsoObject var0) {
      Direction var1 = getDirection(var0);
      if (!var1.isValid()) {
         return null;
      } else {
         IsoGridSquare var2 = var0.getSquare();
         if (var2.getRoom() != null) {
            return var2;
         } else {
            IsoGridSquare var3 = getOppositeSquare(var0);
            return var3 != null && var3.getRoom() != null ? var3 : null;
         }
      }
   }

   public static IsoCurtain getCurtain(IsoObject var0) {
      Direction var1 = getDirection(var0);
      if (!var1.isValid()) {
         return null;
      } else {
         boolean var2 = var1 == IsoWindowFrame.Direction.NORTH;
         IsoCurtain var3 = var0.getSquare().getCurtain(var2 ? IsoObjectType.curtainN : IsoObjectType.curtainW);
         if (var3 != null) {
            return var3;
         } else {
            IsoGridSquare var4 = getOppositeSquare(var0);
            return var4 == null ? null : var4.getCurtain(var2 ? IsoObjectType.curtainS : IsoObjectType.curtainE);
         }
      }
   }

   public static IsoGridSquare getAddSheetSquare(IsoObject var0, IsoGameCharacter var1) {
      Direction var2 = getDirection(var0);
      if (!var2.isValid()) {
         return null;
      } else {
         boolean var3 = var2 == IsoWindowFrame.Direction.NORTH;
         if (var1 != null && var1.getCurrentSquare() != null) {
            IsoGridSquare var4 = var1.getCurrentSquare();
            IsoGridSquare var5 = var0.getSquare();
            if (var3) {
               if (var4.getY() < var5.getY()) {
                  return var5.getAdjacentSquare(IsoDirections.N);
               }
            } else if (var4.getX() < var5.getX()) {
               return var5.getAdjacentSquare(IsoDirections.W);
            }

            return var5;
         } else {
            return null;
         }
      }
   }

   public static void addSheet(IsoObject var0, IsoGameCharacter var1) {
      Direction var2 = getDirection(var0);
      if (var2.isValid()) {
         boolean var3 = var2 == IsoWindowFrame.Direction.NORTH;
         IsoGridSquare var4 = getIndoorSquare(var0);
         if (var4 == null) {
            var4 = var0.getSquare();
         }

         if (var1 != null) {
            var4 = getAddSheetSquare(var0, var1);
         }

         if (var4 != null) {
            IsoObjectType var5;
            if (var4 == var0.getSquare()) {
               var5 = var3 ? IsoObjectType.curtainN : IsoObjectType.curtainW;
            } else {
               var5 = var3 ? IsoObjectType.curtainS : IsoObjectType.curtainE;
            }

            if (var4.getCurtain(var5) == null) {
               int var6 = 16;
               if (var5 == IsoObjectType.curtainE) {
                  ++var6;
               }

               if (var5 == IsoObjectType.curtainS) {
                  var6 += 3;
               }

               if (var5 == IsoObjectType.curtainN) {
                  var6 += 2;
               }

               var6 += 4;
               IsoCurtain var7 = new IsoCurtain(var0.getCell(), var4, "fixtures_windows_curtains_01_" + var6, var3);
               var4.AddSpecialTileObject(var7);
               if (!GameClient.bClient) {
                  InventoryItem var8 = var1.getInventory().FindAndReturn("Sheet");
                  var1.getInventory().Remove(var8);
                  if (GameServer.bServer) {
                     GameServer.sendRemoveItemFromContainer(var1.getInventory(), var8);
                  }
               }

               if (GameServer.bServer) {
                  var7.transmitCompleteItemToClients();
               }

            }
         }
      }
   }

   public static boolean canClimbThrough(IsoObject var0, IsoGameCharacter var1) {
      Direction var2 = getDirection(var0);
      if (!var2.isValid()) {
         return false;
      } else if (var0.getSquare() == null) {
         return false;
      } else {
         IsoWindow var3 = var0.getSquare().getWindow(var2 == IsoWindowFrame.Direction.NORTH);
         if (var3 != null && var3.isBarricaded()) {
            return false;
         } else {
            if (var0 instanceof IsoWindowFrame) {
               IsoWindowFrame var4 = (IsoWindowFrame)var0;
               if (var4.isBarricaded()) {
                  return false;
               }
            }

            if (var1 != null) {
               IsoGridSquare var5 = var2 == IsoWindowFrame.Direction.NORTH ? var0.getSquare().nav[IsoDirections.N.index()] : var0.getSquare().nav[IsoDirections.W.index()];
               if (!IsoWindow.canClimbThroughHelper(var1, var0.getSquare(), var5, var2 == IsoWindowFrame.Direction.NORTH)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private static enum Direction {
      INVALID,
      NORTH,
      WEST;

      private Direction() {
      }

      public boolean isValid() {
         return this != INVALID;
      }
   }
}
