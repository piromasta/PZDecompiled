package zombie.entity;

import zombie.characters.IsoPlayer;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class UsingPlayerUpdateSystem extends EngineSystem {
   EntityBucket isoEntities;

   public UsingPlayerUpdateSystem(int var1) {
      super(true, false, var1);
   }

   public void addedToEngine(Engine var1) {
      this.isoEntities = var1.getIsoObjectBucket();
   }

   public void removedFromEngine(Engine var1) {
   }

   public void update() {
      if (!GameClient.bClient) {
         ImmutableArray var1 = this.isoEntities.getEntities();

         for(int var3 = 0; var3 < var1.size(); ++var3) {
            GameEntity var2 = (GameEntity)var1.get(var3);
            if (var2.isValidEngineEntity()) {
               IsoPlayer var4 = var2.getUsingPlayer();
               if (var2.getUsingPlayer() != null && (var4.getX() < var2.getX() - 10.0F || var4.getX() > var2.getX() + 10.0F || var4.getY() < var2.getY() - 10.0F || var4.getY() > var2.getY() + 10.0F || var4.getZ() != var2.getZ() || var4.isDead())) {
                  var2.setUsingPlayer((IsoPlayer)null);
               }
            }
         }

      }
   }
}
