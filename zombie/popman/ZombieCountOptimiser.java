package zombie.popman;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.network.GameClient;
import zombie.network.MPStatistics;

public class ZombieCountOptimiser {
   private static int zombieCountForDelete = 0;
   public static final int maxZombieCount = 500;
   public static final int minZombieDistance = 20;
   public static final ArrayList<IsoZombie> zombiesForDelete = new ArrayList();

   public ZombieCountOptimiser() {
   }

   private static boolean isOutside(IsoZombie var0) {
      return var0.getCurrentSquare() == null || !var0.getCurrentSquare().isInARoom() && !var0.getCurrentSquare().haveRoof;
   }

   public static void startCount() {
      zombieCountForDelete = (int)(1.0F * (float)Math.max(0, GameClient.IDToZombieMap.values().length - SandboxOptions.instance.zombieConfig.ZombiesCountBeforeDeletion.getValue()));
   }

   public static void incrementZombie(IsoZombie var0) {
      if (zombieCountForDelete > 0 && Rand.Next(10) == 0 && var0.getTarget() == null && isOutside(var0) && var0.canBeDeletedUnnoticed(20.0F) && !var0.isReanimatedPlayer()) {
         synchronized(zombiesForDelete) {
            zombiesForDelete.add(var0);
         }

         --zombieCountForDelete;
         MPStatistics.clientZombieCulled();
      }

   }
}
