package zombie.entity;

public abstract class EngineSystem {
   private final int updatePriority;
   private final int renderLastPriority;
   private boolean updater;
   private boolean simulationUpdater;
   private boolean renderer;
   private boolean enabled;
   private Engine engine;
   MembershipListener membershipListener;

   public EngineSystem() {
      this(false, false, 2147483647, false, 2147483647);
   }

   public EngineSystem(boolean var1, boolean var2, int var3) {
      this(var1, var2, var3, false, 2147483647);
   }

   public EngineSystem(boolean var1, boolean var2, int var3, boolean var4, int var5) {
      this.enabled = true;
      this.updater = var1;
      this.simulationUpdater = var2;
      this.updatePriority = var3;
      this.renderer = var4;
      this.renderLastPriority = var5;
   }

   public final boolean isEnabled() {
      return this.enabled;
   }

   public final void setEnabled(boolean var1) {
      if (this.enabled != var1) {
         this.enabled = var1;
         if (this.membershipListener != null) {
            this.membershipListener.onMembershipPropertyChanged(this);
         }
      }

   }

   public final void setUpdater(boolean var1) {
      if (this.updater != var1) {
         this.updater = var1;
         if (this.membershipListener != null) {
            this.membershipListener.onMembershipPropertyChanged(this);
         }
      }

   }

   public final void setSimulationUpdater(boolean var1) {
      if (this.simulationUpdater != var1) {
         this.simulationUpdater = var1;
         if (this.membershipListener != null) {
            this.membershipListener.onMembershipPropertyChanged(this);
         }
      }

   }

   public final void setRenderer(boolean var1) {
      if (this.renderer != var1) {
         this.renderer = var1;
         if (this.membershipListener != null) {
            this.membershipListener.onMembershipPropertyChanged(this);
         }
      }

   }

   public final Engine getEngine() {
      return this.engine;
   }

   final void addedToEngineInternal(Engine var1) {
      this.engine = var1;
      this.addedToEngine(var1);
   }

   final void removedFromEngineInternal(Engine var1) {
      this.engine = null;
      this.removedFromEngine(var1);
   }

   public void addedToEngine(Engine var1) {
   }

   public void removedFromEngine(Engine var1) {
   }

   public final int getUpdatePriority() {
      return this.updatePriority;
   }

   public final boolean isUpdater() {
      return this.updater;
   }

   public void update() {
   }

   public final int getUpdateSimulationPriority() {
      return this.updatePriority;
   }

   public final boolean isSimulationUpdater() {
      return this.simulationUpdater;
   }

   public void updateSimulation() {
   }

   public final int getRenderLastPriority() {
      return this.renderLastPriority;
   }

   public final boolean isRenderer() {
      return this.renderer;
   }

   public void renderLast() {
   }

   interface MembershipListener {
      void onMembershipPropertyChanged(EngineSystem var1);
   }
}
