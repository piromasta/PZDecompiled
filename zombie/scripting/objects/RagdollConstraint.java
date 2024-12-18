package zombie.scripting.objects;

import zombie.iso.Vector3;

public class RagdollConstraint {
   public int joint;
   public int constraintType;
   public int constraintPartA;
   public int constraintPartB;
   public Vector3 constraintAxisA = new Vector3();
   public Vector3 constraintAxisB = new Vector3();
   public Vector3 constraintPositionOffsetA = new Vector3();
   public Vector3 constraintPositionOffsetB = new Vector3();
   public Vector3 constraintLimit = new Vector3();
   public Vector3 constraintLimitExtended = new Vector3();
   public Vector3 defaultConstraintAxisA = new Vector3();
   public Vector3 defaultConstraintAxisB = new Vector3();
   public Vector3 defaultConstraintPositionOffsetA = new Vector3();
   public Vector3 defaultConstraintPositionOffsetB = new Vector3();
   public Vector3 defaultConstraintLimit = new Vector3();
   public Vector3 defaultConstraintLimitExtended = new Vector3();

   public RagdollConstraint() {
   }
}
