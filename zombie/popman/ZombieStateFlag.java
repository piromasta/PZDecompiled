package zombie.popman;

public enum ZombieStateFlag {
   Initialized(1),
   Crawling(2),
   CanWalk(4),
   FakeDead(8),
   CanCrawlUnderVehicle(16),
   ReanimatedForGrappleOnly(32);

   public final int Flag;

   private ZombieStateFlag(int var3) {
      this.Flag = var3;
   }
}
