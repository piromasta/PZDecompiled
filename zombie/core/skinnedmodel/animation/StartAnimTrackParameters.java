package zombie.core.skinnedmodel.animation;

import java.util.List;
import zombie.core.skinnedmodel.advancedanimation.AnimBoneWeight;
import zombie.util.Pool;
import zombie.util.PooledObject;

public class StartAnimTrackParameters extends PooledObject {
   public String animName;
   public int priority;
   public boolean isPrimary;
   public boolean isRagdoll;
   public float ragdollStartTime;
   public float ragdollMaxTime;
   public List<AnimBoneWeight> subLayerBoneWeights;
   public boolean syncTrackingEnabled;
   public boolean aimingIKLeftArm;
   public boolean aimingIKRightArm;
   public float speedScale;
   public float initialWeight;
   public boolean isLooped;
   public boolean isReversed;
   public String deferredBoneName;
   public BoneAxis deferredBoneAxis;
   public boolean useDeferredMovement;
   public boolean useDeferredRotation;
   public float deferredRotationScale;
   public String matchingGrappledAnimNode;
   private static final Pool<StartAnimTrackParameters> s_pool = new Pool(StartAnimTrackParameters::new);

   private void reset() {
      this.animName = null;
      this.priority = 0;
      this.isPrimary = false;
      this.isRagdoll = false;
      this.ragdollStartTime = -1.0F;
      this.ragdollMaxTime = -1.0F;
      this.subLayerBoneWeights = null;
      this.syncTrackingEnabled = false;
      this.aimingIKLeftArm = false;
      this.aimingIKRightArm = false;
      this.speedScale = 1.0F;
      this.initialWeight = 0.0F;
      this.isLooped = false;
      this.isReversed = false;
      this.deferredBoneName = null;
      this.deferredBoneAxis = BoneAxis.Y;
      this.useDeferredMovement = true;
      this.useDeferredRotation = false;
      this.deferredRotationScale = 1.0F;
      this.matchingGrappledAnimNode = "";
   }

   public void onReleased() {
      this.reset();
   }

   protected StartAnimTrackParameters() {
   }

   public static StartAnimTrackParameters alloc() {
      return (StartAnimTrackParameters)s_pool.alloc();
   }
}
