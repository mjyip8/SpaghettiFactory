package cs348c.particles;

/**
 * Default constants.
 * 
 * @author Doug James, February 2009
 */
public interface Constants
{
    /** Mass-proportional damping. */
    public static final double DAMPING_MASS      = 0; 

    /** Mass of a particle. */
    public static final double PARTICLE_MASS     = 1.0;

    /** Spring stretching stiffness. */
    public static final double STIFFNESS_STRETCH = 10000.0; 

    /** Spring bending stiffness. */
    public static final double STIFFNESS_BEND    = 4.; 
    
    public static final double PENALTY_STIFFNESS = 14;
    
    public static final double PENALTY_DAMPING = .1;

}
