package zombie.network.anticheats;

import java.util.HashMap;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

public class AntiCheatNoClip extends AbstractAntiCheat {
   private static final Vector3 sourcePoint = new Vector3();
   private static final Vector3 targetPoint = new Vector3();
   private static final Vector2 direction2 = new Vector2();
   private static final Vector3 direction3 = new Vector3();
   private static final HashMap<Short, Long> teleports = new HashMap();
   private static IsoGridSquare sourceSquare;
   private static IsoGridSquare targetSquare;

   public AntiCheatNoClip() {
   }

   public static void teleport(IsoPlayer var0) {
      teleports.put(var0.getOnlineID(), System.currentTimeMillis());
   }

   public void react(UdpConnection var1, INetworkPacket var2) {
      super.react(var1, var2);
      IAntiCheat var3 = (IAntiCheat)var2;
      IsoPlayer var4 = var1.players[var3.getPlayerIndex()];
      if ((!var1.role.haveCapability(Capability.ToggleNoclipHimself) || !var4.isNoClip()) && (!var1.role.haveCapability(Capability.UseFastMoveCheat) || !var4.isFastMoveCheat())) {
         sourcePoint.set(var1.ReleventPos[var3.getPlayerIndex()]);
         GameServer.sendTeleport(var4, sourcePoint.x, sourcePoint.y, sourcePoint.z);
         teleports.remove(var4.getOnlineID());
      }
   }

   public String validate(UdpConnection var1, INetworkPacket var2) {
      String var3 = super.validate(var1, var2);
      IAntiCheat var4 = (IAntiCheat)var2;
      IsoPlayer var5 = var1.players[var4.getPlayerIndex()];
      long var6 = (Long)teleports.getOrDefault(var5.getOnlineID(), 0L);
      if ((!var1.role.haveCapability(Capability.ToggleNoclipHimself) || !var5.isNoClip()) && (!var1.role.haveCapability(Capability.UseFastMoveCheat) || !var5.isFastMoveCheat())) {
         if (var5.getVehicle() != null) {
            return var3;
         } else if (System.currentTimeMillis() - var6 <= 500L) {
            return var3;
         } else {
            teleports.remove(var5.getOnlineID());
            sourcePoint.set(var1.ReleventPos[var4.getPlayerIndex()]);
            sourceSquare = ServerMap.getGridSquare(sourcePoint);
            var4.getPosition(targetPoint);
            targetSquare = ServerMap.getGridSquare(targetPoint);
            if (sourceSquare != null && targetSquare != null && sourceSquare != targetSquare) {
               direction2.set((float)(targetSquare.x - sourceSquare.x), (float)(targetSquare.y - sourceSquare.y));
               IsoDirections var8 = IsoDirections.fromAngle(direction2);
               direction3.set((float)(targetSquare.x - sourceSquare.x), (float)(targetSquare.y - sourceSquare.y), (float)(targetSquare.z - sourceSquare.z));
               IsoGridSquare var9 = ServerMap.instance.getGridSquare(targetSquare.x, targetSquare.y, sourceSquare.z);
               if (direction2.getLength() > 1.5F) {
                  var3 = String.format("Long blocked (%d,%d,%d) => (%d,%d,%d) len=%f", sourceSquare.x, sourceSquare.y, sourceSquare.z, targetSquare.x, targetSquare.y, targetSquare.z, direction3.getLength());
               } else if (1.0F < direction2.getLength() && direction2.getLength() < 1.5F) {
                  IsoGridSquare var13 = ServerMap.instance.getGridSquare(sourceSquare.x, targetSquare.y, targetSquare.z);
                  IsoGridSquare var11 = ServerMap.instance.getGridSquare(targetSquare.x, sourceSquare.y, targetSquare.z);
                  boolean var12 = checkPathClamp(sourceSquare, var13) && checkPathClamp(var13, targetSquare) || checkPathClamp(sourceSquare, var11) && checkPathClamp(var11, targetSquare);
                  if (!var12) {
                     var3 = String.format("Diagonal blocked (%d,%d,%d) => (%d,%d,%d) len=%f", sourceSquare.x, sourceSquare.y, sourceSquare.z, targetSquare.x, targetSquare.y, targetSquare.z, direction3.getLength());
                  }
               } else {
                  boolean var10;
                  if (direction3.getLength() > 1.0F && sourceSquare.z < 0 && targetSquare.z == 0 && !var9.TreatAsSolidFloor() && !sourceSquare.HasStairs()) {
                     var10 = checkPathClamp(sourceSquare, var9);
                     if (!var10) {
                        var3 = String.format("Basement blocked (%d,%d,%d) => (%d,%d,%d) len=%f", sourceSquare.x, sourceSquare.y, sourceSquare.z, targetSquare.x, targetSquare.y, targetSquare.z, direction3.getLength());
                     }
                  } else {
                     var10 = checkPathClamp(sourceSquare, targetSquare);
                     if (var10) {
                        var10 = sourceSquare.z > targetSquare.z || checkReachablePath(var8, var5);
                        if (!var10) {
                           var3 = String.format("Reachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f", sourceSquare.x, sourceSquare.y, sourceSquare.z, targetSquare.x, targetSquare.y, targetSquare.z, direction3.getLength());
                        }
                     } else {
                        var10 = checkUnreachablePath(var8, var5);
                        if (!var10) {
                           var3 = String.format("Unreachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f", sourceSquare.x, sourceSquare.y, sourceSquare.z, targetSquare.x, targetSquare.y, targetSquare.z, direction3.getLength());
                        }
                     }
                  }
               }
            }

            return var3;
         }
      } else {
         return var3;
      }
   }

   private static boolean checkPathClamp(IsoGridSquare var0, IsoGridSquare var1) {
      boolean var2 = !var0.getPathMatrix(PZMath.clamp(var1.x - var0.x, -1, 1), PZMath.clamp(var1.y - var0.y, -1, 1), PZMath.clamp(var1.z - var0.z, -1, 1));
      boolean var3 = !var1.getPathMatrix(PZMath.clamp(var0.x - var1.x, -1, 1), PZMath.clamp(var0.y - var1.y, -1, 1), PZMath.clamp(var0.z - var1.z, -1, 1));
      return var2 || var3 && var1.z == var0.z;
   }

   private static boolean isClose(IsoGridSquare var0, IsoGridSquare var1) {
      return (-1 <= var1.x - var0.x || var1.x - var0.x <= 1) && (-1 <= var1.y - var0.y || var1.y - var0.y <= 1);
   }

   private static boolean checkUnreachablePath(IsoDirections var0, IsoPlayer var1) {
      IsoObject var2 = null;
      boolean var3 = false;
      if (IsoDirections.N.equals(var0)) {
         var2 = sourceSquare.getWall(true);
         var3 = sourceSquare.Is(IsoFlagType.HoppableN);
      } else if (IsoDirections.W.equals(var0)) {
         var2 = sourceSquare.getWall(false);
         var3 = sourceSquare.Is(IsoFlagType.HoppableW);
      } else if (IsoDirections.S.equals(var0)) {
         var2 = targetSquare.getWall(true);
         var3 = targetSquare.Is(IsoFlagType.HoppableN);
      } else if (IsoDirections.E.equals(var0)) {
         var2 = targetSquare.getWall(false);
         var3 = targetSquare.Is(IsoFlagType.HoppableW);
      }

      if (var2 != null) {
         if (var2.isHoppable() || var1.canClimbOverWall(var0) || var3) {
            return true;
         }

         if (var2 instanceof IsoThumpable) {
            IsoThumpable var7 = (IsoThumpable)var2;
            return var7.canClimbThrough(var1) || var7.canClimbOver(var1) || var7.IsOpen() || var7.couldBeOpen(var1);
         }

         if (var2 instanceof IsoDoor) {
            IsoDoor var5 = (IsoDoor)var2;
            return var5.IsOpen() || var5.canClimbOver(var1) || var5.couldBeOpen(var1);
         }

         if (var2 instanceof IsoWindow) {
            IsoWindow var6 = (IsoWindow)var2;
            return var6.canClimbThrough(var1);
         }
      }

      boolean var4 = isClose(sourceSquare, targetSquare);
      if (var4) {
         if (targetSquare.z < sourceSquare.z) {
            return true;
         }

         if (sourceSquare.haveSheetRope || targetSquare.haveSheetRope) {
            return true;
         }

         if (sourceSquare.HasStairs() || targetSquare.HasStairs()) {
            return true;
         }

         if (sourceSquare.getProperties().Is(IsoFlagType.burntOut) || targetSquare.getProperties().Is(IsoFlagType.burntOut)) {
            return true;
         }
      }

      return false;
   }

   private static boolean checkReachablePath(IsoDirections var0, IsoPlayer var1) {
      IsoObject var2 = null;
      if (IsoDirections.N.equals(var0)) {
         var2 = sourceSquare.getDoorOrWindow(true);
      } else if (IsoDirections.W.equals(var0)) {
         var2 = sourceSquare.getDoorOrWindow(false);
      } else if (IsoDirections.S.equals(var0)) {
         var2 = targetSquare.getDoorOrWindow(true);
      } else if (IsoDirections.E.equals(var0)) {
         var2 = targetSquare.getDoorOrWindow(false);
      }

      if (var2 instanceof IsoThumpable var3) {
         return var3.canClimbThrough(var1) || var3.canClimbOver(var1) || var3.IsOpen() || var3.couldBeOpen(var1);
      } else if (!(var2 instanceof IsoDoor var4)) {
         if (var2 instanceof IsoWindow var5) {
            return var5.canClimbThrough(var1);
         } else {
            return true;
         }
      } else {
         return var4.IsOpen() || var4.canClimbOver(var1) || var4.couldBeOpen(var1);
      }
   }

   public interface IAntiCheat {
      byte getPlayerIndex();

      Vector3 getPosition(Vector3 var1);
   }
}
