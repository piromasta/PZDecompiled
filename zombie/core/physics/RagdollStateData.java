package zombie.core.physics;

import zombie.iso.Vector2;
import zombie.iso.Vector3;

public class RagdollStateData {
   public float simulationTimeout;
   public boolean isSimulating = false;
   public boolean isSimulationMovement = false;
   public boolean isCalculated = false;
   public final Vector2 simulationDirection = new Vector2();
   public final Vector3 pelvisDirection = new Vector3();

   public RagdollStateData() {
      this.reset();
   }

   public void reset() {
      this.simulationTimeout = 1.5F;
      this.isSimulating = false;
      this.isSimulationMovement = false;
      this.isCalculated = false;
      this.simulationDirection.set(0.0F, 0.0F);
      this.pelvisDirection.set(0.0F, 0.0F, 0.0F);
   }
}
