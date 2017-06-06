package guru.syzygy.armatron;

/**
 * Created by discar on 5/17/2017.
 *
 * This class is the IK representation of the arm
 *
 */

import java.util.ArrayList;
import java.util.List;

import au.edu.federation.caliko.FabrikBone2D;
import au.edu.federation.caliko.FabrikChain2D;
import au.edu.federation.caliko.FabrikStructure2D;

import au.edu.federation.utils.Vec2f;

@SuppressWarnings("all")

/**
 * The ArmModel represents the robotic arm.  This model uses extensive use of the Caliko project's
 * Inverse Kinematic library in order to do the IK calculations (see https://github.com/feduni/caliko)
 * But be aware that although programmatically the Caliko library allows very complex structures,
 * I have found that under certain conditions, the caliko library could snap through a constraint
 * giving a very distrurbing solution.
 *
 * For this reason, the AL5D model is simplified to only have two bones (and the gripper/rotator/wrist
 * is contolled via another mechanism).  This reduces the
 * likelihood of finding a fake solution and causing undue stress on the AL5D hardware.
 */

public class ArmModel {
    FabrikStructure2D mStructure;   // Holds the structure
    FabrikChain2D chain;            // Holds the chain
    ArrayList<LocalBone> bones = new ArrayList<LocalBone>();  // Holds our bones (as distinguished from the Fabrik bones)

    /**
     * Initialize the model
     */
    public ArmModel() {
        mStructure = new FabrikStructure2D("Armatron");
        chain = new FabrikChain2D();
        mStructure.addChain(chain);
    }

    /**
     * Return the local bone representation of this model
     *
     * @return
     */
    public ArrayList<LocalBone> getBones() {
        return bones;
    }

    /**
     * Add a bone.  This bone will start at the end of the last bone or 0,0 if it is the first bone.
     *
     * @param boneName
     * @param length
     */
    public LocalBone addBone(String boneName, double length, double startAngle, double constraintAngleCW, double constraintAngleCCW) {
        // Last endpoint
        Vec2f lastpoint = null;
        double x = 0.0;
        double y = 0.0;

        if (bones.size() > 0) {
            // This is a normal bone
            lastpoint = bones.get(bones.size()-1).getBone().getEndLocation();
            x = lastpoint.x;
            y = lastpoint.y;

            Vec2f uv = Util.calcUV(startAngle);
            System.out.println("::: addConsecutiveConstrainedBone(new Vec2f("+uv.toString()+"), "+length+", "+Math.abs(constraintAngleCW)+", "+Math.abs(constraintAngleCCW));
            chain.addConsecutiveConstrainedBone(uv, (float) length, (float) Math.abs(constraintAngleCW), (float) Math.abs(constraintAngleCCW));
        }  else {
            // This is the base bone
            float endX = (float) Util.round((Math.cos(Util.degToR(startAngle))*length),1);
            float endY = (float) Util.round(Math.sin(Util.degToR(startAngle))*length, 1);


            // Add the basebone to the chain
            System.out.println("::: new FabrikeBone2D(new Vec2f(0,0), new Vec2f("+endX+","+endY+")");
            FabrikBone2D basebone = new FabrikBone2D(new Vec2f(0f,0f), new Vec2f((float) endX, (float) endY));
            // FabrikBone2D basebone = new FabrikBone2D(new Vec2f(0,(float) -length), new Vec2f(0, 0));
            basebone.setClockwiseConstraintDegs( (float) Math.abs(constraintAngleCW));
            basebone.setAnticlockwiseConstraintDegs((float) Math.abs(constraintAngleCCW));
            chain.addBone(basebone);

            // Configure the chain
            chain.setFixedBaseMode(true);
            chain.setBaseboneConstraintType(FabrikChain2D.BaseboneConstraintType2D.GLOBAL_ABSOLUTE);
            chain.setBaseboneConstraintUV( new Vec2f(0.0f, 1.0f));
            chain.setEmbeddedTargetMode(false);
        }

        // Associate the fabrik bone with a new localbone
        FabrikBone2D newBone = chain.getBone(chain.getNumBones()-1);
        LocalBone b = new LocalBone(boneName, newBone, startAngle);
        bones.add(b);
        return b;
    }


    /**
     * Makes our model calculate a solution to reach the given objective.
     * The objective X and Y coordinates are ARM coordinates (e.g. relative
     * to the lengths of the arm in inches) NOT screen coordinates.
     *
     * @param objectiveX
     * @param objectiveY
     */
    public void solveForTarget(double objectiveX, double objectiveY) {

        mStructure.setFixedBaseMode(true);
        chain.setEmbeddedTargetMode(false);
        Vec2f objective = new Vec2f((float) objectiveX, (float) objectiveY);
        mStructure.solveForTarget(objective);


        // Update Chain... the Fabrik library apparently creates new
        // FabrikBone objects in the chain, so we need to update our
        // model's Fabrik Bone references to point to the new
        // Fabrik Bones
        List<FabrikBone2D> listOfBones = chain.getChain();
        for (FabrikBone2D f: listOfBones) {
            String name = f.getName();
            LocalBone b = findBone(name);
            b.setBone(f);
        }

    }

    /**
     * Returns the angle for the named bone.
     *
     * @param boneName
     * @return
     */
    public double getAngleForBone(String boneName) {
        LocalBone bone = findBone(boneName);
        if (bone != null) {
            // System.out.println("Found bone: "+bone.getName()+" / "+bone.getBone().getName());
            return bone.getAngleDeg();
        }
        return 0.0;
    }

    /**
     * Locates a bone by its name
     *
     * @param boneName
     * @return
     */
    public LocalBone findBone(String boneName) {
        for (LocalBone b: bones) {
            if (b.getName().equalsIgnoreCase(boneName)) return b;
        }
        return null;
    }

    /**
     * The total length (AKA the "reach") of the arm.
     * @return
     */
    public float getTotalLength() {
        return chain.getChainLength();
    }

    public FabrikChain2D getChain() {
        return chain;
    }

}

/**
 * LocalBone is a convenience class that wraps fabrik bones and provides a way to
 * maintain state, name the bone, and do some rudimentary calculations.
 */
@SuppressWarnings("All")
class LocalBone {
    String boneName;

    FabrikBone2D bone;

    double angleOffset = 0d;

    /**
     * Constructor for the LocalBone.
     *
     * @param boneName
     * @param bone
     * @param angleOffset - This is the initial angle for the bone
     */
    public LocalBone(String boneName, FabrikBone2D bone, double angleOffset) {
        this.boneName = boneName;

        this.bone = bone;
        this.bone.setName(boneName);
        this.angleOffset = angleOffset;
    }

    /**
     * Binds the given FabrikBone to this LocalBone
     *
     * @param bone
     */
    public void setBone(FabrikBone2D bone) {
        this.bone = bone;
    }


    /**
     * Set the starting angle of the bone
     *
     * @param n
     */
    public void setAngleOffset(double n) {
        angleOffset = n;
    }

    /**
     * Return the fabrik bone object attached to this bone
     *
     * @return
     */
    public FabrikBone2D getBone() {
        return bone;
    }

    /**
     * Every local bone is named.  Get the name.
     *
     * @return
     */
    public String getName() {
        return boneName;
    }

    /**
     * Return the current angle of this bone.  We do this by getting the starting
     * and ending points of the bone and calculating the angle between them.
     *
     * @return
     */
    public float getAngleDeg() {
        Vec2f startVec = bone.getStartLocation();
        Vec2f endVec = bone.getEndLocation();

        // If missing one of them, we default to zero
        if (startVec == null || endVec == null) return 0;

        // We can also alternatively use the unit vector of the bone.
        // I use the start/end points to keep all calculations in this program
        // consistent.
        Vec2f v = bone.getDirectionUV();

        // Calculae the dx/dy of the bone
        float dx = endVec.x - startVec.x;
        float dy = endVec.y - startVec.y;

        // And get the angle
        float angle = (float) Util.round(Util.RToDeg(Math.atan2( (double) dy, (double) dx)),1);

        return angle;
    }

    /**
     * Return the starting angle of the bone when it was created.
     * @return
     */
    public double getAngleOffset() {
        return angleOffset;
    }

}

