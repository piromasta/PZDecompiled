package zombie.characters;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.ai.ZombieGroupManager;
import zombie.core.random.Rand;
import zombie.iso.IsoUtils;

public final class ZombieGroup {
   private final ArrayList<IsoZombie> members = new ArrayList();
   public float lastSpreadOutTime;
   public float idealSizeFactor = 1.0F;
   int randomizer;
   boolean negativeValue;

   public ZombieGroup() {
      this.randomizer = Rand.Next(SandboxOptions.instance.zombieConfig.RallyGroupSizeVariance.getValue());
      this.negativeValue = Rand.NextBool(2);
   }

   public ZombieGroup reset() {
      this.members.clear();
      this.lastSpreadOutTime = -1.0F;
      this.idealSizeFactor = 1.0F;
      int var1 = Rand.Next(this.randomizer);
      if (Rand.NextBool(2)) {
         var1 *= -1;
      }

      this.idealSizeFactor += (float)var1 / 100.0F;
      if (this.idealSizeFactor < 0.01F) {
         this.idealSizeFactor = 0.01F;
      }

      return this;
   }

   public void add(IsoZombie var1) {
      if (!this.members.contains(var1)) {
         if (var1.group != null) {
            var1.group.remove(var1);
         }

         this.members.add(var1);
         var1.group = this;
      }
   }

   public void remove(IsoZombie var1) {
      this.members.remove(var1);
      var1.group = null;
   }

   public IsoZombie getLeader() {
      return this.members.isEmpty() ? null : (IsoZombie)this.members.get(0);
   }

   public boolean isEmpty() {
      return this.members.isEmpty();
   }

   public int size() {
      return this.members.size();
   }

   public void update() {
      int var1 = SandboxOptions.instance.zombieConfig.RallyTravelDistance.getValue();

      for(int var2 = 0; var2 < this.members.size(); ++var2) {
         IsoZombie var3 = (IsoZombie)this.members.get(var2);
         float var4 = 0.0F;
         if (var2 > 0) {
            var4 = IsoUtils.DistanceToSquared(((IsoZombie)this.members.get(0)).getX(), ((IsoZombie)this.members.get(0)).getY(), var3.getX(), var3.getY());
         }

         if (var3.group != this || var4 > (float)(var1 * var1) || !ZombieGroupManager.instance.shouldBeInGroup(var3)) {
            if (var3.group == this) {
               var3.group = null;
            }

            this.members.remove(var2--);
         }
      }

   }
}
